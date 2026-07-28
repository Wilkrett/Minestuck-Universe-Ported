package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.life;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.life.TechLifeFertility}
 * ("Song of Fertility") - hold half a second to breed every {@link Animal} within {@link #RADIUS}
 * blocks (same mechanism as {@code TechLifeBreed}, just wider) and grow every real
 * {@link BonemealableBlock} in a full {@code (2*RADIUS+1)}-wide cube around you at once - kept as a
 * literal cube scan, matching the original's own triple loop; the cost is inherent to what this
 * ability was actually designed to do, not something to quietly shrink.
 */
public class TechLifeFertility extends TechHeroAspect
{
	private static final int RADIUS = 20;

	public TechLifeFertility()
	{
		super(Minestuckuniverseported.id("song_of_fertility"), EnumAspect.LIFE, 630, MSUTechType.UTILITY);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE || time >= 11)
			return false;
		if(!(level instanceof ServerLevel serverLevel))
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 4)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(time > 5)
			MSUAbilitechParticles.burst(level, player, EnumAspect.LIFE, 20);
		else
			MSUAbilitechParticles.aura(level, player, EnumAspect.LIFE, 6);

		if(time >= 10)
		{
			for(Animal target : level.getEntitiesOfClass(Animal.class, player.getBoundingBox().inflate(RADIUS)))
			{
				MSUAbilitechParticles.oneshot(level, target, EnumAspect.LIFE, 3);
				target.setInLove(player);
			}

			if(!player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 4);

			BlockPos center = player.blockPosition();
			for(BlockPos target : BlockPos.betweenClosed(center.offset(-RADIUS, -RADIUS, -RADIUS), center.offset(RADIUS, RADIUS, RADIUS)))
			{
				BlockState targetState = level.getBlockState(target);
				if(targetState.getBlock() instanceof BonemealableBlock growable
						&& growable.isValidBonemealTarget(level, target, targetState)
						&& growable.isBonemealSuccess(level, level.getRandom(), target, targetState))
				{
					growable.performBonemeal(serverLevel, level.getRandom(), target, targetState);
					level.levelEvent(2005, target, 0);
					MSUAbilitechParticles.aura(level, player, EnumAspect.LIFE, 4);
				}
			}
		}

		return true;
	}
}
