package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.AbilitechLoadout;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported (significantly simplified) from MinestuckUniverse (1.12.2)'s {@code TechTimeTickUp}. Hold and
 * aim at an entity to "tether" it (keeps tracking it even if you look away), giving it one extra tick per
 * game tick for as long as you hold the key and stay within 20 blocks. Costs 2 food per second.
 * <p>
 * <b>Simplified out:</b> the original tracked "tick-up stacks" on the *target* (via {@code IBadgeEffects}),
 * so if multiple players tethered the same entity it would only get ticked once per extra-tick regardless
 * of how many players were boosting it, and cleaned up correctly on logout. This version has no such
 * bookkeeping - if two players tether the same entity simultaneously it really will get double-double
 * ticked. Given this project's current sandbox-mode scope, that's an accepted rough edge rather than
 * something worth the complexity to fix right now. Also dropped: the client-side self-tick-boost hook
 * (letting a player's own client run extra local ticks for smoother prediction) and the
 * {@code AbilitechTargetedEvent} interception hook.
 */
public class TechTimeTickUp extends TechHeroAspect
{
	public TechTimeTickUp()
	{
		super(Minestuckuniverseported.id("accelerando"), EnumAspect.TIME, 1300000, MSUTechType.HYBRID);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		AbilitechLoadout badgeEffects = player.getData(MSUAttachments.ABILITECH_LOADOUT);

		if(state == AbilitechKeyState.NONE)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 2)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			badgeEffects.setTether(techSlot, null);
			return false;
		}

		if(state == AbilitechKeyState.RELEASED)
		{
			badgeEffects.setTether(techSlot, null);
			return true;
		}

		Entity target = badgeEffects.getTether(techSlot);
		if(target == null)
		{
			target = MSUAbilitechRayTrace.getTargetEntity(player);
			badgeEffects.setTether(techSlot, target);
		}

		if(target != null && target.distanceTo(player) > 20)
		{
			badgeEffects.setTether(techSlot, null);
			target = null;
		}

		if(target != null)
		{
			if(!player.isCreative() && time % 20 == 0)
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 2);

			target.tick();
			MSUAbilitechParticles.oneshot(level, target, EnumAspect.TIME, 2);
		}

		MSUAbilitechParticles.aura(level, player, EnumAspect.TIME, target == null ? 2 : 5);

		return true;
	}
}
