package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.wilkretawesomesauce.minestuckuniverseported.skills.TechBoondollarCost;

import javax.annotation.Nullable;

/**
 * Ported from MinestuckUniverse (1.12.2)'s tech-slot half of {@code GodTierData} (equip/unequip/slots)
 * combined with {@code capabilities.keyStates.SkillKeyStates} (the activation input state machine) into
 * one attachment - the originals were two separate capabilities, but there's no reason to keep that split
 * now that neither is entangled with the skills/badges/god-tier systems they used to sit alongside.
 * <p>
 * 3 slots, matching the original's {@code new Abilitech[3]} and the 3 {@link AbilitechKey} values 1:1.
 */
public class AbilitechLoadout implements INBTSerializable<CompoundTag>
{
	public static final int SLOTS = 3;

	private final Abilitech[] equipped = new Abilitech[SLOTS];
	private final boolean[] passiveEnabled = new boolean[SLOTS];

	private final AbilitechKeyState[] receivedKeyStates = new AbilitechKeyState[SLOTS];
	private final AbilitechKeyState[] keyStates = new AbilitechKeyState[SLOTS];
	private final int[] keyTimes = new int[SLOTS];

	// Transient (never serialized) per-slot scratch state for techs that need to remember something
	// across ticks without needing a dedicated attachment of their own - e.g. TechTimeTickUp's tethered
	// target, TechTimeRecall's position history. Not present in the original (which used IBadgeEffects
	// for this sort of thing); added here since it's genuinely reusable by more than one tech.
	@SuppressWarnings("unchecked")
	private final java.util.Deque<Object>[] slotHistory = new java.util.ArrayDeque[SLOTS];
	private final net.minecraft.world.entity.Entity[] slotTether = new net.minecraft.world.entity.Entity[SLOTS];

	public java.util.Deque<Object> getSlotHistory(int slot)
	{
		int i = clampSlot(slot);
		if(slotHistory[i] == null)
			slotHistory[i] = new java.util.ArrayDeque<>();
		return slotHistory[i];
	}

	@Nullable
	public net.minecraft.world.entity.Entity getSlotTether(int slot)
	{
		return slotTether[clampSlot(slot)];
	}

	public void setSlotTether(int slot, @Nullable net.minecraft.world.entity.Entity tether)
	{
		slotTether[clampSlot(slot)] = tether;
	}

	// heroClass "external tech" borrowing - ported from IBadgeEffects#getExternalTech/setExternalTech.
	// Used by heroClass.bard.TechBardMetronome/heroClass.mage.TechMageStudy/heroClass.rogue.TechRogueSteal
	// to remember which *other* registered Abilitech a given slot is currently driving on the owner's
	// behalf. Transient like slotTether/slotHistory above - the original's own IBadgeEffects state this
	// backs isn't persisted across relog either.
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

	// heroClass.seer.TechSeerDodge's own dodge cooldown - ported from IBadgeEffects#getLastSeerDodge/
	// setLastSeerDodge (there tracked in ticksExisted units; here in Level#getGameTime() ticks, the same
	// timing source this project's other cooldown-style scratch state already uses). Transient, same
	// reasoning as the rest of this section.
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
	// as, if any (see that class's own doc comment for what "disguised" currently means in this port).
	// Player-level, not per-slot, matching the original's own single IBadgeEffects cloak field.
	@Nullable
	private net.minecraft.world.entity.EntityType<?> cloakType;

	@Nullable
	public net.minecraft.world.entity.EntityType<?> getCloakType()
	{
		return cloakType;
	}

	public void setCloakType(@Nullable net.minecraft.world.entity.EntityType<?> type)
	{
		cloakType = type;
	}

	// Space aspect "warp point" - ported from MinestuckUniverse (1.12.2)'s IBadgeEffects#setWarpPoint/
	// hasWarpPoint/unsetWarpPoint. Genuinely shared player-level state (not per-slot): both
	// TechSpaceAnchoredTele and TechSpaceTargetTele in the original read/write the exact same warp
	// point, so it lives here rather than on either tech's own slot scratch state.
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

