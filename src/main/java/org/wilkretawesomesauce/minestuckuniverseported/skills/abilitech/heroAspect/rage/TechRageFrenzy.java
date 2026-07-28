package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.rage;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

import java.util.List;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.rage.TechRageFrenzy}
 * ("Frenzied Mayhem") - hold for 1 second, release to send every creature within
 * {@link #RADIUS} blocks berserk against anything nearby, itself included in each other's targeting
 * pool - real mutual chaos via {@link FrenzyTargetGoal}, not just a status flag. See that class's and
 * {@link RageAI}'s own doc comments for the goal-injection mechanics.
 */
public class TechRageFrenzy extends TechHeroAspect
{
	private static final int RADIUS = 16;
	private static final int CHARGE_TICKS = 20;

	public TechRageFrenzy()
	{
		super(Minestuckuniverseported.id("frenzied_mayhem"), EnumAspect.RAGE, 1390, MSUTechType.UTILITY);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE || time > CHARGE_TICKS)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 5)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		MSUAbilitechParticles.aura(level, player, EnumAspect.RAGE, 2);

		if(time == CHARGE_TICKS)
		{
			List<Mob> nearby = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(RADIUS));

			for(Mob target : nearby)
			{
				if(!target.hasEffect(MSUMobEffects.FRENZIED))
					RageAI.enableFrenzy(target);
				MSUAbilitechParticles.oneshot(level, target, EnumAspect.RAGE, 10);
			}

			if(!player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 5);

			MSUAbilitechParticles.burst(level, player, EnumAspect.RAGE, nearby.isEmpty() ? 1 : 4);
		}

		return true;
	}
}
