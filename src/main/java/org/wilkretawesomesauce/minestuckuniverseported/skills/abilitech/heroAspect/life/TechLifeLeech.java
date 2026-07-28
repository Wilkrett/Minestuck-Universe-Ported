package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.life;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.AbilitechLoadout;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.life.TechLifeLeech}
 * ("Lifeforce Leech") - hold and aim at a target to tether it (persists even if you look away, same
 * tether idiom {@code TechTimeTickUp}/{@code TechBloodTransfusion} already use); every second, drain 2
 * armor-bypassing damage from it and heal yourself the same amount, up to 20 blocks away. Reuses
 * {@code damageSources().magic()} for the drain, matching this project's established reuse for exactly
 * this "needs armor bypass, not worth a bespoke DamageType" situation.
 */
public class TechLifeLeech extends TechHeroAspect
{
	private static final int DRAIN_INTERVAL_TICKS = 20;
	private static final float DRAIN_AMOUNT = 2.0F;
	private static final double MAX_RANGE = 20;

	public TechLifeLeech()
	{
		super(Minestuckuniverseported.id("lifeforce_leech"), EnumAspect.LIFE, 79700, MSUTechType.OFFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE)
			return false;
		if(!(level instanceof ServerLevel serverLevel))
			return false;

		AbilitechLoadout badgeEffects = player.getData(MSUAttachments.ABILITECH_LOADOUT);

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 1)
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

		if(target != null && target.distanceTo(player) > MAX_RANGE)
		{
			badgeEffects.setTether(techSlot, null);
			target = null;
		}

		if((time + 1) % DRAIN_INTERVAL_TICKS != 0)
		{
			MSUAbilitechParticles.aura(level, player, EnumAspect.LIFE, 5);
			if(target != null)
				MSUAbilitechParticles.oneshot(level, target, EnumAspect.LIFE, 2);
			return true;
		}

		if(target instanceof LivingEntity livingTarget)
		{
			MSUAbilitechParticles.aura(level, player, EnumAspect.LIFE, 10);

			livingTarget.hurt(serverLevel.damageSources().magic(), DRAIN_AMOUNT);
			player.heal(DRAIN_AMOUNT);

			MSUAbilitechParticles.oneshot(level, livingTarget, EnumAspect.LIFE, 5);

			if(!player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);
		}
		else
			MSUAbilitechParticles.aura(level, player, EnumAspect.LIFE, 5);

		return true;
	}
}
