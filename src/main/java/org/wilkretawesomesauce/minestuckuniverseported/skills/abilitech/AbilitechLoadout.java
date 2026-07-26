package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Genuinely new, project-original scratch state ({@link #getSlotHistory}/{@link #getLandEntryPos}/
 * {@link #isDragonAuraActive}) with no original MinestuckUniverse (1.12.2) counterpart, merged back
 * together with the real {@code capabilities.badgeEffects.IBadgeEffects} fields
 * ({@link #getTether}/{@link #getExternalTech}/{@link #getLastSeerDodgeTick}/{@link #getCloakType}/
 * warp point/{@link #getManipulatedPos1}/{@link #getSavingGraceTargets}) this class briefly held once
 * before, then gave up to a dedicated {@code capabilities.badgeEffects.BadgeEffects} attachment, and now
 * holds again - user-requested consolidation back onto one attachment. <b>Real tradeoff, not free</b>: several
 * of these fields (the per-slot tether especially) are genuinely read/written by well over a dozen unrelated
 * tech classes across several aspects/classes (Time Clone, Tick-Up, Life Leech, three different bubble
 * techs, Soul Stun, Sylph Karma Restore, Heart Bond, Sylph, Space Grab, Mind Control, Witch Trap, and more)
 * - consolidating them here re-couples all of those classes to this one attachment's shape again, the exact
 * thing the earlier split was meant to avoid. Kept anyway per explicit instruction, not because the sharing
 * concern turned out to be wrong.
 * <p>
 * Only {@link #landEntryPos}/{@link #landEntryDim} and {@link #warpPointPos}/{@link #warpPointDim} are
 * persisted; everything else here is transient scratch state (an in-progress cloak, an in-progress corner
 * selection, a per-slot tether that only ever matters while its owning tech is actively running) -
 * unchanged from how each field behaved in whichever attachment it lived in before.
 */
public class AbilitechLoadout implements INBTSerializable<CompoundTag>
{
	public static final int SLOTS = 3;

	// Transient (never serialized) per-slot scratch state for techs that need to remember something
	// across ticks without needing a dedicated attachment of their own - e.g. TechTimeTickUp's tethered
	// target, TechTimeRecall's position history.
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

	// Per-slot tether - which entity (if any) a currently-running tech is "attached" to (a summoned clone,
	// a bubble, a tethered target, ...). Shared by well over a dozen unrelated tech classes - see this
	// class's own doc comment.
	private final Entity[] tether = new Entity[SLOTS];

	@Nullable
	public Entity getTether(int slot)
	{
		return tether[clampSlot(slot)];
	}

	public void setTether(int slot, @Nullable Entity entity)
	{
		tether[clampSlot(slot)] = entity;
	}

	public void clearTether(int slot)
	{
		tether[clampSlot(slot)] = null;
	}

	// Per-slot "borrowed external tech" id - TechMageStudy/TechBardMetronome/TechRogueSteal's shared
	// borrow-another-player's-equipped-tech mechanic.
	private final ResourceLocation[] externalTech = new ResourceLocation[SLOTS];

	@Nullable
	public ResourceLocation getExternalTech(int slot)
	{
		return externalTech[clampSlot(slot)];
	}

	public void setExternalTech(int slot, @Nullable ResourceLocation id)
	{
		externalTech[clampSlot(slot)] = id;
	}

	// heroClass.seer.TechSeerDodge's own dodge cooldown - tracked in Level#getGameTime() ticks, the same
	// timing source this project's other cooldown-style scratch state already uses.
	private long lastSeerDodgeTick = Long.MIN_VALUE;

	public long getLastSeerDodgeTick()
	{
		return lastSeerDodgeTick;
	}

	public void setLastSeerDodgeTick(long tick)
	{
		lastSeerDodgeTick = tick;
	}

	// Mind aspect "cloak" state for TechMindCloak - which EntityType this player is currently disguised
	// as, if any. Player-level, not per-slot.
	@Nullable
	private EntityType<?> cloakType;

	@Nullable
	public EntityType<?> getCloakType()
	{
		return cloakType;
	}

	public void setCloakType(@Nullable EntityType<?> type)
	{
		cloakType = type;
	}

	// Space aspect "warp point" - genuinely shared player-level state (not per-slot): both
	// TechSpaceAnchoredTele and TechSpaceTargetTele read/write the exact same warp point.
	@Nullable
	private BlockPos warpPointPos;
	@Nullable
	private ResourceKey<Level> warpPointDim;

	public boolean hasWarpPoint()
	{
		return warpPointPos != null;
	}

	@Nullable
	public BlockPos getWarpPointPos()
	{
		return warpPointPos;
	}

	@Nullable
	public ResourceKey<Level> getWarpPointDim()
	{
		return warpPointDim;
	}

	public void setWarpPoint(BlockPos pos, ResourceKey<Level> dimension)
	{
		warpPointPos = pos;
		warpPointDim = dimension;
	}

	public void clearWarpPoint()
	{
		warpPointPos = null;
		warpPointDim = null;
	}

	// Space aspect "manipulated matter" corner selection. Transient (resets on relog, an accepted rough
	// edge for what's just an in-progress block selection) rather than serialized like the warp point.
	@Nullable
	private BlockPos manipulatedPos1, manipulatedPos2;
	@Nullable
	private ResourceKey<Level> manipulatedPos1Dim, manipulatedPos2Dim;

	@Nullable
	public BlockPos getManipulatedPos1()
	{
		return manipulatedPos1;
	}

	@Nullable
	public BlockPos getManipulatedPos2()
	{
		return manipulatedPos2;
	}

	@Nullable
	public ResourceKey<Level> getManipulatedPos1Dim()
	{
		return manipulatedPos1Dim;
	}

	@Nullable
	public ResourceKey<Level> getManipulatedPos2Dim()
	{
		return manipulatedPos2Dim;
	}

	public void setManipulatedPos1(@Nullable BlockPos pos, @Nullable ResourceKey<Level> dim)
	{
		manipulatedPos1 = pos;
		manipulatedPos1Dim = dim;
	}

	public void setManipulatedPos2(@Nullable BlockPos pos, @Nullable ResourceKey<Level> dim)
	{
		manipulatedPos2 = pos;
		manipulatedPos2Dim = dim;
	}

	// Life aspect "saving grace targets" - see heroAspect.life.TechLifeGrace's own doc comment for how
	// it's actually used. Transient like the other scratch state above.
	private final Set<UUID> savingGraceTargets = new HashSet<>();

	public Set<UUID> getSavingGraceTargets()
	{
		return savingGraceTargets;
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
		if(warpPointPos != null && warpPointDim != null)
		{
			CompoundTag warp = new CompoundTag();
			warp.putInt("X", warpPointPos.getX());
			warp.putInt("Y", warpPointPos.getY());
			warp.putInt("Z", warpPointPos.getZ());
			warp.putString("Dim", warpPointDim.location().toString());
			nbt.put("WarpPoint", warp);
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

		if(nbt.contains("WarpPoint"))
		{
			CompoundTag warp = nbt.getCompound("WarpPoint");
			warpPointPos = new BlockPos(warp.getInt("X"), warp.getInt("Y"), warp.getInt("Z"));
			warpPointDim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(warp.getString("Dim")));
		}
		else
		{
			warpPointPos = null;
			warpPointDim = null;
		}
	}
}
