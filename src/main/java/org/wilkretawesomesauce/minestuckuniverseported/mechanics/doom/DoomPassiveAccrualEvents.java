package org.wilkretawesomesauce.minestuckuniverseported.mechanics.doom;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Config;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;

/**
 * MVP source hook - "living for long periods" slowly accrues Doom on its own, applying to every
 * {@code LivingEntity} per the design doc's "every entity possesses Doom" framing, though in practice
 * only long-lived players/kept mobs ever survive long enough to cross the age threshold. Original
 * design for this project, no 1.12.2 counterpart. Hooks {@link EntityTickEvent.Post}, the same event
 * {@code capabilities.consortCosmetics.ConsortHatsData} already uses for per-entity-tick work.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class DoomPassiveAccrualEvents
{
	private DoomPassiveAccrualEvents()
	{
	}

	@SubscribeEvent
	private static void onEntityTick(EntityTickEvent.Post event)
	{
		if(!(event.getEntity() instanceof LivingEntity entity) || entity.level().isClientSide())
			return;
		if(entity.tickCount % Config.doomPassiveAccrualCheckIntervalTicks != 0)
			return;

		DoomData data = entity.getData(MSUAttachments.DOOM_DATA);
		data.addTicksAliveAccrued(Config.doomPassiveAccrualCheckIntervalTicks);
		if(data.getTicksAliveAccrued() > Config.doomPassiveAccrualAgeThresholdTicks)
			data.addDoom(Config.doomPassiveAccrualPerInterval);
	}
}
