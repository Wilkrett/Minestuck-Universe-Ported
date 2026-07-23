package org.wilkretawesomesauce.minestuckuniverseported.client;

import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code client.MSUKeys}.
 * <p>
 * Registers under Minestuck's own "key.categories.minestuck" category (reusing the base mod's category
 * string, rather than defining our own), so these show up grouped together with Minestuck's keybinds in
 * the controls menu.
 * <p>
 * Note there's no separate "quickswitcher" keybind: in the original (and here), the quickswitcher is a
 * <i>mode</i> of {@link #strifeKey}/{@link #swapOffhandStrifeKey} - holding either while sneaking swaps
 * from cycling weapons within a specibus to cycling between specibi entirely. See
 * {@link MSUStrifeSwitcherClient} for that logic.
 */
public final class MSUKeyMappings
{
	private static final String CATEGORY = "key.categories.minestuck";

	public static KeyMapping strifeKey;
	public static KeyMapping strifeSelectorLeftKey;
	public static KeyMapping strifeSelectorRightKey;
	public static KeyMapping swapOffhandStrifeKey;

	private MSUKeyMappings()
	{
	}

	public static void register(RegisterKeyMappingsEvent event)
	{
		strifeKey = new KeyMapping("key.minestuckuniverseported.strife", GLFW.GLFW_KEY_V, CATEGORY);
		event.register(strifeKey);

		strifeSelectorLeftKey = new KeyMapping("key.minestuckuniverseported.strifeSelectorLeft", GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
		event.register(strifeSelectorLeftKey);

		strifeSelectorRightKey = new KeyMapping("key.minestuckuniverseported.strifeSelectorRight", GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
		event.register(strifeSelectorRightKey);

		swapOffhandStrifeKey = new KeyMapping("key.minestuckuniverseported.swapOffhandStrife", GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
		event.register(swapOffhandStrifeKey);
	}
}
