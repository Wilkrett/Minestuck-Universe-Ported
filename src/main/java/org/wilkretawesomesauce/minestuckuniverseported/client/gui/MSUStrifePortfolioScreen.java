package org.wilkretawesomesauce.minestuckuniverseported.client.gui;

import com.mraof.minestuck.client.gui.playerStats.PlayerStatsScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.network.MSUStrifeRequestPackets;
import org.wilkretawesomesauce.minestuckuniverseported.strife.KindAbstratus;
import org.wilkretawesomesauce.minestuckuniverseported.strife.StrifePortfolio;
import org.wilkretawesomesauce.minestuckuniverseported.strife.StrifeSpecibus;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code gui.GuiStrifePortfolio}. Shows the player's strife
 * portfolio as a fan of cards; left-click a card to select its specibus, right-click to pull it back out
 * as a physical card.
 * <p>
 * Structural differences from the original, both required by the modern rendering/input APIs:
 * <ul>
 *     <li>1.12.2 read {@code Mouse.isButtonDown()} during rendering with a manual debounce flag. Modern
 *     screens are event-driven, so clicks are now handled in {@link #mouseClicked}, sharing the same
 *     card hit-test table ({@link #cardSlots()}) that rendering uses.</li>
 *     <li>{@code GL11.glScalef}/{@code GlStateManager} calls are replaced with
 *     {@code guiGraphics.pose().pushPose()/scale()/popPose()}, matching how Minestuck itself does
 *     GUI scaling (see {@code ReadableSburbCodeScreen}). Coordinates are still divided by the target
 *     scale before drawing, exactly as the original did, so the on-screen result should match.</li>
 *     <li>Text is drawn with the default Minecraft font rather than the original's custom
 *     {@code MSUFontRenderer.fontSpecibus} bitmap font, which hasn't been ported.</li>
 * </ul>
 * Not yet ported: the data-checker corner icon (needs {@code MinestuckConfig.dataCheckerAccess} and the
 * data checker screen itself, neither ported).
 */
public class MSUStrifePortfolioScreen extends PlayerStatsScreen
{
	public static final String TITLE = "minestuck.strife_specibus";

	private static final ResourceLocation TEX_PORTFOLIO = tex("strife_portfolio");
	private static final ResourceLocation TEX_TABS = tex("portfolio_tabs");
	private static final ResourceLocation TEX_BG = tex("portfolio_bg");
	private static final ResourceLocation TEX_CARD = tex("strife_card");
	private static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath("minestuck", "textures/gui/icons.png");
	private static final String ICON_PATH = "textures/gui/strife_specibus/icons/";

	private static ResourceLocation tex(String name)
	{
		return Minestuckuniverseported.id("textures/gui/strife_specibus/" + name + ".png");
	}

	private static final float CARD_SCALE = 0.25f;

	/** (cardX, cardY, portfolioIndex) fan positions, ported verbatim from the original's draw call sequence. */
	private static final int[][] FAN_POSITIONS = {
			{11, 9, 6}, {59, 7, 7}, {12, 50, 9}, {107, 7, 8}, {56, 40, 0},
			{107, 33, 5}, {56, 80, 4}, {159, 25, 3}, {107, 77, 1}, {159, 69, 2}
	};
	private static final int ACTIVE_X = 10, ACTIVE_Y = 85;

	private StrifeSpecibus[] portfolio = new StrifeSpecibus[0];
	private int activeSpecibus = -1;
	private int hoveredIndex = -1;

	public MSUStrifePortfolioScreen()
	{
		super(Component.translatable(TITLE));
		guiWidth = 226;
		guiHeight = 188;
	}

	private record CardSlot(int index, int x, int y, boolean isActiveAnchor)
	{
	}

	private List<CardSlot> cardSlots()
	{
		List<CardSlot> slots = new ArrayList<>();
		for(int[] pos : FAN_POSITIONS)
			if(pos[2] != activeSpecibus)
				slots.add(new CardSlot(pos[2], pos[0], pos[1], false));
		slots.add(new CardSlot(activeSpecibus, ACTIVE_X, ACTIVE_Y, true));
		return slots;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
	{
		if(minecraft == null || minecraft.player == null)
			return;

		portfolio = minecraft.player.getData(MSUAttachments.STRIFE_PORTFOLIO).getPortfolio();
		activeSpecibus = minecraft.player.getData(MSUAttachments.STRIFE_PORTFOLIO).getSelectedSpecibusIndex();
		hoveredIndex = -1;

		super.render(guiGraphics, mouseX, mouseY, partialTicks);

		guiGraphics.blit(TEX_BG, xOffset, yOffset, 0, 0, guiWidth, guiHeight);

		guiGraphics.blit(TEX_TABS, xOffset, yOffset, 20, 58, 98, 94);
		drawCard(guiGraphics, 11, 9, 6);
		guiGraphics.blit(TEX_TABS, xOffset + 2, yOffset + 6, 4, 44, 132, 120);

		drawCard(guiGraphics, 59, 7, 7);
		drawCard(guiGraphics, 12, 50, 9);
		guiGraphics.blit(TEX_TABS, xOffset + 21, yOffset + 4, 0, 18, 152, 134);

		drawCard(guiGraphics, 107, 7, 8);
		drawCard(guiGraphics, 56, 40, 0);
		guiGraphics.blit(TEX_TABS, xOffset + 45, yOffset + 10, 0, 0, 164, 152);
		guiGraphics.blit(TEX_TABS, xOffset + 173, yOffset + 6, 124, 0, 7, 4);

		drawCard(guiGraphics, 107, 33, 5);
		drawCard(guiGraphics, 56, 80, 4);
		guiGraphics.blit(TEX_TABS, xOffset + 81, yOffset + 28, 0, 8, 137, 120);
		guiGraphics.blit(TEX_TABS, xOffset + 218, yOffset + 46, 142, 22, 2, 10);

		drawCard(guiGraphics, 159, 25, 3);
		drawCard(guiGraphics, 107, 77, 1);
		guiGraphics.blit(TEX_TABS, xOffset + 124, yOffset + 52, 0, 32, 96, 96);

		drawCard(guiGraphics, 159, 69, 2);
		guiGraphics.blit(TEX_TABS, xOffset + 168, yOffset + 96, 204, 0, 52, 52);

		if(activeSpecibus >= 0 && activeSpecibus < portfolio.length)
			drawCard(guiGraphics, ACTIVE_X, ACTIVE_Y, activeSpecibus);

		drawTabs(guiGraphics);
		guiGraphics.blit(TEX_PORTFOLIO, xOffset, yOffset, 0, 0, guiWidth, guiHeight);
		drawActiveTabAndOther(guiGraphics, mouseX, mouseY);

		// bottom row: one small kind icon per portfolio slot
		for(int i = 0; i < portfolio.length; i++)
		{
			StrifeSpecibus specibus = portfolio[i];
			if(specibus == null || specibus.getKindAbstratus() == null)
				continue;
			ResourceLocation icon = iconFor(specibus.getKindAbstratus());
			guiGraphics.blit(icon, xOffset + 23 + 20 * i, yOffset + 166, 0, 0, 16, 16, 16, 16);
		}

		// hover tooltip + tracking for click handling
		for(CardSlot slot : cardSlots())
		{
			if(slot.index() < 0 || slot.index() >= portfolio.length)
				continue;
			StrifeSpecibus specibus = portfolio[slot.index()];
			if(specibus == null)
				continue;

			int x1 = xOffset + slot.x(), y1 = yOffset + slot.y();
			int xs = (int) (200 * CARD_SCALE), ys = (int) (256 * CARD_SCALE);
			if(isPointInRegion(x1, y1, xs, ys, mouseX, mouseY))
			{
				hoveredIndex = slot.index();
				if(!specibus.getDisplayName().isEmpty())
					guiGraphics.renderTooltip(font, Component.literal(specibus.getDisplayName()), mouseX, mouseY);
			}
		}
	}

	private ResourceLocation iconFor(KindAbstratus kind)
	{
		ResourceLocation name = kind.getRegistryName();
		return ResourceLocation.fromNamespaceAndPath(name.getNamespace(), ICON_PATH + name.getPath() + ".png");
	}

	/**
	 * Mirrors the original's {@code x/scale + offset} coordinate math exactly: under a pose scaled by
	 * {@code scale}, this is the pre-scale coordinate that lands at final screen position
	 * {@code anchor + offset*scale}.
	 * <p>
	 * Getting this wrong is exactly what caused the "kind icon is way off-center" bug: an earlier version
	 * multiplied the offset by {@code CARD_SCALE} instead of by the scale actually active for that
	 * specific draw (e.g. {@code iconScale}), which for a 2.5x-smaller icon scale put the icon roughly
	 * 2.5x further from the card than intended.
	 */
	private static int scaledCoord(int anchor, float scale, int offset)
	{
		return Math.round(anchor / scale) + offset;
	}

	private void drawCard(GuiGraphics guiGraphics, int cardX, int cardY, int index)
	{
		if(index < 0 || index >= portfolio.length)
			return;
		StrifeSpecibus specibus = portfolio[index];
		if(specibus == null)
			return;

		boolean highlighted = index == hoveredIndex;
		int x = xOffset + cardX - (highlighted ? 5 : 0);
		int y = yOffset + cardY - (highlighted ? 5 : 0);

		// card background
		withScale(guiGraphics, CARD_SCALE, () -> guiGraphics.blit(TEX_CARD, scaledCoord(x, CARD_SCALE, 0), scaledCoord(y, CARD_SCALE, 0), 28, 0, 200, 256));

		// kind icon
		KindAbstratus kind = specibus.getKindAbstratus();
		if(kind != null)
		{
			ResourceLocation icon = iconFor(kind);
			float iconScale = CARD_SCALE / 2.5f;
			withScale(guiGraphics, iconScale, () -> guiGraphics.blit(icon, scaledCoord(x, iconScale, 57), scaledCoord(y, iconScale, 148), 0, 0, 256, 256));
		}

		// "strife specibus" header
		float headerScale = CARD_SCALE * 2;
		withScale(guiGraphics, headerScale, () ->
				guiGraphics.drawString(font, Component.translatable("gui.strifePortfolio.specibus"), scaledCoord(x, headerScale, 5), scaledCoord(y, headerScale, 4), 0xFF00E371, false));

		// kind name, right-aligned above the icon
		String displayName = specibus.getDisplayNameForCard();
		int fontWidth = font.width(displayName);
		float nameScale = CARD_SCALE * 2.5f;
		withScale(guiGraphics, nameScale, () ->
				guiGraphics.drawString(font, displayName, scaledCoord(x, nameScale, 52 - fontWidth), scaledCoord(y, nameScale, 91), 0xFF00E371, false));

		// "sylladex :: strife deck" footer
		withScale(guiGraphics, CARD_SCALE, () ->
				guiGraphics.drawString(font, Component.translatable("gui.strifePortfolio.deck"), scaledCoord(x, CARD_SCALE, 16), scaledCoord(y, CARD_SCALE, 179), 0xFFFFFFFF, false));

		// deck contents
		List<ItemStack> items = specibus.getContents();
		int deckXPos = (int) (94 - 23 * (Math.min(items.size(), 5) / 2f));
		for(int n = 0; n < items.size(); n++)
		{
			int ixOff = (int) (deckXPos + (n % 5) * 23) - (int) (n / 5f);
			int iyOff = 193 - (int) (n / 5f) * 2;

			// slot frame
			withScale(guiGraphics, CARD_SCALE, () -> guiGraphics.blit(ICONS, scaledCoord(x, CARD_SCALE, ixOff), scaledCoord(y, CARD_SCALE, iyOff), 0, 122, 21, 26));

			if(n >= items.size() - 5)
			{
				// item icon, also shrunk to CARD_SCALE so it actually fits its slot frame instead of
				// rendering at native 16x16 size (which looked comically oversized against a ~5px slot)
				ItemStack stack = items.get(n);
				withScale(guiGraphics, CARD_SCALE, () ->
				{
					int ix = scaledCoord(x, CARD_SCALE, ixOff + 2);
					int iy = scaledCoord(y, CARD_SCALE, iyOff + 4);
					guiGraphics.renderItem(stack, ix, iy);
					guiGraphics.renderItemDecorations(font, stack, ix, iy);
				});
			}
		}
	}

	private interface DrawCall
	{
		void draw();
	}

	private void withScale(GuiGraphics guiGraphics, float scale, DrawCall draw)
	{
		guiGraphics.pose().pushPose();
		guiGraphics.pose().scale(scale, scale, scale);
		draw.draw();
		guiGraphics.pose().popPose();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		if(minecraft != null && minecraft.player != null && (button == 0 || button == 1))
		{
			for(CardSlot slot : cardSlots())
			{
				if(slot.index() < 0 || slot.index() >= portfolio.length || portfolio[slot.index()] == null)
					continue;

				int x1 = xOffset + slot.x(), y1 = yOffset + slot.y();
				int xs = (int) (200 * CARD_SCALE), ys = (int) (256 * CARD_SCALE);
				if(isPointInRegion(x1, y1, xs, ys, (int) mouseX, (int) mouseY))
				{
					if(button == 1)
						PacketDistributor.sendToServer(new MSUStrifeRequestPackets.RetrieveStrife(slot.index(), true, InteractionHand.MAIN_HAND));
					else
						PacketDistributor.sendToServer(new MSUStrifeRequestPackets.SetActiveStrife(slot.index(), true));
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}
}
