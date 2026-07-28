package org.wilkretawesomesauce.minestuckuniverseported.client.render;

import com.mraof.minestuck.client.renderer.entity.ConsortRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import software.bernie.geckolib.event.GeoRenderEvent;

/**
 * Attaches {@link ConsortHatGeoLayer} (and {@link ConsortChestGeoLayer}, the user-requested chestplate
 * equivalent) to every real Minestuck {@code ConsortRenderer} instance - all four Consort species
 * (iguana/nakagator/salamander/turtle, {@code com.mraof.minestuck.entity.MSEntityTypes}) share that one
 * renderer class, each with its own instance, so this single {@code instanceof} check covers all of them.
 * Confirmed via {@code javap} (not guessed) that GeckoLib fires
 * {@code GeoRenderEvent.Entity.CompileRenderLayers} on the real {@code net.neoforged.neoforge.common.NeoForge
 * .EVENT_BUS} (the GAME bus) exactly once per {@code GeoEntityRenderer} construction - i.e. once per Consort
 * species renderer, at client startup, not once per frame.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ConsortHatRenderEvents
{
	private ConsortHatRenderEvents()
	{
	}

	@SubscribeEvent
	private static void onCompileConsortLayers(GeoRenderEvent.Entity.CompileRenderLayers event)
	{
		if(event.getRenderer() instanceof ConsortRenderer<?> renderer)
		{
			event.addLayer(new ConsortHatGeoLayer<>(renderer));
			event.addLayer(new ConsortChestGeoLayer<>(renderer));
		}
	}
}
