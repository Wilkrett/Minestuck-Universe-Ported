package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.life;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.life.TechLifeBreed}
 * ("Mating Season") - hold for ~three quarters of a second, release to put every {@link Animal} within
 * {@link #RADIUS} blocks into a breeding mood ({@link Animal#setInLove}, the real vanilla mechanism
 * behind feeding animals by hand).
 */
public class TechLifeBreed extends TechHeroAspect
{
	private static final int RADIUS = 8;

	public TechLifeBreed()
	{
		super(Minestuckuniverseported.id("mating_season"), EnumAspect.LIFE, 115, MSUTechType.UTILITY);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE || time >= 16)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 4)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(time > 10)
			MSUAbilitechParticles.burst(level, player, EnumAspect.LIFE, 20);
		else
			MSUAbilitechParticles.aura(level, player, EnumAspect.LIFE, 6);

		if(time >= 15)
		{
			for(Animal target : level.getEntitiesOfClass(Animal.class, player.getBoundingBox().inflate(RADIUS)))
			{
				MSUAbilitechParticles.oneshot(level, target, EnumAspect.LIFE, 3);
				target.setInLove(player);
			}

			if(!player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 4);
		}

		return true;
	}
}
