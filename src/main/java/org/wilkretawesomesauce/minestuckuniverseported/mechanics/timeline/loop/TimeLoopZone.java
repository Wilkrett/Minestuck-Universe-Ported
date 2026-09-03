package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.loop;

import net.minecraft.world.phys.Vec3;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.DoomedTimelineClone;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.EntitySnapshot;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.WorldTickSnapshot;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A radius-scoped, repeating rewind - the "TimeLoop" ability. Captures a window of already-recorded
 * history <b>once</b>, non-destructively (see {@code abilitech.heroAspect.time.TechTimeLoopAlpha}, same
 * copy-not-pop pattern {@code TechRetrocognition} already uses for {@code timeline.vision.PastVisionSession}), then
 * replays that same window forward on repeat for {@link #totalDurationTicks}, resetting back to the
 * window's start at the beginning of every pass - see {@code timeline.loop.TimeLoopPlayback} for the
 * actual per-tick reset/replay logic this class is just the state for.
 * <p>
 * <b>Every pass's own "reset" is a real {@link #reverseTicks}-tick walk backward, not an instant snap</b> -
 * user-requested: an instant teleport read as "resetting", not "rewinding". {@link #isReversing()}/
 * {@link #reverseTick()}/{@link #isReverseStart()} expose that lead-in phase to
 * {@code timeline.loop.TimeLoopPlayback}, which drives {@code timeline.loop.TimeLoopReplay#reverseStep}
 * every reversing tick - the same real per-tick mover {@code skills.abilitech.heroAspect.time.TechTimeLoopBeta}'s
 * own dedicated tick driver uses for the real dying player, who isn't part of {@link #puppetedEntityIds}
 * (see this class's own "real players... never puppeted" note below) and so needs a separate driver walking
 * the identical shared math ({@code mechanics.timeline.RewindVisuals#sampleReversePath}).
 * <p>
 * <b>Real players inside the zone are never puppeted or reset</b> - only {@link #puppetedEntityIds}
 * (living, non-player entities that appeared in the window within {@link #radius} at least once, fixed
 * for the zone's whole lifetime to avoid boundary flicker) are moved by the loop. A player acting
 * differently than their own recorded past self inside a loop is a real possibility this doesn't detect
 * or turn into a paradox - stated plainly as a known limitation, not attempted here.
 * <p>
 * Not an {@code INBTSerializable} attachment or persisted at all - transient, like the other "currently
 * active effect" entries on {@code mechanics.timeline.TimelineData} ({@code ActiveRewind}, {@code DoomedTimelineClone},
 * {@code timeline.vision.PastVisionSession}). A 30-60 second effect has no reason to survive a restart.
 * <p>
 * <b>The caster themselves is puppeted by a dedicated, repeating {@link DoomedTimelineClone}</b>
 * ({@link #clone}, built by {@code timeline.loop.TimeLoopCaster}), not by {@link #puppetedEntityIds} -
 * a real connected {@code ServerPlayer} should never be teleported/reset directly (same reasoning
 * {@code mechanics.timeline.TimelineManager#applySnapshot} already documents), so the caster's own id is excluded
 * from {@link #puppetedEntityIds} and a separate fake-player ghost mimics their recorded path instead,
 * reset to the window's start every pass exactly like the zone itself.
 */
public final class TimeLoopZone
{
	/**
	 * Every Time Loop tunable lives here now, as plain constants, not in {@code Config} - user-requested:
	 * these are ability-specific numbers owned by the abilities/subsystem that actually use them, not
	 * server-wide gameplay knobs, so they belong with the code that reads them rather than scattered into a
	 * global config file. {@code skills.abilitech.heroAspect.time.TechTimeLoopOmega}'s own {@code REWIND_TICKS}
	 * local constant was already doing this correctly before this pass - these are the same idea, just for
	 * the values genuinely shared across Alpha/Omega/Beta instead of one tech's own.
	 */
	public static final int MAX_DURATION_TICKS = 600; // 30 seconds - the cap TechTimeLoopAlpha's own charge time clamps against
	public static final int DEFAULT_WINDOW_TICKS = 100; // 5 seconds - the replay window Alpha/Omega use when they don't request an explicit one
	public static final double RADIUS = 15.0; // blocks
	/** Baseline (1.0x speed) reverse-walk duration - see {@link #REVERSE_SPEED}'s own comment for the actual multiplier applied on top. */
	public static final int REVERSE_TICKS = 20; // 1 second at 1.0x
	/** Standard playback-speed semantics, not a tick count: 1.0 = {@link #REVERSE_TICKS} as-is, 0.25 (the user-requested default) = quarter speed, i.e. 4x longer/slower. */
	public static final double REVERSE_SPEED = 0.25;
	/** {@code REVERSE_TICKS / REVERSE_SPEED}, computed once here rather than at every call site - the actually-used reverse-walk duration. */
	public static final int REVERSE_TICKS_EFFECTIVE = Math.max(1, Math.round(REVERSE_TICKS / (float) REVERSE_SPEED));

	public enum StackMode
	{
		/** Any number of zones can freely overlap with zero coordination - see {@code TechTimeLoopAlpha}. */
		INDEPENDENT,
		/** Casting within range of an existing NESTED zone attaches as its child instead of starting an unrelated zone - see {@code TechTimeLoopBeta}. */
		NESTED
	}

	private final UUID id = UUID.randomUUID();
	private final Vec3 center;
	private final double radius;
	private final List<WorldTickSnapshot> window;
	private final int totalDurationTicks;
	private final int reverseTicks;
	private final StackMode stackMode;
	@Nullable
	private final UUID parentZoneId;
	private final Set<UUID> puppetedEntityIds;
	private final UUID creatorId;
	@Nullable
	private final DoomedTimelineClone clone;

	private int elapsedTicks = 0;
	/** Only ticks where the zone is actually forward-replaying count against {@link #totalDurationTicks} - see {@link #isDone()}'s own doc comment for why a reversing lead-in doesn't eat into that budget. */
	private int forwardTicksElapsed = 0;

	public TimeLoopZone(Vec3 center, double radius, List<WorldTickSnapshot> window, int totalDurationTicks, int reverseTicks,
			StackMode stackMode, @Nullable UUID parentZoneId, UUID creatorId, @Nullable DoomedTimelineClone clone)
	{
		this.center = center;
		this.radius = radius;
		this.window = window;
		this.totalDurationTicks = totalDurationTicks;
		// A zero-or-negative window has nothing to reverse-walk through in the first place - clamp so
		// isReversing()/currentPassIndex() below can't divide the cycle by a non-positive length.
		this.reverseTicks = window.isEmpty() ? 0 : Math.max(0, reverseTicks);
		this.stackMode = stackMode;
		this.parentZoneId = parentZoneId;
		this.creatorId = creatorId;
		this.clone = clone;
		this.puppetedEntityIds = findPuppetedEntities(window, center, radius, creatorId);
	}

	/** Every living entity is a candidate here, {@code creatorId} excluded - see this class's own doc comment for why the caster gets a dedicated {@link #clone} instead. Other real players who happen to be in the window are filtered out separately, at replay time, in {@code timeline.loop.TimeLoopReplay} (no entity-type information is available from a snapshot alone). */
	private static Set<UUID> findPuppetedEntities(List<WorldTickSnapshot> window, Vec3 center, double radius, UUID creatorId)
	{
		Set<UUID> ids = new HashSet<>();
		double radiusSqr = radius * radius;
		for(WorldTickSnapshot step : window)
			for(var entry : step.entitySnapshots().entrySet())
			{
				if(entry.getKey().equals(creatorId))
					continue;
				EntitySnapshot snapshot = entry.getValue();
				if(snapshot.pos().distanceToSqr(center) <= radiusSqr)
					ids.add(entry.getKey());
			}
		return ids;
	}

	public UUID getId()
	{
		return id;
	}

	public Vec3 getCenter()
	{
		return center;
	}

	public double getRadius()
	{
		return radius;
	}

	public List<WorldTickSnapshot> getWindow()
	{
		return window;
	}

	public int getWindowLength()
	{
		return window.size();
	}

	/** How many ticks every pass's real reverse-walk lead-in takes - see this class's own doc comment. */
	public int getReverseTicks()
	{
		return reverseTicks;
	}

	public StackMode getStackMode()
	{
		return stackMode;
	}

	@Nullable
	public UUID getParentZoneId()
	{
		return parentZoneId;
	}

	public Set<UUID> getPuppetedEntityIds()
	{
		return puppetedEntityIds;
	}

	public UUID getCreatorId()
	{
		return creatorId;
	}

	/** The repeating ghost that mimics the caster's own recorded path - null if the caster had no recorded presence in the captured window (e.g. only just started being tracked). */
	@Nullable
	public DoomedTimelineClone getClone()
	{
		return clone;
	}

	/**
	 * Checks {@link #forwardTicksElapsed}, not raw {@link #elapsedTicks} - a real, user-requested change:
	 * every pass now spends {@link #reverseTicks} ticks actually walking puppeted entities (and, for
	 * Timeloop &beta; specifically, the real dying player - see
	 * {@code skills.abilitech.heroAspect.time.TechTimeLoopBeta}'s own tick driver) backward through their
	 * real recorded path before forward replay begins, instead of instantly snapping to the window's start.
	 * If that reversing time counted against {@link #totalDurationTicks} the same way forward-replay ticks
	 * do, a zone whose duration exactly equals one pass (Timeloop &beta;'s own real shape) would end
	 * partway through its first forward pass, cut short by however long reversing took - checking only the
	 * forward-replay ticks keeps the original "duration = how long forward replay actually plays" contract
	 * intact regardless of how the reversing lead-in is implemented.
	 */
	public boolean isDone()
	{
		return forwardTicksElapsed >= totalDurationTicks;
	}

	private int cycleLength()
	{
		return reverseTicks + window.size();
	}

	/** True for the first {@link #reverseTicks} ticks of every pass - see {@link #reverseTick()}/{@link #isReverseStart()}. */
	public boolean isReversing()
	{
		return elapsedTicks % cycleLength() < reverseTicks;
	}

	/** 0-indexed position within the current reversing phase - only meaningful while {@link #isReversing()}. */
	public int reverseTick()
	{
		return elapsedTicks % cycleLength();
	}

	/** True on the exact tick a new pass's reversing phase begins - blocks get their own (still instant) undo applied here, same as the old {@code isPassStart()} used to do for the whole reset. */
	public boolean isReverseStart()
	{
		return elapsedTicks % cycleLength() == 0;
	}

	/** Which index into {@link #getWindow()} the current forward-replay pass is on - only meaningful while <b>not</b> {@link #isReversing()}. 0 means forward replay just began this tick (entities are already at the window's start, having just finished reversing there - nothing more to apply). */
	public int currentPassIndex()
	{
		return elapsedTicks % cycleLength() - reverseTicks;
	}

	public boolean isPassStart()
	{
		return !isReversing() && currentPassIndex() == 0;
	}

	public void advanceOneTick()
	{
		if(!isReversing())
			forwardTicksElapsed++;
		elapsedTicks++;
	}
}
