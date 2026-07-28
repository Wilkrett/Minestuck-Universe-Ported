package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.mind;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.network.CloakSyncPacket;

/**
 * Sends a newly-tracking observer the real current cloak state of whatever they just started tracking -
 * covers late joiners and anyone simply walking into render distance of an already-cloaked player,
 * which {@code TechMindCloak}'s own on-change broadcast alone wouldn't reach.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class CloakTrackingEvents
{
	private CloakTrackingEvents()
	{
	}

	@SubscribeEvent
	private static void onStartTracking(PlayerEvent.StartTracking event)
	{
		if(!(event.getTarget() instanceof Player cloaked) || !(event.getEntity() instanceof ServerPlayer observer))
			return;

		EntityType<?> cloakType = cloaked.getData(MSUAttachments.ABILITECH_LOADOUT).getCloakType();
		if(cloakType == null)
			return;

		PacketDistributor.sendToPlayer(observer, new CloakSyncPacket(cloaked.getId(), true, BuiltInRegistries.ENTITY_TYPE.getKey(cloakType)));
	}
}
