package org.wilkretawesomesauce.minestuckuniverseported.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.network.MSUStrifeRequestPackets;
import org.wilkretawesomesauce.minestuckuniverseported.strife.KindAbstratus;
import org.wilkretawesomesauce.minestuckuniverseported.strife.MSUKindAbstrataRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code gui.GuiStrifeCard}: a scrollable list for picking a
 * kind to stamp onto a blank strife card held in {@code hand}.
 * <p>
 * Simplified from the original: 1.12.2 supported showing partial rows at the top/bottom of the list
 * while scrolling smoothly between exact item boundaries (the {@code extraLines} logic). This version
 * scrolls a whole row at a time, which is a lot simpler and loses very little in practice for a list
 * this size. Mouse handling is event-driven ({@link #mouseClicked}/{@link #mouseScrolled}) rather than
 * polled during rendering, same reasoning as {@link MSUStrifePortfolioScreen}.
 */
public class MSUKindSelectScreen extends Screen
{
	private static final ResourceLocation TEX_SELECTOR = Minestuckuniverseported.id("textures/gui/strife_specibus/strife_selector.png");
	private static final int GUI_WIDTH = 147, GUI_HEIGHT = 185;
	private static final int COLUMN_WIDTH = 50, COLUMNS = 2, ROWS_VISIBLE = 13;

	private final Player player;
	private final InteractionHand hand;
	private final List<KindAbstratus> kinds = new ArrayList<>();

	private int xOffset, yOffset;
	private int scrollRow = 0;

	public MSUKindSelectScreen(Player player, InteractionHand hand)
	{
		super(Component.translatable("gui.strifeCard.label"));
		this.player = player;
		this.hand = hand;
		for(KindAbstratus kind : MSUKindAbstrataRegistry.getAll())
			if(kind.canSelect())
				kinds.add(kind);
	}

	/**
	 * Entry point for common code (e.g. {@code items.StrifeCardItem}) to open this screen without
	 * referencing {@link Minecraft}/{@link Screen} directly itself - see
	 * {@code client.gui.MSUAbilitechScreen#open()}'s own doc comment for why a bare
	 * {@code Minecraft.getInstance().setScreen(...)} call inlined into a common class used to crash a
	 * dedicated server outright.
	 */
	public static void open(Player player, InteractionHand hand)
	{
		Minecraft.getInstance().setScreen(new MSUKindSelectScreen(player, hand));
	}

	@Override
	protected void init()
	{
		super.init();
		xOffset = (width - GUI_WIDTH) / 2;
		yOffset = (height - GUI_HEIGHT) / 2;
	}

	@Override
	public boolean isPauseScreen()
	{
		return false;
	}

	private int maxScrollRow()
	{
		int rows = (int) Math.ceil(kinds.size() / (float) COLUMNS);
		return Math.max(0, rows - ROWS_VISIBLE);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
	{
		super.render(guiGraphics, mouseX, mouseY, partialTicks);

		RenderSystem.setShaderColor(1, 1, 1, 1);
		guiGraphics.fill(xOffset + 27, yOffset + 23, xOffset + 127, yOffset + 178, 0xFF000000);

		int listX = xOffset + 16, listY = yOffset + 59;
		int firstIndex = scrollRow * COLUMNS;

		for(int i = 0; i < kinds.size() - firstIndex && i < ROWS_VISIBLE * COLUMNS; i++)
		{
			KindAbstratus kind = kinds.get(firstIndex + i);
			String name = kind.getDisplayName().getString();

			int col = i % COLUMNS, row = i / COLUMNS;
			int x = listX + COLUMN_WIDTH * col;
			int y = listY + font.lineHeight * row;

			boolean hovered = isPointInRegion(x, y, COLUMN_WIDTH, font.lineHeight, mouseX, mouseY);
			if(hovered)
				guiGraphics.fill(x, y, x + COLUMN_WIDTH, y + font.lineHeight, 0xFFAFAFAF);

			int textX = x + COLUMN_WIDTH - font.width(name) - 2;
			guiGraphics.drawString(font, name, textX, y + 2, hovered ? 0x000000 : 0xFFFFFF, false);
		}

		int maxScroll = maxScrollRow();
		int scrollY = maxScroll <= 0 ? 0 : (int) (140f * scrollRow / maxScroll);
		guiGraphics.blit(TEX_SELECTOR, xOffset, yOffset, 0, 0, GUI_WIDTH, GUI_HEIGHT);
		guiGraphics.blit(TEX_SELECTOR, xOffset + 128, yOffset + 23 + scrollY, maxScroll > 0 ? 232 : 244, 0, 12, 15);

		// Simplified from the original, which drew this label rotated 90 degrees along the left edge of
		// the selector frame. Not confident enough in the exact pose-rotation call to guess it blind, so
		// this is a plain horizontal label under the frame instead - purely cosmetic, easy to upgrade later.
		String label = title.getString();
		guiGraphics.drawString(font, label, xOffset + (GUI_WIDTH - font.width(label)) / 2, yOffset + GUI_HEIGHT + 4, 0xFFFFFF, false);
	}

	private boolean isPointInRegion(int regionX, int regionY, int regionWidth, int regionHeight, int pointX, int pointY)
	{
		return pointX >= regionX && pointX < regionX + regionWidth && pointY >= regionY && pointY < regionY + regionHeight;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		if(button == 0)
		{
			int listX = xOffset + 16, listY = yOffset + 59;
			int firstIndex = scrollRow * COLUMNS;

			for(int i = 0; i < kinds.size() - firstIndex && i < ROWS_VISIBLE * COLUMNS; i++)
			{
				int col = i % COLUMNS, row = i / COLUMNS;
				int x = listX + COLUMN_WIDTH * col;
				int y = listY + font.lineHeight * row;

				if(isPointInRegion(x, y, COLUMN_WIDTH, font.lineHeight, (int) mouseX, (int) mouseY))
				{
					KindAbstratus kind = kinds.get(firstIndex + i);
					PacketDistributor.sendToServer(new MSUStrifeRequestPackets.AssignStrifeKind(kind.getRegistryName(), hand));
					onClose();
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
	{
		int maxScroll = maxScrollRow();
		if(maxScroll > 0)
		{
			scrollRow = Math.max(0, Math.min(maxScroll, scrollRow - (int) Math.signum(scrollY)));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}
}
