package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

/**
 * Client-only "your own perspective is slowing down" overlay for whoever is actually looking through a
 * {@code MSUMobEffects#TIME_DILATION}-affected screen - only ever true for the local player, since a
 * screen effect makes no sense for a non-player target (a targeted mob still gets the real movement/
 * attack-speed slow from the effect itself, just never this overlay - nothing here needs to special-case
 * that, {@link Minecraft#player} is only ever the local player to begin with).
 * <p>
 * Rendered as a darkened band across the top and bottom of the screen (a vignette, not a hard on/off
 * strobe) via {@link GuiGraphics#fillGradient} - vanilla's own gradient fill only ever interpolates
 * vertically (top color to bottom color), which is exactly right for top/bottom bands but can't produce a
 * true left/right-inclusive radial vignette without a custom shader; two vertical bands reads as
 * "vignette" well enough without needing one.
 * <p>
 * Timing reuses {@link TimeDilationEffect#PULSE_CYCLE_TICKS}/{@link TimeDilationEffect#FREEZE_DURATION_TICKS}
 * directly - the same cycle {@code TimeDilationLagEvents} uses server-side to hold position/deal chip
 * damage - so the screen visibly darkens hardest at exactly the moment the real "lag spike" hits, then
 * fades back down over the rest of the cycle, instead of pulsing on an unrelated rhythm.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class TimeDilationVignette
{
	private static final int MIN_ALPHA = 20;
	private static final int MAX_ALPHA = 200;
	private static final float BAND_FRACTION = 0.3F;

	private TimeDilationVignette()
	{
	}

	@SubscribeEvent
	private static void onRenderGui(RenderGuiEvent.Post event)
	{
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if(player == null || !player.hasEffect(MSUMobEffects.TIME_DILATION))
			return;

		GuiGraphics guiGraphics = event.getGuiGraphics();
		int screenWidth = guiGraphics.guiWidth();
		int screenHeight = guiGraphics.guiHeight();

		// Same "+ getId()" per-entity offset TimeDilationLagEvents uses server-side, so this always
		// agrees with whether the player is inside a freeze window right now.
		int cycle = TimeDilationEffect.PULSE_CYCLE_TICKS;
		int freeze = TimeDilationEffect.FREEZE_DURATION_TICKS;
		int local = (int) Math.floorMod(mc.level.getGameTime() + player.getId(), (long) cycle);

		float darkness;
		if(local < freeze)
			darkness = 1.0F;
		else
		{
			float decayProgress = (local - freeze) / (float) (cycle - freeze);
			darkness = Math.max(0F, 1.0F - decayProgress);
		}
		int alpha = MIN_ALPHA + (int) ((MAX_ALPHA - MIN_ALPHA) * darkness);

		int dark = alpha << 24;
		int clear = 0;
		int bandHeight = (int) (screenHeight * BAND_FRACTION);

		guiGraphics.fillGradient(0, 0, screenWidth, bandHeight, dark, clear);
		guiGraphics.fillGradient(0, screenHeight - bandHeight, screenWidth, screenHeight, clear, dark);
	}
}
