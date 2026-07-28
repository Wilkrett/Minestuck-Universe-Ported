package org.wilkretawesomesauce.minestuckuniverseported.client.streak;

import net.minecraft.resources.ResourceLocation;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

import java.util.List;

/**
 * Ported from iChun's <a href="https://ichun.me/mods/streak/">Streak</a> (Forge 1.16.3, LGPL-3.0)'s
 * "flavour" concept - a named texture strip the ribbon trail is drawn with. The original unpacked a
 * bundled zip into an external, user-scannable "Streak Flavours" folder at runtime; this port skips
 * that machinery (see {@code streak} package's own doc notes) in favor of a small fixed registry of
 * bundled resource-pack textures. Adding a new flavour is just a new PNG under
 * {@code textures/streak/} plus a new entry in {@link #NAMES} - no code elsewhere needs to change.
 * <p>
 * Both textures currently registered ({@code rainbow}, {@code fire}) are new, original, procedurally
 * generated art - not iChun's own bundled flavour images - since this port deliberately avoids
 * redistributing the original mod's assets.
 */
public final class StreakFlavours
{
	public static final List<String> NAMES = List.of("rainbow", "fire");

	private StreakFlavours()
	{
	}

	public static boolean isValid(String name)
	{
		return NAMES.contains(name);
	}

	public static ResourceLocation texture(String name)
	{
		return Minestuckuniverseported.id("textures/streak/" + name + ".png");
	}
}
