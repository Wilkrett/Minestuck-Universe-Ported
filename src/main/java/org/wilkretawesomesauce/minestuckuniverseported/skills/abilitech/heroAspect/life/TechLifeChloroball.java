package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.life;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.MSUBlocks;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.life.TechLifeChloroball}
 * ("Chloroball") - hold for 1 second while standing in an air block to place a real
 * {@code block.ChloroballBlock} there (see that class's own doc comment for what it actually does once
 * placed).
 */
public class TechLifeChloroball extends TechHeroAspect
{
	private static final int CHARGE_TICKS = 20;

	public TechLifeChloroball()
	{
		super(Minestuckuniverseported.id("chloroball"), EnumAspect.LIFE, 715, MSUTechType.UTILITY);
	}

	@Override
	public boolean canUse(Level level, Player player)
	{
		return super.canUse(level, player) && player.getAbilities().mayBuild;
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE)
			return false;
		if(!level.getBlockState(player.blockPosition()).isAir())
			return false;

		if(time < CHARGE_TICKS && !player.isCreative() && player.getFoodData().getFoodLevel() < 6)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		MSUAbilitechParticles.aura(level, player, EnumAspect.LIFE, 2);

		if(time == CHARGE_TICKS)
		{
			if(!player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 6);

			level.setBlockAndUpdate(player.blockPosition(), MSUBlocks.CHLOROBALL.get().defaultBlockState());
		}

		return true;
	}
}
