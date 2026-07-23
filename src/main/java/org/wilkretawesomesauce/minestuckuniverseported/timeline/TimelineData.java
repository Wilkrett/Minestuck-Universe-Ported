package org.wilkretawesomesauce.minestuckuniverseported.timeline;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Attached directly to a {@code Level} (see {@link org.wilkretawesomesauce.minestuckuniverseported.MSUAttachments#TIMELINE}) -
 * NeoForge's {@code Level} class extends {@code AttachmentHolder} the same way entities do, so a
 * dimension can carry its own data just like a player can.
 * <p>
 * Implements {@link INBTSerializable} (matching every other attachment in this project) so the
 * registration can use the same confirmed {@code AttachmentType.serializable(...)} pattern - but
 * {@link #serializeNBT}/{@link #deserializeNBT} only actually persist {@link #doomPoints} and
 * {@link #totalRewinds}. The recorded {@link #history} deliberately isn't saved: it's a short rolling
 * window of recent ticks (see {@link org.wilkretawesomesauce.minestuckuniverseported.Config#timelineHistoryTicks}),
 * not something that needs to survive a restart, and serializing potentially-large per-tick block-diff
 * maps into the level's save data on every autosave would be real, ongoing cost for data that's only ever
 * useful for a few seconds. Starting with empty history on load is exactly the behavior wanted anyway.
 * <p>
 * <b>Doom Points (DP)</b> replace the earlier "Timeline Debt" mechanic entirely - Timeline Debt and its
 * consequences ({@code TimelineDebtEvents}, the Weakness/Mining Fatigue "temporal instability sickness")
 * are gone, not renamed. DP is currently just a tracked, accumulating number with no behavior attached -
 * a deliberate placeholder for a real design to be built later, not a like-for-like swap.
 */
public class TimelineData implements INBTSerializable<CompoundTag>
{
	private final Deque<WorldTickSnapshot> history = new ArrayDeque<>();
	private Map<BlockPos, WorldTickSnapshot.BlockChangeRecord> pendingBlockChanges = new HashMap<>();
	private final java.util.List<ActiveRewind> activeRewinds = new java.util.ArrayList<>();
	private final java.util.List<DoomedTimelineClone> doomedClones = new java.util.ArrayList<>();
	private final java.util.List<org.wilkretawesomesauce.minestuckuniverseported.timeline.vision.PastVisionSession> activeVisions = new java.util.ArrayList<>();
	private final java.util.List<org.wilkretawesomesauce.minestuckuniverseported.timeline.loop.TimeLoopZone> activeLoops = new java.util.ArrayList<>();

	/** Currently-active Time Loop zones (see {@code timeline.loop.TimeLoopZone}) - transient, like the other active-effect lists here, ticked by {@code timeline.loop.TimeLoopPlayback}. */
	public java.util.List<org.wilkretawesomesauce.minestuckuniverseported.timeline.loop.TimeLoopZone> getActiveLoops()
	{
		return activeLoops;
	}

	/**
	 * Currently-active Retrocognition visions (see {@code timeline.vision.PastVisionSession}) - transient,
	 * like the other active-effect lists here, ticked by {@code timeline.vision.PastVisionPlayback}.
	 * Replaces the old spectator-teleport {@code PastObserverSession}/{@code activeObservers} entirely -
	 * the new mechanic never touches gamemode or position, so there's nothing left to "return" from.
	 */
	public java.util.List<org.wilkretawesomesauce.minestuckuniverseported.timeline.vision.PastVisionSession> getActiveVisions()
	{
		return activeVisions;
	}

	private double doomPoints = 0;
	private int totalRewinds = 0;

	/** Called from {@link TimelineManager#rewind} - queues a rewind to be played back over time by {@link TimelineRewindPlayback} instead of applied instantly. */
	public void queueRewind(ActiveRewind rewind)
	{
		activeRewinds.add(rewind);
	}

	public java.util.List<ActiveRewind> getActiveRewinds()
	{
		return activeRewinds;
	}

	/** The doomed-timeline clones left standing from past rewinds - see {@link DoomedTimelineClone}. */
	public void addDoomedClone(DoomedTimelineClone clone)
	{
		doomedClones.add(clone);
	}

	public java.util.List<DoomedTimelineClone> getDoomedClones()
	{
		return doomedClones;
	}

	/**
	 * Called from {@link TimelineRecorder}'s block-change event handlers - records the *old* and *new*
	 * state and, if a player caused it, their UUID (see {@link WorldTickSnapshot.BlockChangeRecord}).
	 */
	public void recordBlockChange(BlockPos pos, BlockState oldState, BlockState newState, @javax.annotation.Nullable UUID causedBy)
	{
		pendingBlockChanges.putIfAbsent(pos, new WorldTickSnapshot.BlockChangeRecord(oldState, newState, causedBy));
	}

	/** Called once per level tick to finalize this tick's snapshot and push it onto the history. */
	public void pushTick(Map<UUID, EntitySnapshot> entitySnapshots, int maxHistoryTicks)
	{
		history.addLast(new WorldTickSnapshot(Map.copyOf(pendingBlockChanges), Map.copyOf(entitySnapshots)));
		pendingBlockChanges = new HashMap<>();

		while(history.size() > maxHistoryTicks)
			history.removeFirst();
	}

	public Deque<WorldTickSnapshot> getHistory()
	{
		return history;
	}

	public int getRecordedTicks()
	{
		return history.size();
	}

	/**
	 * Doom Points - a placeholder mechanic. Currently just accumulates from time manipulation with no
	 * consequences attached; the actual design (what DP should do once it builds up) is intentionally
	 * left for later rather than guessed at here.
	 */
	public double getDoomPoints()
	{
		return doomPoints;
	}

	public void addDoomPoints(double amount)
	{
		this.doomPoints = Math.max(0, doomPoints + amount);
	}

	public int getTotalRewinds()
	{
		return totalRewinds;
	}

	public void incrementRewinds()
	{
		totalRewinds++;
	}

	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider provider)
	{
		CompoundTag nbt = new CompoundTag();
		nbt.putDouble("DoomPoints", doomPoints);
		nbt.putInt("TotalRewinds", totalRewinds);
		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt)
	{
		doomPoints = nbt.getDouble("DoomPoints");
		totalRewinds = nbt.getInt("TotalRewinds");
	}
}
