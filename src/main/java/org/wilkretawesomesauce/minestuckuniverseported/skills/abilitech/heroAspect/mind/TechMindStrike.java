package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.mind;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.mind.TechMindStrike}
 * ("Calculated Strike") - hold to charge, release to lock in a damage multiplier for your very next
 * successful hit ({@link MindStrikeEvents} spends it on the first {@code LivingIncomingDamageEvent}
 * this player causes afterwards). The charge-time formula, the sine-wave power curve, and the
 * low/med/high status thresholds are all kept exactly as sourced - see {@link CalculatingEffect}'s own
 * doc comment for why its remaining duration doubles as the original's raw tick counter.
 */
public class TechMindStrike extends TechHeroAspect
{
	public TechMindStrike()
	{
		super(Minestuckuniverseported.id("calculated_strike"), EnumAspect.MIND, 62330, MSUTechType.OFFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE)
			return false;

		if(!player.isCreative())
		{
			if(player.getFoodData().getFoodLevel() < 1)
			{
				player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
				return false;
			}
			if(time % 20 == 0)
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);
		}

		if(state == AbilitechKeyState.RELEASED)
		{
			int existing = player.hasEffect(MSUMobEffects.CALCULATING) ? player.getEffect(MSUMobEffects.CALCULATING).getDuration() : 0;
			int calculating = Math.max(time + existing, 100);
			player.addEffect(new MobEffectInstance(MSUMobEffects.CALCULATING, calculating, 0));

			double power = Math.sin(calculating * 1.1 + Math.PI * 1.5) / 2 + calculating * 0.017 + 0.5;
			if(power < 0.8)
				player.displayClientMessage(Component.translatable("status.calculatedStrike.low"), true);
			else if(power < 1.2)
				player.displayClientMessage(Component.translatable("status.calculatedStrike.med"), true);
			else
				player.displayClientMessage(Component.translatable("status.calculatedStrike.high"), true);
			MSUAbilitechParticles.oneshot(level, player, EnumAspect.MIND, 2);
			return true;
		}

		MSUAbilitechParticles.aura(level, player, EnumAspect.MIND, 2);
		return true;
	}
}
