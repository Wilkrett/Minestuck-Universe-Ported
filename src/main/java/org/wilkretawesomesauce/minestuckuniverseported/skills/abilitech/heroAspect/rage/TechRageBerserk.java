package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.rage;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.rage.TechRageBerserk}
 * ("Enraged Berserk") - press to enter Berserk for 60 seconds ({@link #DURATION_TICKS}, matching the
 * original's 1200-tick duration), a no-op if already berserk. See {@link BerserkEffect}'s own doc
 * comment for what Berserk actually grants.
 */
public class TechRageBerserk extends TechHeroAspect
{
	private static final int ENERGY_USE = 3;
	private static final int DURATION_TICKS = 1200;

	public TechRageBerserk()
	{
		super(Minestuckuniverseported.id("enraged_berserk"), EnumAspect.RAGE, 7100, MSUTechType.OFFENSE);
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

		if(!player.hasEffect(MSUMobEffects.RAGE_BERSERK))
		{
			MSUAbilitechParticles.aura(level, player, EnumAspect.RAGE, 10);

			player.addEffect(new MobEffectInstance(MSUMobEffects.RAGE_BERSERK, DURATION_TICKS, 0));
			if(!player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - ENERGY_USE);
		}

		return true;
	}
}
