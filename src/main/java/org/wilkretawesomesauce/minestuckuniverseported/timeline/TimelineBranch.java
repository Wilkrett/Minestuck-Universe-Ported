package org.wilkretawesomesauce.minestuckuniverseported.timeline;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.Registries;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Metadata for one parallel timeline branch - a real {@link net.minecraft.server.level.ServerLevel}
 * dynamically created via Infiniverse (see {@link BranchForker}), not the level itself. Tracked in
 * {@link TimelineBranchRegistry}, which lives on the Overworld (the Alpha Timeline) since that's the
 * one level guaranteed to always be loaded, even while any given branch is dormant/unregistered.
 * <p>
 * {@link #parentBranchId} is {@code null} when this branch was forked directly from Alpha - branches
 * can otherwise be forked from any other branch, forming an arbitrary tree, not just a flat list.
 * <p>
 * Not itself an {@code INBTSerializable} attachment - {@link #toNBT()}/{@link #fromNBT(CompoundTag)}
 * are called by {@link TimelineBranchRegistry}'s own (de)serialization, matching how {@code StrifeData}
 * hand-rolls a {@code ListTag} of per-element {@code CompoundTag}s rather than using a Codec.
 */
public final class TimelineBranch
{
	private final UUID id;
	private final ResourceKey<Level> dimensionKey;
	private final String displayName;
	@Nullable
	private final UUID parentBranchId;
	private final UUID creatorId;
	private final long createdGameTime;

	/** Mirrors whether Infiniverse currently has this branch's level registered/ticking. */
	private boolean registered;
	/** Game time (Overworld ticks) this branch went dormant - the idle-prune clock starts here, not at creation. */
	private long lastVisitedGameTime;

	public TimelineBranch(UUID id, ResourceKey<Level> dimensionKey, String displayName, @Nullable UUID parentBranchId,
			UUID creatorId, long createdGameTime, boolean registered, long lastVisitedGameTime)
	{
		this.id = id;
		this.dimensionKey = dimensionKey;
		this.displayName = displayName;
		this.parentBranchId = parentBranchId;
		this.creatorId = creatorId;
		this.createdGameTime = createdGameTime;
		this.registered = registered;
		this.lastVisitedGameTime = lastVisitedGameTime;
	}

	public UUID getId()
	{
		return id;
	}

	public ResourceKey<Level> getDimensionKey()
	{
		return dimensionKey;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	@Nullable
	public UUID getParentBranchId()
	{
		return parentBranchId;
	}

	public UUID getCreatorId()
	{
		return creatorId;
	}

	public long getCreatedGameTime()
	{
		return createdGameTime;
	}

	public boolean isRegistered()
	{
		return registered;
	}

	public void setRegistered(boolean registered)
	{
		this.registered = registered;
	}

	public long getLastVisitedGameTime()
	{
		return lastVisitedGameTime;
	}

	public void setLastVisitedGameTime(long lastVisitedGameTime)
	{
		this.lastVisitedGameTime = lastVisitedGameTime;
	}

	public CompoundTag toNBT()
	{
		CompoundTag nbt = new CompoundTag();
		nbt.putUUID("Id", id);
		nbt.putString("Dimension", dimensionKey.location().toString());
		nbt.putString("Name", displayName);
		if(parentBranchId != null)
			nbt.putUUID("Parent", parentBranchId);
		nbt.putUUID("Creator", creatorId);
		nbt.putLong("Created", createdGameTime);
		nbt.putBoolean("Registered", registered);
		nbt.putLong("LastVisited", lastVisitedGameTime);
		return nbt;
	}

	public static TimelineBranch fromNBT(CompoundTag nbt)
	{
		UUID id = nbt.getUUID("Id");
		ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(nbt.getString("Dimension")));
		String name = nbt.getString("Name");
		UUID parent = nbt.contains("Parent") ? nbt.getUUID("Parent") : null;
		UUID creator = nbt.getUUID("Creator");
		long created = nbt.getLong("Created");
		boolean registered = nbt.getBoolean("Registered");
		long lastVisited = nbt.getLong("LastVisited");
		return new TimelineBranch(id, dimensionKey, name, parent, creator, created, registered, lastVisited);
	}
}
