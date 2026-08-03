package org.wilkretawesomesauce.minestuckuniverseported.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side cache of active Breath wind ribbons (caster entity id -&gt; target entity id + style),
 * populated entirely from {@code network.WindRibbonSyncPacket}. Consumed by
 * {@code client.render.WindRibbonRenderer} every frame - same "packet-fed cache, separate renderer reads
 * it" split this project already uses for {@code TetherBondClientState}/{@code TetherBondRenderer}.
 */
public final class WindRibbonClientState
{
	private static final Map<Integer, Ribbon> ribbons = new ConcurrentHashMap<>();

	private WindRibbonClientState()
	{
	}

	public static void setRibbon(int casterId, int targetId, boolean inward, float intensity)
	{
		ribbons.put(casterId, new Ribbon(targetId, inward, intensity));
	}

	public static void clearRibbon(int casterId)
	{
		ribbons.remove(casterId);
	}

	public static Map<Integer, Ribbon> getRibbons()
	{
		return ribbons;
	}

	public record Ribbon(int targetId, boolean inward, float intensity)
	{
	}
}
