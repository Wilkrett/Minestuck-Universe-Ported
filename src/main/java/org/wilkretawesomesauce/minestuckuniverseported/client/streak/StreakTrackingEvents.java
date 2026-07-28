package org.wilkretawesomesauce.minestuckuniverseported.client.streak;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.network.StreakStateSyncPacket;

/**
 * Sends a newly-tracking observer the real current streak-toggle state of whatever they just started
 * tracking - a line-for-line mirror of
 * {@code abilitech.heroAspect.mind.CloakTrackingEvents}, covering late joiners and anyone simply
 * walking into render distance of an entity that already has {@code /msustreak toggle} turned on.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class StreakTrackingEvents
{
	private StreakTrackingEvents()
	{
	}

	@SubscribeEvent
	private static void onStartTracking(PlayerEvent.StartTracking event)
	{
		if(!(event.getTarget() instanceof LivingEntity target) || !(event.getEntity() instanceof ServerPlayer observer))
			return;

		StreakPreference preference = target.getData(MSUAttachments.STREAK_PREFERENCE);
		if(!preference.isEnabled())
			return;

		PacketDistributor.sendToPlayer(observer, new StreakStateSyncPacket(target.getId(), true, preference.resolveFlavour(),
				preference.isHideTrail(), preference.isGhostsIgnoreSprint(), preference.getGhostTint()));
	}
}
