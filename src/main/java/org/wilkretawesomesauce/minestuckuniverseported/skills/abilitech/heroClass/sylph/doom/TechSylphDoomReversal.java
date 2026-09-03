package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.sylph.doom;

import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.badgeEffects.BadgeEffects;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.events.AbilitechTargetedEvent;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;

/**
 * "Doom Reversal" - new tech, ported from the "Doom Class Abilities Framework" design document (no
 * 1.12.2 original), Sylph of Doom's Core ability: "the Sylph reduces or repairs accumulated
 * Doom... stabilizing doomed situations." The direct inverse of {@code heroClass.maid.doom.TechMaidDoomforge}:
 * hold while aiming at a target to lock onto them (the same real per-slot tether
 * {@code heroClass.sylph.TechSylph}'s own healing hold already established via
 * {@code BadgeEffects#getTether}), removing {@link #DOOM_REMOVED_PER_SECOND} from their
 * {@code mechanics.doom.DoomData} every second at 1 food/second cost to the caster - the exact same cost shape as
 * {@code TechSylph} itself.
 * <p>
 * Priced in the low-hundred-thousands, well below Sylph's own existing generic tech
 * ({@code TechSylph}, 995000) - a repeatable sustain tool, not a one-time ultimate.
 */
public class TechSylphDoomReversal extends TechHeroClass
{
	/** Doom removed per second from a tethered target. */
	private static final double DOOM_REMOVED_PER_SECOND = 3.0;

	public TechSylphDoomReversal()
	{
		super(Minestuckuniverseported.id("doom_reversal"), EnumClass.SYLPH, EnumAspect.DOOM, 300000, MSUTechType.DEFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		BadgeEffects badgeEffects = player.getData(MSUAttachments.BADGE_EFFECTS);

		if(state == AbilitechKeyState.RELEASED)
			badgeEffects.setTether(techSlot, null);

		if(state != AbilitechKeyState.HELD)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 1)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		LivingEntity target = badgeEffects.getTether(techSlot) instanceof LivingEntity living ? living : null;
		if(target == null)
		{
			target = MSUAbilitechRayTrace.getTargetEntity(player);
			badgeEffects.setTether(techSlot, target);
		}

		if(target == null || target.getData(MSUAttachments.DOOM_DATA).getDoom() <= 0)
			return false;

		if(NeoForge.EVENT_BUS.post(new AbilitechTargetedEvent(player, target, this, techSlot, true)).isCanceled())
			return false;

		MSUAbilitechParticles.aura(level, player, EnumAspect.DOOM, 5);

		if(time % 20 == 0)
		{
			target.getData(MSUAttachments.DOOM_DATA).removeDoom(DOOM_REMOVED_PER_SECOND);
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);
			MSUAbilitechParticles.oneshot(level, target, EnumAspect.DOOM, 5);
		}

		return true;
	}

	@Override
	public boolean isUsableExternally(Level level, Player player)
	{
		return player.getFoodData().getFoodLevel() >= 1 && super.isUsableExternally(level, player);
	}
}
