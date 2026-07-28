package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.light;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
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
}
