package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.doom;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.doom.TechDoomDecay}
 * ("Withering Whisper") - hold for ~1.25 seconds to afflict every player and hostile mob within
 * {@link #RADIUS} with the original's own real {@link MSUMobEffects#DECAY} effect (an escalating,
 * armor-bypassing drain - see {@code DecayEffect}'s own doc comment), replacing this project's earlier
 * vanilla-Wither stand-in. Duration is shorter against allies (same team) than against everyone else,
 * matching the original exactly.
 */
public class TechDoomDecay extends TechHeroAspect
{
	private static final double RADIUS = 16.0;
	private static final int TRIGGER_TICKS = 25;

	public TechDoomDecay()
	{
		super(Minestuckuniverseported.id("withering_whisper"), EnumAspect.DOOM, 985000, MSUTechType.OFFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE || time >= 26)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 8)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(time >= TRIGGER_TICKS)
		{
			for(LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RADIUS), e -> !e.equals(player)))
			{
				if(!(target instanceof Player || target instanceof Enemy))
					continue;

				int duration = player.isAlliedTo(target) ? 180 : 400;
				target.addEffect(new MobEffectInstance(MSUMobEffects.DECAY, duration, 1, false, false));
				MSUAbilitechParticles.oneshot(level, target, EnumAspect.DOOM, 10);
			}

			if(!player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 8);

			MSUAbilitechParticles.burst(level, player, EnumAspect.DOOM, 20);
		}

		MSUAbilitechParticles.aura(level, player, EnumAspect.DOOM, 10);

		return true;
	}
}
