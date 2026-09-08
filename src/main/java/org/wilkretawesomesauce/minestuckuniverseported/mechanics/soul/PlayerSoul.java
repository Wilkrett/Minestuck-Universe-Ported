package org.wilkretawesomesauce.minestuckuniverseported.mechanics.soul;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * The persistent Soul every real player carries (see {@code MSUAttachments#PLAYER_SOUL}) - original
 * design for this project (no 1.12.2 counterpart), built from a user-supplied design doc ("Player Soul,
 * Hosts &amp; Possession System"). Phases 1-5 of that doc only: leaving/returning to a body and body
 * theft. Generic mob/animal/item hosts and optional living-player possession are explicitly out of
 * scope for this pass - see {@link SoulHost}'s own doc comment for how that's left extensible without
 * touching this class.
 * <p>
 * Per the doc's own §3 ("only store information that cannot be reliably derived from existing
 * relationships"), this deliberately does <b>not</b> store an {@code ownerUUID} field - the attachment
 * holder's own {@code Player#getUUID()} already is that, matching {@code FreedomData}/{@code DecisionData}'s
 * established "don't store what's derivable" convention. It also does not store inventory/XP/health/food
 * ("a Soul must not have its own inventory or XP" - doc §9) - that physical state lives entirely on
 * whichever {@code UninhabitedBodyEntity} is currently vacated, or on the real {@code ServerPlayer} itself
 * while occupying something; see {@link SoulManager}'s own doc comment for the full swap mechanism.
 * <p>
 * <b>{@link #freeSpirit} is a plain boolean, not a tracked entity's UUID - a real, direct user
 * correction.</b> An earlier version of this system represented "hostless" by spawning a separate
 * {@code PlayerSpiritEntity} and swapping the real {@code ServerPlayer}'s camera onto it via
 * {@code setCamera(Entity)}, relaying WASD input across a network round-trip. In a live client this
 * produced reversed controls, a locked/unresponsive camera, and a real FPS drop - all three traced to the
 * same root cause: camera rotation for a <i>non-locally-controlled</i> entity only updates via normal
 * entity-tracking sync + interpolation, not the zero-latency path a real local player's own camera gets,
 * and relaying input through a packet reintroduces exactly the round-trip a local player's movement is
 * supposed to never have. Free-spirit state is now just the real {@code ServerPlayer} itself, given
 * {@code Abilities#mayfly} (the same toggleable, walk-or-fly ability Creative mode grants - not full
 * noclip/spectator, so the Soul can walk on the ground normally and fly only when the player actually
 * toggles it) and rendered translucently client-side (see {@code client.SoulSpiritRenderEvents}) - zero
 * custom input/camera code at all, so vanilla's own always-correct local movement just applies.
 * {@link #currentHostUUID} (occupying an {@code UninhabitedBodyEntity}) is unaffected by this change and
 * still uses the camera-swap+relay design, since that entity genuinely isn't the local player.
 * <p>
 * <b>{@code copyOnDeath()}</b> - must survive the respawn clone, same reasoning {@code MSUAttachments}
 * already documents for unlock-tracking attachments (a soul mid-projection shouldn't silently reset to
 * "normal" just because the real underlying {@code ServerPlayer} instance was swapped out by a respawn).
 */
public class PlayerSoul implements ISoulData, INBTSerializable<CompoundTag>
{
	@Nullable
	private UUID currentHostUUID;
	private boolean freeSpirit;
	private boolean previousMayFly;

	@Override
	@Nullable
	public UUID getCurrentHostUUID()
	{
		return currentHostUUID;
	}

	@Override
	public void setCurrentHostUUID(@Nullable UUID id)
	{
		currentHostUUID = id;
	}

	@Override
	public boolean isFreeSpirit()
	{
		return freeSpirit;
	}

	@Override
	public void setFreeSpirit(boolean freeSpirit)
	{
		this.freeSpirit = freeSpirit;
	}

	@Override
	public boolean getPreviousMayFly()
	{
		return previousMayFly;
	}

	@Override
	public void setPreviousMayFly(boolean previousMayFly)
	{
		this.previousMayFly = previousMayFly;
	}

	@Override
	public boolean isProjecting()
	{
		return currentHostUUID != null || freeSpirit;
	}

	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider provider)
	{
		CompoundTag nbt = new CompoundTag();
		if(currentHostUUID != null)
			nbt.putUUID("CurrentHost", currentHostUUID);
		nbt.putBoolean("FreeSpirit", freeSpirit);
		nbt.putBoolean("PreviousMayFly", previousMayFly);
		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt)
	{
		currentHostUUID = nbt.hasUUID("CurrentHost") ? nbt.getUUID("CurrentHost") : null;
		freeSpirit = nbt.getBoolean("FreeSpirit");
		previousMayFly = nbt.getBoolean("PreviousMayFly");
	}
}
