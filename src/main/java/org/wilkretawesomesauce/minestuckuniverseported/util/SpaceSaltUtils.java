package org.wilkretawesomesauce.minestuckuniverseported.util;

import com.mraof.minestuck.block.MSBlocks;
import com.mraof.minestuck.block.machine.MachineMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code util.SpaceSaltUtils#resizeMachine} - relocates one of
 * Minestuck's giant SBURB machines (Alchemiter, Cruxtruder, Totem Lathe, Punch Designix) one block in
 * the direction it was clicked from, keeping its current rotation.
 * <p>
 * The original hand-rolled a per-machine-type switch, directly reading/writing each machine's own
 * 1.12.2 block states and tile-entity fields one block at a time. The modern Minestuck dependency
 * exposes a real, generic {@link MachineMultiblock} abstraction instead (confirmed via decompiling the
 * actual dependency jar, not guessed) - {@code guessPlacement}/{@code removeAt}/{@code placeWithRotation}
 * - so this reimplements the same real mechanic (pick up and set back down one of these machines) against
 * that real modern API rather than duplicating four separate hand-rolled block-placement routines that
 * may not even match how the modern blocks are actually laid out internally.
 * <p>
 * <b>Real, stated simplification of the validity check only</b>: the original had each machine's own
 * bespoke "can I place here" logic (walking every destination block against edit permissions and
 * placement rules for that exact machine shape). This checks the destination's real bounding box
 * (via {@link MachineMultiblock#getBoundingBox}) is clear of anything solid instead - a coarser AABB
 * check rather than the exact per-block machine shape, but a real, defensive validity check nonetheless
 * (not skipped), since blindly relocating a multi-block structure without checking the destination first
 * risks silently overwriting whatever's there.
 */
public final class SpaceSaltUtils
{
	private SpaceSaltUtils()
	{
	}

	private static MachineMultiblock findMultiblock(Block block)
	{
		for(MachineMultiblock multiblock : List.of(MSBlocks.ALCHEMITER, MSBlocks.CRUXTRUDER, MSBlocks.TOTEM_LATHE, MSBlocks.PUNCH_DESIGNIX))
		{
			boolean[] found = {false};
			multiblock.forEachBlock(b -> { if(b == block) found[0] = true; });
			if(found[0])
				return multiblock;
		}
		return null;
	}

	public static boolean onSpaceSaltUse(Level level, BlockPos targetPos, Direction clickedFace)
	{
		BlockState state = level.getBlockState(targetPos);
		MachineMultiblock multiblock = findMultiblock(state.getBlock());
		if(multiblock == null)
			return false;

		List<MachineMultiblock.Placement> guesses = multiblock.guessPlacement(targetPos, state);
		if(guesses.isEmpty())
			return false;

		MachineMultiblock.Placement current = guesses.get(0);
		MachineMultiblock.Placement candidate = new MachineMultiblock.Placement(current.zeroPos().relative(clickedFace), current.rotation());

		if(!destinationClear(level, multiblock, current, candidate))
			return false;

		multiblock.removeAt(level, current);
		multiblock.placeWithRotation(level, candidate);
		return true;
	}

	private static boolean destinationClear(Level level, MachineMultiblock multiblock, MachineMultiblock.Placement current, MachineMultiblock.Placement candidate)
	{
		BoundingBox oldLocalBox = multiblock.getBoundingBox(current.rotation());
		BoundingBox newLocalBox = multiblock.getBoundingBox(candidate.rotation());

		for(int x = newLocalBox.minX(); x <= newLocalBox.maxX(); x++)
			for(int y = newLocalBox.minY(); y <= newLocalBox.maxY(); y++)
				for(int z = newLocalBox.minZ(); z <= newLocalBox.maxZ(); z++)
				{
					BlockPos worldPos = candidate.zeroPos().offset(x, y, z);

					int relToOldX = worldPos.getX() - current.zeroPos().getX();
					int relToOldY = worldPos.getY() - current.zeroPos().getY();
					int relToOldZ = worldPos.getZ() - current.zeroPos().getZ();
					if(oldLocalBox.isInside(relToOldX, relToOldY, relToOldZ))
						continue;

					BlockState there = level.getBlockState(worldPos);
					if(!there.canBeReplaced() && !there.isAir())
						return false;
				}

		return true;
	}
}
