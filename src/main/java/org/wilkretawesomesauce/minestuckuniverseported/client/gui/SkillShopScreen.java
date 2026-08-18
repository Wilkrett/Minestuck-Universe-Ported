package org.wilkretawesomesauce.minestuckuniverseported.client.gui;

import com.mraof.minestuck.player.ClientPlayerData;
import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.util.*;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.Abilitech;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;
import org.wilkretawesomesauce.minestuckuniverseported.network.SkillShopRequestPackets;

import java.util.ArrayList;
import java.util.List;

/**
 * Real port of MinestuckUniverse (1.12.2)'s {@code gui.GuiSkillShop} - opened via
 * {@code command.SkillShopCommand} (itself reached from a real Consort dialogue response, see that
 * command class's own doc comment for why a command is the real trigger point instead of a custom
 * dialogue {@code Trigger} - Minestuck's real {@code Trigger} interface turned out to be sealed).
 * Uses the real imported {@code textures/gui/skill_shop.png} backdrop.
 * <p>
 * <b>Real scrollbar art, corrected from an earlier wrong assumption</b>: an earlier pass here claimed the
 * original's scrollbar UV regions weren't available and drew plain filled rectangles instead - wrong, the
 * real thumb art was in {@code textures/gui/skill_shop.png} all along (a real user catch), just never
 * looked for below the main 240x162 panel region. The real original ({@code gui.GuiSkillShop}, read
 * directly, not guessed) draws a 10x15 thumb at UV {@code (0,241)} (scrollable) or {@code (10,241)}
 * (nothing to scroll, a darker/disabled variant) for <i>both</i> the tech list and description panels -
 * {@link #renderList}/{@link #renderDescription} now do the same. The original's real
 * {@code descBoxHeight}/{@code descBoxWidth} (90/100) and description text origin
 * ({@code xOffset+119, yOffset+56}) are also now matched exactly ({@link #DESC_Y} was 36, a made-up value
 * that put text partly under the icon - it's 56 for real). Real vanilla {@link Button} widgets are still
 * used for the Buy button itself (no original screenshot of that specific element's own skin to match), and
 * scroll position is tracked as an integer line/row offset here rather than the original's {@code float}
 * 0-1 fraction - a real behavioral simplification, not a visual one; the rendered result matches.
 * <p>
 * <b>Real hardening over the original</b>: purchases are server-authoritative here (see
 * {@code network.SkillShopRequestPackets}) - this screen only ever sends a purchase <i>request</i> and
 * waits for the resulting {@code AbilitechLoadoutSyncPacket} to reflect whether it actually succeeded,
 * rather than assuming success and mutating local state immediately the way the original's own
 * client-predicted purchase flow did.
 * <p>
 * <b>Known gap</b>: only lists {@link Abilitech}s - the real {@code Badge} item hierarchy (master
 * badges, class badges, sacrifice-gated badges) isn't built yet, so it can't appear here yet either.
 * <p>
 * <b>Description panel scrolling, real fix over an unclipped-overflow bug</b>: {@link #renderDescription}
 * used to draw name/tags/unlock-requirement/tooltip sequentially with no height bound at all - for any
 * tech with enough description text, it silently ran off the bottom of the panel and into the game world
 * behind the GUI (a real bug, caught from a live screenshot). Fixed the same way
 * {@code MSUAbilitechScreen}'s own description panel already does it (see that class's own doc comment):
 * flatten everything into one combined {@link DescLine} list, scissor-clip to a fixed
 * {@link #DESC_HEIGHT}, and scroll via {@link #descScrollPos} (mouse wheel over the panel, reset on
 * selection change) - plus the real scrollbar thumb art described above, not a placeholder.
 */
public class SkillShopScreen extends Screen
{
	private static final ResourceLocation TEXTURE = Minestuckuniverseported.id("textures/gui/skill_shop.png");
	private static final int GUI_WIDTH = 240, GUI_HEIGHT = 162;
	private static final int LIST_X = 8, LIST_Y = 28, LIST_WIDTH = 90, LIST_HEIGHT = 100;
	private static final int DESC_X = 119, DESC_Y = 56, DESC_WIDTH = 100, DESC_HEIGHT = 90;
	private static final int ICON_X = 151, ICON_Y = 5, ICON_SIZE = 48;

