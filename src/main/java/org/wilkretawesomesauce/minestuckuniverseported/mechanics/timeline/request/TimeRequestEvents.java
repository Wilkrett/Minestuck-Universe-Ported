package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.request;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.network.TimeRequestSyncPacket;

/**
 * Login/respawn sync for {@link TimeRequestData} - same reasoning and same event pair as
 * {@code abilitech.AbilitechEvents}' own login/respawn sync for {@code AbilitechLoadout}.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class TimeRequestEvents
{
	private TimeRequestEvents()
	{
	}

	@SubscribeEvent
	private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
	{
		if(event.getEntity() instanceof ServerPlayer player)
			PacketDistributor.sendToPlayer(player, TimeRequestSyncPacket.create(player));
	}

	@SubscribeEvent
	private static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event)
	{
		if(event.getEntity() instanceof ServerPlayer player)
			PacketDistributor.sendToPlayer(player, TimeRequestSyncPacket.create(player));
	}
}
