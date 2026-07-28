package org.wilkretawesomesauce.minestuckuniverseported.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.inventory.TemporalSendificatorMenu;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.request.TimeRequest;

import java.util.List;
import java.util.Locale;

/**
 * A plain, texture-less GUI (fills instead of a custom background image, same tradeoff
 * {@code MSUKindSelectScreen}'s label rendering notes for its own untranslated pose-rotation gap) - this
 * project has no art asset for a new machine, and getting one is out of scope for the Time Request / Doom
 * System's first pass (see {@code CLAUDE.md}, same "known limitation, not an oversight" framing as this
 * project's other documented asset gaps like God Tier's missing worn-armor models).
 * <p>
 * Reads the open-request list directly from {@code this.minecraft.player}'s already-synced
 * {@code timeline.request.TimeRequestData} attachment rather than through {@link #menu} - see
 * {@link TemporalSendificatorMenu}'s own doc comment for why the menu deliberately doesn't carry it.
 */
public class TemporalSendificatorScreen extends AbstractContainerScreen<TemporalSendificatorMenu>
{
	public TemporalSendificatorScreen(TemporalSendificatorMenu menu, Inventory inventory, Component title)
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
		guiGraphics.fill(x + 79, y + 19, x + 97, y + 37, 0xFF8B8B8B);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
	{
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		renderTooltip(guiGraphics, mouseX, mouseY);

		List<TimeRequest> requests = this.minecraft.player.getData(MSUAttachments.TIME_REQUEST_DATA).getOpenRequests();
		int textY = topPos + 4;
		if(requests.isEmpty())
		{
			guiGraphics.drawString(font, Component.translatable("status.minestuckuniverseported.timeRequest.none_open"), leftPos + 4, textY, 0x404040, false);
			return;
		}

		for(TimeRequest request : requests)
		{
			Item item = BuiltInRegistries.ITEM.get(request.getItem());
			Component line = Component.translatable("status.minestuckuniverseported.timeRequest.open_line",
					Component.translatable(item.getDescriptionId()), String.format(Locale.ROOT, "%.1f", request.getDoomPoints()));
			guiGraphics.drawString(font, line, leftPos + 4, textY, 0x404040, false);
			textY += font.lineHeight + 1;
		}
	}
}
