package org.wilkretawesomesauce.minestuckuniverseported.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.gui.itemvoid.ItemVoidMenu;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code gui.GuiItemVoid}. Uses the real imported
 * {@code textures/gui/container/item_void.png} backdrop - a prior version of this class predated that art
 * being imported and fell back to a plain flat-fill background; that stand-in is gone now that the real
 * texture exists (same legacy-256 UV convention as this project's other ported GUIs, see
 * {@code MSUAbilitechScreen}/{@code SkillShopScreen}).
 */
public class ItemVoidScreen extends AbstractContainerScreen<ItemVoidMenu>
{
	private static final ResourceLocation TEXTURE = Minestuckuniverseported.id("textures/gui/container/item_void.png");

	public ItemVoidScreen(ItemVoidMenu menu, Inventory inventory, Component title)
	{
		super(menu, inventory, title);
		this.imageWidth = 176;
		this.imageHeight = 176;
		this.inventoryLabelY = this.imageHeight - 94;
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY)
	{
		guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
	{
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		renderTooltip(guiGraphics, mouseX, mouseY);
	}
}
