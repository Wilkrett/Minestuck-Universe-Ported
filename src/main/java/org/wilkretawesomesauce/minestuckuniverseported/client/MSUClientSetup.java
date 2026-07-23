package org.wilkretawesomesauce.minestuckuniverseported.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import org.wilkretawesomesauce.minestuckuniverseported.MSUEntityTypes;
import org.wilkretawesomesauce.minestuckuniverseported.MSUItemComponents;
import org.wilkretawesomesauce.minestuckuniverseported.MSUItems;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMenuTypes;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUParticles;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.client.gui.ItemVoidScreen;
import org.wilkretawesomesauce.minestuckuniverseported.client.gui.JujuScreen;
import org.wilkretawesomesauce.minestuckuniverseported.client.gui.TemporalSendificatorScreen;
import org.wilkretawesomesauce.minestuckuniverseported.client.model.BubbleModel;
import org.wilkretawesomesauce.minestuckuniverseported.client.model.MSUModelLayers;
import org.wilkretawesomesauce.minestuckuniverseported.client.particles.InkParticle;
import org.wilkretawesomesauce.minestuckuniverseported.client.particles.PowerParticle;
import org.wilkretawesomesauce.minestuckuniverseported.client.particles.TimeGearsRiseParticle;
import org.wilkretawesomesauce.minestuckuniverseported.client.render.BubbleRenderer;
import org.wilkretawesomesauce.minestuckuniverseported.client.render.HopeGolemRenderer;
import org.wilkretawesomesauce.minestuckuniverseported.strife.StrifeSpecibusData;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code ItemStrifeCard#addPropertyOverride}. 1.21.1 still uses
 * the same predicate/{@code overrides}-array item model system the original relied on (the newer
 * component-based item model system that replaces this is 1.21.2+), so this ports directly rather than
 * needing a redesign - see {@code models/item/strife_card.json}'s {@code overrides} for the other half.
 * <p>
 * Registered the same way Minestuck itself registers its own item properties, in
 * {@code ClientProxy#init(FMLClientSetupEvent)}.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MSUClientSetup
{
	private MSUClientSetup()
	{
	}

	@SubscribeEvent
	private static void onClientSetup(FMLClientSetupEvent event)
	{
		event.enqueueWork(() -> ItemProperties.register(MSUItems.STRIFE_CARD.get(), Minestuckuniverseported.id("assigned"), (stack, level, holder, seed) ->
		{
			StrifeSpecibusData data = stack.get(MSUItemComponents.STRIFE_SPECIBUS);
			if(data == null)
				return 0f;
			return data.isAssigned() ? 1f : 0.5f;
		}));
	}

	@SubscribeEvent
	private static void onRegisterParticleProviders(RegisterParticleProvidersEvent event)
	{
		event.registerSpriteSet(MSUParticles.TIME_GEARS_RISE.get(), TimeGearsRiseParticle.Provider::new);
		event.registerSpriteSet(MSUParticles.POWER.get(), PowerParticle.Provider::new);
		event.registerSpriteSet(MSUParticles.INK.get(), InkParticle.Provider::new);
	}

	@SubscribeEvent
	private static void onRegisterMenuScreens(RegisterMenuScreensEvent event)
	{
		event.register(MSUMenuTypes.TEMPORAL_SENDIFICATOR.get(), TemporalSendificatorScreen::new);
		event.register(MSUMenuTypes.ITEM_VOID.get(), ItemVoidScreen::new);
		event.register(MSUMenuTypes.JUJU.get(), JujuScreen::new);
	}

	@SubscribeEvent
	private static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event)
	{
		event.registerLayerDefinition(MSUModelLayers.BUBBLE, BubbleModel::createBodyLayer);
	}

	@SubscribeEvent
	private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event)
	{
		event.registerEntityRenderer(MSUEntityTypes.BUBBLE.get(), BubbleRenderer::new);
		event.registerEntityRenderer(MSUEntityTypes.HOPE_GOLEM.get(), HopeGolemRenderer::new);
	}
}
