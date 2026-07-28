package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.mind;

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
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.mind.TechMindConfusion}
 * ("Sensory Break") - press and aim at a target to scramble their mind for 20 seconds
 * ({@link #DURATION_TICKS}, matching the original's 400-tick duration exactly), reversing their controls
 * for as long as it lasts. See {@link MindConfusionEffect}'s own doc comment for how the reversal itself
 * is implemented client-side.
 */
public class TechMindConfusion extends TechHeroAspect
{
	private static final int ENERGY_USE = 9;
	private static final int DURATION_TICKS = 400;

	public TechMindConfusion()
	{
		super(Minestuckuniverseported.id("sensory_break"), EnumAspect.MIND, 38780, MSUTechType.OFFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.PRESS)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < ENERGY_USE)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		MSUAbilitechParticles.aura(level, player, EnumAspect.MIND, 5);

		LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);
		if(target == null)
			return true;

		target.addEffect(new MobEffectInstance(MSUMobEffects.MIND_CONFUSION, DURATION_TICKS, 0));
		MSUAbilitechParticles.oneshot(level, target, EnumAspect.MIND, 10);

		if(!player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - ENERGY_USE);

		return true;
	}
}
