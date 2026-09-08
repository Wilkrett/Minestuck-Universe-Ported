package org.wilkretawesomesauce.minestuckuniverseported.mechanics.soul;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The per-{@code Level} (Overworld-only, mirroring {@code mechanics.doom.DoomReleasePool}/
 * {@code mechanics.timeline.TimelineBranchRegistry}'s existing "one shared registry, not one per
 * dimension" convention) owner-UUID -&gt; active-body-entity-UUID index - see
 * {@code MSUAttachments#BODY_REGISTRY}. Original design for this project, no 1.12.2 counterpart.
 * <p>
 * This is the single source of truth for the design doc's §21 "one active body per owner" invariant -
 * deliberately kept separate from {@link PlayerSoul} (which only tracks what a soul is currently
 * <i>doing</i>) so ownership bookkeeping can never desync from control state. {@link BodyManager} is the
 * only real consumer; nothing else should touch this map directly.
 */
public class BodyRegistryData implements INBTSerializable<CompoundTag>
{
	private final Map<UUID, UUID> ownerToBody = new LinkedHashMap<>();

	@Nullable
	UUID getBody(UUID owner)
	{
		return ownerToBody.get(owner);
	}

	void setBody(UUID owner, UUID bodyEntityId)
	{
		ownerToBody.put(owner, bodyEntityId);
	}

	void clearBody(UUID owner)
	{
		ownerToBody.remove(owner);
	}

	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider provider)
	{
		CompoundTag nbt = new CompoundTag();
		ListTag list = new ListTag();
		for(Map.Entry<UUID, UUID> entry : ownerToBody.entrySet())
		{
			CompoundTag entryTag = new CompoundTag();
			entryTag.putUUID("Owner", entry.getKey());
			entryTag.putUUID("Body", entry.getValue());
			list.add(entryTag);
		}
		nbt.put("Bodies", list);
		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt)
	{
		ownerToBody.clear();
		if(nbt.contains("Bodies"))
		{
			ListTag list = nbt.getList("Bodies", Tag.TAG_COMPOUND);
			for(int i = 0; i < list.size(); i++)
			{
				CompoundTag entryTag = list.getCompound(i);
				ownerToBody.put(entryTag.getUUID("Owner"), entryTag.getUUID("Body"));
			}
		}
	}
}
