package org.wilkretawesomesauce.minestuckuniverseported.client;

import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code client.MSUKeys} skill-key section. Same category as
 * {@link MSUKeyMappings} for the same reason (grouped with Minestuck's own keys in the controls menu).
 */
public final class MSUAbilitechKeyMappings
{
	private static final String CATEGORY = "key.categories.minestuck";

	public static KeyMapping primaryKey;
	public static KeyMapping secondaryKey;
	public static KeyMapping tertiaryKey;

	private MSUAbilitechKeyMappings()
	{
	}

	public static void register(RegisterKeyMappingsEvent event)
	{
		primaryKey = new KeyMapping("key.minestuckuniverseported.abilitechPrimary", GLFW.GLFW_KEY_H, CATEGORY);
		event.register(primaryKey);

		secondaryKey = new KeyMapping("key.minestuckuniverseported.abilitechSecondary", GLFW.GLFW_KEY_J, CATEGORY);
		event.register(secondaryKey);

		tertiaryKey = new KeyMapping("key.minestuckuniverseported.abilitechTertiary", GLFW.GLFW_KEY_K, CATEGORY);
		event.register(tertiaryKey);
	}
}