	// Real scrollbar thumb art (gui.GuiSkillShop, read directly) - a 10x15 pair sitting just below the
	// main 240x162 panel in the same texture: UV (0,241) when there's something to scroll, (10,241) - a
	// darker/disabled look - when there isn't. Both list and description scrollbars share this same art.
	private static final int SCROLLBAR_WIDTH = 10, SCROLLBAR_HEIGHT = 15;
	private static final int SCROLLBAR_TEX_V = 241;
	private static final int SCROLLBAR_TEX_U_ACTIVE = 0, SCROLLBAR_TEX_U_INACTIVE = 10;
	private static final int LIST_SCROLLBAR_X = 99, LIST_SCROLLBAR_TRAVEL = LIST_HEIGHT - SCROLLBAR_HEIGHT;
	private static final int DESC_SCROLLBAR_X = 222, DESC_SCROLLBAR_TRAVEL = DESC_HEIGHT - SCROLLBAR_HEIGHT;

	private int guiLeft, guiTop;
	private final List<Abilitech> available = new ArrayList<>();
	private Abilitech selected;
	private int scrollOffset;
	private int descScrollPos;
	private Button buyButton;

	public SkillShopScreen()
	{
		super(Component.translatable("gui.minestuckuniverseported.skill_shop.title"));
	}

	@Override
	protected void init()
	{
		super.init();
		guiLeft = width / 2 - GUI_WIDTH / 2;
		guiTop = height / 2 - GUI_HEIGHT / 2;

		GodTierData godTier = minecraft.player.getData(MSUAttachments.GOD_TIER);
		available.clear();
		for(Abilitech tech : MSUAbilitechRegistry.getAll())
			if(!godTier.isUnlocked(tech) && tech.canAppearOnList(minecraft.player.level(), minecraft.player))
				available.add(tech);

		// Wide enough for the longer "Can't afford" state label, not just "Buy" - AbstractWidget's own
		// text rendering scissors to the button bounds with no ellipsis, so a too-narrow button silently
		// chops the label mid-word instead of overflowing or truncating cleanly.
		buyButton = addRenderableWidget(Button.builder(Component.translatable("gui.minestuckuniverseported.skill_shop.buy"), b -> buy())
				.bounds(guiLeft + 16, guiTop + 135, 90, 16)
				.build());
		updateBuyButton();
	}

	private void updateBuyButton()
	{
		buyButton.active = selected != null && selected.canUnlock(minecraft.player.level(), minecraft.player);
		buyButton.setMessage(Component.translatable(buyButton.active
				? "gui.minestuckuniverseported.skill_shop.buy"
				: "gui.minestuckuniverseported.skill_shop.cant_afford"));
	}

	private void buy()
	{
		if(selected == null)
			return;
		PacketDistributor.sendToServer(new SkillShopRequestPackets.Purchase(selected.getId()));
	}

