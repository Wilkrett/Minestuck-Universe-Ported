package org.wilkretawesomesauce.minestuckuniverseported.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

/**
 * Client-side half of {@code beam.BeamEvents} - ticks the locally-loaded level's {@code beam.BeamData}
 * every client tick, mirroring the original's {@code MSUCapabilities#onClientTick}. See {@code beam.Beam}'s
 * own doc comment for why this (not per-tick network sync) is how the growing-beam animation works.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class BeamClientEvents
{
	private BeamClientEvents()
	{
	}

	@SubscribeEvent
	private static void onClientTick(ClientTickEvent.Post event)
	{
		Minecraft mc = Minecraft.getInstance();
		if(mc.level == null || mc.isPaused())
			return;

		mc.level.getData(MSUAttachments.BEAM_DATA).tickBeams(mc.level);
	}
}
