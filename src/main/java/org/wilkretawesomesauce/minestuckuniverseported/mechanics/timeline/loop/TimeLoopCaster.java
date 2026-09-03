package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.loop;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.DoomedTimelineClone;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.TimelineData;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.TimelineManager;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.WorldTickSnapshot;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUFakePlayer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shared "actually create a Time Loop zone" logic behind both {@code TechTimeLoopAlpha} and
 * {@code TechTimeLoopBeta} - same separation-of-concerns as {@code mechanics.timeline.BranchForker} sitting
 * behind {@code TechTimelineBranch}. Captures the window non-destructively (copy, not pop - same pattern
 * {@code TechRetrocognition} already uses for {@code timeline.vision.PastVisionSession}, so the same recorded ticks
 * remain available to destructive rewind/travel too), charges Doom Points the same way
 * {@code TimelineManager#rewind} already does, spawns the caster's repeating ghost clone (see
 * {@code TimeLoopZone}'s doc comment), and registers the new zone.
 */
public final class TimeLoopCaster
{
	private TimeLoopCaster()
	{
	}

	/**
	 * @param chargedTicks how long the ability was held - becomes the zone's total lifetime (clamped to
	 *                      {@link TimeLoopZone#MAX_DURATION_TICKS} by the calling tech already), <b>not</b>
	 *                      the window length - conflating the two was a real bug (see that constant's own
	 *                      comment): a loop whose duration equalled its window size only ever played
	 *                      through once instead of repeating. The window is a separate, fixed
	 *                      {@link TimeLoopZone#DEFAULT_WINDOW_TICKS}.
	 * @param parentZoneId  non-null only for a {@code NESTED} cast attaching onto an existing zone
	 * @return the created zone, or null if there's no recorded history to capture yet
	 */
	@Nullable
	public static TimeLoopZone cast(ServerLevel level, ServerPlayer player, int chargedTicks,
			TimeLoopZone.StackMode stackMode, @Nullable UUID parentZoneId)
	{
		return cast(level, player, chargedTicks, TimeLoopZone.DEFAULT_WINDOW_TICKS, stackMode, parentZoneId);
	}

	/**
	 * Same as {@link #cast(ServerLevel, ServerPlayer, int, TimeLoopZone.StackMode, UUID)}, but with an
	 * explicit window length instead of always deriving it from {@link TimeLoopZone#DEFAULT_WINDOW_TICKS} -
	 * for a caller like {@code TechTimeLoopBeta}'s own death-save rewind, where how far back to capture is
	 * the whole point of the ability rather than an incidental replay length shared with every other Time
	 * Loop cast.
	 *
	 * @param requestedWindowTicks how many ticks of history to capture - clamped to however much is
	 *                              actually recorded, same as the default-window overload
	 */
	@Nullable
	public static TimeLoopZone cast(ServerLevel level, ServerPlayer player, int chargedTicks, int requestedWindowTicks,
			TimeLoopZone.StackMode stackMode, @Nullable UUID parentZoneId)
	{
		TimelineData data = level.getData(MSUAttachments.TIMELINE);

		List<WorldTickSnapshot> history = new ArrayList<>(data.getHistory());
		int windowTicks = Math.min(requestedWindowTicks, history.size());
		if(windowTicks <= 0)
			return null;

		List<WorldTickSnapshot> window = new ArrayList<>(history.subList(history.size() - windowTicks, history.size()));
		return castWithCapturedWindow(level, player, chargedTicks, player.position(), window, stackMode, parentZoneId);
	}

	/**
	 * Same as {@link #cast(ServerLevel, ServerPlayer, int, int, TimeLoopZone.StackMode, UUID)}, but for a
	 * caller that already captured its own {@code center}/{@code window} earlier than the moment the zone
	 * actually gets registered - {@code TechTimeLoopBeta}'s own real need: a player's death-save prompt can
	 * sit open for several real seconds while they decide, and
	 * {@code TimelineRecorder} keeps recording the whole time, so deriving the window live at
	 * <i>accept</i> time (the other overloads' own behavior) would anchor the rewind to whenever they
	 * happened to answer, not to the actual moment of death - a real, user-reported gap ("should play back
	 * from the point of death"). The caller is responsible for capturing both at the moment that actually
	 * matters and holding onto them until the zone is ready to be cast.
	 *
	 * @return the created zone, or null if {@code window} is empty
	 */
	@Nullable
	public static TimeLoopZone castWithCapturedWindow(ServerLevel level, ServerPlayer player, int chargedTicks,
			Vec3 center, List<WorldTickSnapshot> window, TimeLoopZone.StackMode stackMode, @Nullable UUID parentZoneId)
	{
		if(window.isEmpty())
			return null;

		TimelineData data = level.getData(MSUAttachments.TIMELINE);

		DoomedTimelineClone clone = spawnCasterClone(level, player, window);

		TimeLoopZone zone = new TimeLoopZone(center, TimeLoopZone.RADIUS, window, chargedTicks, TimeLoopZone.REVERSE_TICKS_EFFECTIVE,
				stackMode, parentZoneId, player.getUUID(), clone);
		data.getActiveLoops().add(zone);

		// Same Alpha-exempt gate TimelineManager#rewind/#travelBackwards already use - a time loop is
		// squarely part of that timeline-manipulation DP family, not the separate timeRequest DP.
		if(level.dimension() != Level.OVERWORLD)
			data.addDoomPoints(chargedTicks * TimelineManager.DOOM_POINTS_PER_TICK);

		return zone;
	}

	/** Builds the caster's own path out of the captured window and spawns a repeating ghost to walk it - null if the caster has no recorded presence in the window at all. */
	@Nullable
	private static DoomedTimelineClone spawnCasterClone(ServerLevel level, ServerPlayer player, List<WorldTickSnapshot> window)
	{
		List<DoomedTimelineClone.Step> path = DoomedTimelineClone.buildSteps(player.getUUID(), window);
		if(path.isEmpty())
			return null;

		GameProfile profile = new GameProfile(UUID.randomUUID(), player.getName().getString());
		MSUFakePlayer fakePlayer = DoomedTimelineClone.spawn(level, profile, path.get(0).state());
		return new DoomedTimelineClone(path, fakePlayer);
	}

	/** Finds an existing NESTED zone within {@link TimeLoopZone#RADIUS} of {@code position}, for {@code TechTimeLoopBeta} to attach onto - null if none, meaning the new cast starts a fresh root. */
	@Nullable
	public static TimeLoopZone findNestParent(ServerLevel level, net.minecraft.world.phys.Vec3 position)
	{
		TimelineData data = level.getData(MSUAttachments.TIMELINE);
		double radiusSqr = TimeLoopZone.RADIUS * TimeLoopZone.RADIUS;

		for(TimeLoopZone zone : data.getActiveLoops())
			if(zone.getStackMode() == TimeLoopZone.StackMode.NESTED && zone.getCenter().distanceToSqr(position) <= radiusSqr)
				return zone;
		return null;
	}
}
