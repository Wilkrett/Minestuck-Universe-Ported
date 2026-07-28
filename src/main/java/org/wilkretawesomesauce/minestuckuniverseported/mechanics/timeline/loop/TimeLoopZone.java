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
	private final StackMode stackMode;
	@Nullable
	private final UUID parentZoneId;
	private final Set<UUID> puppetedEntityIds;
	private final UUID creatorId;
	@Nullable
	private final DoomedTimelineClone clone;

	private int elapsedTicks = 0;

	public TimeLoopZone(Vec3 center, double radius, List<WorldTickSnapshot> window, int totalDurationTicks,
			StackMode stackMode, @Nullable UUID parentZoneId, UUID creatorId, @Nullable DoomedTimelineClone clone)
	{
		this.center = center;
		this.radius = radius;
		this.window = window;
		this.totalDurationTicks = totalDurationTicks;
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

	public boolean isDone()
	{
		return elapsedTicks >= totalDurationTicks;
	}

	/** Which index into {@link #getWindow()} the current pass is on - 0 means this tick starts a fresh pass (reset). */
	public int currentPassIndex()
	{
		return elapsedTicks % window.size();
	}

	public boolean isPassStart()
	{
		return currentPassIndex() == 0;
	}

	public void advanceOneTick()
	{
		elapsedTicks++;
	}
}
