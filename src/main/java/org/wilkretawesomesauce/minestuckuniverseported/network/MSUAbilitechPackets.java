package org.wilkretawesomesauce.minestuckuniverseported.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class MSUAbilitechPackets
{
	private MSUAbilitechPackets()
	{
	}

	public static void sendLoadoutSync(ServerPlayer player)
	{
		PacketDistributor.sendToPlayer(player, AbilitechLoadoutSyncPacket.create(player));
	}
}
