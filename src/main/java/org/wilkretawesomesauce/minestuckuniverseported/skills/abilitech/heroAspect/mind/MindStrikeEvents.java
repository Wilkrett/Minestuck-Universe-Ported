package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.mind;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;
import org.wilkretawesomesauce.minestuckuniverseported.skills.MSUSkills;

/**
 * Spends {@code TechMindStrike}'s charged {@link CalculatingEffect} on this player's very next hit -
 * see that tech's own doc comment. Matches the original's own gate exactly: the attacker must still
 * have Calculated Strike equipped (not just carry a leftover effect instance) for the payoff to fire.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class MindStrikeEvents
{
	private MindStrikeEvents()
	{
	}

	@SubscribeEvent
	private static void onIncomingDamage(LivingIncomingDamageEvent event)
	{
		if(!(event.getSource().getEntity() instanceof Player attacker) || !attacker.hasEffect(MSUMobEffects.CALCULATING))
			return;

		GodTierData godTier = attacker.getData(MSUAttachments.GOD_TIER);
		if(!godTier.isTechEquipped(MSUSkills.MIND_STRIKE))
			return;

		int calculating = attacker.getEffect(MSUMobEffects.CALCULATING).getDuration();
		double power = Math.sin(calculating * 1.1 + Math.PI * 1.5) / 2 + calculating * 0.017 + 0.5;

		event.setAmount((float)(event.getAmount() * Math.max(2, power)));
		attacker.removeEffect(MSUMobEffects.CALCULATING);
	}
}
