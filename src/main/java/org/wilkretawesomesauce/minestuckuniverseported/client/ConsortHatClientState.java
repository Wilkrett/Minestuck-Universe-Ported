package org.wilkretawesomesauce.minestuckuniverseported.client;

import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side cache of which Consort/Frog entity ids are currently wearing which hat - populated entirely
 * from {@code network.ConsortHatSyncPacket}. Ready infrastructure for a future GeckoLib render layer (see
 * {@code capabilities.consortCosmetics.ConsortHatsData}'s own doc comment for that known, permanent gap -
 * Consorts render via a custom GeckoLib model, not vanilla's armor layer, so nothing currently reads this
 * cache to actually draw anything yet), same category as this project's other "data is real, rendering
 * isn't" gaps.
 */
public final class ConsortHatClientState
{
	private static final Map<Integer, ItemStack> hats = new ConcurrentHashMap<>();
	private static final Map<Integer, Boolean> upsideDown = new ConcurrentHashMap<>();
	private static final Map<Integer, ItemStack> chests = new ConcurrentHashMap<>();

	private ConsortHatClientState()
	{
	}

	public static void setHat(int entityId, ItemStack hat, boolean hatUpsideDown)
	{
		if(hat.isEmpty())
		{
			hats.remove(entityId);
			upsideDown.remove(entityId);
		}
		else
		{
			hats.put(entityId, hat);
			upsideDown.put(entityId, hatUpsideDown);
		}
	}

	public static ItemStack getHat(int entityId)
	{
		return hats.getOrDefault(entityId, ItemStack.EMPTY);
	}

	/** Genuinely new, project-original quirk - see {@code capabilities.consortCosmetics.IConsortHatsData}'s own doc comment. */
	public static boolean isHatUpsideDown(int entityId)
	{
		return upsideDown.getOrDefault(entityId, false);
	}

	/** Chestplate equivalent of {@link #setHat}/{@link #getHat} - populated from {@code network.ConsortChestSyncPacket}. */
	public static void setChest(int entityId, ItemStack chest)
	{
		if(chest.isEmpty())
			chests.remove(entityId);
		else
			chests.put(entityId, chest);
	}

	public static ItemStack getChest(int entityId)
	{
		return chests.getOrDefault(entityId, ItemStack.EMPTY);
	}
}
