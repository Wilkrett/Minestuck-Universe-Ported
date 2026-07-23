package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code TechTimeStop}. Hold and aim at a target, release after
 * charging 40+ ticks to freeze it (see {@link TimeStopEffect}). Costs 8 food.
 * <p>
 * Not ported: the {@code AbilitechTargetedEvent} interception hook (nothing in this project posts to it).
 */
public class TechTimeStop extends TechHeroAspect
{
	private static final int ENERGY_USE = 8;

	public TechTimeStop()
	{
		super(Minestuckuniverseported.id("chronofreeze"), EnumAspect.TIME, 2500000, MSUTechType.OFFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < ENERGY_USE)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(state == AbilitechKeyState.RELEASED && time >= 40)
		{
			LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);
			if(target != null)
			{
				target.addEffect(new MobEffectInstance(MSUMobEffects.TIME_STOP, 80, 0));
				if(!player.isCreative())
					player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - ENERGY_USE);

				MSUAbilitechParticles.oneshot(level, target, EnumAspect.TIME, 10);
			}
		}

		MSUAbilitechParticles.aura(level, player, EnumAspect.TIME, time >= 40 ? 5 : 2);

		return true;
	}
}
