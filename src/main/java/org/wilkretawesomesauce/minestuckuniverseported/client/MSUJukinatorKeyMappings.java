package org.wilkretawesomesauce.minestuckuniverseported.client;

import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * The 4 lane keys for {@code client.jukinator.JukinatorScreen}'s rhythm minigame - real, original design
 * for this project, no 1.12.2 counterpart. Own category (rather than reusing
 * {@code MSUAbilitechKeyMappings}'s) so these 4 keys are independently rebindable in the Controls menu.
 * Unlike the abilitech keys, nothing polls these every client tick - the minigame screen itself checks
 * them directly via {@link KeyMapping#matches(int, int)} inside its own {@code keyPressed}, since input
 * only matters while that screen is actually open.
 */
public final class MSUJukinatorKeyMappings
{
	private static final String CATEGORY = "key.categories.minestuckuniverseported.jukinator";

	public static KeyMapping lane0;
	public static KeyMapping lane1;
	public static KeyMapping lane2;
	public static KeyMapping lane3;

	private MSUJukinatorKeyMappings()
	{
	}

	public static void register(RegisterKeyMappingsEvent event)
	{
		lane0 = new KeyMapping("key.minestuckuniverseported.jukinatorLane0", GLFW.GLFW_KEY_S, CATEGORY);
		event.register(lane0);

		lane1 = new KeyMapping("key.minestuckuniverseported.jukinatorLane1", GLFW.GLFW_KEY_D, CATEGORY);
		event.register(lane1);

		lane2 = new KeyMapping("key.minestuckuniverseported.jukinatorLane2", GLFW.GLFW_KEY_J, CATEGORY);
		event.register(lane2);

		lane3 = new KeyMapping("key.minestuckuniverseported.jukinatorLane3", GLFW.GLFW_KEY_K, CATEGORY);
		event.register(lane3);
	}
}
