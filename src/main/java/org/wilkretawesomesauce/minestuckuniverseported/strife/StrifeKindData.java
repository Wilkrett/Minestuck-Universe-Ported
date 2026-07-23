package org.wilkretawesomesauce.minestuckuniverseported.strife;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Datapack format for extending (or, for a kind id that doesn't exist in code, defining from scratch) a
 * {@link KindAbstratus}'s item/keyword matchers. Lives at
 * {@code data/<namespace>/minestuckuniverseported/strife_kinds/<kind_path>.json}, where the file's
 * location determines which kind it targets - a file at
 * {@code data/minestuckuniverseported/minestuckuniverseported/strife_kinds/hammer.json} targets the
 * {@code minestuckuniverseported:hammer} kind, matching how the file's implied id lines up with the
 * kind's registry name.
 * <p>
 * Only item-list and keyword matching are datapack-controllable, since item-class matching and
 * conditionals are inherently Java concepts. See {@link MSUKindAbstrataDataLoader} for the loader and
 * {@link KindAbstratus} for what's data-driven vs. code-only.
 *
 * @param items    item ids to add as exact matches
 * @param keywords substring keywords to add, matched against the item's registry path
 * @param hidden   if present, overrides whether the kind shows up as selectable in the strife card UI
 * @param replace  if true, clear this kind's existing datapack-controlled matchers before applying this
 *                 file's items/keywords, instead of just adding to whatever's already there
 */
public record StrifeKindData(List<ResourceLocation> items, List<String> keywords, boolean hidden, boolean replace)
{
	public static final Codec<StrifeKindData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ResourceLocation.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(StrifeKindData::items),
			Codec.STRING.listOf().optionalFieldOf("keywords", List.of()).forGetter(StrifeKindData::keywords),
			Codec.BOOL.optionalFieldOf("hidden", false).forGetter(StrifeKindData::hidden),
			Codec.BOOL.optionalFieldOf("replace", false).forGetter(StrifeKindData::replace)
	).apply(instance, StrifeKindData::new));
}
