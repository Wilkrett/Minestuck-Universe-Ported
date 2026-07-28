package org.wilkretawesomesauce.minestuckuniverseported.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.wilkretawesomesauce.minestuckuniverseported.block.AbilitechnosynthBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code items.ItemAbilitechnosyth} - places all 16 real
 * positions of the {@link AbilitechnosynthBlock} multiblock at once, relative to the clicked ground
 * position and the placing player's own facing (matching the original's "faces the player" placement,
 * furnace-style). See that class's own doc comment for where this structure's real geometry/collision
 * math came from.
 */
public class AbilitechnosynthItem extends BlockItem
{
	public AbilitechnosynthItem(Block block, Item.Properties properties)
	{
		super(block, properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context)
	{
		Level level = context.getLevel();
		Player player = context.getPlayer();
		if(player == null || context.getClickedFace() != Direction.UP)
			return InteractionResult.FAIL;

		BlockPos pos = context.getClickedPos();
		if(!level.getBlockState(pos).canBeReplaced())
			pos = pos.above();

		Direction facing = player.getDirection().getOpposite();

		if(!canPlaceAt(level, pos, facing))
			return InteractionResult.FAIL;

		if(!level.isClientSide())
		{
			place(level, pos, facing);

			ItemStack stack = context.getItemInHand();
			if(!player.getAbilities().instabuild)
				stack.shrink(1);
		}

		return InteractionResult.sidedSuccess(level.isClientSide());
	}

	/**
	 * Ported from {@code ItemAbilitechnosyth#canPlaceAt} - checks the full 3-wide x 2-deep x 4-tall bounding
	 * envelope, not just the 16 actually-occupied cells, matching the original exactly. Public so
	 * {@code client.AbilitechnosynthPreviewClientEvents} can reuse the exact same check to color its
	 * placement-preview outline.
	 */
	public static boolean canPlaceAt(Level level, BlockPos pos, Direction facing)
	{
		for(int y = 0; y < 4; y++)
			for(int z = 0; z >= -1; z--)
				for(int x = -1; x <= 1; x++)
					if(!level.getBlockState(pos.relative(facing, z).relative(facing.getCounterClockWise(), x).above(y)).canBeReplaced())
						return false;
		return true;
	}

	/**
	 * The same full 3-wide x 2-deep x 4-tall bounding envelope {@link #canPlaceAt} checks, as a single
	 * world-space {@link AABB} - real fix over an earlier attempt at
	 * {@code client.AbilitechnosynthPreviewClientEvents} that drew all 16 individual per-cell boxes instead
	 * (a jumbled wireframe mess, caught from a live screenshot next to the real original's own single clean
	 * bounding-box outline - {@code com.mraof.minestuck.client.renderer.MachineOutlineRenderer}, confirmed
	 * via {@code javap}, draws one box per real placed multiblock, not one per sub-block).
	 */
	public static AABB getPlacementEnvelope(BlockPos pos, Direction facing)
	{
		BlockPos corner1 = pos.relative(facing.getCounterClockWise(), -1);
		BlockPos corner2 = pos.relative(facing, -1).relative(facing.getCounterClockWise(), 1).above(3);
		return new AABB(
				Math.min(corner1.getX(), corner2.getX()), Math.min(corner1.getY(), corner2.getY()), Math.min(corner1.getZ(), corner2.getZ()),
				Math.max(corner1.getX(), corner2.getX()) + 1, Math.max(corner1.getY(), corner2.getY()) + 1, Math.max(corner1.getZ(), corner2.getZ()) + 1);
	}

	/** The real 16 block positions {@link #place} fills in, in {@code PART} order (0-15) - extracted purely to avoid duplicating this loop between {@link #place} and {@link #canPlaceAt}'s own siblings. */
	public static List<BlockPos> getPlacementPositions(BlockPos pos, Direction facing)
	{
		List<BlockPos> positions = new ArrayList<>(16);
		for(int y = 0; y < 2; y++)
			for(int z = 0; z >= -1; z--)
				for(int x = -1; x <= 1; x++)
					positions.add(pos.relative(facing, z).relative(facing.getCounterClockWise(), x).above(y));

		for(int x = -1; x <= 1; x++)
			positions.add(pos.relative(facing, -1).relative(facing.getCounterClockWise(), x).above(2));

		positions.add(pos.relative(facing, -1).above(3));
		return positions;
	}

	/** Ported from {@code ItemAbilitechnosyth#placeBlockAt}. */
	private static void place(Level level, BlockPos pos, Direction facing)
	{
		BlockState base = org.wilkretawesomesauce.minestuckuniverseported.MSUBlocks.ABILITECHNOSYNTH.get()
				.defaultBlockState().setValue(AbilitechnosynthBlock.FACING, facing);

		List<BlockPos> positions = getPlacementPositions(pos, facing);
		for(int i = 0; i < positions.size(); i++)
			level.setBlock(positions.get(i), base.setValue(AbilitechnosynthBlock.PART, i), 3);
	}
}
