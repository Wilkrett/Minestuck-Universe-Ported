package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

/**
 * Client-only "charging up" overlay for whoever is actually holding {@code TechTimeAccelerateSelf}
 * ("Accelerate") - only ever true for the local player, same reasoning as {@link TimeDilationVignette}
 * (a screen effect makes no sense for anyone but the one player actually looking through it).
 * <p>
 * Rendered as the same red-tinted top/bottom gradient-band shape {@link TimeDilationVignette} already
 * established for this project (see that class's own doc comment for why bands, not a true radial
 * vignette), except intensity here is driven directly by {@link AcceleratingEffect}'s amplifier - which
 * {@code TechTimeAccelerateSelf} refreshes every charging tick to the current charge percentage (0-100) -
 * rather than a fixed pulse cycle, so the vignette simply gets stronger the longer the ability is held,
 * capping out alongside the burst itself at {@code TechTimeAccelerateSelf#MAX_CHARGE_TICKS}.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class AcceleratingVignette
{
	private static final int MAX_ALPHA = 160;
	private static final float BAND_FRACTION = 0.25F;
	private static final int TINT = 0xFF4040;

	private AcceleratingVignette()
	{
	}

	@SubscribeEvent
	private static void onRenderGui(RenderGuiEvent.Post event)
	{
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if(player == null)
			return;

		MobEffectInstance instance = player.getEffect(MSUMobEffects.ACCELERATING);
		if(instance == null)
			return;

		float chargeRatio = Mth.clamp(instance.getAmplifier() / 100F, 0F, 1F);
		if(chargeRatio <= 0F)
			return;

		GuiGraphics guiGraphics = event.getGuiGraphics();
		int screenWidth = guiGraphics.guiWidth();
		int screenHeight = guiGraphics.guiHeight();

		int alpha = (int) (MAX_ALPHA * chargeRatio);
		int dark = (alpha << 24) | (TINT & 0xFFFFFF);
		int clear = 0;
		int bandHeight = (int) (screenHeight * BAND_FRACTION * chargeRatio);

		guiGraphics.fillGradient(0, 0, screenWidth, bandHeight, dark, clear);
		guiGraphics.fillGradient(0, screenHeight - bandHeight, screenWidth, screenHeight, clear, dark);
	}
}
