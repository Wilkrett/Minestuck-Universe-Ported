package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.request;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * One open "paradox" - a single borrow-now-repay-later loop, from the Time Request / Doom System design
 * (see {@code CLAUDE.md}'s Time Request / Doom System section). Tracked per-player in
 * {@link TimeRequestData}, entirely separate from {@code mechanics.timeline.TimelineData}'s own Doom Points -
 * same name, unrelated bookkeeping, by deliberate design decision.
 * <p>
 * {@link #item} is what a repayment stack must match to resolve this request - it's a plain
 * {@link ResourceLocation}, not a stored {@code ItemStack}, since the tier tables
 * ({@link TimeRequestTierRegistry}) only ever pick a base item, never NBT/components. The borrowed
 * stack the player was actually given carries {@code MSUItemComponents.BORROWED_REQUEST_ID} = {@link #id}
 * so the Temporal Sendificator can reject it as its own repayment - see that component's own doc comment
 * for why tagging, not provenance-checking, is how "must be a new copy" is actually enforced. Losing or
 * destroying that borrowed stack doesn't block resolving this request: the requirement was always
 * "obtain an untagged copy", never "still hold the original".
 * <p>
 * {@link #doomPoints} is this request's own share of its owner's total DP - {@code TimeRequestDoomEvents}
 * accrues it over time (faster the more requests a player has open at once), and it simply stops existing
 * once the request resolves, taking that DP with it. Not itself an {@code INBTSerializable} attachment -
 * {@link #toNBT()}/{@link #fromNBT(CompoundTag)} are called by {@link TimeRequestData}'s own
 * (de)serialization, matching how {@code mechanics.timeline.TimelineBranch} hand-rolls its own NBT for the same
 * reason (a plain per-element record inside a list-holding attachment, not a Codec).
 */
public final class TimeRequest
{
	private final UUID id;
	private final TimeRequestCategory category;
	private final ResourceLocation item;
	private final long requestedAtGameTime;
	private double doomPoints;

	public TimeRequest(UUID id, TimeRequestCategory category, ResourceLocation item, long requestedAtGameTime, double doomPoints)
	{
		this.id = id;
		this.category = category;
		this.item = item;
		this.requestedAtGameTime = requestedAtGameTime;
		this.doomPoints = doomPoints;
	}

	public UUID getId()
	{
		return id;
	}

	public TimeRequestCategory getCategory()
	{
		return category;
	}

	public ResourceLocation getItem()
	{
		return item;
	}

	public long getRequestedAtGameTime()
	{
		return requestedAtGameTime;
	}

	public double getDoomPoints()
	{
		return doomPoints;
	}

	public void addDoomPoints(double amount)
	{
		this.doomPoints = Math.max(0, doomPoints + amount);
	}

	public CompoundTag toNBT()
	{
		CompoundTag nbt = new CompoundTag();
		nbt.putUUID("Id", id);
		nbt.putString("Category", category.name());
		nbt.putString("Item", item.toString());
		nbt.putLong("RequestedAt", requestedAtGameTime);
		nbt.putDouble("DoomPoints", doomPoints);
		return nbt;
	}

	public static TimeRequest fromNBT(CompoundTag nbt)
	{
		UUID id = nbt.getUUID("Id");
		TimeRequestCategory category = TimeRequestCategory.valueOf(nbt.getString("Category"));
		ResourceLocation item = ResourceLocation.parse(nbt.getString("Item"));
		long requestedAt = nbt.getLong("RequestedAt");
		double doomPoints = nbt.getDouble("DoomPoints");
		return new TimeRequest(id, category, item, requestedAt, doomPoints);
	}
}
