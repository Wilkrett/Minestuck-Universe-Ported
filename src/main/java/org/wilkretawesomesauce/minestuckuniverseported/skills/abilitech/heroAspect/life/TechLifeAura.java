package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.life;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.life.TechLifeAura}
 * ("Healing Aura") - press for an immediate self Regeneration V (30s); charge to 18+ ticks instead to
 * grant a weaker Regeneration IV (60s) to <i>every</i> living creature within {@link #RADIUS} blocks,
 * allies and enemies alike - the original never filtered by team here, so this doesn't either.
 */
public class TechLifeAura extends TechHeroAspect
{
	private static final int RADIUS = 8;

	public TechLifeAura()
	{
		super(Minestuckuniverseported.id("healing_aura"), EnumAspect.LIFE, 72970, MSUTechType.DEFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.PRESS)
		{
			if(!player.isCreative() && player.getFoodData().getFoodLevel() < 6)
			{
				player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
				return false;
			}

			MSUAbilitechParticles.aura(level, player, EnumAspect.LIFE, 10);

			player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 4));

			if(!player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 6);
		}

		if(state == AbilitechKeyState.NONE || time >= 19)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 4)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(time > 15)
			MSUAbilitechParticles.burst(level, player, EnumAspect.LIFE, 20);
		else
			MSUAbilitechParticles.aura(level, player, EnumAspect.LIFE, 10);

		if(time >= 18)
		{
			for(LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RADIUS)))
			{
				MSUAbilitechParticles.oneshot(level, target, EnumAspect.LIFE, 10);
				target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 3));
			}

			if(!player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 4);
		}

		return true;
	}
}
