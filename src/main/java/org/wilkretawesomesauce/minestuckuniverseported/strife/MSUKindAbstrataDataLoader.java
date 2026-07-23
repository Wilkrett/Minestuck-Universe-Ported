package org.wilkretawesomesauce.minestuckuniverseported.strife;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

import java.util.Map;

/**
 * Loads {@code data/<namespace>/minestuckuniverseported/strife_kinds/*.json} files and applies them to
 * {@link MSUKindAbstrataRegistry}, either extending an existing code-registered {@link KindAbstratus}
 * (see {@link MSUKindAbstrata}) with more items/keywords, or - if no kind with that id is registered yet
 * - defining a brand new one purely from data (item-list/keyword matching only, since class-matching and
 * conditionals can't be expressed in JSON).
 * <p>
 * This is a genuine gap-filler for {@link MSUKindAbstrata}'s "TODO(items subsystem)" placeholders: once
 * this addon's own tool items exist, a datapack (including this mod's own, via
 * {@code src/main/resources/data/...}) can wire them into e.g. {@code hammerkind} without touching Java
 * at all.
 * <p>
 * Known limitation: since {@link KindAbstratus} instances are mutated in place rather than rebuilt from
 * scratch on every reload, removing a datapack that previously added items to a kind won't retract those
 * items until a full restart - {@code "replace": true} only clears+reapplies the matchers for kinds whose
 * JSON file is still present on the reload in question, it doesn't detect "this file used to exist and
 * doesn't anymore". Full clean-slate reloading would need {@link MSUKindAbstrata}'s baseline kinds to be
 * cheaply reconstructable, which they currently aren't (they're populated once into static fields at mod
 * construction time).
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class MSUKindAbstrataDataLoader
{
	private static final Logger LOGGER = LoggerFactory.getLogger("MinestuckUniversePorted/StrifeKinds");
	private static final String DIRECTORY = "minestuckuniverseported/strife_kinds";

	private MSUKindAbstrataDataLoader()
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
			int applied = 0, created = 0;

			for(Map.Entry<ResourceLocation, JsonElement> entry : jsonEntries.entrySet())
			{
				ResourceLocation kindId = entry.getKey();

				StrifeKindData data = StrifeKindData.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
						.resultOrPartial(error -> LOGGER.error("Couldn't parse strife kind {}: {}", kindId, error))
						.orElse(null);
				if(data == null)
					continue;

				KindAbstratus kind = MSUKindAbstrataRegistry.get(kindId);
				if(kind == null)
				{
					kind = MSUKindAbstrataRegistry.register(new KindAbstratus(kindId));
					created++;
				}
				else applied++;

				if(data.replace())
					kind.clearDataDrivenMatchers();

				for(ResourceLocation itemId : data.items())
				{
					if(!BuiltInRegistries.ITEM.containsKey(itemId))
					{
						LOGGER.warn("Strife kind {} references unknown item {}, skipping", kindId, itemId);
						continue;
					}
					Item item = BuiltInRegistries.ITEM.get(itemId);
					if(item != Items.AIR)
						kind.addItem(item);
				}

				kind.addKeywords(data.keywords().toArray(new String[0]));
				if(data.hidden())
					kind.setHidden(true);
			}

			LOGGER.info("Loaded {} strife kind datapack entries ({} extending existing kinds, {} new)", jsonEntries.size(), applied, created);
		}
	}
}
