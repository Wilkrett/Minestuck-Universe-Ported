package org.wilkretawesomesauce.minestuckuniverseported.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.block.AbilitechnosynthBlock;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.Abilitech;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRegistry;
import org.wilkretawesomesauce.minestuckuniverseported.network.AbilitechRequestPackets;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code gui.GuiFraymachine} - the only way to manage the
 * abilitech loadout, opened from {@link AbilitechnosynthBlock}.
 * <p>
 * The grid position formula, the {@code guiLeft} offset, the title position, and the equip-slot position
 * are all copied directly from the original's {@code drawScreen} - an earlier version of this screen used
 * a naive square grid and centered title instead, which is why things looked visibly off (icons in a
 * single overlapping row, title overlapping them). This version reproduces the original's staggered
 * honeycomb tessellation ({@code x = (col%2==0 ? 6:18) + (i%4)*24}, rows only 18px apart so hexagons
 * interlock) and its slightly-off-center {@code guiLeft} (the original centers on {@code xSize-16}, not
 * {@code xSize}, for reasons not documented in the source - preserved anyway for pixel fidelity since nothing
 * about it looked accidental).
 * <p>
 * <b>Real drag-and-drop now, replacing an earlier click-to-select-then-click-to-place stand-in</b>: the
 * original used a full drag-and-drop interaction (pick up a tech icon, carry it under the cursor, drop it
 * on a slot) - {@link #mouseClicked} now picks up {@link #draggedTech} (remembering
 * {@link #draggedFromSlot} if it came from an already-equipped slot, {@code -1} if from the grid) and
 * {@link #mouseReleased} resolves the drop: onto a different equip slot sends
 * {@link AbilitechRequestPackets.Unequip} (only if it came from a slot) then
 * {@link AbilitechRequestPackets.Equip}; dropped back on its own origin slot is a no-op; dropped anywhere
 * else while it came from an equipped slot sends just {@code Unequip} (drag-off-to-remove, matching the
 * original); dropped anywhere else after coming from the grid is a plain cancel (it was never equipped, so
 * there's nothing to undo). {@link #render} draws the dragged icon following the cursor each frame. Right-
 * click a slot to unequip, shift+left-click a slot to toggle passive mode - the original used right-click
 * for passive toggle, matching here, but used the drag-off gesture above (not a dedicated right-click) for
 * unequip; this port keeps right-click-to-unequip as a real, additional shortcut alongside the new drag-off
 * behavior, not a replacement for it. Hit-testing also uses a plain 24x24 box instead of the original's
 * exact hexagon point-in-polygon test - a minor precision difference at the very corners of each tile, not
 * worth the added risk of porting the polygon math blind.
 * <p>
 * <b>Real bug fix</b>: {@link #init} used to list every registered {@link Abilitech} unconditionally
 * (grabbing the whole {@code MSUAbilitechRegistry}), showing techs the player had never unlocked as
 * selectable grid tiles - a real, reported bug. Confirmed against the real original
 * ({@code GuiFraymachine#setupTech} calls {@code IGodTierData#getAllAbilitechs()}, whose own real
 * implementation - read directly, not guessed - filters to only {@code Abilitech}s already present in the
 * player's own unlocked-skills map): the original only ever showed techs you'd actually unlocked. Fixed by
 * filtering on {@link GodTierData#isUnlocked}, matching the same real check {@code SkillShopScreen} already
 * uses (there, to exclude what you already own; here, to include only what you do).
 */
public class MSUAbilitechScreen extends Screen
{
	private static final ResourceLocation TEXTURE = Minestuckuniverseported.id("textures/gui/abilitechnosynth.png");
	private static final int GUI_WIDTH = 256, GUI_HEIGHT = 182;
	private static final int SLOT_SIZE = 24;
	private static final int MAX_PER_PAGE = 24;

	// Description panel scrolling, ported from the original's own real GuiFraymachine fields
	// (textBoxWidth/textBoxHeight/scrollPos/descLines) - see renderDescriptionPanel's own doc comment.
	private static final int DESC_BOX_WIDTH = 100;
	private static final int DESC_BOX_HEIGHT = 110;
	private static final int SCROLLBAR_TRAVEL = 95;

	private int guiLeft, guiTop;
	private final List<Abilitech> allTechs = new ArrayList<>();
	private Abilitech draggedTech;
	private int draggedFromSlot = -1;
	private int page = 0;

	private Abilitech lastDescribedTech;
	private float scrollPos = 0;
	private int descOverflowLines = 0;

	public MSUAbilitechScreen()
	{
		super(Component.translatable("gui.abilitechnosynth"));
	}

	/**
	 * Entry point for common code (e.g. {@code blocks.AbilitechnosynthBlock}) to open this screen without
	 * referencing {@link Minecraft}/{@link Screen} directly itself - see that class's own doc comment and
	 * this project's "Recurring bug patterns" / known-gap #6 in CLAUDE.md for why a bare
	 * {@code Minecraft.getInstance().setScreen(...)} call inlined into a common class crashes a dedicated
	 * server: the caller's own bytecode never needs to resolve this screen's type (or {@code Minecraft}'s)
	 * as long as it only ever calls this zero-argument, void-returning static method.
	 */
	public static void open()
	{
		Minecraft.getInstance().setScreen(new MSUAbilitechScreen());
	}

	@Override
	protected void init()
	{
		super.init();
		// Matches the original exactly: centers on (xSize - 16), not xSize.
		guiLeft = width / 2 - (GUI_WIDTH - 16) / 2;
		guiTop = height / 2 - GUI_HEIGHT / 2;

		allTechs.clear();
		GodTierData godTier = godTier();
		for(Abilitech tech : MSUAbilitechRegistry.getAll())
			if(godTier.isUnlocked(tech))
				allTechs.add(tech);
		// Real port of the original's own tech.sort(Comparator.comparingInt(Skill::getSortIndex)) - the
		// sortIndex machinery itself (Skill#sortIndex/getSortIndex/compareTo) was already ported faithfully,
		// this screen just never actually called it.
		allTechs.sort(java.util.Comparator.comparingInt(org.wilkretawesomesauce.minestuckuniverseported.skills.Skill::getSortIndex));
	}

	@Override
	public boolean isPauseScreen()
	{
		return false;
	}

	private GodTierData godTier()
	{
		return minecraft.player.getData(MSUAttachments.GOD_TIER);
	}

	private int pageCount()
	{
		return allTechs.size() / MAX_PER_PAGE + 1;
	}

	/** Honeycomb grid slot position, ported directly from the original's {@code drawScreen}. */
	private int gridX(int i)
	{
		return guiLeft + (i / 4 % 2 == 0 ? 6 : 18) + (i % 4) * 24;
	}

	private int gridY(int i)
	{
		return guiTop + 10 + (i / 4) * 18;
	}

	private int equipX(int slot, int totalSlots)
	{
		return guiLeft + 120 - totalSlots * 12 + slot * 24;
	}

	private int equipY()
	{
		return guiTop + 153;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
	{
		super.render(guiGraphics, mouseX, mouseY, partialTicks);

		guiGraphics.blit(TEXTURE, guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);
		guiGraphics.drawString(font, title, guiLeft + 124, guiTop + 18, 0xFFFFFF, true);

		// pagination arrows + label
		guiGraphics.blit(TEXTURE, guiLeft + 30, guiTop + 130, 0, page > 0 ? 229 : 243, 14, 11);
		guiGraphics.blit(TEXTURE, guiLeft + 76, guiTop + 130, 14, page < pageCount() - 1 ? 229 : 243, 14, 11);
		String pageLabel = (page + 1) + "/" + pageCount();
		guiGraphics.drawString(font, pageLabel, guiLeft + 60 - font.width(pageLabel) / 2, guiTop + 131, 0xFFFFFF, true);

		Abilitech hovered = null;
		int hoveredX = 0, hoveredY = 0;

		int base = page * MAX_PER_PAGE;
		for(int i = 0; base + i < allTechs.size() && i < MAX_PER_PAGE; i++)
		{
			Abilitech tech = allTechs.get(base + i);
			int x = gridX(i), y = gridY(i);

			if(tech == draggedTech && draggedFromSlot == -1)
				guiGraphics.fill(x - 1, y - 1, x + SLOT_SIZE + 1, y + SLOT_SIZE + 1, 0x8000FF00);

			guiGraphics.blit(iconFor(tech), x, y, 0, 0, 24, 24, 24, 24);

			if(isHovering(x, y, SLOT_SIZE, SLOT_SIZE, mouseX, mouseY))
			{
				hovered = tech;
				hoveredX = x;
				hoveredY = y;
			}
		}

		GodTierData godTier = godTier();
		int slots = godTier.getTechSlots();

		for(int i = 0; i < slots; i++)
		{
			int x = equipX(i, slots), y = equipY();

			Abilitech tech = godTier.getTech(i);
			if(tech != null)
			{
				// Skip drawing the icon in its own origin slot while it's being dragged out of it - drawn
				// following the cursor instead, further down, matching a real "picked the icon up" feel.
				if(i != draggedFromSlot)
					guiGraphics.blit(iconFor(tech), x, y, 0, 0, 24, 24, 24, 24);
				if(godTier.isPassiveEnabled(i))
					drawBorder(guiGraphics, x, y, SLOT_SIZE, SLOT_SIZE, 0xFF00FF00);

				if(isHovering(x, y, SLOT_SIZE, SLOT_SIZE, mouseX, mouseY))
				{
					hovered = tech;
					hoveredX = x;
					hoveredY = y;
				}
			}
		}

		// ported from the original's "selected" ring: a texture-based highlight from the same atlas
		// (UV 0,202, 26x26), not a floating tooltip - drawn wherever the mouse is currently hovering
		if(hovered != null)
		{
			guiGraphics.blit(TEXTURE, hoveredX - 1, hoveredY - 1, 0, 202, 26, 26);
			renderDescriptionPanel(guiGraphics, hovered);
		}
		else
		{
			// matches the original's own "nothing selected" branch: descLines resets to 0 and the
			// scrollbar thumb still draws, parked at the top of its track (UV 38,241, not 28,241).
			lastDescribedTech = null;
			descOverflowLines = 0;
			guiGraphics.blit(TEXTURE, guiLeft + 222, guiTop + 35, 38, 241, 10, 15);
		}

		// Real drag-and-drop: the picked-up icon follows the cursor, centered on it, drawn last so it's
		// always on top of everything else.
		if(draggedTech != null)
			guiGraphics.blit(iconFor(draggedTech), mouseX - SLOT_SIZE / 2, mouseY - SLOT_SIZE / 2, 0, 0, 24, 24, 24, 24);
	}

	private record DescLine(FormattedCharSequence text, int color)
	{
	}

	/**
	 * Real port of the original's {@code drawSplitString}-into-a-fixed-box behavior, previously a
	 * simplified stand-in that drew every line top-to-bottom with no height limit at all - since the
	 * panel itself was never clipped or scrolled, long descriptions overflowed straight past the box and
	 * over the rest of the screen. Now real: name + tags + tooltip are flattened into one combined line
	 * list, only {@code DESC_BOX_HEIGHT / font.lineHeight} of them are ever drawn per frame, and
	 * {@link #scrollPos} (real mouse-wheel-driven, see {@link #mouseScrolled}) picks which window of that
	 * list is currently visible - the same real {@code scrollPos}/{@code descLines} idiom the original's
	 * own {@code GuiFraymachine} used, just flattened into one list instead of two separately-offset
	 * {@code drawSplitString} calls sharing a padded string.
	 */
	private void renderDescriptionPanel(GuiGraphics guiGraphics, Abilitech tech)
	{
		if(tech != lastDescribedTech)
		{
			lastDescribedTech = tech;
			scrollPos = 0;
		}

		List<DescLine> lines = buildDescriptionLines(tech);
		int visibleLines = DESC_BOX_HEIGHT / font.lineHeight;
		descOverflowLines = Math.max(0, lines.size() - visibleLines);

		int boxX = guiLeft + 121, boxY = guiTop + 36;
		int startingLine = (int) (scrollPos * descOverflowLines);

		int y = boxY;
		for(int i = startingLine; i < lines.size() && i - startingLine < visibleLines; i++)
		{
			DescLine line = lines.get(i);
			guiGraphics.drawString(font, line.text(), boxX, y, line.color(), false);
			y += font.lineHeight;
		}

		guiGraphics.blit(TEXTURE, guiLeft + 222, guiTop + 35 + (int) (scrollPos * SCROLLBAR_TRAVEL), descOverflowLines > 0 ? 28 : 38, 241, 10, 15);
	}

	/** Both tags (aspect/class and every tech type) are shown, each colored to match the original's
	 * respective color tables - {@code heroClass} techs now get real tag lines too, matching
	 * {@code heroAspect} techs (a gap in the original simplified version of this method, not the original
	 * mod itself). */
	private List<DescLine> buildDescriptionLines(Abilitech tech)
	{
		List<DescLine> lines = new ArrayList<>();

		for(FormattedCharSequence line : font.split(tech.getDisplayName(), DESC_BOX_WIDTH))
			lines.add(new DescLine(line, 0xFFFFFF));

		if(tech instanceof org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect heroAspectTech)
		{
			Component aspectTag = Component.literal("[").append(heroAspectTech.getHeroAspect().asTextComponent()).append(Component.literal("]"));
			lines.add(new DescLine(aspectTag.getVisualOrderText(), aspectColor(heroAspectTech.getHeroAspect())));

			MSUTechType techType = heroAspectTech.getTechType();
			Component typeTag = Component.literal("[").append(Component.translatable(techType.unloc)).append(Component.literal("]"));
			lines.add(new DescLine(typeTag.getVisualOrderText(), techType.color));

			// Purely descriptive classpect "flavor" tags - see TechHeroAspect's own flavor-tagging
			// constructor doc comment. Empty for every tech that doesn't opt in.
			for(com.mraof.minestuck.player.EnumClass flavorClass : heroAspectTech.getFlavorClasses())
			{
				Component flavorTag = Component.literal("[")
						.append(Component.translatable(org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUHeroClass.from(flavorClass).unloc))
						.append(Component.literal("]"));
				int[] flavorColor = org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUClassColors.get(flavorClass);
				lines.add(new DescLine(flavorTag.getVisualOrderText(), flavorColor != null && flavorColor.length > 0 ? flavorColor[0] : 0xFFFFFF));
			}
		}
		else if(tech instanceof org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass heroClassTech)
		{
			Component classTag = Component.literal("[").append(Component.translatable(heroClassTech.getHeroClass().unloc)).append(Component.literal("]"));
			int[] classColor = org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUClassColors.get(heroClassTech.getRealHeroClass());
			lines.add(new DescLine(classTag.getVisualOrderText(), classColor != null && classColor.length > 0 ? classColor[0] : 0xFFFFFF));

			for(MSUTechType type : heroClassTech.getTechTypes())
			{
				Component typeTag = Component.literal("[").append(Component.translatable(type.unloc)).append(Component.literal("]"));
				lines.add(new DescLine(typeTag.getVisualOrderText(), type.color));
			}
		}

		lines.add(new DescLine(FormattedCharSequence.EMPTY, 0xFFFFFF));

		for(FormattedCharSequence line : font.split(tech.getDisplayTooltip(), DESC_BOX_WIDTH))
			lines.add(new DescLine(line, 0xFFFFFF));

		return lines;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
	{
		if(descOverflowLines > 0)
		{
			scrollPos = net.minecraft.util.Mth.clamp(scrollPos + (1.0F / descOverflowLines) * (float) -Math.signum(scrollY), 0F, 1F);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	/** Ported from the original's {@code GuiFraymachine.TEXT_REPLACEMENTS} aspect color table. */
	private static int aspectColor(com.mraof.minestuck.player.EnumAspect aspect)
	{
		return switch(aspect)
		{
			case BREATH -> 0x47E2FA;
			case LIGHT -> 0xF6FA4E;
			case SPACE -> 0x202020;
			case TIME -> 0xFF2106;
			case LIFE -> 0x72EB34;
			case VOID -> 0x001856;
			case HEART -> 0xBD1864;
			case HOPE -> 0xFFDE55;
			case BLOOD -> 0xB71015;
			case RAGE -> 0x9C4DAC;
			case MIND -> 0x06FFC9;
			case DOOM -> 0x306800;
		};
	}

	private static ResourceLocation iconFor(Abilitech tech)
	{
		return ResourceLocation.fromNamespaceAndPath(tech.getId().getNamespace(), "textures/gui/abilitechs/icons/" + tech.getIconId() + ".png");
	}

	/** 1px border via plain fills - avoids relying on a GuiGraphics outline method I couldn't verify exists. */
	private static void drawBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int color)
	{
		guiGraphics.fill(x, y, x + width, y + 1, color);
		guiGraphics.fill(x, y + height - 1, x + width, y + height, color);
		guiGraphics.fill(x, y, x + 1, y + height, color);
		guiGraphics.fill(x + width - 1, y, x + width, y + height, color);
	}

	private static boolean isHovering(int x, int y, int width, int height, int mouseX, int mouseY)
	{
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		GodTierData godTier = godTier();
		int slots = godTier.getTechSlots();

		// pagination arrows
		if(button == 0)
		{
			if(isHovering(guiLeft + 30, guiTop + 130, 14, 11, (int) mouseX, (int) mouseY) && page > 0)
			{
				page--;
				return true;
			}
			if(isHovering(guiLeft + 76, guiTop + 130, 14, 11, (int) mouseX, (int) mouseY) && page < pageCount() - 1)
			{
				page++;
				return true;
			}
		}

		for(int i = 0; i < slots; i++)
		{
			int x = equipX(i, slots), y = equipY();
			if(!isHovering(x, y, SLOT_SIZE, SLOT_SIZE, (int) mouseX, (int) mouseY))
				continue;

			if(button == 1)
			{
				if(godTier.getTech(i) != null)
					PacketDistributor.sendToServer(new AbilitechRequestPackets.Unequip(i));
				return true;
			}
			if(button == 0 && hasShiftDown())
			{
				if(godTier.getTech(i) != null)
					PacketDistributor.sendToServer(new AbilitechRequestPackets.TogglePassive(i));
				return true;
			}
			if(button == 0)
			{
				// Pick the tech already in this slot back up, ready to be dropped elsewhere (or back here,
				// a no-op) - see mouseReleased for how the drop itself is resolved.
				Abilitech tech = godTier.getTech(i);
				if(tech != null)
				{
					draggedTech = tech;
					draggedFromSlot = i;
				}
				return true;
			}
		}

		if(button == 0)
		{
			int base = page * MAX_PER_PAGE;
			for(int i = 0; base + i < allTechs.size() && i < MAX_PER_PAGE; i++)
			{
				int x = gridX(i), y = gridY(i);
				if(isHovering(x, y, SLOT_SIZE, SLOT_SIZE, (int) mouseX, (int) mouseY))
				{
					draggedTech = allTechs.get(base + i);
					draggedFromSlot = -1;
					return true;
				}
			}
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button)
	{
		if(button != 0 || draggedTech == null)
			return super.mouseReleased(mouseX, mouseY, button);

		Abilitech tech = draggedTech;
		int fromSlot = draggedFromSlot;
		draggedTech = null;
		draggedFromSlot = -1;

		GodTierData godTier = godTier();
		int slots = godTier.getTechSlots();
		for(int i = 0; i < slots; i++)
		{
			int x = equipX(i, slots), y = equipY();
			if(!isHovering(x, y, SLOT_SIZE, SLOT_SIZE, (int) mouseX, (int) mouseY))
				continue;

			if(i == fromSlot)
				return true; // dropped back where it started - no-op

			if(fromSlot != -1)
				PacketDistributor.sendToServer(new AbilitechRequestPackets.Unequip(fromSlot));
			PacketDistributor.sendToServer(new AbilitechRequestPackets.Equip(tech.getId(), i));
			return true;
		}

		// Dropped outside any equip slot: if it came from one, that's the original's real "drag off-screen
		// to unequip" gesture. If it came from the grid, it was never equipped in the first place - a plain
		// cancel, nothing to undo.
		if(fromSlot != -1)
			PacketDistributor.sendToServer(new AbilitechRequestPackets.Unequip(fromSlot));

		return true;
	}
}
