package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.heart;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.client.gui.SoulShockScreen;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code TechSoulStun#onClientTick} - forces
 * {@link SoulShockScreen} open on the local player for as long as they're soul-shocked, and closes it
 * again the instant they're not (whether the effect wore off, or {@code TechSoulStun} was released on
 * the server, both sync the same way any potion effect already does).
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class SoulShockClientEvents
{
	private SoulShockClientEvents()
	{
	}

	@SubscribeEvent
	private static void onClientTick(ClientTickEvent.Post event)
	{
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if(player == null)
			return;

		boolean shocked = player.hasEffect(MSUMobEffects.SOUL_SHOCKED);

		// PauseScreen is deliberately exempt - SoulShockScreen#onClose() sends the player there on
		// Escape specifically so it stays reachable; re-forcing the stun screen open over it here would
		// defeat that entirely.
		if(shocked && !(mc.screen instanceof SoulShockScreen) && !(mc.screen instanceof PauseScreen))
			mc.setScreen(new SoulShockScreen());
		else if(!shocked && mc.screen instanceof SoulShockScreen)
			mc.setScreen(null);
	}
}