	// Space aspect "manipulated matter" corner selection - ported from IBadgeEffects#getManipulatedPos1/
	// getManipulatedPos2. Transient like slotTether/slotHistory above (resets on relog, an accepted
	// rough edge for what's just an in-progress block selection) rather than serialized like the warp
	// point, which is meant to persist as a real destination.
	@Nullable
	private BlockPos manipulatedPos1, manipulatedPos2;
	@Nullable
	private ResourceKey<Level> manipulatedPos1Dim, manipulatedPos2Dim;

	@Nullable
	public BlockPos getManipulatedPos1() { return manipulatedPos1; }
	@Nullable
	public BlockPos getManipulatedPos2() { return manipulatedPos2; }
	@Nullable
	public ResourceKey<Level> getManipulatedPos1Dim() { return manipulatedPos1Dim; }
	@Nullable
	public ResourceKey<Level> getManipulatedPos2Dim() { return manipulatedPos2Dim; }

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

	// Life aspect "saving grace targets" - ported from IBadgeEffects#getSavingGraceTargets. Transient
	// like the other scratch state above; see heroAspect.life.TechLifeGrace's own doc comment for how
	// it's actually used (the original never showed an explicit .add() call in the decompiled source
	// this was ported from, so this reproduces the only functionally sensible reading: added to on a
	// successful grant, checked to block re-granting the same target, cleared on that target's own death).
	private final java.util.Set<java.util.UUID> savingGraceTargets = new java.util.HashSet<>();

	public java.util.Set<java.util.UUID> getSavingGraceTargets()
	{
		return savingGraceTargets;
	}

	// Real per-player unlock state for the boondollar-cost economy (see abilitech.TechBoondollarCost) -
	// genuinely new, the original had no equivalent single set (unlock state there was implicitly "does
	// IGodTierData#hasSkill(tech) return true", backed by its own separate skill-list field). Persisted
	// (not transient) since losing purchased unlocks on relog would be a real regression, not an accepted
	// rough edge like the transient scratch state above.
	private final java.util.Set<ResourceLocation> unlockedTechs = new java.util.HashSet<>();

	public boolean isUnlocked(Abilitech tech)
	{
		if(!(tech instanceof TechBoondollarCost cost))
			return true; // no cost concept at all for a plain Abilitech - always usable, matching this project's original "no gating" default for anything not under the real cost economy

		return (cost.cost == 0 && cost.requiredStacks.isEmpty()) || unlockedTechs.contains(tech.getId());
	}

	public void markUnlocked(Abilitech tech)
	{
		unlockedTechs.add(tech.getId());
	}

	/** Revokes one previously-unlocked tech - a no-op for a tech with no real cost concept (see
	 * {@link #isUnlocked}, those are always considered unlocked and can't be revoked). Used by
	 * {@code command.AbilitechUserCommand}'s debug "revoke" action. */
	public void revokeUnlocked(Abilitech tech)
	{
		unlockedTechs.remove(tech.getId());
	}

	/** Revokes every unlocked tech at once. */
	public void clearUnlockedTechs()
	{
		unlockedTechs.clear();
	}

	public AbilitechLoadout()
	{
		resetKeyStates();
	}

	private static int clampSlot(int slot)
	{
		return Math.min(SLOTS - 1, Math.max(0, slot));
	}

	// --- equip slots, ported from GodTierData -----------------------------------------------------

	@Nullable
	public Abilitech getTech(int slot)
	{
		return equipped[clampSlot(slot)];
	}

	public int getTechSlots()
	{
		return SLOTS;
	}

	public boolean isTechEquipped(Abilitech tech)
	{
		for(Abilitech t : equipped)
			if(tech.equals(t))
				return true;
		return false;
	}

	/**
	 * Whether {@code tech} is both equipped in some slot and has that slot's passive toggle on - for techs
	 * whose actual behavior lives in a static event handler rather than {@link Abilitech#onPassiveTick}
	 * (e.g. {@code heroAspect.blood.TechBloodBleeding}, which only matters on someone else's
	 * {@code LivingDamageEvent}, not a per-tick check of its own owner).
	 */
	public boolean isPassiveEnabledFor(Abilitech tech)
	{
		for(int i = 0; i < SLOTS; i++)
			if(tech.equals(equipped[i]) && passiveEnabled[i])
				return true;
		return false;
	}

