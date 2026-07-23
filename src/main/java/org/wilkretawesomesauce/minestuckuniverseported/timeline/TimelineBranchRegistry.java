package org.wilkretawesomesauce.minestuckuniverseported.timeline;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The tree of every {@link TimelineBranch} that currently exists, attached to and only ever fetched
 * from the Overworld (see {@code MSUAttachments#TIMELINE_BRANCHES}) - the Overworld is the Alpha
 * Timeline, always loaded whenever the server is up, which makes it a safe single source of truth
 * for the tree even while any individual branch's own {@code ServerLevel} is dormant/unregistered.
 * <p>
 * Follows {@code TimelineData}/{@code StrifePortfolio}'s existing manual-NBT convention (a hand-rolled
 * {@link ListTag} of per-branch {@link CompoundTag}s via {@link TimelineBranch#toNBT()}) rather than
 * introducing Codec-based (de)serialization, which nothing else in this attachment system uses.
 */
public class TimelineBranchRegistry implements INBTSerializable<CompoundTag>
{
	private final Map<UUID, TimelineBranch> branches = new LinkedHashMap<>();

	public void add(TimelineBranch branch)
	{
		branches.put(branch.getId(), branch);
	}

	public void remove(UUID id)
	{
		branches.remove(id);
	}

	@Nullable
	public TimelineBranch get(UUID id)
	{
		return branches.get(id);
	}

	public Collection<TimelineBranch> getAll()
	{
		return branches.values();
	}

	@Nullable
	public TimelineBranch findByDimension(ResourceKey<Level> dimensionKey)
	{
		for(TimelineBranch branch : branches.values())
			if(branch.getDimensionKey().equals(dimensionKey))
				return branch;
		return null;
	}

	@Nullable
	public TimelineBranch findByName(String name)
	{
		for(TimelineBranch branch : branches.values())
			if(branch.getDisplayName().equalsIgnoreCase(name))
				return branch;
		return null;
	}

	@Nullable
	public TimelineBranch findByIdOrName(String idOrName)
	{
		try
		{
			UUID id = UUID.fromString(idOrName);
			TimelineBranch branch = get(id);
			if(branch != null)
				return branch;
		}
		catch(IllegalArgumentException ignored)
		{
		}
		return findByName(idOrName);
	}

	public List<TimelineBranch> childrenOf(@Nullable UUID parentId)
	{
		List<TimelineBranch> children = new ArrayList<>();
		for(TimelineBranch branch : branches.values())
			if(java.util.Objects.equals(branch.getParentBranchId(), parentId))
				children.add(branch);
		return children;
	}

	public int countCreatedBy(UUID creatorId)
	{
		int count = 0;
		for(TimelineBranch branch : branches.values())
			if(branch.getCreatorId().equals(creatorId))
				count++;
		return count;
	}

	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider provider)
	{
		CompoundTag nbt = new CompoundTag();
		ListTag list = new ListTag();
		for(TimelineBranch branch : branches.values())
			list.add(branch.toNBT());
		nbt.put("Branches", list);
		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt)
	{
		branches.clear();
		if(nbt.contains("Branches"))
		{
			ListTag list = nbt.getList("Branches", Tag.TAG_COMPOUND);
			for(int i = 0; i < list.size(); i++)
			{
				TimelineBranch branch = TimelineBranch.fromNBT(list.getCompound(i));
				branches.put(branch.getId(), branch);
			}
		}
	}
}
