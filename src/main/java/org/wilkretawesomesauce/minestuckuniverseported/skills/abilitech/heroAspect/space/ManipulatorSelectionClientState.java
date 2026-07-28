package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.space;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-side cache of the local player's own in-progress Matter Manipulator corner selection, kept in
 * sync via {@code network.ManipulatorSelectionSyncPacket}. {@code AbilitechLoadout} (which
 * {@code TechSpaceManipulator} mutates server-side) is never synced to the client, so
 * {@code SpaceManipulatorClientEvents} reads from here instead of the attachment directly.
 */
public final class ManipulatorSelectionClientState
{
	public static final int STATE_CLEARED = 0;
	public static final int STATE_POS1_ONLY = 1;
	public static final int STATE_BOTH = 2;

	private static int state = STATE_CLEARED;
	private static BlockPos pos1 = BlockPos.ZERO;
	private static BlockPos pos2 = BlockPos.ZERO;
	private static ResourceLocation dimension = ResourceLocation.withDefaultNamespace("overworld");

	private ManipulatorSelectionClientState()
	{
	}

	public static void set(int newState, BlockPos newPos1, BlockPos newPos2, ResourceLocation newDimension)
	{
		state = newState;
		pos1 = newPos1;
		pos2 = newPos2;
		dimension = newDimension;
	}

	public static int getState()
	{
		return state;
	}

	public static BlockPos getPos1()
	{
		return pos1;
	}

	public static BlockPos getPos2()
	{
		return pos2;
	}

	public static ResourceLocation getDimension()
	{
		return dimension;
	}
}
