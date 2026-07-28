package org.wilkretawesomesauce.minestuckuniverseported.mechanics.doom;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * The universal Doom value every {@code LivingEntity} carries (see {@code MSUAttachments#DOOM_DATA}) -
 * original design for this project, described directly by the user rather than ported from any real
 * MinestuckUniverse (1.12.2) source, unlike almost everything else in this project.
 * <p>
 * Doom is NOT health/HP/mana/corruption - it's the accumulated metaphysical weight of an entity's
 * relationship with death/mortality/entropy. It's persistent (healing/resting never removes it, only
 * deliberate manipulation does) and while alive it's <i>bound</i> to the entity; death <i>releases</i>
 * it rather than creating it (see {@code DoomReleaseEvents}).
 * <p>
 * <b>Deliberately NOT {@code copyOnDeath()}</b> - the one explicit inversion of this project's usual
 * attachment convention. Every other {@code LivingEntity} attachment in {@code MSUAttachments} (e.g.
 * {@code STRIFE_PORTFOLIO}) uses {@code copyOnDeath()} specifically because, without it, NeoForge
 * resets the attachment to a fresh default on the post-death entity instance (a respawned player is a
 * brand-new {@code Player} instance; a killed mob simply ceases to exist) - for those attachments that
 * reset is a bug to prevent. For {@code DoomData} it's exactly the wanted behavior: death is supposed
 * to release bound Doom out of the body (into the release pool, or redirected by a Dead Shuffle mark),
 * not carry it forward into whatever comes next. A respawned player's new instance starting at zero
 * bound Doom is correct, not a bug.
 */
public class DoomData implements IDoomData, INBTSerializable<CompoundTag>
{
	private double boundDoom = 0.0;
	private long ticksAliveAccrued = 0;
	private boolean sealed = false;

	@Nullable
	private UUID markCasterId = null;
	@Nullable
	private DoomMarkType markType = null;
	private double markAccrualMultiplier = 1.0;

	@Override
	public double getDoom()
	{
		return boundDoom;
	}

	@Override
	public void addDoom(double amount)
	{
		if(sealed)
			return;

		if(amount > 0 && isMarked())
			amount *= markAccrualMultiplier;

		boundDoom = Math.max(0, boundDoom + amount);
	}

	@Override
	public void addDoomRaw(double amount)
	{
		if(sealed)
			return;

		boundDoom = Math.max(0, boundDoom + amount);
	}

	@Override
	public void removeDoom(double amount)
	{
		if(sealed)
			return;

		boundDoom = Math.max(0, boundDoom - amount);
	}

	@Override
	public void setDoom(double amount)
	{
		if(sealed)
			return;

		boundDoom = Math.max(0, amount);
	}

	@Override
	public double transferTo(IDoomData other, double amount)
	{
		double moved = Math.min(amount, boundDoom);
		if(moved <= 0)
			return 0;

		removeDoom(moved);
		other.addDoom(moved);
		return moved;
	}

	@Override
	public boolean isSealed()
	{
		return sealed;
	}

	@Override
	public void setSealed(boolean sealed)
	{
		this.sealed = sealed;
	}

	@Override
	public boolean isMarked()
	{
		return markType != null;
	}

	@Override
	@Nullable
	public DoomMarkType getMarkType()
	{
		return markType;
	}

	@Override
	@Nullable
	public UUID getMarkCasterId()
	{
		return markCasterId;
	}

	@Override
	public double getMarkAccrualMultiplier()
	{
		return markAccrualMultiplier;
	}

	@Override
	public void applyMark(UUID casterId, DoomMarkType type, double accrualMultiplier)
	{
		this.markCasterId = casterId;
		this.markType = type;
		this.markAccrualMultiplier = accrualMultiplier;
	}

	@Override
	public void clearMark()
	{
		markCasterId = null;
		markType = null;
		markAccrualMultiplier = 1.0;
	}

	/** Package-visible - only {@code DoomPassiveAccrualEvents} tracks this. */
	long getTicksAliveAccrued()
	{
		return ticksAliveAccrued;
	}

	/** Package-visible - only {@code DoomPassiveAccrualEvents} tracks this. */
	void addTicksAliveAccrued(long ticks)
	{
		ticksAliveAccrued += ticks;
	}

	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider provider)
	{
		CompoundTag nbt = new CompoundTag();
		nbt.putDouble("BoundDoom", boundDoom);
		nbt.putLong("TicksAliveAccrued", ticksAliveAccrued);
		nbt.putBoolean("Sealed", sealed);
		if(markCasterId != null)
		{
			nbt.putUUID("MarkCasterId", markCasterId);
			nbt.putString("MarkType", markType.name());
			nbt.putDouble("MarkAccrualMultiplier", markAccrualMultiplier);
		}
		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt)
	{
		boundDoom = nbt.getDouble("BoundDoom");
		ticksAliveAccrued = nbt.getLong("TicksAliveAccrued");
		sealed = nbt.getBoolean("Sealed");
		if(nbt.hasUUID("MarkCasterId"))
		{
			markCasterId = nbt.getUUID("MarkCasterId");
			markType = DoomMarkType.valueOf(nbt.getString("MarkType"));
			markAccrualMultiplier = nbt.getDouble("MarkAccrualMultiplier");
		}
		else
		{
			markCasterId = null;
			markType = null;
			markAccrualMultiplier = 1.0;
		}
	}
}
