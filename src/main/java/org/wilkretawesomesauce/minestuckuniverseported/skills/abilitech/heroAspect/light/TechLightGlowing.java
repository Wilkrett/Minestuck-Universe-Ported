package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.light;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.light.TechLightGlowing}
 * ("Lightbound's Wisdom") - press to mark every player within {@link #RADIUS} blocks with Glowing for
 * 30 seconds; charge past 18 ticks instead to mark <i>every</i> living entity in range (not just
 * players). Both branches and both food costs are kept exactly as sourced.
 */
public class TechLightGlowing extends TechHeroAspect
{
	private static final int RADIUS = 64;

	public TechLightGlowing()
	{
		super(Minestuckuniverseported.id("lightbound_wisdom"), EnumAspect.LIGHT, 888, MSUTechType.DEFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.PRESS)
		{
			if(!player.isCreative() && player.getFoodData().getFoodLevel() < 4)
			{
				player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
				return false;
			}

			MSUAbilitechParticles.burst(level, player, EnumAspect.LIGHT, 10);

			for(LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RADIUS), e -> e instanceof Player))
				markGlowing(level, target);

			if(!player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 4);
		}

		if(state == AbilitechKeyState.NONE || time >= 19)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 4)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(time > 15)
			MSUAbilitechParticles.burst(level, player, EnumAspect.LIGHT, 20);
		else
			MSUAbilitechParticles.aura(level, player, EnumAspect.LIGHT, 10);

		if(time >= 18)
		{
			for(LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RADIUS), e -> e != player))
				markGlowing(level, target);

			if(!player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 4);
		}

		return true;
	}

	private static void markGlowing(Level level, LivingEntity target)
	{
		target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600, 0));
		MSUAbilitechParticles.oneshot(level, target, EnumAspect.LIGHT, 10);
	}
}
