package org.wilkretawesomesauce.minestuckuniverseported.timeline;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

/**
 * One tick's worth of recorded world state, enough to undo that tick: what each changed block *was*
 * before it changed (and who changed it, if a player did), and what each tracked living entity's state
 * *was* at the start of the tick.
 * <p>
 * Restoring a snapshot means writing {@link #blockChanges} back onto the world and applying
 * {@link #entitySnapshots} back onto whichever of those entities still exist - walking backward through
 * consecutive snapshots this way reconstructs "the world N ticks ago" one tick at a time, the same way an
 * undo stack works, rather than storing full world copies (which would be prohibitively expensive).
 *
 * @param blockChanges    block position -> what it was before this tick's change and who caused it
 *                        (empty if nothing changed this tick)
 * @param entitySnapshots entity UUID -> its state at the *start* of this tick, for every living entity
 *                        that was being tracked this tick (see {@link TimelineRecorder} for what "tracked"
 *                        means - not every entity in the level, for cost reasons)
 */
public record WorldTickSnapshot(Map<BlockPos, BlockChangeRecord> blockChanges, Map<UUID, EntitySnapshot> entitySnapshots)
{
	public static WorldTickSnapshot empty()
	{
		return new WorldTickSnapshot(Map.of(), Map.of());
	}

	/**
	 * @param oldState the block's state before the change this record undoes
	 * @param newState the block's state after the change - what it became. Not needed by the
	 *                 destructive-undo path (rewind only ever walks backward via {@code oldState}), but
	 *                 needed by {@code timeline.loop.TimeLoopZone}'s forward replay pass, which needs to
	 *                 know what a block visibly becomes as the loop plays forward again, not just what it
	 *                 used to be. For a break, this is approximated as air ({@code BlockEvent.BreakEvent}
	 *                 fires before removal and carries no post-break state) - stated as a known
	 *                 approximation, not exact for the rare block with special after-break behavior.
	 * @param causedBy the player who caused the change, if any (null for fluid spread and other
	 *                 non-player causes) - used by {@link DoomedTimelineClone} to know which block changes
	 *                 it should visibly "perform" (swing + trigger) as it replays, rather than the change
	 *                 just happening on its own with no apparent cause
	 */
	public record BlockChangeRecord(BlockState oldState, BlockState newState, @Nullable UUID causedBy)
	{
	}
}

