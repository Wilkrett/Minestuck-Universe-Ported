package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.request;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads {@code data/<namespace>/minestuckuniverseported/time_request_tiers/<category>.json} files into
 * {@link TimeRequestTierRegistry}. Mirrors {@code strife.MSUKindAbstrataDataLoader}'s shape (a
 * {@link SimpleJsonResourceReloadListener} registered via {@link AddReloadListenerEvent}), but simpler:
 * there's no code-registered baseline to extend, so {@code apply} just clears and rebuilds the whole
 * registry from whatever's present each reload.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class TimeRequestTierDataLoader
{
	private static final Logger LOGGER = LoggerFactory.getLogger("MinestuckUniversePorted/TimeRequestTiers");
	private static final String DIRECTORY = "minestuckuniverseported/time_request_tiers";

	private TimeRequestTierDataLoader()
	{
	}

	@SubscribeEvent
	private static void onAddReloadListeners(AddReloadListenerEvent event)
	{
		event.addListener(new Loader());
	}

	private static final class Loader extends SimpleJsonResourceReloadListener
	{
		Loader()
		{
			super(new GsonBuilder().create(), DIRECTORY);
		}

		@Override
		protected void apply(Map<ResourceLocation, JsonElement> jsonEntries, ResourceManager resourceManager, ProfilerFiller profiler)
		{
			TimeRequestTierRegistry.clear();
			int loaded = 0;

			for(Map.Entry<ResourceLocation, JsonElement> entry : jsonEntries.entrySet())
			{
				ResourceLocation fileId = entry.getKey();

				TimeRequestCategory category;
				try
				{
					category = TimeRequestCategory.valueOf(fileId.getPath().toUpperCase(Locale.ROOT));
				}
				catch(IllegalArgumentException e)
				{
					LOGGER.warn("Time request tier file {} doesn't match any TimeRequestCategory, skipping", fileId);
					continue;
				}

				List<TimeRequestTierEntry> tiers = TimeRequestTierEntry.LIST_CODEC.parse(JsonOps.INSTANCE, entry.getValue())
						.resultOrPartial(error -> LOGGER.error("Couldn't parse time request tiers {}: {}", fileId, error))
						.orElse(null);
				if(tiers == null)
					continue;

				List<TimeRequestTierEntry> valid = new ArrayList<>();
				for(TimeRequestTierEntry tier : tiers)
				{
					if(BuiltInRegistries.ITEM.containsKey(tier.item()))
						valid.add(tier);
					else
						LOGGER.warn("Time request tier for {} references unknown item {}, skipping that entry", category, tier.item());
				}

				TimeRequestTierRegistry.set(category, valid);
				loaded++;
			}

			LOGGER.info("Loaded {} time request tier categories", loaded);
		}
	}
}
