package org.wilkretawesomesauce.minestuckuniverseported.mechanics.doom;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
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
	/** How often (in ticks) each living entity's age-based passive Doom accrual is checked. 1200 = once a minute. */
	private static final int CHECK_INTERVAL_TICKS = 1200;
	/** An entity must have been continuously alive at least this long before passive age-based Doom accrual starts at all. 24000 = 20 minutes. */
	private static final int AGE_THRESHOLD_TICKS = 24000;
	/** How much Doom accrues per {@link #CHECK_INTERVAL_TICKS} once {@link #AGE_THRESHOLD_TICKS} is exceeded. */
	private static final double PER_INTERVAL = 0.1;

	private DoomPassiveAccrualEvents()
	{
	}

	@SubscribeEvent
	private static void onEntityTick(EntityTickEvent.Post event)
	{
		if(!(event.getEntity() instanceof LivingEntity entity) || entity.level().isClientSide())
			return;
		if(entity.tickCount % CHECK_INTERVAL_TICKS != 0)
			return;

		DoomData data = entity.getData(MSUAttachments.DOOM_DATA);
		data.addTicksAliveAccrued(CHECK_INTERVAL_TICKS);
		if(data.getTicksAliveAccrued() > AGE_THRESHOLD_TICKS)
			data.addDoom(PER_INTERVAL);
	}
}
