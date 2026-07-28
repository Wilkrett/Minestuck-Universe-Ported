package org.wilkretawesomesauce.minestuckuniverseported.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code blocks.MinestuckUniverseBlocks#chloroball} - placed
 * by {@code TechLifeChloroball} ("Chloroball"), a small floating orb that passively fertilizes nearby
 * crops. Reuses vanilla's own random-tick scheduling ({@link Block#isRandomlyTicking}/
 * {@link Block#randomTick}, the same mechanism that grows crops on their own) rather than a dedicated
 * block entity ticker - a real, direct, lighter-weight equivalent since this doesn't need to persist any
 * state of its own between ticks.
 * <p>
 * <b>Known gap:</b> no custom model/texture exists for this block yet, so it currently renders with the
 * default missing-texture placeholder - same category of gap as this project's other stated missing-art
 * cases (e.g. God Tier's worn-armor models).
 */
public class ChloroballBlock extends Block
{
	private static final int RADIUS = 3;

	public ChloroballBlock(Properties properties)
	{
		super(properties);
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state)
	{
		return true;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
	{
		for(BlockPos target : BlockPos.betweenClosed(pos.offset(-RADIUS, -RADIUS, -RADIUS), pos.offset(RADIUS, RADIUS, RADIUS)))
		{
			BlockState targetState = level.getBlockState(target);
			if(targetState.getBlock() instanceof BonemealableBlock growable
					&& growable.isValidBonemealTarget(level, target, targetState)
					&& growable.isBonemealSuccess(level, random, target, targetState))
			{
				growable.performBonemeal(level, random, target, targetState);
			}
		}
	}
}
