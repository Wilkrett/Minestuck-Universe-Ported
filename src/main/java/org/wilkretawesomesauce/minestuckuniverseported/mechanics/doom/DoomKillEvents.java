package org.wilkretawesomesauce.minestuckuniverseported.mechanics.doom;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Config;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;

/**
 * MVP source hook - "killing other beings" contributes Doom to the killer. Original design for this
 * project, no 1.12.2 counterpart.
 * <p>
 * Its own independent {@link LivingDeathEvent} subscriber, separate from {@link DoomReleaseEvents}'s
 * own subscriber on the same event - they touch different entities' data (killer here, victim there)
 * so there's no ordering dependency, the same pattern already proven by
 * {@code capabilities.consortCosmetics.ConsortHatsData} and
 * {@code skills.abilitech.heroAspect.life.SavingGraceEvents} both independently subscribing to
 * {@code LivingDeathEvent} today.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class DoomKillEvents
{
	private DoomKillEvents()
	{
	}

	@SubscribeEvent
	private static void onDeath(LivingDeathEvent event)
	{
		LivingEntity victim = event.getEntity();
		if(victim.level().isClientSide())
			return;

		if(!(event.getSource().getEntity() instanceof LivingEntity killer) || killer == victim)
			return;

		double gain = Math.min(Config.doomKillCap, Config.doomKillBase + Config.doomKillPerMaxHealth * victim.getMaxHealth());
		killer.getData(MSUAttachments.DOOM_DATA).addDoom(gain);
	}
}
