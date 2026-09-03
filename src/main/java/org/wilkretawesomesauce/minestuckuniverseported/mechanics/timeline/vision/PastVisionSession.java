package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.vision;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.WorldTickSnapshot;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One player's active Retrocognition vision - the real "see the past" mechanic, replacing the old
 * spectator-teleport {@code PastObserverSession} entirely (different enough mechanically that
 * extending it didn't make sense - no gamemode or position is ever touched here). See
 * {@code abilitech.heroAspect.time.TechRetrocognition} for how one gets created and
 * {@code timeline.vision.PastVisionPlayback} for how it's driven.
 * <p>
 * The player stays in their own body the whole time; the overlay radius is recomputed around their
 * live position every tick by {@code PastVisionPlayback} (using its own {@code OVERLAY_RADIUS}),
 * not fixed at cast time. {@link #overlaidBlocks} and
 * {@link #activeGhosts} track what's currently faked for this specific player's connection, so
 * {@code PastVisionPlayback} can tell what needs to newly appear, update, or resync-and-disappear each
 * tick as the radius moves and playback advances.
 * <p>
 * {@link #blockChangeIndex} is built once, here, from the whole captured window - {@code TimelineRecorder}
 * only ever records sparse per-position diffs, so a position with zero entries in this index is already
 * showing its historical truth (nothing to fake, ever, for that position). See
 * {@code PastVisionSession#historicalStateAt} for how an index entry list resolves to "the correct state
 * to show at tick i".
 */
public final class PastVisionSession
{
	/** One recorded change to a position, keyed by its index into the session's window (not the game tick itself). */
	private record IndexedChange(int tickIndex, BlockState oldState, BlockState newState)
	{
	}

	private final UUID playerId;
	private final List<WorldTickSnapshot> window;
	private final Map<BlockPos, List<IndexedChange>> blockChangeIndex;
	private final Map<BlockPos, BlockState> overlaidBlocks = new HashMap<>();
	private final Map<UUID, GhostEntity> activeGhosts = new HashMap<>();

	private int playbackIndex = 0;

	public PastVisionSession(UUID playerId, List<WorldTickSnapshot> window)
	{
		this.playerId = playerId;
		this.window = window;
		this.blockChangeIndex = buildBlockChangeIndex(window);
	}

	private static Map<BlockPos, List<IndexedChange>> buildBlockChangeIndex(List<WorldTickSnapshot> window)
	{
		Map<BlockPos, List<IndexedChange>> index = new HashMap<>();
		for(int i = 0; i < window.size(); i++)
			for(Map.Entry<BlockPos, WorldTickSnapshot.BlockChangeRecord> entry : window.get(i).blockChanges().entrySet())
			{
				WorldTickSnapshot.BlockChangeRecord record = entry.getValue();
				index.computeIfAbsent(entry.getKey(), pos -> new ArrayList<>())
						.add(new IndexedChange(i, record.oldState(), record.newState()));
			}
		return index;
	}

	/**
	 * The historically-correct state to show at {@code tickIndex} for {@code pos}, or null if this
	 * position never changes anywhere in the captured window (meaning the real, current block already
	 * matches its own history - nothing to fake).
	 */
	@Nullable
	public BlockState historicalStateAt(BlockPos pos, int tickIndex)
	{
		List<IndexedChange> changes = blockChangeIndex.get(pos);
		if(changes == null)
			return null;

		IndexedChange lastAtOrBefore = null;
		for(IndexedChange change : changes)
			if(change.tickIndex() <= tickIndex)
				lastAtOrBefore = change;
			else
				break;

		// A change exists, but only later in the window - the position held its pre-window value
		// (that change's own oldState) for this whole span so far.
		return lastAtOrBefore != null ? lastAtOrBefore.newState() : changes.get(0).oldState();
	}

	public UUID getPlayerId()
	{
		return playerId;
	}

	public List<WorldTickSnapshot> getWindow()
	{
		return window;
	}

	public int getPlaybackIndex()
	{
		return playbackIndex;
	}

	public void advance()
	{
		playbackIndex++;
	}

	public boolean isDone()
	{
		return playbackIndex >= window.size();
	}

	public Map<BlockPos, BlockState> getOverlaidBlocks()
	{
		return overlaidBlocks;
	}

	public Map<UUID, GhostEntity> getActiveGhosts()
	{
		return activeGhosts;
	}
}
