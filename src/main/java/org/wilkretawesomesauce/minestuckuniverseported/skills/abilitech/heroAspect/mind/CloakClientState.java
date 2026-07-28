package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.mind;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-only registry of "which real entity id is currently disguised as which {@link EntityType}",
 * kept in sync by {@link org.wilkretawesomesauce.minestuckuniverseported.network.CloakSyncPacket}.
 * Read by {@link CloakRenderEvents} every frame it renders a player.
 */
public final class CloakClientState
{
	private static final Map<Integer, EntityType<?>> cloaked = new HashMap<>();

	private CloakClientState()
	{
	}

	public static void setCloaked(int entityId, ResourceLocation entityType)
	{
		cloaked.put(entityId, BuiltInRegistries.ENTITY_TYPE.get(entityType));
	}

	public static void clearCloaked(int entityId)
	{
		cloaked.remove(entityId);
	}

	public static EntityType<?> getCloakType(int entityId)
	{
		return cloaked.get(entityId);
	}
}
