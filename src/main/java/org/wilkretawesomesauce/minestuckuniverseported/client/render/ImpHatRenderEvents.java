package org.wilkretawesomesauce.minestuckuniverseported.client.render;

import com.mraof.minestuck.client.renderer.entity.UnderlingRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import software.bernie.geckolib.event.GeoRenderEvent;

/**
 * Attaches {@link ImpHatGeoLayer} to every real Minestuck {@code UnderlingRenderer} instance - confirmed via
 * {@code javap} that this one generic renderer class is shared across every Underling species (Imps,
 * Ogres, Basilisks, ...), unlike Consorts' one-instance-per-species {@code ConsortRenderer}, so this can't
 * narrow by renderer type the way {@link ConsortHatRenderEvents} does. {@link ImpHatGeoLayer#renderForBone}
 * itself gates on {@code animatable instanceof ImpEntity}, so attaching to every Underling renderer is safe
 * - it simply never renders anything for a non-Imp Underling.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ImpHatRenderEvents
{
	private ImpHatRenderEvents()
	{
	}

	@SubscribeEvent
	private static void onCompileUnderlingLayers(GeoRenderEvent.Entity.CompileRenderLayers event)
	{
		if(event.getRenderer() instanceof UnderlingRenderer<?> renderer)
			event.addLayer(new ImpHatGeoLayer<>(renderer));
	}
}
