package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.request;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory, purely data-driven table of what item {@link TechFutureRequest} hands out per category and
 * {@link com.mraof.minestuck.player.Echeladder} rung. Populated entirely by {@link TimeRequestTierDataLoader}
 * on every resource reload - unlike {@code strife.MSUKindAbstrataRegistry}, there's no code-registered
 * baseline to extend, so a full clear-and-rebuild on every reload is safe (no stale-data risk to work
 * around, unlike the known limitation documented on {@code strife.MSUKindAbstrataDataLoader}).
 */
public final class TimeRequestTierRegistry
{
	private static final Map<TimeRequestCategory, List<TimeRequestTierEntry>> TIERS = new EnumMap<>(TimeRequestCategory.class);

	private TimeRequestTierRegistry()
	{
	}

	static void clear()
	{
		TIERS.clear();
	}

	static void set(TimeRequestCategory category, List<TimeRequestTierEntry> entries)
	{
		TIERS.put(category, entries);
	}

	/** The highest-rung entry at or below {@code rung} for this category, or null if the category has no eligible entries (e.g. no datapack loaded yet, or the player is below every threshold). */
	@Nullable
	public static ResourceLocation pickItem(TimeRequestCategory category, int rung)
	{
		ResourceLocation best = null;
		int bestRung = Integer.MIN_VALUE;
		for(TimeRequestTierEntry entry : TIERS.getOrDefault(category, List.of()))
			if(entry.rung() <= rung && entry.rung() > bestRung)
			{
				best = entry.item();
				bestRung = entry.rung();
			}
		return best;
	}
}
