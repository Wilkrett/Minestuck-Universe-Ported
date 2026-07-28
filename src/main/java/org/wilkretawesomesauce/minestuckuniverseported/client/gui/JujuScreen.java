package org.wilkretawesomesauce.minestuckuniverseported.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.wilkretawesomesauce.minestuckuniverseported.juju.JujuMenu;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code gui.captchalogue.JujuGuiHandler} - see
 * {@code juju.JujuMenu}'s own doc comment for why this is a new real menu/screen rather than Minestuck's
 * own internal sylladex screen classes. Plain, texture-less GUI, same asset-gap tradeoff already documented
 * on {@code TemporalSendificatorScreen}/{@code ItemVoidScreen}.
 */
public class JujuScreen extends AbstractContainerScreen<JujuMenu>
{
	public JujuScreen(JujuMenu menu, Inventory inventory, Component title)
	{
		super(menu, inventory, title);
		this.imageWidth = 176;
		this.imageHeight = 166;
		this.inventoryLabelY = this.imageHeight - 94;
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY)
	{
		int x = leftPos, y = topPos;
		guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
		guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
	{
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		renderTooltip(guiGraphics, mouseX, mouseY);
	}
}
