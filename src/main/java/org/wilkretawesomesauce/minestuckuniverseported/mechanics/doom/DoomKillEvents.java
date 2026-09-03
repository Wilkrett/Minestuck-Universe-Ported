package org.wilkretawesomesauce.minestuckuniverseported.mechanics.doom;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
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
	/** Flat Doom a killer gains for killing any other LivingEntity, before the per-max-health scaling below. */
	private static final double KILL_BASE = 1.0;
	/** Additional Doom a killer gains per point of the victim's max health, capped by {@link #KILL_CAP}. */
	private static final double KILL_PER_MAX_HEALTH = 0.05;
	/** Max Doom a single kill can ever grant, regardless of the victim's max health. */
	private static final double KILL_CAP = 15.0;

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

		double gain = Math.min(KILL_CAP, KILL_BASE + KILL_PER_MAX_HEALTH * victim.getMaxHealth());
		killer.getData(MSUAttachments.DOOM_DATA).addDoom(gain);
	}
}