	public void equipTech(Level level, Player player, Abilitech tech, int slot)
	{
		int i = clampSlot(slot);
		equipped[i] = tech;
		tech.onEquipped(level, player, i);
	}

	public void unequipTech(Level level, Player player, int slot)
	{
		int i = clampSlot(slot);
		if(equipped[i] != null)
			equipped[i].onUnequipped(level, player, i);
		equipped[i] = null;
		passiveEnabled[i] = false;
	}

	public boolean isPassiveEnabled(int slot)
	{
		return passiveEnabled[clampSlot(slot)];
	}

	public void setPassiveEnabled(int slot, boolean enabled)
	{
		passiveEnabled[clampSlot(slot)] = enabled;
	}

	// --- key state machine, ported from SkillKeyStates ---------------------------------------------

	/** Call with the raw client-reported press/release state; the actual {@link AbilitechKeyState} only
	 * advances one step per {@link #tickKeyStates()} call, same as the original. */
	public void updateKeyState(AbilitechKey key, boolean pressed)
	{
		int i = key.ordinal();
		if(pressed && receivedKeyStates[i] != AbilitechKeyState.HELD)
			receivedKeyStates[i] = AbilitechKeyState.PRESS;
		else if(!pressed && receivedKeyStates[i] != AbilitechKeyState.NONE)
			receivedKeyStates[i] = AbilitechKeyState.RELEASED;
	}

	public AbilitechKeyState getKeyState(AbilitechKey key)
	{
		return keyStates[key.ordinal()];
	}

	public int getKeyTime(AbilitechKey key)
	{
		return keyTimes[key.ordinal()];
	}

	/** Advances the key state machine by one tick; call once per server player tick. */
	public void tickKeyStates()
	{
		for(int i = 0; i < SLOTS; i++)
		{
			if(receivedKeyStates[i] == AbilitechKeyState.PRESS)
				receivedKeyStates[i] = AbilitechKeyState.HELD;
			else if(receivedKeyStates[i] == AbilitechKeyState.RELEASED)
				receivedKeyStates[i] = AbilitechKeyState.NONE;

			if(keyStates[i] != receivedKeyStates[i])
				keyStates[i] = AbilitechKeyState.values()[(keyStates[i].ordinal() + 1) % AbilitechKeyState.values().length];

			if(keyStates[i] == AbilitechKeyState.PRESS)
				keyTimes[i] = 0;
			else
				keyTimes[i]++;
		}
	}

	public void resetKeyStates()
	{
		for(int i = 0; i < SLOTS; i++)
		{
			receivedKeyStates[i] = AbilitechKeyState.NONE;
			keyStates[i] = AbilitechKeyState.NONE;
			keyTimes[i] = 0;
		}
	}

	// --- persistence ---------------------------------------------------------------------------------

	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider provider)
	{
		CompoundTag nbt = new CompoundTag();
		for(int i = 0; i < SLOTS; i++)
		{
			if(equipped[i] != null)
				nbt.putString("Tech" + i, equipped[i].getId().toString());
			nbt.putBoolean("Passive" + i, passiveEnabled[i]);
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
		net.minecraft.nbt.ListTag unlocked = new net.minecraft.nbt.ListTag();
		for(ResourceLocation id : unlockedTechs)
			unlocked.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));
		nbt.put("UnlockedTechs", unlocked);
		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt)
	{
		for(int i = 0; i < SLOTS; i++)
		{
			equipped[i] = nbt.contains("Tech" + i) ? MSUAbilitechRegistry.get(ResourceLocation.parse(nbt.getString("Tech" + i))) : null;
			passiveEnabled[i] = nbt.getBoolean("Passive" + i);
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
		unlockedTechs.clear();
		if(nbt.contains("UnlockedTechs"))
		{
			net.minecraft.nbt.ListTag unlocked = nbt.getList("UnlockedTechs", net.minecraft.nbt.Tag.TAG_STRING);
			for(int i = 0; i < unlocked.size(); i++)
				unlockedTechs.add(ResourceLocation.parse(unlocked.getString(i)));
		}
		resetKeyStates();
	}
}
