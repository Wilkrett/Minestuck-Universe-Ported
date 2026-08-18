package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nullable;

/**
 * Genuinely new, project-original scratch state ({@link #getSlotHistory}/{@link #getLandEntryPos}/
 * {@link #isDragonAuraActive}) with no original MinestuckUniverse (1.12.2) counterpart. This class used to
 * also hold the {@code capabilities.badgeEffects.IBadgeEffects} fields (tether, external-tech borrowing,
 * warp point, etc.) as a user-requested consolidation, but those moved back out to their own
 * {@code capabilities.badgeEffects.BadgeEffects} attachment per a later, explicit correction - see that
 * class's own doc comment. Nothing left here is a ported badge effect.
 * <p>
 * Only {@link #landEntryPos}/{@link #landEntryDim} is persisted; everything else here is transient scratch
 * state.
 */
public class AbilitechLoadout implements INBTSerializable<CompoundTag>
{
	public static final int SLOTS = 3;

	// Transient (never serialized) per-slot scratch state for techs that need to remember something
	// across ticks without needing a dedicated attachment of their own - e.g. TechHeartProject's astral
	// origin, TechTimeRecall's position history.
	@SuppressWarnings("unchecked")
	private final java.util.Deque<Object>[] slotHistory = new java.util.ArrayDeque[SLOTS];

	public java.util.Deque<Object> getSlotHistory(int slot)
	{
		int i = clampSlot(slot);
		if(slotHistory[i] == null)
			slotHistory[i] = new java.util.ArrayDeque<>();
		return slotHistory[i];
	}

	private static int clampSlot(int slot)
	{
		return Math.min(SLOTS - 1, Math.max(0, slot));
	}

	// Transient (never serialized) marker for TechDragonAura - is the caster currently holding the key
	// down? Used to be a synced MobEffect (DragonAuraEffect) purely so a global LivingDamageEvent.Post
	// handler (which only ever gets handed an arbitrary Player, not a TechDragonAura instance) could ask
	// "is this player currently holding Dragon Aura" - but that handler only ever runs server-side and
	// never actually needed the sync, so a plain scratch field here (same "remember something across
	// ticks without a dedicated attachment" idiom as slotHistory above) is the real, simpler equivalent -
	// one fewer registered MobEffect for a flag nothing else ever read.
	private boolean dragonAuraActive;

	public boolean isDragonAuraActive()
	{
		return dragonAuraActive;
	}

	public void setDragonAuraActive(boolean active)
	{
		dragonAuraActive = active;
	}

	// Real, persisted - see this class's own doc comment for why TechReturn needs this recorded directly
	// rather than queried from anywhere else.
	@Nullable
	private BlockPos landEntryPos;
	@Nullable
	private ResourceKey<Level> landEntryDim;

	@Nullable
	public BlockPos getLandEntryPos()
	{
		return landEntryPos;
	}

	@Nullable
	public ResourceKey<Level> getLandEntryDim()
	{
		return landEntryDim;
	}

	public void setLandEntryPoint(BlockPos pos, ResourceKey<Level> dim)
	{
		landEntryPos = pos;
		landEntryDim = dim;
	}

	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider provider)
	{
		CompoundTag nbt = new CompoundTag();
		if(landEntryPos != null && landEntryDim != null)
		{
			CompoundTag entry = new CompoundTag();
			entry.putInt("X", landEntryPos.getX());
			entry.putInt("Y", landEntryPos.getY());
			entry.putInt("Z", landEntryPos.getZ());
			entry.putString("Dim", landEntryDim.location().toString());
			nbt.put("LandEntry", entry);
		}
		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt)
	{
		if(nbt.contains("LandEntry"))
		{
			CompoundTag entry = nbt.getCompound("LandEntry");
			landEntryPos = new BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z"));
			landEntryDim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(entry.getString("Dim")));
		}
		else
		{
			landEntryPos = null;
			landEntryDim = null;
		}
	}
}
