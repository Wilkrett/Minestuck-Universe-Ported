package org.wilkretawesomesauce.minestuckuniverseported.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.client.util.StreakRibbonUtils;
import org.wilkretawesomesauce.minestuckuniverseported.client.util.StreakSettings;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-only per-tick position/pose history for whichever entities currently have the streak effect
 * toggled on (see {@link StreakClientState}). Nothing in this codebase already maintains this kind of
 * rolling client-side history - {@code mechanics.timeline.EntitySnapshot} is the closest existing analog, but
 * it's server-side and built for a completely different purpose (destructive rewind), so this is a
 * from-scratch, purpose-built ring buffer rather than a reuse of that class.
 * <p>
 * Ported from iChun's Streak's own {@code EntityTracker}/{@code addInfo} - each new sample's
 * {@code texU} accumulates by distance-traveled-since-the-last-sample divided by the entity's own
 * height, exactly matching the original's {@code StreakTag#createFor} math (see
 * {@code client.util.StreakRibbonUtils#texUDelta}).
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class StreakTracker
{
	/** One recorded tick of a tracked entity's position/pose, oldest-first within each deque. */
	public record Sample(double x, double y, double z, float height, boolean invisible, boolean sprinting, float texU)
	{
	}

	private static final Map<Integer, Deque<Sample>> history = new ConcurrentHashMap<>();

	private StreakTracker()
	{
	}

	public static Map<Integer, Deque<Sample>> getHistory()
	{
		return history;
	}

	@SubscribeEvent
	private static void onClientTick(ClientTickEvent.Post event)
	{
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if(level == null)
		{
			history.clear();
			return;
		}

		int maxSamples = Math.max(StreakSettings.TRAIL_LENGTH_TICKS,
				StreakSettings.SPRINT_GHOST_SPACING_TICKS * StreakSettings.SPRINT_GHOST_COUNT);

		for(Integer entityId : StreakClientState.trackedIds())
		{
			Entity entity = level.getEntity(entityId);
			if(!(entity instanceof LivingEntity living) || !living.isAlive())
			{
				history.remove(entityId);
				continue;
			}

			Deque<Sample> samples = history.computeIfAbsent(entityId, id -> new ArrayDeque<>());
			Sample previous = samples.peekLast();

			float height = living.getBbHeight();
			float texU = previous == null ? 0F
					: previous.texU() + StreakRibbonUtils.texUDelta(living.getX() - previous.x(), living.getZ() - previous.z(),
					Math.min(height, previous.height()));

			samples.addLast(new Sample(living.getX(), living.getY(), living.getZ(), height, living.isInvisible(), living.isSprinting(), texU));
			while(samples.size() > maxSamples)
				samples.removeFirst();
		}

		// Evict anything no longer toggled on and past its ghost fade-out window, so a disabled/unloaded
		// entity's history doesn't linger forever.
		Iterator<Integer> it = history.keySet().iterator();
		while(it.hasNext())
		{
			if(!StreakClientState.isTracked(it.next()))
				it.remove();
		}
	}
}
