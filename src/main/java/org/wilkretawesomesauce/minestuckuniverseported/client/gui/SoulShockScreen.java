package org.wilkretawesomesauce.minestuckuniverseported.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code gui.GuiSoulStun} - a helpless "you are stunned" screen
 * forced open on whoever is currently soul-shocked ({@code abilitech.heroAspect.heart.TechSoulStun}),
 * which they can't back out of through the normal Escape-closes-the-screen behavior.
 * <p>
 * The original polled the Escape key directly each client tick to specifically let it through to the
 * pause menu while blocking everything else. Overriding {@link #onClose()} is the more idiomatic modern
 * equivalent - vanilla already calls it whenever Escape is pressed (since {@link #shouldCloseOnEsc()}
 * still returns {@code true}, unchanged from {@link Screen}'s own default), so redirecting to the pause
 * screen there achieves the same "Escape still gets you to the pause menu, nothing else does" result
 * without needing to hand-poll raw key state.
 */
public class SoulShockScreen extends Screen
{
	public SoulShockScreen()
	{
		super(Component.translatable("gui.soulStun.title"));
	}

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
		guiGraphics.drawCenteredString(font, title, width / 2, height / 2 - 10, 0xFFB745);
	}
}
