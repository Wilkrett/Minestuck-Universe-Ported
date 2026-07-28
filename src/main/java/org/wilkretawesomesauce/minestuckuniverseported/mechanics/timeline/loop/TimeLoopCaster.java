package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.loop;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.Config;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.DoomedTimelineClone;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.TimelineData;
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
	 *                      {@code Config.timeLoopMaxDurationTicks} by the calling tech already), <b>not</b>
	 *                      the window length - conflating the two was a real bug (see
	 *                      {@code Config.timeLoopMaxDurationTicks}'s own comment): a loop whose duration
	 *                      equalled its window size only ever played through once instead of repeating.
	 *                      The window is a separate, fixed {@code Config.timeLoopWindowTicks}.
	 * @param parentZoneId  non-null only for a {@code NESTED} cast attaching onto an existing zone
	 * @return the created zone, or null if there's no recorded history to capture yet
	 */
	@Nullable
	public static TimeLoopZone cast(ServerLevel level, ServerPlayer player, int chargedTicks,
			TimeLoopZone.StackMode stackMode, @Nullable UUID parentZoneId)
	{
		TimelineData data = level.getData(MSUAttachments.TIMELINE);

		List<WorldTickSnapshot> history = new ArrayList<>(data.getHistory());
		int windowTicks = Math.min(Config.timeLoopWindowTicks, history.size());
		if(windowTicks <= 0)
			return null;

		List<WorldTickSnapshot> window = new ArrayList<>(history.subList(history.size() - windowTicks, history.size()));

		DoomedTimelineClone clone = spawnCasterClone(level, player, window);

		TimeLoopZone zone = new TimeLoopZone(player.position(), Config.timeLoopRadius, window, chargedTicks,
				stackMode, parentZoneId, player.getUUID(), clone);
		data.getActiveLoops().add(zone);

		// Same Alpha-exempt gate TimelineManager#rewind/#travelBackwards already use - a time loop is
		// squarely part of that timeline-manipulation DP family, not the separate timeRequest DP.
		if(level.dimension() != Level.OVERWORLD)
			data.addDoomPoints(chargedTicks * Config.timelineDoomPointsPerTick);

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

	/** Finds an existing NESTED zone within {@code Config.timeLoopRadius} of {@code position}, for {@code TechTimeLoopBeta} to attach onto - null if none, meaning the new cast starts a fresh root. */
	@Nullable
	public static TimeLoopZone findNestParent(ServerLevel level, net.minecraft.world.phys.Vec3 position)
	{
		TimelineData data = level.getData(MSUAttachments.TIMELINE);
		double radiusSqr = Config.timeLoopRadius * Config.timeLoopRadius;

		for(TimeLoopZone zone : data.getActiveLoops())
			if(zone.getStackMode() == TimeLoopZone.StackMode.NESTED && zone.getCenter().distanceToSqr(position) <= radiusSqr)
				return zone;
		return null;
	}
}
