package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.rage;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

import java.util.List;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.rage.TechRageManagement}
 * ("Anger Management") - quick tap and release (under 2 seconds) toggles a single targeted creature's
 * hostility towards players/Iron Golems on or off; holding for the full 2 seconds instead toggles every
 * creature within {@link #RADIUS} blocks at once, capped by available food. Turning hostility <i>off</i>
 * is kept exactly as drastic as the original - it wipes the creature's entire goal/target selector, not
 * just the goals this tech itself added (see {@link RageAI#clearAllGoals}'s own doc comment for why no
 * reflection is needed for that anymore).
 */
public class TechRageManagement extends TechHeroAspect
{
	private static final int RADIUS = 16;
	private static final int SINGLE_TARGET_WINDOW = 40;

	public TechRageManagement()
	{
		super(Minestuckuniverseported.id("anger_management"), EnumAspect.RAGE, 1240, MSUTechType.UTILITY);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 3)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(time < SINGLE_TARGET_WINDOW)
		{
			if(state == AbilitechKeyState.RELEASED)
			{
				LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);
				if(!(target instanceof Mob mob))
					return false;

				toggleRageShift(mob);
				MSUAbilitechParticles.oneshot(level, mob, EnumAspect.RAGE, 10);
				if(!player.isCreative())
					player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 3);
			}

			MSUAbilitechParticles.aura(level, player, EnumAspect.RAGE, 5);
		}
		else if(time == SINGLE_TARGET_WINDOW)
		{
			List<Mob> nearby = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(RADIUS));
			int count = 0;

			for(Mob target : nearby)
			{
				if(!player.isCreative() && player.getFoodData().getFoodLevel() < 3)
					break;

				toggleRageShift(target);
				MSUAbilitechParticles.oneshot(level, target, EnumAspect.RAGE, 10);
				count++;

				if(!player.isCreative())
					player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 3);
			}

			if(count == 0 && !nearby.isEmpty())
			{
				player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
				return false;
			}

			MSUAbilitechParticles.burst(level, player, EnumAspect.RAGE, nearby.isEmpty() ? 1 : 4);
		}

		return true;
	}

	private static void toggleRageShift(Mob mob)
	{
		if(!mob.hasEffect(MSUMobEffects.RAGE_SHIFTED) && !mob.hasEffect(MSUMobEffects.FRENZIED))
			RageAI.enableRageShift(mob);
		else
		{
			RageAI.clearAllGoals(mob);
			mob.removeEffect(MSUMobEffects.RAGE_SHIFTED);
			mob.removeEffect(MSUMobEffects.FRENZIED);
		}
	}
}
