package org.wilkretawesomesauce.minestuckuniverseported.badges;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.network.BuilderBadgeSyncPacket;
import org.wilkretawesomesauce.minestuckuniverseported.skills.MSUSkills;

/**
 * Login/respawn sync for {@link BadgeBuilder} - same reasoning as {@code capabilities.godTier.GodTierData}'s
 * own login/respawn sync: attachments aren't automatically synced to the client, so without this a
 * relogged player's client wouldn't know the badge is active until the next unlock. {@link #sync} is also
 * called directly by {@code command.GodTierDebugCommand} right after a real Builder Badge unlock - the
 * only currently-reachable unlock path for any badge in this project (see {@code badges.Badge}'s own doc
 * comment on the Skill Shop not listing badges yet).
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class BuilderBadgeEvents
{
	private BuilderBadgeEvents()
	{
	}

	public static void sync(ServerPlayer player)
	{
		boolean active = player.getData(MSUAttachments.GOD_TIER).isBadgeActive(MSUSkills.BUILDER_BADGE, player.level(), player);
		PacketDistributor.sendToPlayer(player, new BuilderBadgeSyncPacket(active));
	}

	@SubscribeEvent
	private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
	{
		if(event.getEntity() instanceof ServerPlayer player)
			sync(player);
	}

	@SubscribeEvent
	private static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event)
	{
		if(event.getEntity() instanceof ServerPlayer player)
			sync(player);
	}
}