	@Override
	public boolean isPauseScreen()
	{
		return false;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
	{
		super.render(guiGraphics, mouseX, mouseY, partialTicks);

		guiGraphics.blit(TEXTURE, guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);
		guiGraphics.drawString(font, title, guiLeft + 8, guiTop + 6, 0xFFFFFF, true);

		String balance = Component.translatable("gui.minestuckuniverseported.skill_shop.balance", ClientPlayerData.getBoondollars()).getString();
		guiGraphics.drawString(font, balance, guiLeft + 8, guiTop + 16, 0xFFD700, true);

		renderList(guiGraphics, mouseX, mouseY);

		if(selected != null)
			renderDescription(guiGraphics);

		updateBuyButton();
	}

	private void renderList(GuiGraphics guiGraphics, int mouseX, int mouseY)
	{
		int rowHeight = font.lineHeight + 2;
		int visibleRows = LIST_HEIGHT / rowHeight;
		int maxScroll = Math.max(0, available.size() - visibleRows);
		scrollOffset = Math.min(scrollOffset, maxScroll);

		guiGraphics.enableScissor(guiLeft + LIST_X, guiTop + LIST_Y, guiLeft + LIST_X + LIST_WIDTH, guiTop + LIST_Y + LIST_HEIGHT);
		for(int i = 0; i < visibleRows && scrollOffset + i < available.size(); i++)
		{
			Abilitech tech = available.get(scrollOffset + i);
			int rowY = guiTop + LIST_Y + i * rowHeight;

			if(tech == selected)
				guiGraphics.fill(guiLeft + LIST_X, rowY, guiLeft + LIST_X + LIST_WIDTH, rowY + rowHeight, 0x8000FF00);
			else if(mouseX >= guiLeft + LIST_X && mouseX < guiLeft + LIST_X + LIST_WIDTH && mouseY >= rowY && mouseY < rowY + rowHeight)
				guiGraphics.fill(guiLeft + LIST_X, rowY, guiLeft + LIST_X + LIST_WIDTH, rowY + rowHeight, 0x40FFFFFF);

			guiGraphics.drawString(font, tech.getDisplayName(), guiLeft + LIST_X + 2, rowY + 1, 0xFFFFFF, false);
		}
		guiGraphics.disableScissor();

		int barY = guiTop + LIST_Y + (maxScroll > 0 ? LIST_SCROLLBAR_TRAVEL * scrollOffset / maxScroll : 0);
		int barU = maxScroll > 0 ? SCROLLBAR_TEX_U_ACTIVE : SCROLLBAR_TEX_U_INACTIVE;
		guiGraphics.blit(TEXTURE, guiLeft + LIST_SCROLLBAR_X, barY, barU, SCROLLBAR_TEX_V, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT);
	}

	/**
	 * Name + tags + unlock requirement + tooltip, flattened into one combined scrollable line list -
	 * matches {@code MSUAbilitechScreen}'s own established real-scrolling pattern (see that class's own
	 * doc comment) rather than the unclipped, unbounded sequential draw this replaced, which silently ran
	 * text off the bottom of the panel and into the game world behind it for any tech with a long enough
	 * description (a real bug, caught from a live screenshot).
	 */
	private List<DescLine> buildDescriptionLines()
	{
		List<DescLine> lines = new ArrayList<>();

		for(FormattedCharSequence line : font.split(selected.getDisplayName(), DESC_WIDTH))
			lines.add(new DescLine(line, 0xFFFFFF));

		if(selected instanceof TechHeroAspect heroAspectTech)
		{
			EnumAspect aspect = heroAspectTech.getHeroAspect();
			Component aspectTag = Component.literal("[").append(aspect.asTextComponent()).append(Component.literal("]"));
			lines.add(new DescLine(aspectTag.getVisualOrderText(), AspectColorHandler.get(aspect)[0]));

			MSUTechType techType = heroAspectTech.getTechType();
			Component typeTag = Component.literal("[").append(Component.translatable(techType.unloc)).append(Component.literal("]"));
			lines.add(new DescLine(typeTag.getVisualOrderText(), techType.color));

			// Purely descriptive classpect "flavor" tags - see TechHeroAspect's own flavor-tagging
			// constructor doc comment. Empty for every tech that doesn't opt in. Mirrors
			// client.gui.MSUAbilitechScreen's own identical branch.
			for(com.mraof.minestuck.player.EnumClass flavorClass : heroAspectTech.getFlavorClasses())
			{
				Component flavorTag = Component.literal("[")
						.append(Component.translatable(MSUHeroClass.from(flavorClass).unloc))
						.append(Component.literal("]"));
				int[] flavorColor = ClasspectColorHandler.get(flavorClass);
				lines.add(new DescLine(flavorTag.getVisualOrderText(), flavorColor != null && flavorColor.length > 0 ? flavorColor[0] : 0xFFFFFF));
			}
		}
		// Real bug fix: this branch was entirely missing - heroClass techs fell through to the generic
		// selected.getTags() loop below, which only ever produces TechHeroClass#getTags()'s raw "@PRINCE@"
		// internal marker string, never translated - shown literally as "[@PRINCE@]" in a live screenshot.
		// Mirrors client.gui.MSUAbilitechScreen's own real TechHeroClass tag branch exactly.
		else if(selected instanceof TechHeroClass heroClassTech)
		{
			Component classTag = Component.literal("[").append(Component.translatable(heroClassTech.getHeroClass().unloc)).append(Component.literal("]"));
			int[] classColor = ClasspectColorHandler.get(heroClassTech.getRealHeroClass());
			lines.add(new DescLine(classTag.getVisualOrderText(), classColor != null && classColor.length > 0 ? classColor[0] : 0xFFFFFF));

			for(MSUTechType type : heroClassTech.getTechTypes())
			{
				Component typeTag = Component.literal("[").append(Component.translatable(type.unloc)).append(Component.literal("]"));
				lines.add(new DescLine(typeTag.getVisualOrderText(), type.color));
			}
		}

		lines.add(DescLine.BLANK);
		for(FormattedCharSequence line : font.split(selected.getUnlockRequirements(), DESC_WIDTH))
			lines.add(new DescLine(line, 0xFFD700));
		lines.add(DescLine.BLANK);

		for(FormattedCharSequence line : font.split(selected.getDisplayTooltip(), DESC_WIDTH))
			lines.add(new DescLine(line, 0xCCCCCC));

		return lines;
	}

	private void renderDescription(GuiGraphics guiGraphics)
	{
		int boxX = guiLeft + DESC_X, boxY = guiTop + DESC_Y;

		guiGraphics.blit(iconFor(selected), guiLeft + ICON_X, guiTop + ICON_Y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

		List<DescLine> lines = buildDescriptionLines();
		int visibleLines = Math.max(1, DESC_HEIGHT / font.lineHeight);
		int maxScroll = Math.max(0, lines.size() - visibleLines);
		descScrollPos = Math.min(descScrollPos, maxScroll);

		guiGraphics.enableScissor(boxX, boxY, boxX + DESC_WIDTH, boxY + DESC_HEIGHT);
		int y = boxY;
		for(int i = descScrollPos; i < lines.size() && i < descScrollPos + visibleLines; i++)
		{
			guiGraphics.drawString(font, lines.get(i).text(), boxX, y, lines.get(i).color(), false);
			y += font.lineHeight;
		}
		guiGraphics.disableScissor();

		int barY = boxY + (maxScroll > 0 ? DESC_SCROLLBAR_TRAVEL * descScrollPos / maxScroll : 0);
		int barU = maxScroll > 0 ? SCROLLBAR_TEX_U_ACTIVE : SCROLLBAR_TEX_U_INACTIVE;
		guiGraphics.blit(TEXTURE, guiLeft + DESC_SCROLLBAR_X, barY, barU, SCROLLBAR_TEX_V, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT);
	}

	private record DescLine(FormattedCharSequence text, int color)
	{
		static final DescLine BLANK = new DescLine(FormattedCharSequence.EMPTY, 0);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		if(mouseX >= guiLeft + LIST_X && mouseX < guiLeft + LIST_X + LIST_WIDTH
				&& mouseY >= guiTop + LIST_Y && mouseY < guiTop + LIST_Y + LIST_HEIGHT)
		{
			int rowHeight = font.lineHeight + 2;
			int row = (int) ((mouseY - (guiTop + LIST_Y)) / rowHeight) + scrollOffset;
			if(row >= 0 && row < available.size())
			{
				selected = available.get(row);
				descScrollPos = 0;
				updateBuyButton();
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
	{
		if(mouseX >= guiLeft + LIST_X && mouseX < guiLeft + LIST_X + LIST_WIDTH
				&& mouseY >= guiTop + LIST_Y && mouseY < guiTop + LIST_Y + LIST_HEIGHT)
		{
			scrollOffset = Math.max(0, scrollOffset - (int) Math.signum(scrollY));
			return true;
		}
		if(selected != null && mouseX >= guiLeft + DESC_X && mouseX < guiLeft + DESC_X + DESC_WIDTH
				&& mouseY >= guiTop + DESC_Y && mouseY < guiTop + DESC_Y + DESC_HEIGHT)
		{
			descScrollPos = Math.max(0, descScrollPos - (int) Math.signum(scrollY));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private static ResourceLocation iconFor(Abilitech tech)
	{
		return ResourceLocation.fromNamespaceAndPath(tech.getId().getNamespace(), "textures/gui/abilitechs/icons/" + tech.getIconId() + ".png");
	}
}
