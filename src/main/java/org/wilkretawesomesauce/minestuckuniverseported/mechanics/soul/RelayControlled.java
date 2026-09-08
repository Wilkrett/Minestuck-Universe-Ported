package org.wilkretawesomesauce.minestuckuniverseported.mechanics.soul;

/**
 * Implemented by {@code entity.UninhabitedBodyEntity} - a host a Soul's real hidden {@code ServerPlayer}
 * anchor can currently be projected into and drive with relayed WASD/jump/sneak input (see
 * {@code SoulManager}'s own doc comment for the whole "hidden anchor" design, and {@link PlayerSoul}'s
 * own doc comment for why this relay is scoped to host occupation only - a free Spirit is now just the
 * real player, needing no relay of any kind).
 */
public interface RelayControlled
{
	void setRelayInput(float worldX, float worldZ, boolean jump, boolean sneak);
}
