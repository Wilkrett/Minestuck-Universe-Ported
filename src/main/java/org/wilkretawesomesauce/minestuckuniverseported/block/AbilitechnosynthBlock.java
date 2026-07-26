package org.wilkretawesomesauce.minestuckuniverseported.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.wilkretawesomesauce.minestuckuniverseported.client.gui.MSUAbilitechScreen;

import java.util.List;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code blocks.BlockAbilitechnosynth} - a real, 16-position
 * multiblock ({@code com.mraof.minestuck.block.BlockLargeMachine} in the original, 4 separate block
 * classes each carrying a {@code PART} property 0-3, indexed together as {@code classIndex*4 + PART}).
 * An earlier pass of this port substituted a single ordinary block for this because the real geometry
 * "wasn't portable without being able to render and check it" - that's no longer true: the original's
 * real per-part Blockbench geometry ({@code models/block/abilitechnosynth_1.json}...{@code _16.json})
 * was already sitting in this project's resources (just wired with a stale {@code minestuckuniverse:}
 * texture namespace and blockstate variants that didn't match anything), and the original's exact
 * placement/collision/validation math was recovered from the decompiled source
 * ({@code BlockAbilitechnosynth}/{@code ItemAbilitechnosyth}) rather than guessed. This class now
 * models the same 16 positions as one {@link Block} with two properties - {@link #FACING} (4 values)
 * and {@link #PART} (0-15, replacing the original's {@code classIndex*4 + PART} indexing scheme
 * directly, one value per position) - instead of 4 separate block classes, since modern blockstates
 * don't need separate classes to carry extra properties the way 1.12.2's did. Placement is handled by
 * {@link org.wilkretawesomesauce.minestuckuniverseported.item.AbilitechnosynthItem}, ported from
 * {@code items.ItemAbilitechnosyth}.
 * <p>
 * The GUI itself ({@code gui.GuiFraymachine} in the original, shared with the original's separate,
 * never-modeled "mini" single-block Fraymachine variant) is ported as {@link MSUAbilitechScreen}, only
 * ever opened once {@link #isValid} confirms the full 16-block structure is actually intact - matching
 * the original's own {@code ItemAbilitechnosyth.isValid} gate.
 * <p>
 * Opens that screen via {@link MSUAbilitechScreen#open()} rather than calling
 * {@code Minecraft.getInstance().setScreen(...)} inline here - see that method's own doc comment for why
 * a bare call like that, inlined into a common class, used to crash a dedicated server outright (this
 * was CLAUDE.md's documented known gap #6, now fixed here and in {@code items.StrifeCardItem}).
 */
public class AbilitechnosynthBlock extends Block
{
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, 15);

	/**
	 * Ported 1:1 from the original's {@code BlockAbilitechnosynth#COLLISION_AABBS} - one sub-box list per
	 * part index (0-15), authored assuming {@link Direction#NORTH} (kept exactly, including the original's
	 * own {@code 5/15d} (not {@code 5/16d}) typo in parts 0/1/2/3/5 - not "corrected", per this project's
	 * standing practice of not quietly rebalancing the original's own numbers).
	 */
	private static final List<AABB>[] PART_BOXES = buildPartBoxes();

	/** Precomputed per (part, facing) union shape - same rotation math as the original's own per-call rotation, just cached once. */
	private static final VoxelShape[][] SHAPES = buildShapes();

	public AbilitechnosynthBlock(Properties properties)
	{
		super(properties);
		registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH).setValue(PART, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		builder.add(FACING, PART);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		return shapeFor(state);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		return shapeFor(state);
	}

	private static VoxelShape shapeFor(BlockState state)
	{
		return SHAPES[state.getValue(PART)][state.getValue(FACING).get2DDataValue()];
	}

	/**
	 * Ported from {@code BlockAbilitechnosynth#onBlockActivated}: only the front face (matching
	 * {@link #FACING}) or, for the middle "screen" row (part 6-11), the top face opens the GUI - and
	 * only once {@link #isValid} confirms the full structure. Matches the original's own behavior of
	 * silently consuming the click (no message) if the structure is incomplete, rather than failing
	 * the interaction outright.
	 */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
	{
		Direction facing = state.getValue(FACING);
		int part = state.getValue(PART);
		Direction clickedFace = hit.getDirection();

		if(!player.isShiftKeyDown() && (clickedFace == facing || (part >= 6 && part < 12 && clickedFace == Direction.UP)))
		{
			if(level.isClientSide() && isValid(state, level, pos))
				MSUAbilitechScreen.open();
			return InteractionResult.sidedSuccess(level.isClientSide());
		}
		return InteractionResult.PASS;
	}

	/**
	 * Ported from {@code ItemAbilitechnosyth#isValid} - walks back from this block's own known
	 * {@link #PART} to the structure's placement anchor (the same anchor
	 * {@link org.wilkretawesomesauce.minestuckuniverseported.item.AbilitechnosynthItem} placed
	 * relative to), then re-derives and checks all 16 expected positions/states forward from there,
	 * exactly like the original.
	 */
	public static boolean isValid(BlockState state, Level level, BlockPos pos)
	{
		if(!(state.getBlock() instanceof AbilitechnosynthBlock))
			return false;

		Direction facing = state.getValue(FACING);
		int part = state.getValue(PART);

		BlockPos anchor;
		if(part < 12)
			anchor = pos.relative(facing.getClockWise(), (part % 3) - 1).below(part / 6).relative(facing, (part / 3) % 2);
		else if(part < 15)
			anchor = pos.relative(facing.getClockWise(), (part % 3) - 1).below(2).relative(facing, 1);
		else
			anchor = pos.below(3).relative(facing, 1);

		int i = 0;
		for(int y = 0; y < 2; y++)
			for(int z = 0; z >= -1; z--)
				for(int x = -1; x <= 1; x++)
				{
					BlockPos check = anchor.relative(facing, z).relative(facing.getCounterClockWise(), x).above(y);
					if(!matchesPart(level.getBlockState(check), facing, i))
						return false;
					i++;
				}

		for(int x = -1; x <= 1; x++)
		{
			BlockPos check = anchor.relative(facing, -1).relative(facing.getCounterClockWise(), x).above(2);
			if(!matchesPart(level.getBlockState(check), facing, i))
				return false;
			i++;
		}

		return matchesPart(level.getBlockState(anchor.relative(facing, -1).above(3)), facing, i);
	}

	private static boolean matchesPart(BlockState state, Direction facing, int part)
	{
		return state.getBlock() instanceof AbilitechnosynthBlock
				&& state.getValue(FACING) == facing
				&& state.getValue(PART) == part;
	}

	@SuppressWarnings("unchecked")
	private static List<AABB>[] buildPartBoxes()
	{
		return new List[]{
				List.of(new AABB(0, 0, 0, 13 / 16d, 5 / 15d, 1), new AABB(7 / 16d, 5 / 16d, 2 / 16d, 11 / 16d, 1, 1)),
				List.of(new AABB(0, 0, 0, 1, 5 / 15d, 1)),
				List.of(new AABB(3 / 16d, 0, 0, 1, 5 / 15d, 1), new AABB(5 / 16d, 5 / 16d, 2 / 16d, 9 / 16d, 1, 1)),
				List.of(new AABB(0, 0, 0, 13 / 16d, 5 / 15d, 1), new AABB(0, 5 / 16d, 0, 12 / 16d, 1, 1)),
				List.of(new AABB(0, 0, 0, 1, 1, 1)),
				List.of(new AABB(3 / 16d, 0, 0, 1, 5 / 15d, 1), new AABB(1 / 16d, 5 / 16d, 0, 1, 1, 1)),
				List.of(new AABB(7 / 16d, 0, 0, 11 / 16d, 7.5 / 16d, 1), new AABB(0, 0, 12.1 / 16d, 7 / 16d, 2 / 16d, 1)),
				List.of(new AABB(0, 0, 12.1 / 16d, 1, 2 / 16d, 1)),
				List.of(new AABB(5 / 16d, 0, 0, 9 / 16d, 7.5 / 16d, 1), new AABB(9 / 16d, 0, 12.1 / 16d, 1, 2 / 16d, 1)),
				List.of(new AABB(6.99 / 16d, 0, 0, 11.02 / 16d, 7.5 / 16d, 12 / 16d), new AABB(0, 0, 0, 7 / 16d, 4.5 / 16d, .5d),
						new AABB(0, 0, 0.5d, .5d, 1, .5d), new AABB(.5d, 0, 12.01 / 16d, 4 / 16d, 14 / 16d, 1)),
				List.of(new AABB(0, 0, 0, 1, 4.5 / 16d, .5d), new AABB(0, 0, 0.5d, 1, 1, .5d)),
				List.of(new AABB(9 / 16d, 0, 0, 7 / 16d, 4.5 / 16d, .5d), new AABB(4.99 / 16d, 0, 0, 9.01 / 16d, 7.5 / 16d, 12 / 16d),
						new AABB(.5d, 0, .5d, 1, 1, 1), new AABB(4 / 16d, 0, 12.01 / 16d, .5d, 14 / 16d, 1)),
				List.of(new AABB(0, 0, 12 / 16d, 4 / 16d, 14 / 16d, 1), new AABB(4 / 16d, 0, 12 / 16d, .5d, 7 / 16d, 1)),
				List.of(new AABB(0, 0, .5d, 4 / 16d, 6 / 16d, 12 / 16d), new AABB(4 / 16d, 0, .5d, 12 / 16d, 9 / 16d, 12 / 16d),
						new AABB(12 / 16d, 0, .5d, 1, 6 / 16d, 12 / 16d), new AABB(0, 0, 12 / 16d, 1, 1, 1)),
				List.of(new AABB(.5d, 0, 12 / 16d, 12 / 16d, 7 / 16d, 1), new AABB(12 / 16d, 0, 12 / 16d, 1, 1, 1)),
				List.of(new AABB(12 / 16d, 0, 12 / 16d, 1, 3 / 16d, 1), new AABB(4 / 16d, 0, 12 / 16d, 12 / 16d, 7 / 16d, 1),
						new AABB(0, 0, 12 / 16d, 4 / 16d, 3 / 16d, 1))
		};
	}

	/** Ported from {@code BlockAbilitechnosynth}'s use of {@code BlockHolopad.modifyAABBForDirection} - {@link Direction#NORTH} is the identity orientation the boxes above were authored for. */
	private static AABB rotate(Direction facing, AABB bb)
	{
		return switch(facing)
		{
			case SOUTH -> new AABB(1 - bb.maxX, bb.minY, 1 - bb.maxZ, 1 - bb.minX, bb.maxY, 1 - bb.minZ);
			case WEST -> new AABB(bb.minZ, bb.minY, 1 - bb.maxX, bb.maxZ, bb.maxY, 1 - bb.minX);
			case EAST -> new AABB(1 - bb.maxZ, bb.minY, bb.minX, 1 - bb.minZ, bb.maxY, bb.maxX);
			default -> bb;
		};
	}

	private static VoxelShape[][] buildShapes()
	{
		VoxelShape[][] shapes = new VoxelShape[16][4];
		Direction[] horizontals = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};
		for(int part = 0; part < 16; part++)
		{
			for(Direction facing : horizontals)
			{
				VoxelShape shape = Shapes.empty();
				for(AABB box : PART_BOXES[part])
					shape = Shapes.or(shape, Shapes.create(rotate(facing, box)));
				shapes[part][facing.get2DDataValue()] = shape;
			}
		}
		return shapes;
	}
}
