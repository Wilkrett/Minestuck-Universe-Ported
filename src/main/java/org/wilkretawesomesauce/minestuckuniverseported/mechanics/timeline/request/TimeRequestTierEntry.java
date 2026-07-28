package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.request;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * One rung threshold in a category's item-tier table (see {@link TimeRequestTierRegistry}). Datapack
 * format lives at {@code data/<namespace>/minestuckuniverseported/time_request_tiers/<category>.json},
 * where the filename (lowercased {@link TimeRequestCategory} name, e.g. {@code weapon.json}) determines
 * which category the file's tier list applies to - same "location determines target" convention
 * {@code strife.StrifeKindData} uses for kind ids.
 *
 * @param rung the minimum {@link com.mraof.minestuck.player.Echeladder} rung a player needs to be at or
 *             above for this entry to be eligible - {@link TimeRequestTierRegistry#pickItem} picks the
 *             highest-rung eligible entry, not the first match
 * @param item the concrete item this tier resolves to
 */
public record TimeRequestTierEntry(int rung, ResourceLocation item)
{
	public static final Codec<TimeRequestTierEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("rung").forGetter(TimeRequestTierEntry::rung),
			ResourceLocation.CODEC.fieldOf("item").forGetter(TimeRequestTierEntry::item)
	).apply(instance, TimeRequestTierEntry::new));

	public static final Codec<List<TimeRequestTierEntry>> LIST_CODEC = CODEC.listOf();
}
