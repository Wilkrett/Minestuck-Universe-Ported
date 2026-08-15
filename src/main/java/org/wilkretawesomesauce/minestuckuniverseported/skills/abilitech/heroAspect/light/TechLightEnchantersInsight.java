package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.light;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.MSUSkills;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * New "Enchanter's Insight" Light tech, user-requested, no original 1.12.2 counterpart. Two bundled
 * passive effects while toggled on (same {@code isPassiveEnabledFor}-gated shape
 * {@code breath.TechBreathSpaceFallProof} already established for a passive with an explicit on/off
 * toggle, not just an equip check):
 * <ul>
 *     <li>{@link #XP_MULTIPLIER} extra experience on every real XP gain, via {@link PlayerXpEvent.XpChange}
 *     - registers {@code this} directly on {@link NeoForge#EVENT_BUS} (same reasoning
 *     {@code TechBreathSpaceFallProof} documents: a static handler would need to reach back into a
 *     not-yet-constructed {@code MSUSkills} field to identify itself).</li>
 *     <li>Reading the enchanting table's Standard Galactic Alphabet hint text for what it actually says -
 *     the real client-side render half of that is {@link EnchantTranslationClientEvents}, split out the
 *     same way every other client-only render/input hook in this project now lives alongside its ability
 *     instead of under {@code client/}, since a GUI overlay can't safely live inline in this common-loaded
 *     class (see that class's own doc comment for exactly what it draws and why it's an overlay rather
 *     than a literal in-place glyph replacement).</li>
 * </ul>
 */
public class TechLightEnchantersInsight extends TechHeroAspect
{
	static final float XP_MULTIPLIER = 1.5F;

	public TechLightEnchantersInsight()
	{
		super(Minestuckuniverseported.id("enchanters_insight"), EnumAspect.LIGHT, 15000, MSUTechType.PASSIVE); // new tech, no original cost to port - picked to fit this project's own cost spread, see class doc comment
		NeoForge.EVENT_BUS.register(this);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		return false;
	}

	@Override
	public void onPassiveToggle(Level level, Player player, boolean active)
	{
		super.onPassiveToggle(level, player, active);
		sendToggleMessage(player, active);
	}

	@SubscribeEvent
	private void onXpChange(PlayerXpEvent.XpChange event)
	{
		if(event.getAmount() <= 0)
			return;

		Player player = event.getEntity();
		GodTierData godTier = player.getData(MSUAttachments.GOD_TIER);
		if(!godTier.isPassiveEnabledFor(this))
			return;

		event.setAmount(Math.round(event.getAmount() * XP_MULTIPLIER));
	}

	/**
	 * Client-side "translate the enchanting table's Standard Galactic Alphabet" half of this tech.
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
	public static final class ClientEvents
	{
		private static final int MARGIN = 4;

		private ClientEvents()
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
}
