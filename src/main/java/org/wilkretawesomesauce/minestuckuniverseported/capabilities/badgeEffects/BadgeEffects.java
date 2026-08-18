package org.wilkretawesomesauce.minestuckuniverseported.capabilities.badgeEffects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.network.CloakSyncPacket;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code capabilities.badgeEffects.BadgeEffects} - this project's
 * own attachment equivalent of that capability, holding the handful of fields that are genuine ports of the
 * original's own (much larger, ~40-method) {@code IBadgeEffects}: per-slot tethers ({@link #getTether} -
 * not the original's raw {@code tether(int slot)} field name, matching this project's own accessor
 * convention), external-tech borrowing, the seer-dodge cooldown, the mind-cloak type, the space warp point,
 * the matter-manipulator corner selection, saving-grace targets, the "calculating" charge counter for
 * Calculated Strike, and the frenzied/rage-shifted/saving-graced boolean flags (moved here from being
 * one-off synced {@code MobEffect}s - see each field's own doc comment for why a plain capability boolean
 * is actually correct for these, unlike the fields that stayed real potion effects). This used to live
 * directly on {@code skills.abilitech.AbilitechLoadout} (a
 * user-requested consolidation) - moved back out to its own attachment per a later, explicit correction:
 * mirroring the original's own package layout matters more than saving one attachment lookup, and several
 * of these fields (the per-slot tether especially) are shared by well over a dozen unrelated tech classes
 * across several aspects/classes (Time Clone, Tick-Up, Life Leech, three different bubble techs, Soul Stun,
 * Sylph Karma Restore, Heart Bond, Sylph, Space Grab, Mind Control, Witch Trap, and more) that have nothing
 * else to do with a player's equipped-tech loadout - bundling them onto {@code AbilitechLoadout} coupled all
 * of those classes to a shape that was never really about loadout slots.
 * <p>
 * Deliberately scoped, not a full transplant of the original's ~40-method interface: the already-
 * redistributed marker-{@code MobEffect}-based fields (conceal, time-stop, rage, mindflayer, soul-shock,
 * soul-link, FOV, tick-up stacks, movement puppeting, power-particle tracking) stay exactly as they are,
 * synced potion effects with a genuine client-side reason to exist - not pulled in here. {@code AbilitechLoadout}
 * itself keeps its own genuinely new, no-original-counterpart scratch state ({@code slotHistory},
 * {@code dragonAuraActive}, {@code landEntryPos}/{@code landEntryDim}) - none of that is a badge effect.
 * <p>
 * The warp point and the frenzied/rage-shifted/saving-graced flags are persisted (the latter three because
 * a previously-frenzied/rage-shifted/warded entity needs to survive a chunk unload/reload, matching what
 * their old {@code MobEffect} form got for free via vanilla's own potion persistence) - everything else
 * here is transient scratch state (an in-progress cloak, an in-progress corner selection, a per-slot tether
 * that only ever matters while its owning tech is actively running).
 * <p>
 * {@link #onStartTracking} mirrors the original's own {@code BadgeEffects.onStartTracking} - the original
 * fired one wholesale "send every badge effect" packet to a newly-tracking observer; this port only ever
 * needs to sync the one field with an actual client-side render consumer ({@link #cloakType}, via
 * {@code TechMindCloak}'s {@code client.CloakRenderEvents}), so it sends just {@link CloakSyncPacket}
 * instead - covers late joiners and anyone walking into render distance of an already-cloaked player, which
 * {@code TechMindCloak}'s own on-change broadcast alone wouldn't reach. Kept directly on this class rather
 * than a separate one-off event class, the same shape {@code consortCosmetics.ConsortHatsData}'s own
 * {@code onStartTracking} already establishes for a capability with a client-visible field.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public class BadgeEffects implements IBadgeEffects, INBTSerializable<CompoundTag>
{
	private static final int SLOTS = 3;

	private static int clampSlot(int slot)
	{
		return Math.min(SLOTS - 1, Math.max(0, slot));
	}

	private final Entity[] tether = new Entity[SLOTS];

	@Override
	@Nullable
	public Entity getTether(int slot)
	{
		return tether[clampSlot(slot)];
	}

	@Override
	public void setTether(int slot, @Nullable Entity entity)
	{
		tether[clampSlot(slot)] = entity;
	}

	@Override
	public void clearTether(int slot)
	{
		tether[clampSlot(slot)] = null;
	}

	private final ResourceLocation[] externalTech = new ResourceLocation[SLOTS];

	@Override
	@Nullable
	public ResourceLocation getExternalTech(int slot)
	{
		return externalTech[clampSlot(slot)];
	}

	@Override
	public void setExternalTech(int slot, @Nullable ResourceLocation id)
	{
		externalTech[clampSlot(slot)] = id;
	}

	private long lastSeerDodgeTick = Long.MIN_VALUE;

	@Override
	public long getLastSeerDodgeTick()
	{
		return lastSeerDodgeTick;
	}

	@Override
	public void setLastSeerDodgeTick(long tick)
	{
		lastSeerDodgeTick = tick;
	}

	@Nullable
	private EntityType<?> cloakType;

	@Override
	@Nullable
	public EntityType<?> getCloakType()
	{
		return cloakType;
	}

	@Override
	public void setCloakType(@Nullable EntityType<?> type)
	{
		cloakType = type;
	}

	@Nullable
	private BlockPos warpPointPos;
	@Nullable
	private ResourceKey<Level> warpPointDim;

	@Override
	public boolean hasWarpPoint()
	{
		return warpPointPos != null;
	}

	@Override
	@Nullable
	public BlockPos getWarpPointPos()
	{
		return warpPointPos;
	}

	@Override
	@Nullable
	public ResourceKey<Level> getWarpPointDim()
	{
		return warpPointDim;
	}

	@Override
	public void setWarpPoint(BlockPos pos, ResourceKey<Level> dimension)
	{
		warpPointPos = pos;
		warpPointDim = dimension;
	}

	@Override
	public void clearWarpPoint()
	{
		warpPointPos = null;
		warpPointDim = null;
	}

	@Nullable
	private BlockPos manipulatedPos1, manipulatedPos2;
	@Nullable
	private ResourceKey<Level> manipulatedPos1Dim, manipulatedPos2Dim;

	@Override
	@Nullable
	public BlockPos getManipulatedPos1()
	{
		return manipulatedPos1;
	}

	@Override
	@Nullable
	public BlockPos getManipulatedPos2()
	{
		return manipulatedPos2;
	}

	@Override
	@Nullable
	public ResourceKey<Level> getManipulatedPos1Dim()
	{
		return manipulatedPos1Dim;
	}

	@Override
	@Nullable
	public ResourceKey<Level> getManipulatedPos2Dim()
	{
		return manipulatedPos2Dim;
	}

	@Override
	public void setManipulatedPos1(@Nullable BlockPos pos, @Nullable ResourceKey<Level> dim)
	{
		manipulatedPos1 = pos;
		manipulatedPos1Dim = dim;
	}

	@Override
	public void setManipulatedPos2(@Nullable BlockPos pos, @Nullable ResourceKey<Level> dim)
	{
		manipulatedPos2 = pos;
		manipulatedPos2Dim = dim;
	}

	private final Set<UUID> savingGraceTargets = new HashSet<>();

	@Override
	public Set<UUID> getSavingGraceTargets()
	{
		return savingGraceTargets;
	}

	private int calculating;

	@Override
	public int getCalculating()
	{
		return calculating;
	}

	@Override
	public void setCalculating(int calculating)
	{
		this.calculating = calculating;
	}

	// Rage aspect "frenzied"/"rage-shifted" flags - the original's own raw IBadgeEffects#isFrenzied()/
	// isRageShifted() booleans on any creature, ported here instead of as one-off synced MobEffects
	// (FrenziedEffect/RageShiftedEffect, both deleted). Persisted (unlike this class's other transient
	// fields) because rage.RageMobEvents needs to know whether a reloading Mob was mid-frenzy/rage-shift to
	// re-inject its goals - the same real requirement the deleted MobEffects met for free via vanilla's own
	// potion NBT persistence, now met by this attachment's own serializeNBT/deserializeNBT instead.
	private boolean frenzied;
	private boolean rageShifted;

	@Override
	public boolean isFrenzied()
	{
		return frenzied;
	}

	@Override
	public void setFrenzied(boolean frenzied)
	{
		this.frenzied = frenzied;
	}

	@Override
	public boolean isRageShifted()
	{
		return rageShifted;
	}

	@Override
	public void setRageShifted(boolean rageShifted)
	{
		this.rageShifted = rageShifted;
	}

	// Life aspect "saving graced" flag for TechLifeGrace ("Saving Grace") - the original's own raw
	// IBadgeEffects#isSavingGraced() boolean, ported here instead of as a one-off synced MobEffect
	// (SavingGracedEffect, deleted) with an arbitrary WARD_DURATION_TICKS the original never actually had
	// (a plain capability boolean never expired on its own either). Persisted for the same reason as
	// frenzied/rageShifted above - a warded target shouldn't lose its ward on a chunk unload/reload.
	private boolean savingGraced;

	@Override
	public boolean isSavingGraced()
	{
		return savingGraced;
	}

	@Override
	public void setSavingGraced(boolean savingGraced)
	{
		this.savingGraced = savingGraced;
	}

	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider provider)
	{
		CompoundTag nbt = new CompoundTag();
		if(warpPointPos != null && warpPointDim != null)
		{
			CompoundTag warp = new CompoundTag();
			warp.putInt("X", warpPointPos.getX());
			warp.putInt("Y", warpPointPos.getY());
			warp.putInt("Z", warpPointPos.getZ());
			warp.putString("Dim", warpPointDim.location().toString());
			nbt.put("WarpPoint", warp);
		}
		nbt.putBoolean("Frenzied", frenzied);
		nbt.putBoolean("RageShifted", rageShifted);
		nbt.putBoolean("SavingGraced", savingGraced);
		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt)
	{
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
		frenzied = nbt.getBoolean("Frenzied");
		rageShifted = nbt.getBoolean("RageShifted");
		savingGraced = nbt.getBoolean("SavingGraced");
	}

	@SubscribeEvent
	private static void onStartTracking(PlayerEvent.StartTracking event)
	{
		if(!(event.getTarget() instanceof Player cloaked) || !(event.getEntity() instanceof ServerPlayer observer))
			return;

		EntityType<?> cloakType = cloaked.getData(MSUAttachments.BADGE_EFFECTS).getCloakType();
		if(cloakType == null)
			return;

		PacketDistributor.sendToPlayer(observer, new CloakSyncPacket(cloaked.getId(), true, BuiltInRegistries.ENTITY_TYPE.getKey(cloakType)));
	}
}
