package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.loop;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.TimelineData;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ticks every active {@link TimeLoopZone}, mirroring {@code mechanics.timeline.TimelineRewindPlayback}'s shape
 * (a {@link LevelTickEvent.Post} driver iterating {@code TimelineData}'s active-effect lists). Each tick,
 * a zone either resets to its window's start (pass start) or replays one step forward - see
 * {@link TimeLoopReplay}. Zones are processed parent-before-child (see {@link #depthOrder}) so a
 * {@code TechTimeLoopBeta} child's reset/replay always lands on top of its parent's for their
 * overlapping area, not in an arbitrary order.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class TimeLoopPlayback
{
	private TimeLoopPlayback()
	{
	}

	@SubscribeEvent
	private static void onLevelTick(LevelTickEvent.Post event)
	{
		if(!(event.getLevel() instanceof ServerLevel level))
			return;

		TimelineData data = level.getData(MSUAttachments.TIMELINE);
		List<TimeLoopZone> zones = data.getActiveLoops();
		if(zones.isEmpty())
			return;

		for(TimeLoopZone zone : depthOrder(zones))
		{
			if(zone.isPassStart())
			{
				TimeLoopReplay.resetToWindowStart(level, zone);
				if(zone.getClone() != null)
					zone.getClone().resetToStart();
			}
			else
			{
				TimeLoopReplay.replayStepForward(level, zone, zone.getWindow().get(zone.currentPassIndex()));
				if(zone.getClone() != null)
					zone.getClone().advanceOneStep(level);
			}

			zone.advanceOneTick();
		}

		Iterator<TimeLoopZone> iterator = zones.iterator();
		while(iterator.hasNext())
		{
			TimeLoopZone zone = iterator.next();
			if(!zone.isDone())
				continue;

			if(zone.getClone() != null)
				zone.getClone().despawn(level);
			iterator.remove();
		}
	}

	/** Sorts a copy of {@code zones} so every zone appears after its parent (and its parent's parent, etc.) - a shallow parent-child tree, not a deep one, so a simple depth count is enough. */
	private static List<TimeLoopZone> depthOrder(List<TimeLoopZone> zones)
	{
		Map<UUID, TimeLoopZone> byId = new java.util.HashMap<>();
		for(TimeLoopZone zone : zones)
			byId.put(zone.getId(), zone);

		List<TimeLoopZone> ordered = new java.util.ArrayList<>(zones);
		ordered.sort(Comparator.comparingInt(zone -> depthOf(zone, byId)));
		return ordered;
	}

	private static int depthOf(TimeLoopZone zone, Map<UUID, TimeLoopZone> byId)
	{
		int depth = 0;
		UUID parentId = zone.getParentZoneId();
		while(parentId != null && depth < 64) // safety cap, a parent chain should never realistically be this deep
		{
			TimeLoopZone parent = byId.get(parentId);
			if(parent == null)
				break;
			depth++;
			parentId = parent.getParentZoneId();
		}
		return depth;
	}
}
