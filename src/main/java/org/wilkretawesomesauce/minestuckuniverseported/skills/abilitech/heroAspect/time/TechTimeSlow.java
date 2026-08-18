package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time;

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
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * New "basic command" tech from the Time Aspect design discussion - "Slow": reduces a targeted entity's
 * action speed (movement + mining/attack speed) for as long as held, via vanilla Slowness + Mining
 * Fatigue. Costs 1 food per 20 ticks.
 */
public class TechTimeSlow extends TechHeroAspect
{
	public TechTimeSlow()
	{
		super(Minestuckuniverseported.id("slow"), EnumAspect.TIME, 3000, MSUTechType.OFFENSE); // new tech, no original cost to port - picked to fit this project's own cost spread, see class doc comment
		setIcon("default");
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.HELD)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 1)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);
		if(target == null)
			return true;

		target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, false));
		target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 20, 1, false, false));

		MSUAbilitechParticles.aura(level, target, EnumAspect.TIME, 5);

		if(time % 20 == 0 && !player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);

		return true;
	}
}
