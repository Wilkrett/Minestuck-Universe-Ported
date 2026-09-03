package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.vision;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.EntitySnapshot;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.TimelineData;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.WorldTickSnapshot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Drives every active {@link PastVisionSession}, mirroring {@code timeline.loop.TimeLoopPlayback}'s
 * driver shape ({@code LevelTickEvent.Post} iterating a {@code TimelineData} list). Per session, per
 * tick, reconciles what's currently faked for that one observer against what *should* be faked right
 * now - the overlay radius follows their live position, so positions/entities can enter or leave it as
 * they move, independent of the window itself playing forward. See {@code timeline.vision} package's
 * classes for the packet-level mechanics this drives.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class PastVisionPlayback
{
	/** How far around the observer's live position the overlay follows, in blocks. */
	private static final double OVERLAY_RADIUS = 24.0;

	private PastVisionPlayback()
	{
	}

	@SubscribeEvent
	private static void onLevelTick(LevelTickEvent.Post event)
	{
		if(!(event.getLevel() instanceof ServerLevel level))
			return;

		TimelineData data = level.getData(MSUAttachments.TIMELINE);
		List<PastVisionSession> sessions = data.getActiveVisions();
		if(sessions.isEmpty())
			return;

		Iterator<PastVisionSession> iterator = sessions.iterator();
		while(iterator.hasNext())
		{
			PastVisionSession session = iterator.next();
			ServerPlayer observer = level.getServer().getPlayerList().getPlayer(session.getPlayerId());

			// Offline, or moved to a different dimension since casting - nothing sensible left to show
			// them here; tear down rather than keep faking packets into a level they're not even in.
			if(observer == null || observer.level() != level)
			{
				tearDown(level, session, observer);
				iterator.remove();
				continue;
			}

			tickSession(level, observer, session);
			session.advance();

			if(session.isDone())
			{
				tearDown(level, session, observer);
				iterator.remove();
			}
		}
	}

	@SubscribeEvent
	private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
	{
		if(!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level))
			return;

		TimelineData data = level.getData(MSUAttachments.TIMELINE);
		data.getActiveVisions().removeIf(session -> session.getPlayerId().equals(player.getUUID()));
	}

	private static void tickSession(ServerLevel level, ServerPlayer observer, PastVisionSession session)
	{
		double radius = OVERLAY_RADIUS;
		double radiusSqr = radius * radius;
		int index = session.getPlaybackIndex();

		reconcileBlocks(level, observer, session, radiusSqr, index);
		reconcileEntities(level, observer, session, radiusSqr, index);
	}

	private static void reconcileBlocks(ServerLevel level, ServerPlayer observer, PastVisionSession session, double radiusSqr, int index)
	{
		Map<BlockPos, BlockState> overlaid = session.getOverlaidBlocks();

		for(BlockPos pos : new ArrayList<>(overlaid.keySet()))
			if(!withinRadius(observer, pos, radiusSqr))
			{
				PastVisionPacketSender.resyncBlock(observer, level, pos);
				overlaid.remove(pos);
			}

		for(BlockPos pos : candidateBlockPositions(session))
		{
			if(!withinRadius(observer, pos, radiusSqr))
				continue;

			BlockState correct = session.historicalStateAt(pos, index);
			if(correct == null)
				continue;

			if(!correct.equals(overlaid.get(pos)))
			{
				PastVisionPacketSender.sendFakeBlock(observer, pos, correct);
				overlaid.put(pos, correct);
			}
		}
	}

	/** Every position the captured window ever recorded a change for - the only positions that could ever need faking, see {@code PastVisionSession#historicalStateAt}. */
	private static List<BlockPos> candidateBlockPositions(PastVisionSession session)
	{
		List<BlockPos> positions = new ArrayList<>();
		for(WorldTickSnapshot step : session.getWindow())
			for(BlockPos pos : step.blockChanges().keySet())
				if(!positions.contains(pos))
					positions.add(pos);
		return positions;
	}

	private static boolean withinRadius(ServerPlayer observer, BlockPos pos, double radiusSqr)
	{
		return observer.position().distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= radiusSqr;
	}

	private static void reconcileEntities(ServerLevel level, ServerPlayer observer, PastVisionSession session, double radiusSqr, int index)
	{
		Map<UUID, GhostEntity> ghosts = session.getActiveGhosts();
		WorldTickSnapshot step = session.getWindow().get(index);
		Map<UUID, EntitySnapshot> recorded = step.entitySnapshots();

		for(Map.Entry<UUID, EntitySnapshot> entry : recorded.entrySet())
		{
			UUID entityId = entry.getKey();
			if(entityId.equals(session.getPlayerId()))
				continue; // never ghost the observer themselves

			EntitySnapshot snapshot = entry.getValue();
			boolean inRadius = observer.position().distanceToSqr(snapshot.pos()) <= radiusSqr;
			GhostEntity ghost = ghosts.get(entityId);

			if(!inRadius)
			{
				if(ghost != null)
				{
					PastVisionPacketSender.despawnGhostAndRestore(observer, level, ghost);
					ghosts.remove(entityId);
				}
				continue;
			}

			if(ghost != null)
			{
				PastVisionPacketSender.moveGhost(observer, ghost, snapshot);
				continue;
			}

			if(!(level.getEntity(entityId) instanceof LivingEntity real) || real instanceof Player)
				continue; // gone, or a real other player - see PastVisionSession's doc comment, decision #5

			GhostEntity spawned = PastVisionPacketSender.spawnGhost(observer, level, real, snapshot);
			if(spawned != null)
				ghosts.put(entityId, spawned);
		}

		// Entities ghosted last tick that this tick's step has no data for at all (e.g. they died/unloaded
		// mid-window) - nothing to move them to, so dismiss the ghost rather than leave it frozen.
		Iterator<Map.Entry<UUID, GhostEntity>> ghostIterator = ghosts.entrySet().iterator();
		while(ghostIterator.hasNext())
		{
			Map.Entry<UUID, GhostEntity> entry = ghostIterator.next();
			if(!recorded.containsKey(entry.getKey()))
			{
				PastVisionPacketSender.despawnGhostAndRestore(observer, level, entry.getValue());
				ghostIterator.remove();
			}
		}
	}

	private static void tearDown(ServerLevel level, PastVisionSession session, ServerPlayer observerOrNull)
	{
		if(observerOrNull == null)
			return; // disconnected - nothing left to send packets to, just drop the session

		for(BlockPos pos : session.getOverlaidBlocks().keySet())
			PastVisionPacketSender.resyncBlock(observerOrNull, level, pos);

		for(GhostEntity ghost : session.getActiveGhosts().values())
			PastVisionPacketSender.despawnGhostAndRestore(observerOrNull, level, ghost);
	}
}
