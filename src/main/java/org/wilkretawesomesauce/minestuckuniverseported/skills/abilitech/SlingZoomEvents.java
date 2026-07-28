package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

/**
 * Client-only FOV zoom while charging {@code skills.abilitech.TechSling} ("Sylladex Sling") - real port
 * of the original's own {@code IBadgeEffects#getFOV()} nudge (1 narrower per charging tick, capped at 20).
 * Driven by {@link SlingChargeEffect}'s amplifier (see that class's own doc comment for why it carries
 * charge ticks instead of a real effect strength), only ever meaningful for the local player - same
 * reasoning as {@code heroAspect.time.AcceleratingVignette}/{@code heroAspect.time.TimeDilationVignette}.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class SlingZoomEvents
{
	private static final int MAX_CHARGE_TICKS = 20;
	private static final float MAX_ZOOM_FACTOR = 0.5F;

	private SlingZoomEvents()
	{
	}

	@SubscribeEvent
	private static void onComputeFov(ViewportEvent.ComputeFov event)
	{
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if(player == null)
			return;

		MobEffectInstance instance = player.getEffect(MSUMobEffects.SLING_CHARGE);
		if(instance == null)
			return;

		float chargeRatio = Mth.clamp(instance.getAmplifier() / (float) MAX_CHARGE_TICKS, 0F, 1F);
		event.setFOV(event.getFOV() * (1.0F - chargeRatio * MAX_ZOOM_FACTOR));
	}
}
