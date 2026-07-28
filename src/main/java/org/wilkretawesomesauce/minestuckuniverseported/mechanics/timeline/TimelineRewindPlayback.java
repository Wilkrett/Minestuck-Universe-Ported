package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Config;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

import java.util.Iterator;

/**
 * Actually plays a queued {@link ActiveRewind} back over time - this is the part that makes the world
 * visibly un-happen instead of just snapping to its past state. Ticks every level, applying
 * {@link Config#timelineRewindPlaybackSpeed} recorded ticks' worth of undos per real tick to each level's
 * in-progress rewinds, and separately advancing any {@link DoomedTimelineClone}s at
 * {@link Config#timelineCloneReplaySpeed} (defaulting to real time, 1 recorded tick per real tick) - the
 * two are deliberately decoupled, and both now default to real-time pacing (1) rather than the earlier
 * fast-forwarded default, since blocks restoring in visible batches read as generic "world resetting"
 * rather than something actually happening - see each config option's own comment for the reasoning.
 * <p>
 * Also despawns each {@link DoomedTimelineClone} (playing the "gears" effect again, same as on spawn)
 * once its replay finishes, instead of leaving it standing forever.
 * <p>
 * Retrocognition's own playback (Retrocognition visions - {@code timeline.vision.PastVisionSession}) is
 * driven separately by {@code timeline.vision.PastVisionPlayback}, not here - it never shared much with
 * rewind playback beyond both being "tick something forward over real time," and the in-body-overlay
 * rework needs its own reconciliation logic (see that class) that doesn't fit this one's shape.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class TimelineRewindPlayback
{
	private TimelineRewindPlayback()
	{
	}

	@SubscribeEvent
	private static void onLevelTick(LevelTickEvent.Post event)
	{
		if(!(event.getLevel() instanceof ServerLevel level))
			return;

		TimelineData data = level.getData(MSUAttachments.TIMELINE);

		Iterator<ActiveRewind> rewindIterator = data.getActiveRewinds().iterator();
		while(rewindIterator.hasNext())
		{
			ActiveRewind rewind = rewindIterator.next();
			ServerPlayer initiator = level.getServer().getPlayerList().getPlayer(rewind.getInitiatorId());

			for(int i = 0; i < Config.timelineRewindPlaybackSpeed && !rewind.isDone(); i++)
				TimelineManager.applySnapshot(level, rewind.nextStep(), initiator);

			if(rewind.isDone())
				rewindIterator.remove();
		}

		Iterator<DoomedTimelineClone> cloneIterator = data.getDoomedClones().iterator();
		while(cloneIterator.hasNext())
		{
			DoomedTimelineClone clone = cloneIterator.next();

			for(int i = 0; i < Config.timelineCloneReplaySpeed && !clone.isDone(); i++)
				clone.advanceOneStep(level);

			if(clone.isDone())
			{
				clone.despawn(level);
				cloneIterator.remove();
			}
		}
	}
}
