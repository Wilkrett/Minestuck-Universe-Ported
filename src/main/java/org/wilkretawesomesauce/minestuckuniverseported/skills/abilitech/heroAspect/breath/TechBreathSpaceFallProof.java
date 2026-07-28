package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.breath;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s
 * {@code skills.abilitech.heroAspect.breath.TechBreathSpaceFallProof} ("Vertigo Block") - passive: toggle
 * it on and fall damage (and running into a wall while flying) never hurts you.
 * <p>
 * The original also cross-listed this tech for Space-aspect players specifically (available to Breath
 * <i>or</i> Space, via a hand-rolled {@code canAppearOnList} override), on top of the skills/badges
 * unlock system this project has never ported - moot here anyway, since this whole framework's "no
 * unlock gating" decision (see {@code Abilitech}) already means any registered tech is equipable by
 * anyone regardless of aspect.
 * <p>
 * {@code DamageTypeTags.IS_FALL} covers both fall damage and flying-into-a-wall damage in vanilla's own
 * data - a single tag check standing in cleanly for the original's two-way
 * {@code DamageSource.FALL}/{@code DamageSource.FLY_INTO_WALL} equality check. Registers its own
 * instance with {@link NeoForge#EVENT_BUS} directly (same reasoning as {@code blood.TechBloodBleeding} -
 * a static handler would need to reach back into a not-yet-constructed {@code MSUSkills} field to
 * identify itself).
 */
public class TechBreathSpaceFallProof extends TechHeroAspect
{
	public TechBreathSpaceFallProof()
	{
		super(Minestuckuniverseported.id("vertigo_block"), EnumAspect.BREATH, 475, MSUTechType.PASSIVE);
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
	private void onIncomingDamage(LivingIncomingDamageEvent event)
	{
		LivingEntity entity = event.getEntity();
		if(entity.level().isClientSide() || !event.getSource().is(DamageTypeTags.IS_FALL))
			return;

		if(!(entity instanceof Player player))
			return;

		GodTierData godTier = player.getData(MSUAttachments.GOD_TIER);
		if(godTier.isPassiveEnabledFor(this))
			event.setCanceled(true);
	}
}
