package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.request;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Per-player attachment for the Time Request / Doom System (see {@code CLAUDE.md}'s section on it) -
 * registered as {@code MSUAttachments.TIME_REQUEST_DATA}, {@code copyOnDeath()} like {@code StrifeData}/
 * {@code AbilitechLoadout}/{@code godtier.GodTierData} so open paradoxes and their accrued Doom Points
 * survive an ordinary respawn rather than being silently wiped.
 * <p>
 * Deliberately separate from {@code mechanics.timeline.TimelineData} - that attachment (and its own Doom Points
 * field) belongs to the already-built rewind/branch system and is untouched by this one. Same name,
 * unrelated bookkeeping, by design.
 */
public class TimeRequestData implements INBTSerializable<CompoundTag>
{
	private final List<TimeRequest> openRequests = new ArrayList<>();
	private long lastRequestGameTime = Long.MIN_VALUE;

	public List<TimeRequest> getOpenRequests()
	{
		return openRequests;
	}

	public void addRequest(TimeRequest request)
	{
		openRequests.add(request);
	}

	/** Removes and returns the request with the given id, or null if none is open. Called once a repayment is accepted. */
	public TimeRequest removeRequest(UUID id)
	{
		for(int i = 0; i < openRequests.size(); i++)
			if(openRequests.get(i).getId().equals(id))
				return openRequests.remove(i);
		return null;
	}

	public double getTotalDoomPoints()
	{
		double total = 0;
		for(TimeRequest request : openRequests)
			total += request.getDoomPoints();
		return total;
	}

	public long getLastRequestGameTime()
	{
		return lastRequestGameTime;
	}

	public void setLastRequestGameTime(long lastRequestGameTime)
	{
		this.lastRequestGameTime = lastRequestGameTime;
	}

	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider provider)
	{
		CompoundTag nbt = new CompoundTag();

		ListTag list = new ListTag();
		for(TimeRequest request : openRequests)
			list.add(request.toNBT());
		nbt.put("Requests", list);

		nbt.putLong("LastRequest", lastRequestGameTime);
		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt)
	{
		openRequests.clear();
		if(nbt.contains("Requests"))
		{
			ListTag list = nbt.getList("Requests", Tag.TAG_COMPOUND);
			for(int i = 0; i < list.size(); i++)
				openRequests.add(TimeRequest.fromNBT(list.getCompound(i)));
		}

		lastRequestGameTime = nbt.getLong("LastRequest");
	}
}
