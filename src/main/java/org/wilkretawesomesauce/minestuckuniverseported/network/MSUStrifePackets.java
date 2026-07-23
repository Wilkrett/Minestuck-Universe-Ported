package org.wilkretawesomesauce.minestuckuniverseported.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Small wrapper so {@code strife.StrifePortfolioHandler} doesn't need to know about
 * {@link PacketDistributor} directly. Only sends when the given player is actually a
 * {@link ServerPlayer} (i.e. we're running on the logical server) - callers are expected to have
 * already checked {@code level().isClientSide()}, but this is a cheap extra safety net.
 */
public final class MSUStrifePackets
{
	private MSUStrifePackets()
	{
	}

	public static void sendPortfolioSync(Player player, int... specibusIndexesUnused)
	{
		// TODO: specibusIndexesUnused mirrors the original's partial-sync optimization; not implemented
		// yet, we always resync the full portfolio. Wire it in if/when bandwidth becomes a concern.
		if(player instanceof ServerPlayer serverPlayer)
			PacketDistributor.sendToPlayer(serverPlayer, StrifePortfolioSyncPacket.create(serverPlayer));
	}

	public static void sendIndexesSync(Player player)
	{
		if(player instanceof ServerPlayer serverPlayer)
			PacketDistributor.sendToPlayer(serverPlayer, StrifeIndexesSyncPacket.create(serverPlayer));
	}
}
