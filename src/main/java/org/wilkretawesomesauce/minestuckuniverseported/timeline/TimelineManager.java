package org.wilkretawesomesauce.minestuckuniverseported.timeline;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.wilkretawesomesauce.minestuckuniverseported.Config;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUFakePlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Two ways of doing a real, destructive rewind, both built on the same recorded history:
 * <ul>
 *     <li>{@link #rewind} - {@code /msutimeline rewind <seconds>} - queues the undo to play out over
 *     real time via {@link TimelineRewindPlayback}, and spawns a {@link DoomedTimelineClone} that
 *     replays the initiator's own path forward while the world plays backward around it.</li>
 *     <li>{@link #travelBackwards} - {@code /msutimeline travel backwards <seconds>} - applies the exact
 *     same undo instantly, all at once, with no clone and nothing to watch unfold. "You instantly get
 *     set to the beginning of the timeline snapshot."</li>
 * </ul>
 * Both pop the relevant snapshots off {@link TimelineData}'s recorded history immediately (so the same
 * ticks can never be consumed twice) and award Doom Points the same way - they differ only in whether the
 * result is applied gradually or all at once, and whether a clone gets spawned.
 * <p>
 * <b>Scoped to the rewinding player, not the whole level.</b> {@link TimelineData} is attached to the
 * {@code Level} because that's where NeoForge lets attachments live, and the history is genuinely a
 * level-wide record, recorded unconditionally for everyone (see {@link TimelineRecorder}) - but applying
 * it here never restores other {@link ServerPlayer}s, regardless of which method is used. The Alpha
 * Timeline (the shared, canonical world state everyone else experiences) stays intact outside whatever
 * the initiator personally touches - it's the closest approximation of "the canon timeline is untouched,
 * only the Time user's immediate vicinity actually time-travels" that's achievable without literally
 * forking the world.
 */
public final class TimelineManager
{
	private TimelineManager()
	{
	}

	/**
	 * @param requestedTicks how far back to rewind; clamped to however much history is actually recorded
	 * @return how many ticks were actually queued to rewind (may be less than requested if history ran out) -
	 *         this is queued playback, not a completed rewind, by the time this method returns
	 */
	public static int rewind(ServerLevel level, ServerPlayer initiator, int requestedTicks)
	{
		List<WorldTickSnapshot> steps = popSteps(level, requestedTicks);
		if(steps.isEmpty())
			return 0;

		TimelineData data = level.getData(MSUAttachments.TIMELINE);
		spawnDoomedClone(level, initiator, steps, data);

		data.queueRewind(new ActiveRewind(steps, initiator.getUUID()));
		data.incrementRewinds();
		// Alpha (the Overworld) never accrues Doom Points - only branch timelines do. See
		// TimelineBranchRegistry's doc comment for why branches exist at all; DP is meant to eventually
		// punish messing with a *branch*, not the one canonical timeline everyone shares.
		if(level.dimension() != Level.OVERWORLD)
			data.addDoomPoints(steps.size() * Config.timelineDoomPointsPerTick);

		return steps.size();
	}

	/**
	 * @param requestedTicks how far back to travel; clamped to however much history is actually recorded
	 * @return how many ticks were actually applied
	 */
	public static int travelBackwards(ServerLevel level, ServerPlayer initiator, int requestedTicks)
	{
		List<WorldTickSnapshot> steps = popSteps(level, requestedTicks);
		if(steps.isEmpty())
			return 0;

		for(WorldTickSnapshot step : steps)
			applySnapshot(level, step, initiator);

		TimelineData data = level.getData(MSUAttachments.TIMELINE);
		data.incrementRewinds();
		if(level.dimension() != Level.OVERWORLD)
			data.addDoomPoints(steps.size() * Config.timelineDoomPointsPerTick);

		return steps.size();
	}

	private static List<WorldTickSnapshot> popSteps(ServerLevel level, int requestedTicks)
	{
		TimelineData data = level.getData(MSUAttachments.TIMELINE);
		int ticks = Math.min(requestedTicks, data.getRecordedTicks());
		if(ticks <= 0)
			return List.of();

		List<WorldTickSnapshot> steps = new ArrayList<>(ticks);
		for(int i = 0; i < ticks; i++)
		{
			WorldTickSnapshot snapshot = data.getHistory().pollLast();
			if(snapshot == null)
				break;
			steps.add(snapshot);
		}
		return steps;
	}

	/**
	 * Applies one recorded tick's worth of undo directly - shared by {@link #travelBackwards} (applies
	 * every step immediately) and {@link TimelineRewindPlayback} (applies a few steps per real tick).
	 */
	static void applySnapshot(ServerLevel level, WorldTickSnapshot snapshot, ServerPlayer initiator)
	{
		for(Map.Entry<BlockPos, WorldTickSnapshot.BlockChangeRecord> entry : snapshot.blockChanges().entrySet())
			level.setBlock(entry.getKey(), entry.getValue().oldState(), Block.UPDATE_ALL);

		for(Map.Entry<UUID, EntitySnapshot> entry : snapshot.entitySnapshots().entrySet())
		{
			if(level.getEntity(entry.getKey()) instanceof LivingEntity entity && !(entity instanceof ServerPlayer other && other != initiator))
				entry.getValue().applyTo(entity);
		}
	}

	private static void spawnDoomedClone(ServerLevel level, ServerPlayer initiator, List<WorldTickSnapshot> steps, TimelineData data)
	{
		UUID initiatorId = initiator.getUUID();

		// steps is in "most recent tick first" order (undo order); DoomedTimelineClone.buildSteps expects
		// chronological order, so reverse a copy first rather than mutating the caller's list.
		List<WorldTickSnapshot> chronological = new ArrayList<>(steps);
		Collections.reverse(chronological);

		List<DoomedTimelineClone.Step> playerHistory = DoomedTimelineClone.buildSteps(initiatorId, chronological);
		if(playerHistory.isEmpty())
			return;

		GameProfile profile = new GameProfile(UUID.randomUUID(), initiator.getName().getString() + " (Doomed)");
		MSUFakePlayer fakePlayer = DoomedTimelineClone.spawn(level, profile, playerHistory.get(0).state());
		data.addDoomedClone(new DoomedTimelineClone(playerHistory, fakePlayer));
	}
}
