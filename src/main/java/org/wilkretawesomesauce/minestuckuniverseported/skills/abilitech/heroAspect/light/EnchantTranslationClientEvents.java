package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.light;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;
import org.wilkretawesomesauce.minestuckuniverseported.skills.MSUSkills;

/**
 * Client-side "translate the enchanting table's Standard Galactic Alphabet" half of
 * {@code TechLightEnchantersInsight} ("Enchanter's Insight") - see that class's own doc comment for the
 * XP-boost half.
 * <p>
 * <b>Real, but a deliberate overlay rather than a literal glyph replacement</b>: vanilla's own
 * {@link EnchantmentMenu} already knows exactly which real enchantment (and level) each of the 3 hint
 * slots represents at all times - {@code enchantClue}/{@code levelClue}, both real public fields, are how
 * {@link EnchantmentScreen} itself decides what to draw before scrambling it into the alien-looking hint
 * font. Reading those same two fields and resolving them via {@link Enchantment#getFullname} gives the
 * real underlying name/level - genuinely "translating the language," not a guess or a flat reveal-all.
 * What's simplified is <i>where</i> it's drawn: overriding vanilla's own private in-place hint-text
 * rendering would need Mixin (which this project doesn't use, per every other "known gap" of this shape
 * already documented elsewhere); {@link EnchantmentScreen}'s own layout fields
 * ({@code leftPos}/{@code topPos}) are {@code protected}, not reachable from an external event listener
 * either. Drawn instead as a plain 3-line list pinned to the top-left corner of the whole window - not
 * pixel-aligned to the real slot buttons, but the practical result (you can now read what each slot
 * actually is) is the same.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class EnchantTranslationClientEvents
{
	private static final int MARGIN = 4;

	private EnchantTranslationClientEvents()
	{
	}

	@SubscribeEvent
	private static void onRenderScreen(ScreenEvent.Render.Post event)
	{
		if(!(event.getScreen() instanceof EnchantmentScreen screen))
			return;

		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if(player == null || mc.level == null)
			return;

		GodTierData godTier = player.getData(MSUAttachments.GOD_TIER);
		if(!godTier.isPassiveEnabledFor(MSUSkills.ENCHANTERS_INSIGHT))
			return;

		EnchantmentMenu menu = screen.getMenu();
		RegistryAccess registryAccess = mc.level.registryAccess();
		var enchantmentRegistry = registryAccess.registryOrThrow(Registries.ENCHANTMENT);

		GuiGraphics guiGraphics = event.getGuiGraphics();
		int y = MARGIN;
		for(int slot = 0; slot < menu.enchantClue.length; slot++)
		{
			int enchantId = menu.enchantClue[slot];
			int level = menu.levelClue[slot];
			if(enchantId < 0 || level < 0)
				continue;

			Holder<Enchantment> holder = enchantmentRegistry.getHolder(enchantId).orElse(null);
			if(holder == null)
				continue;

			Component name = Enchantment.getFullname(holder, level);
			guiGraphics.drawString(mc.font, name, MARGIN, y, 0xFFFFFF);
			y += mc.font.lineHeight + 2;
		}
	}
}
