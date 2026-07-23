package org.wilkretawesomesauce.minestuckuniverseported.strife;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Codec-friendly stand-in for a {@link StrifeSpecibus}, for storing one on an item stack as a data
 * component. Replaces the 1.12.2 {@code ItemStrifeCard}'s approach of stamping a raw
 * {@code "StrifeSpecibus"} NBT compound onto the stack's tag compound - components are the modern,
 * idiomatic way to attach structured data to an {@link ItemStack} (see how Minestuck itself does this
 * for e.g. {@code CardStoredItemComponent}).
 *
 * @param kind       the assigned kind's registry name, or empty if this is a blank/unassigned card
 * @param contents   items currently stored in the specibus (empty if unassigned)
 * @param customName player-set custom name, empty string if none
 */
public record StrifeSpecibusData(Optional<ResourceLocation> kind, List<ItemStack> contents, String customName)
{
	public static final StrifeSpecibusData EMPTY = new StrifeSpecibusData(Optional.empty(), List.of(), "");

	public static final Codec<StrifeSpecibusData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ResourceLocation.CODEC.optionalFieldOf("kind").forGetter(StrifeSpecibusData::kind),
			ItemStack.CODEC.listOf().optionalFieldOf("contents", List.of()).forGetter(StrifeSpecibusData::contents),
			Codec.STRING.optionalFieldOf("custom_name", "").forGetter(StrifeSpecibusData::customName)
	).apply(instance, StrifeSpecibusData::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, StrifeSpecibusData> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), StrifeSpecibusData::kind,
			ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), StrifeSpecibusData::contents,
			ByteBufCodecs.STRING_UTF8, StrifeSpecibusData::customName,
			StrifeSpecibusData::new
	);

	public static StrifeSpecibusData fromSpecibus(StrifeSpecibus specibus)
	{
		Optional<ResourceLocation> kind = specibus.isAssigned()
				? Optional.of(specibus.getKindAbstratus().getRegistryName())
				: Optional.empty();
		return new StrifeSpecibusData(kind, List.copyOf(specibus.getContents()), specibus.getCustomName());
	}

	public StrifeSpecibus toSpecibus()
	{
		KindAbstratus kindAbstratus = kind.map(MSUKindAbstrataRegistry::get).orElse(null);
		StrifeSpecibus specibus = new StrifeSpecibus(kindAbstratus);
		if(kindAbstratus != null)
			specibus.getContents().addAll(contents);
		specibus.setCustomName(customName);
		return specibus;
	}

	public boolean isAssigned()
	{
		return kind.isPresent();
	}
}
