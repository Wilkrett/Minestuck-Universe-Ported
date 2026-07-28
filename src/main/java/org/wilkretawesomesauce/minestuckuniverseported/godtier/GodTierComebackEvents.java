package org.wilkretawesomesauce.minestuckuniverseported.godtier;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

/**
 * Gives {@link GodTierComebackEffect} a real, new producer - see that class's own doc comment for why
 * this is a deliberate new design choice rather than reproducing the original's Karma/badge-gated
 * production. Also ports the original's incoming-damage-reduction half of {@code PotionComeback}, which
 * lived in a standalone {@code LivingHurtEvent} handler in the original rather than inside the potion
 * class itself - kept in the same shape here (a companion events class), not folded into the effect.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class GodTierComebackEvents
{
	private GodTierComebackEvents()
	{
	}

	@SubscribeEvent
	private static void onPlayerTick(PlayerTickEvent.Post event)
	{
		if(!(event.getEntity() instanceof ServerPlayer player))
			return;

		if(player.getData(MSUAttachments.GOD_TIER).isAscended())
			player.addEffect(new MobEffectInstance(MSUMobEffects.GOD_TIER_COMEBACK, 25, 0, true, false));
	}

	@SubscribeEvent
	private static void onIncomingDamage(LivingIncomingDamageEvent event)
	{
		MobEffectInstance comeback = event.getEntity().getEffect(MSUMobEffects.GOD_TIER_COMEBACK);
		if(comeback == null || event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD))
			return;

		event.setAmount(event.getAmount() * (25 - (comeback.getAmplifier() + 1) * 5) / 25F);
	}
}
