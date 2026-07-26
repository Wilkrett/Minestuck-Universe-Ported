package org.wilkretawesomesauce.minestuckuniverseported.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKey;
import org.wilkretawesomesauce.minestuckuniverseported.network.AbilitechKeyPacket;

/**
 * Ported from the client-input half of MinestuckUniverse's {@code client.MSUKeys} for the 3 abilitech
 * keys. Same edge-triggered send-on-change approach as {@code MSUStrifeSwitcherClient} - only sends a
 * packet when a key's down-state actually flips, not every tick.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class MSUAbilitechClient
{
	private static boolean prevPrimary = false;
	private static boolean prevSecondary = false;
	private static boolean prevTertiary = false;

	private MSUAbilitechClient()
	{
	}

	@SubscribeEvent
	private static void onClientTick(ClientTickEvent.Post event)
	{
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if(player == null || mc.screen != null)
			return;

		boolean primary = MSUAbilitechKeyMappings.primaryKey.isDown();
		if(primary != prevPrimary)
		{
			prevPrimary = primary;
			PacketDistributor.sendToServer(new AbilitechKeyPacket(AbilitechKey.PRIMARY, primary));
		}

		boolean secondary = MSUAbilitechKeyMappings.secondaryKey.isDown();
		if(secondary != prevSecondary)
		{
			prevSecondary = secondary;
			PacketDistributor.sendToServer(new AbilitechKeyPacket(AbilitechKey.SECONDARY, secondary));
		}

		boolean tertiary = MSUAbilitechKeyMappings.tertiaryKey.isDown();
		if(tertiary != prevTertiary)
		{
			prevTertiary = tertiary;
			PacketDistributor.sendToServer(new AbilitechKeyPacket(AbilitechKey.TERTIARY, tertiary));
		}
	}
}
