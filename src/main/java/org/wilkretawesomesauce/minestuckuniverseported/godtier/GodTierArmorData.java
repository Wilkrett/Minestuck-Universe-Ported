package org.wilkretawesomesauce.minestuckuniverseported.godtier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mraof.minestuck.player.Title;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

/**
 * Ported from MinestuckUniverse (1.12.2)'s NBT-stamped {@code class}/{@code aspect} tag on
 * {@code ItemGTArmor} stacks. Reuses Minestuck's own {@link Title} record (and its existing
 * {@code CODEC}/{@code STREAM_CODEC}) directly rather than re-deriving a class+aspect pair from scratch,
 * since that's exactly what a Title already is.
 *
 * @param title empty for a freshly-crafted, not-yet-attuned piece; present once a player has ascended
 *              wearing it (see {@link GodTierEvents})
 */
public record GodTierArmorData(Optional<Title> title)
{
	public static final GodTierArmorData BLANK = new GodTierArmorData(Optional.empty());

	public static final Codec<GodTierArmorData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Title.CODEC.optionalFieldOf("title").forGetter(GodTierArmorData::title)
	).apply(instance, GodTierArmorData::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, GodTierArmorData> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.optional(Title.STREAM_CODEC), GodTierArmorData::title,
			GodTierArmorData::new
	);

	public boolean isAttuned()
	{
		return title.isPresent();
	}
}
