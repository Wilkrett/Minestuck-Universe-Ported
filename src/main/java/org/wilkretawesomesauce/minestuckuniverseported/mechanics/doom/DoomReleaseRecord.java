package org.wilkretawesomesauce.minestuckuniverseported.mechanics.doom;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * One pending release entry in a {@link DoomReleasePool} - the unbound, unstable Doom a dead entity
 * left behind at its death position, harvestable until {@link #expiryGameTime} passes. Original design
 * for this project, no 1.12.2 counterpart. Plain hand-serialized class, following
 * {@code TimelineBranch}'s existing manual-NBT convention rather than introducing Codec-based
 * (de)serialization (nothing else in this project's attachment system uses one).
 */
public final class DoomReleaseRecord
{
	private final UUID id;
	private final BlockPos pos;
	private double amount;
	private final long expiryGameTime;
	@Nullable
	private final UUID sourceEntityId;

	public DoomReleaseRecord(UUID id, BlockPos pos, double amount, long expiryGameTime, @Nullable UUID sourceEntityId)
	{
		this.id = id;
		this.pos = pos;
		this.amount = amount;
		this.expiryGameTime = expiryGameTime;
		this.sourceEntityId = sourceEntityId;
	}

	public UUID getId()
	{
		return id;
	}

	public BlockPos getPos()
	{
		return pos;
	}

	public double getAmount()
	{
		return amount;
	}

	/** Removes up to {@code taken} from this record's remaining amount, returns the actual amount removed. */
	double shrink(double taken)
	{
		double removed = Math.min(taken, amount);
		amount -= removed;
		return removed;
	}

	public long getExpiryGameTime()
	{
		return expiryGameTime;
	}

	@Nullable
	public UUID getSourceEntityId()
	{
		return sourceEntityId;
	}

	public boolean isExpired(long currentGameTime)
	{
		return currentGameTime >= expiryGameTime;
	}

	CompoundTag toNBT()
	{
		CompoundTag nbt = new CompoundTag();
		nbt.putUUID("Id", id);
		nbt.putInt("PosX", pos.getX());
		nbt.putInt("PosY", pos.getY());
		nbt.putInt("PosZ", pos.getZ());
		nbt.putDouble("Amount", amount);
		nbt.putLong("ExpiryGameTime", expiryGameTime);
		if(sourceEntityId != null)
			nbt.putUUID("SourceEntityId", sourceEntityId);
		return nbt;
	}

	static DoomReleaseRecord fromNBT(CompoundTag nbt)
	{
		UUID id = nbt.getUUID("Id");
		BlockPos pos = new BlockPos(nbt.getInt("PosX"), nbt.getInt("PosY"), nbt.getInt("PosZ"));
		double amount = nbt.getDouble("Amount");
		long expiryGameTime = nbt.getLong("ExpiryGameTime");
		UUID sourceEntityId = nbt.hasUUID("SourceEntityId") ? nbt.getUUID("SourceEntityId") : null;
		return new DoomReleaseRecord(id, pos, amount, expiryGameTime, sourceEntityId);
	}
}
