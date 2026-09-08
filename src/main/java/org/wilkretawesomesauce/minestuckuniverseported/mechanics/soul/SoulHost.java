package org.wilkretawesomesauce.minestuckuniverseported.mechanics.soul;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Design doc §13 ("Generic SoulHost Architecture... do not implement independent hard-coded
 * possession/inhabitation logic for every entity class"). Only {@code entity.UninhabitedBodyEntity}
 * implements this in Phases 1-5 - mob/animal/item hosts (design doc §15-16, Phase 6) are explicitly out
 * of scope for this pass, but every method here is written generically (no player-body assumptions baked
 * into the signatures) so those hosts can adopt this interface later without any change to
 * {@link SoulManager} or {@code SoulProjectionEvents}, which are the only real consumers.
 */
public interface SoulHost
{
	/** Whether a free Soul could enter this host right now (not already occupied, not mid-removal, etc.). */
	boolean canBeInhabited();

	@Nullable
	UUID getOccupyingSoul();

	void setOccupyingSoul(UUID soulOwnerUUID);

	void clearOccupyingSoul();

	HostCapabilities getCapabilities();

	/** Called once, the tick a Soul successfully takes control - disable/override normal AI here if the host has any. */
	void onSoulEnter(UUID soulOwnerUUID);

	/** Called once, the tick a Soul voluntarily leaves this host - restore normal AI/behavior here. */
	void onSoulLeave();

	/** Called once, the tick this host is destroyed/removed while occupied - same restoration as {@link #onSoulLeave()}, but the host itself won't survive the call. */
	void onSoulEject();
}
