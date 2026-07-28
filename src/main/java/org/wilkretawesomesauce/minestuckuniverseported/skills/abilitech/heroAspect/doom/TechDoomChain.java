package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.doom;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.doom.TechDoomChain}
 * ("Chains of Despair") - two-stage AoE debuff. Pressing immediately applies a first debuff to everyone
 * within {@link #RADIUS}; holding to ~1 second applies a stronger second one on top.
 * <p>
 * Now using the original's own two real custom effects instead of this project's earlier vanilla
 * Slowness/Weakness stand-in: {@link MSUMobEffects#EARTHBOUND} (disables flight) on press, then
 * {@link MSUMobEffects#BUILD_INHIBIT} (disables building) added on top of that if held past
 * {@link #HOLD_THRESHOLD_TICKS} - see {@code EarthboundEffect}/{@code BuildInhibitEffect}'s own doc
 * comments for what each really does.
 */
public class TechDoomChain extends TechHeroAspect
{
	private static final double RADIUS = 20.0;
	private static final int HOLD_THRESHOLD_TICKS = 18;

	public TechDoomChain()
	{
		super(Minestuckuniverseported.id("chains_of_despair"), EnumAspect.DOOM, 8780, MSUTechType.UTILITY);
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

			for(LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RADIUS)))
			{
				target.addEffect(new MobEffectInstance(MSUMobEffects.EARTHBOUND, 60, 0, false, false));
				MSUAbilitechParticles.oneshot(level, target, EnumAspect.DOOM, 10);
			}

			if(!player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 4);

			MSUAbilitechParticles.burst(level, player, EnumAspect.DOOM, 10);
		}

		if(state == AbilitechKeyState.NONE || time >= 19)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 4)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(time >= HOLD_THRESHOLD_TICKS)
		{
			for(LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RADIUS)))
			{
				target.addEffect(new MobEffectInstance(MSUMobEffects.EARTHBOUND, 60, 0, false, false));
				target.addEffect(new MobEffectInstance(MSUMobEffects.BUILD_INHIBIT, 60, 0, false, false));
				MSUAbilitechParticles.oneshot(level, target, EnumAspect.DOOM, 10);
			}

			if(!player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 4);

			MSUAbilitechParticles.burst(level, player, EnumAspect.DOOM, 20);
		}

		MSUAbilitechParticles.aura(level, player, EnumAspect.DOOM, 10);

		return true;
	}
}
