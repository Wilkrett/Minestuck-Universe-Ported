package org.wilkretawesomesauce.minestuckuniverseported.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.network.TimeLoopRewindDecisionPacket;

/**
 * The real "opted in the death screen to 'rewind time'" prompt for Timeloop &beta;
 * ({@code skills.abilitech.heroAspect.time.TechTimeLoopBeta}) - see that class's own doc comment for the
 * full design and for why this is a custom screen takeover rather than an actual injected button on
 * vanilla's own {@code DeathScreen}. Forced open/closed purely by whether the local player currently has
 * {@link MSUMobEffects#TIME_LOOP_REWIND_PROMPT} (see {@code ClientEvents} on that tech class) - same
 * "let the synced effect be the single source of truth" idiom {@code TechSoulStun}'s own
 * {@code SoulShockScreen} already established, deliberately not closed directly by either button here (see
 * each button's own inline comment for why).
 */
public class TimeLoopRewindScreen extends Screen
{
	public TimeLoopRewindScreen()
	{
		super(Component.translatable("gui.timeLoopBeta.title"));
	}

	@Override
	protected void init()
	{
		addRenderableWidget(Button.builder(Component.translatable("gui.timeLoopBeta.rewind"), b -> decide(b, true))
				.bounds(width / 2 - 100, height / 2 + 10, 200, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.timeLoopBeta.decline"), b -> decide(b, false))
				.bounds(width / 2 - 100, height / 2 + 36, 200, 20).build());
	}

	/**
	 * Deliberately doesn't close the screen itself - the server round-trip that actually resolves the
	 * prompt (removing the marker effect) is what closes it, via the same client-tick watcher that opened
	 * it in the first place (see this class's own doc comment). Closing eagerly here would race that sync:
	 * a stray tick still seeing the not-yet-cleared effect would immediately reopen it. Both buttons are
	 * disabled instead, so a slow round-trip reads as "waiting", not as an unresponsive click, and a second
	 * click can't send a second, redundant packet.
	 */
	private void decide(Button clicked, boolean rewind)
	{
		for(var widget : renderables)
			if(widget instanceof Button button)
				button.active = false;

		PacketDistributor.sendToServer(new TimeLoopRewindDecisionPacket(rewind));
	}

	/** Escape still reaches the pause menu (matching {@code SoulShockScreen}'s own precedent) rather than silently dismissing the prompt - the decision only actually resolves through a button, or through the prompt's own timeout. */
	@Override
	public void onClose()
	{
		Minecraft.getInstance().setScreen(new PauseScreen(true));
	}

	@Override
	public boolean isPauseScreen()
	{
		return false;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
	{
		guiGraphics.fill(0, 0, width, height, 0x88000000);
		guiGraphics.drawCenteredString(font, title, width / 2, height / 2 - 30, 0x66FFE8);

		int remainingTicks = remainingPromptTicks();
		if(remainingTicks >= 0)
		{
			Component subtitle = Component.translatable("gui.timeLoopBeta.subtitle", (remainingTicks / 20) + 1);
			guiGraphics.drawCenteredString(font, subtitle, width / 2, height / 2 - 12, 0xCCCCCC);
		}

		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	/** -1 if the effect somehow isn't present this exact frame (about to close) - see this class's own doc comment on why this screen never assumes the decision is already made. */
	private int remainingPromptTicks()
	{
		Minecraft mc = Minecraft.getInstance();
		if(mc.player == null)
			return -1;
		MobEffectInstance instance = mc.player.getEffect(MSUMobEffects.TIME_LOOP_REWIND_PROMPT);
		return instance != null ? instance.getDuration() : -1;
	}
}
