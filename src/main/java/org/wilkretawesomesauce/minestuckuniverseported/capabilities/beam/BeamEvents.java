package org.wilkretawesomesauce.minestuckuniverseported.capabilities.beam;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.network.BeamSyncPacket;

import net.minecraft.world.level.Level;

/**
 * Ticks {@link BeamData} server-side every tick (mirrors the original's {@code MSUCapabilities#onWorldTick} -
 * the client-side half of that original pair is {@code client.BeamClientEvents}, kept in a separate,
 * {@code Dist.CLIENT}-only class per this project's standing convention for anything referencing
 * client-only classes like {@code Minecraft} - see {@code Beam}'s own doc comment for why ticking on both
 * sides, not per-tick network sync, is how the growing-beam animation actually works) and sends a
 * full-state {@link BeamSyncPacket} resync on the same two triggers the original used: whenever a beam
 * fires or is released ({@link #broadcast}, called from {@code Beam#fireBeam} and
 * {@code beam.BeamWeaponItem}'s own release-on-full-charge path), and to a player individually whenever
 * they (re)join a dimension (ported from the original's {@code onPlayerJoinWorld} - {@code EntityJoinLevelEvent}
 * is this project's usual player-join hook elsewhere, but the original specifically only fired on real
 * logins/dimension changes, which {@link PlayerEvent.PlayerLoggedInEvent}/
 * {@link PlayerEvent.PlayerChangedDimensionEvent} match more precisely than a generic entity-join check).
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class BeamEvents
{
	private BeamEvents()
	{
	}

	@SubscribeEvent
	private static void onLevelTick(LevelTickEvent.Post event)
	{
		if(!(event.getLevel() instanceof ServerLevel level))
			return;

		level.getData(MSUAttachments.BEAM_DATA).tickBeams(level);
	}

	@SubscribeEvent
	private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
	{
		sendSync(event.getEntity());
	}

	@SubscribeEvent
	private static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event)
	{
		sendSync(event.getEntity());
	}

	private static void sendSync(net.minecraft.world.entity.player.Player player)
	{
		if(player instanceof ServerPlayer serverPlayer && player.level() instanceof ServerLevel level)
		{
			BeamData data = level.getData(MSUAttachments.BEAM_DATA);
			PacketDistributor.sendToPlayer(serverPlayer, new BeamSyncPacket(data.serializeNBT(level.registryAccess())));
		}
	}

	/** Called from {@code Beam#fireBeam} and {@code beam.BeamWeaponItem}'s own force-release path. */
	public static void broadcast(Level level)
	{
		if(!(level instanceof ServerLevel serverLevel))
			return;

		BeamData data = serverLevel.getData(MSUAttachments.BEAM_DATA);
		PacketDistributor.sendToPlayersInDimension(serverLevel, new BeamSyncPacket(data.serializeNBT(serverLevel.registryAccess())));
	}
}
