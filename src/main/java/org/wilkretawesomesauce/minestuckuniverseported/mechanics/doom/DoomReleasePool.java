package org.wilkretawesomesauce.minestuckuniverseported.mechanics.doom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The per-{@code Level} registry of unbound, unstable, harvestable Doom released by dead entities -
 * see {@code MSUAttachments#DOOM_RELEASE_POOL} and {@code DoomReleaseEvents}. Original design for
 * this project, no 1.12.2 counterpart. A {@code Level}-attached registry rather than a new
 * {@code Entity} class - the MVP scope has no per-record visual/behavior requirement, and a new
 * entity class is real, non-trivial effort (see this project's one recent shared new-entity
 * precedent, {@code entity.BubbleEntity}) for a mechanic whose only requirement is "query/consume by
 * position and radius within a time window". Follows {@code TimelineBranchRegistry}'s existing
 * manual-NBT {@link ListTag}-of-{@link CompoundTag} convention.
 */
public class DoomReleasePool implements INBTSerializable<CompoundTag>
{
	private final Map<UUID, DoomReleaseRecord> pending = new LinkedHashMap<>();

	/** Called by {@code DoomReleaseEvents} at death. */
	public UUID release(BlockPos pos, double amount, long expiryGameTime, @Nullable UUID sourceEntityId)
	{
		UUID id = UUID.randomUUID();
		pending.put(id, new DoomReleaseRecord(id, pos, amount, expiryGameTime, sourceEntityId));
		return id;
	}

	/**
	 * Public API for a future harvesting tech to call - consumes up to {@code maxAmount} from records
	 * within {@code radius} of {@code center}, oldest-first, shrinking/removing records as consumed.
	 * Returns the actual amount harvested.
	 */
	public double harvest(BlockPos center, double radius, double maxAmount)
	{
		double radiusSq = radius * radius;
		double harvested = 0;

		Iterator<DoomReleaseRecord> it = pending.values().iterator();
		while(it.hasNext() && harvested < maxAmount)
		{
			DoomReleaseRecord record = it.next();
			if(record.getPos().distSqr(center) > radiusSq)
				continue;

			harvested += record.shrink(maxAmount - harvested);
			if(record.getAmount() <= 0)
				it.remove();
		}

		return harvested;
	}

	/** Non-consuming total query within radius - the "harvest window" the design doc describes. */
	public double peekAvailable(BlockPos center, double radius)
	{
		double radiusSq = radius * radius;
		double total = 0;
		for(DoomReleaseRecord record : pending.values())
			if(record.getPos().distSqr(center) <= radiusSq)
				total += record.getAmount();
		return total;
	}

	/**
	 * Called once per {@code Config.doomReleaseTickIntervalTicks} by {@code DoomReleaseEvents} -
	 * removes (dissipates) any record whose window has passed. Dissipation is silent by design (Scope
	 * note: a future pass could hook a removal callback here for ambient particle/sound flavor at the
	 * death site - no forced mechanic yet).
	 */
	public void tick(long currentGameTime)
	{
		pending.values().removeIf(record -> record.isExpired(currentGameTime));
	}

	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider provider)
	{
		CompoundTag nbt = new CompoundTag();
		ListTag list = new ListTag();
		for(DoomReleaseRecord record : pending.values())
			list.add(record.toNBT());
		nbt.put("Pending", list);
		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt)
	{
		pending.clear();
		if(nbt.contains("Pending"))
		{
			ListTag list = nbt.getList("Pending", Tag.TAG_COMPOUND);
			for(int i = 0; i < list.size(); i++)
			{
				DoomReleaseRecord record = DoomReleaseRecord.fromNBT(list.getCompound(i));
				pending.put(record.getId(), record);
			}
		}
	}
}
