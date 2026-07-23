package org.wilkretawesomesauce.minestuckuniverseported.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.network.MSUStrifeRequestPackets;
import org.wilkretawesomesauce.minestuckuniverseported.strife.KindAbstratus;
import org.wilkretawesomesauce.minestuckuniverseported.strife.StrifePortfolio;
import org.wilkretawesomesauce.minestuckuniverseported.strife.StrifePortfolioHandler;
import org.wilkretawesomesauce.minestuckuniverseported.strife.StrifeSpecibus;

import java.util.LinkedList;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code gui.GuiStrifeSwitcher} plus the strife-key handling
 * half of {@code client.MSUKeys} (the skill-key handling half stayed out of scope - not part of this
 * pass). Implements "Strife Allocate/Retrieve" and the quickswitcher HUD.
 * <p>
 * Behaviour, same as the original:
 * <ul>
 *     <li>Tap {@link MSUKeyMappings#strifeKey} while holding an unassigned plain item -> it's assigned as
 *     a weapon immediately (no HUD shown). This is the "Allocate" half.</li>
 *     <li>Hold {@link MSUKeyMappings#strifeKey} (or {@link MSUKeyMappings#swapOffhandStrifeKey}) otherwise
 *     -> the quickswitcher HUD appears, showing the current specibus' weapon deck (cycle with the
 *     selector keys or scroll wheel); releasing equips whichever's centered. This is the "Retrieve"
 *     half, and (with the swap key) the offhand-swap variant.</li>
 *     <li>Holding the strife key <b>while sneaking</b>, with the abstrata switcher unlocked, switches the
 *     HUD to cycle between specibi themselves instead of weapons within one - the quickswitcher proper.</li>
 * </ul>
 * Structural differences from the original: split across {@link ClientTickEvent.Post} (edge detection +
 * the immediate-allocate path, since it doesn't need to draw anything) and {@link RenderGuiEvent.Post}
 * (the HUD row itself), rather than the original's combined {@code RenderTickEvent} handler - modern
 * NeoForge doesn't offer an exact equivalent single event for both, so this is the closest faithful
 * split. The item "pop" scale animation from the original's custom {@code renderItem} helper isn't
 * ported - items just render flat via {@code guiGraphics.renderItem}.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class MSUStrifeSwitcherClient
{
	private static final ResourceLocation WIDGETS = ResourceLocation.fromNamespaceAndPath("minestuck", "textures/gui/icons.png");
	private static final String ICON_PATH = "textures/gui/strife_specibus/icons/";

	private static boolean showSwitcher = false;
	private static Boolean offhandMode = null;
	private static boolean strifeDown = false;

	private static int selSpecibus = -1;
	private static int selWeapon = 0;

	private static boolean prevLeftDown = false;
	private static boolean prevRightDown = false;

	private MSUStrifeSwitcherClient()
	{
	}

	@SubscribeEvent
	private static void onClientTick(ClientTickEvent.Post event)
	{
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if(player == null || mc.screen != null)
			return;

		boolean strifeKeyDown = MSUKeyMappings.strifeKey.isDown();
		boolean swapStrifeKeyDown = MSUKeyMappings.swapOffhandStrifeKey.isDown();

		boolean strifePressed = offhandMode == null ? (strifeKeyDown || swapStrifeKeyDown)
				: offhandMode ? swapStrifeKeyDown : strifeKeyDown;

		if(strifePressed)
		{
			if(offhandMode == null)
			{
				offhandMode = !(!swapStrifeKeyDown && strifeKeyDown);

				ItemStack mainHand = player.getMainHandItem();
				boolean mainAssigned = StrifePortfolioHandler.isHeldWeapon(player, mainHand);
				boolean offAssigned = StrifePortfolioHandler.isHeldWeapon(player, player.getOffhandItem());

				if(offhandMode || mainHand.isEmpty() || mainAssigned || offAssigned)
					showSwitcher = true;
				else if(!player.getData(MSUAttachments.STRIFE_PORTFOLIO).isArmed())
					PacketDistributor.sendToServer(new MSUStrifeRequestPackets.AssignHeldItem(InteractionHand.MAIN_HAND));
			}
		}
		else offhandMode = null;

		if(showSwitcher)
		{
			boolean rightDown = MSUKeyMappings.strifeSelectorRightKey.isDown();
			boolean leftDown = MSUKeyMappings.strifeSelectorLeftKey.isDown();
			int scroll = (rightDown && !prevRightDown ? 1 : 0) - (leftDown && !prevLeftDown ? 1 : 0);
			prevRightDown = rightDown;
			prevLeftDown = leftDown;
			if(scroll != 0)
				cycleSelection(player, -scroll);
		}
	}

	@SubscribeEvent
	private static void onMouseScroll(InputEvent.MouseScrollingEvent event)
	{
		Minecraft mc = Minecraft.getInstance();
		if(!showSwitcher || mc.player == null)
			return;

		cycleSelection(mc.player, (int) Math.signum(-event.getScrollDeltaY()));
		event.setCanceled(true);
	}

	private static void cycleSelection(LocalPlayer player, int direction)
	{
		if(direction == 0)
			return;

		StrifePortfolio cap = player.getData(MSUAttachments.STRIFE_PORTFOLIO);
		StrifeSpecibus[] nonEmpty = cap.getNonEmptyPortfolio();
		if(nonEmpty.length == 0)
			return;

		if(player.isShiftKeyDown() && canUseAbstrataSwitcher(cap))
		{
			StrifeSpecibus[] portfolio = cap.getPortfolio();
			if(selSpecibus < 0 || selSpecibus >= portfolio.length || portfolio[selSpecibus] == null)
			{
				selSpecibus = cap.getSpecibusIndex(nonEmpty[0]);
				return;
			}
			int i = indexOf(nonEmpty, portfolio[selSpecibus]);
			i = Math.floorMod(i + direction, nonEmpty.length);
			selSpecibus = cap.getSpecibusIndex(nonEmpty[i]);
		}
		else if(selSpecibus >= 0 && cap.getPortfolio()[selSpecibus] != null)
		{
			int deckSize = cap.getPortfolio()[selSpecibus].getContents().size();
			if(deckSize > 0)
				selWeapon = Math.floorMod(selWeapon + direction, deckSize);
		}
	}

	private static int indexOf(StrifeSpecibus[] array, StrifeSpecibus value)
	{
		for(int i = 0; i < array.length; i++)
			if(array[i] == value)
				return i;
		return 0;
	}

	private static boolean canUseAbstrataSwitcher(StrifePortfolio cap)
	{
		return cap.abstrataSwitcherUnlocked();
	}

	@SubscribeEvent
	private static void onRenderGui(RenderGuiEvent.Post event)
	{
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if(!showSwitcher || player == null)
			return;

		GuiGraphics guiGraphics = event.getGuiGraphics();
		int screenWidth = guiGraphics.guiWidth();
		int screenHeight = guiGraphics.guiHeight();

		StrifePortfolio cap = player.getData(MSUAttachments.STRIFE_PORTFOLIO);
		boolean isDown = offhandMode != null && offhandMode ? MSUKeyMappings.swapOffhandStrifeKey.isDown() : MSUKeyMappings.strifeKey.isDown();

		if(isDown != strifeDown && isDown)
		{
			selSpecibus = cap.getSelectedSpecibusIndex();
			selWeapon = cap.getSelectedWeaponIndex();
		}

		int selSpecibusIndex = selSpecibus;
		int selWeaponIndex = selWeapon;
		StrifeSpecibus[] portfolio = cap.getNonEmptyPortfolio();

		if(canUseAbstrataSwitcher(cap) && (selSpecibusIndex < 0 || selSpecibusIndex >= cap.getPortfolio().length
				|| cap.getPortfolio()[selSpecibusIndex] == null
				|| (!cap.getPortfolio()[selSpecibusIndex].getKindAbstratus().isFist() && cap.getPortfolio()[selSpecibusIndex].getContents().isEmpty())))
		{
			selSpecibusIndex = portfolio.length <= 0 ? -1 : cap.getSpecibusIndex(portfolio[0]);
			cap.setSelectedSpecibusIndex(selSpecibusIndex);
		}

		if(isDown != strifeDown)
		{
			strifeDown = isDown;
			if(!strifeDown)
			{
				showSwitcher = false;

				if(selSpecibusIndex >= 0)
				{
					InteractionHand hand = StrifePortfolioHandler.isHeldWeapon(player, player.getOffhandItem()) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
					if(offhandMode == null || !offhandMode)
					{
						cap.setSelectedWeaponIndex(selWeaponIndex);
						cap.setSelectedSpecibusIndex(selSpecibusIndex);
						PacketDistributor.sendToServer(new MSUStrifeRequestPackets.UpdateStrifeIndexes(selSpecibusIndex, selWeaponIndex));
					}
					if(!(player.isShiftKeyDown() && canUseAbstrataSwitcher(cap)))
					{
						if(offhandMode != null && offhandMode)
							PacketDistributor.sendToServer(new MSUStrifeRequestPackets.SwapOffhandStrife(selSpecibus, selWeapon));
						else
							PacketDistributor.sendToServer(new MSUStrifeRequestPackets.RetrieveStrife(cap.getSelectedWeaponIndex(), false, hand));
					}
				}
			}
		}

		if(portfolio.length <= 0 || selSpecibusIndex < 0)
			return;

		if(player.isShiftKeyDown() && canUseAbstrataSwitcher(cap))
			renderSpecibusRow(guiGraphics, mc, cap, portfolio, selSpecibusIndex, screenWidth, screenHeight);
		else
			renderWeaponRow(guiGraphics, mc, cap, selSpecibusIndex, selWeaponIndex, screenWidth, screenHeight);
	}

	private static void renderSpecibusRow(GuiGraphics guiGraphics, Minecraft mc, StrifePortfolio cap, StrifeSpecibus[] portfolio,
			int selSpecibusIndex, int screenWidth, int screenHeight)
	{
		int toDisplay = (int) Math.min(5, Math.ceil((portfolio.length - 1) / 2f) * 2);
		int centerIndex = indexOf(portfolio, cap.getPortfolio()[selSpecibusIndex]);

		for(int i = -(toDisplay / 2); i <= toDisplay / 2; i++)
		{
			int index = Math.floorMod(i + centerIndex, portfolio.length);
			StrifeSpecibus specibus = portfolio[index];
			if(specibus == null || specibus.getKindAbstratus() == null)
				continue;

			if(i == 0)
			{
				guiGraphics.blit(WIDGETS, 18 * i + screenWidth / 2 - 11, screenHeight * 3 / 4 - 3, 112, 0, 22, 22, 256, 256);
				String str = specibus.getDisplayName();
				guiGraphics.drawString(mc.font, str, screenWidth / 2 - mc.font.width(str) / 2, screenHeight * 3 / 4 - 14, 0x00AB54, true);
			}

			guiGraphics.blit(iconFor(specibus.getKindAbstratus()), 20 * i + screenWidth / 2 - 8, screenHeight * 3 / 4, 0, 0, 16, 16, 16, 16);
		}
	}

	private static void renderWeaponRow(GuiGraphics guiGraphics, Minecraft mc, StrifePortfolio cap, int selSpecibusIndex, int selWeaponIndex,
			int screenWidth, int screenHeight)
	{
		StrifeSpecibus selected = cap.getPortfolio()[selSpecibusIndex];
		if(selected == null)
			return;

		LinkedList<ItemStack> deck = selected.getContents();
		if(deck.isEmpty())
			return;

		int toDisplay = (int) Math.min(5, Math.ceil((deck.size() - 1) / 2f) * 2);

		for(int i = -(toDisplay / 2); i <= toDisplay / 2; i++)
		{
			int index = Math.floorMod(i + selWeaponIndex, deck.size());
			ItemStack stack = deck.get(index);
			if(stack == null)
				continue;

			if(i == 0)
			{
				boolean offhand = offhandMode != null && offhandMode;
				guiGraphics.blit(WIDGETS, screenWidth / 2 - 11, screenHeight * 3 / 4 - 3, offhand ? 134 : 112, 0, 22, 22, 256, 256);
				Component name = stack.getHoverName();
				guiGraphics.drawString(mc.font, name, screenWidth / 2 - mc.font.width(name) / 2, screenHeight * 3 / 4 - 14, 0x00AB54, true);
			}

			if(cap.isArmed() && offhandMode != null && offhandMode && index == cap.getSelectedWeaponIndex())
				guiGraphics.blit(WIDGETS, i * 20 + screenWidth / 2 - 11, screenHeight * 3 / 4 - 3, 156, 0, 22, 22, 256, 256);

			guiGraphics.renderItem(stack, 20 * i + screenWidth / 2 - 8, screenHeight * 3 / 4);
		}
	}

	private static ResourceLocation iconFor(KindAbstratus kind)
	{
		ResourceLocation name = kind.getRegistryName();
		return ResourceLocation.fromNamespaceAndPath(name.getNamespace(), ICON_PATH + name.getPath() + ".png");
	}
}
