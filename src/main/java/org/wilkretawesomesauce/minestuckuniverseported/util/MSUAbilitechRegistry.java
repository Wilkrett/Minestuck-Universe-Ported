package org.wilkretawesomesauce.minestuckuniverseported.util;

import net.minecraft.resources.ResourceLocation;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.Abilitech;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stand-in for what would have been a Forge registry in the original (via {@code Skill}'s own registry
 * machinery). Mirrors {@code strife.MSUKindAbstrataRegistry}'s approach for the same reasons - see that
 * class for why a plain registrar instead of a full NeoForge {@code Registry<Abilitech>}.
 */
public final class MSUAbilitechRegistry
{
	private static final Map<ResourceLocation, Abilitech> REGISTRY = new LinkedHashMap<>();

	private MSUAbilitechRegistry()
	{
	}

	public static Abilitech register(Abilitech tech)
	{
		if(REGISTRY.containsKey(tech.getId()))
			throw new IllegalStateException("Duplicate Abilitech registered: " + tech.getId());
		REGISTRY.put(tech.getId(), tech);
		return tech;
	}

	@Nullable
	public static Abilitech get(ResourceLocation id)
	{
		return REGISTRY.get(id);
	}

	public static Collection<Abilitech> getAll()
	{
		return Collections.unmodifiableCollection(REGISTRY.values());
	}
}
