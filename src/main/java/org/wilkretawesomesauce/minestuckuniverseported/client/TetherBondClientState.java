package org.wilkretawesomesauce.minestuckuniverseported.client;

import com.mraof.minestuck.player.EnumAspect;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side cache of active {@code heroAspect.TechTetherBond} tethers (caster entity id -> target entity
 * id + which aspect it belongs to, for tinting), populated entirely from {@code network.TetherBondSyncPacket}.
 * Consumed by {@code client.render.TetherBondRenderer} every frame to draw the real tether between the two
 * entities - same "packet-fed cache, separate renderer reads it" split this project already uses for
 * {@code ConsortHatClientState}/{@code ConsortHatGeoLayer}.
 */
public final class TetherBondClientState
{
	private static final Map<Integer, Bond> bonds = new ConcurrentHashMap<>();

	private TetherBondClientState()
	{
	}

	public static void setBond(int casterId, int targetId, EnumAspect aspect, boolean corrupted)
	{
		bonds.put(casterId, new Bond(targetId, aspect, corrupted));
	}

	public static void clearBond(int casterId)
	{
		bonds.remove(casterId);
	}

	public static Map<Integer, Bond> getBonds()
	{
		return bonds;
	}

	/** {@code corrupted} overrides {@code aspect}'s usual color entirely - see {@code network.TetherBondSyncPacket}'s own doc comment. */
	public record Bond(int targetId, EnumAspect aspect, boolean corrupted)
	{
	}
}
