package org.wilkretawesomesauce.minestuckuniverseported.strife;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stand-in for the old Forge {@code ForgeRegistry<KindAbstratus>} from 1.12.2. Populated in code
 * (see {@link MSUKindAbstrata}) rather than via datapacks, since {@link KindAbstratus} carries
 * behaviour (item-class checks, conditionals) that isn't naturally data-driven.
 */
public final class MSUKindAbstrataRegistry
{
	private static final Map<ResourceLocation, KindAbstratus> REGISTRY = new LinkedHashMap<>();

	private MSUKindAbstrataRegistry()
	{
	}

	public static KindAbstratus register(KindAbstratus kind)
	{
		if(REGISTRY.containsKey(kind.getRegistryName()))
			throw new IllegalStateException("Duplicate KindAbstratus registered: " + kind.getRegistryName());
		REGISTRY.put(kind.getRegistryName(), kind);
		return kind;
	}

	@Nullable
	public static KindAbstratus get(ResourceLocation name)
	{
		return REGISTRY.get(name);
	}

	public static Collection<KindAbstratus> getAll()
	{
		return Collections.unmodifiableCollection(REGISTRY.values());
	}
}
