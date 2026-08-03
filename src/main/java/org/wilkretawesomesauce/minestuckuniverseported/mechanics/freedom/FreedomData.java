package org.wilkretawesomesauce.minestuckuniverseported.mechanics.freedom;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * The hidden 0-100 Freedom value every {@code LivingEntity} carries (see
 * {@code MSUAttachments#FREEDOM_DATA}) - original design for this project, described directly by the
 * user (a standalone design doc, "Minestuck - Breath Aspect Mechanic") rather than ported from any real
 * MinestuckUniverse (1.12.2) source, same category as {@code mechanics.doom.DoomData}.
 * <p>
 * Freedom represents physical/mental/spiritual freedom simultaneously - how much room an entity has to
 * act outside its default behavior, not a resource to spend. 50 is neutral (every entity's default). It
 * is <b>not</b> mind control: a low-Freedom entity still wants what it always wanted, it just loses the
 * behavioral slack to act on anything else (see {@code FreedomEvents}' own doc comment for exactly which
 * vanilla behaviors that suppresses, and which parts of the source doc - "more varied AI decisions",
 * "improvised alternate routes" - have no real generic hook to attach to and are deliberately NOT
 * modeled, rather than faked).
 * <p>
 * <b>Deliberately NOT {@code copyOnDeath()}</b>, matching {@code DoomData}'s own precedent and for the
 * same shape of reasoning: a fresh, neutral (50) value on a respawned player's new instance is the
 * wanted behavior here, not a bug to guard against - nothing about this project's design calls for a
 * Freedom debuff to persist through death, unlike e.g. the unlock-tracking/portfolio attachments that do
 * need {@code copyOnDeath()}.
 * <p>
 * {@link #lastLiberatedBy}/{@link #following} back the "Minestuck Systems Overview" design doc's
 * "Relationship and Breath Interaction" section (see {@code FreedomRelationshipEvents}' own doc comment
 * for the full mechanic) - real, persisted facts (unlike {@link #lastAppliedLevel}/{@link #suppressedGoals}
 * above), since "this mob chose to follow this specific player" is a real gameplay fact that should
 * survive a save/load round-trip, not runtime bookkeeping tied to a single live {@code GoalSelector}
 * instance.
 */
public class FreedomData implements IFreedomData, INBTSerializable<CompoundTag>
{
	public static final float DEFAULT = 50.0F;

	private float freedom = DEFAULT;

	// Runtime-only bookkeeping for FreedomEvents' goal-suppression pass (see that class's own doc
	// comment) - deliberately never persisted. A WrappedGoal only means anything against the live Mob
	// instance's own GoalSelector it came from; there is nothing sensible to write to NBT here, and
	// re-deriving "not currently suppressed" fresh on every world load is always safe (worst case, one
	// extra tick before EXTREME_LOW's goal removal (re)applies to a mob that was already at that level
	// when the chunk unloaded).
	@Nullable
	private FreedomLevel lastAppliedLevel;
	@Nullable
	private List<WrappedGoal> suppressedGoals;

	// Real, persisted - see this class's own doc comment above.
	@Nullable
	private UUID lastLiberatedBy;
	@Nullable
	private UUID following;

	@Override
	public float getFreedom()
	{
		return freedom;
	}

	@Override
	public void setFreedom(float value)
	{
		freedom = Math.max(0.0F, Math.min(100.0F, value));
	}

	@Override
	public void addFreedom(float delta)
	{
		setFreedom(freedom + delta);
	}

	@Override
	public FreedomLevel getLevel()
	{
		return FreedomLevel.of(freedom);
	}

	@Nullable
	FreedomLevel getLastAppliedLevel()
	{
		return lastAppliedLevel;
	}

	void setLastAppliedLevel(@Nullable FreedomLevel level)
	{
		lastAppliedLevel = level;
	}

	@Nullable
	List<WrappedGoal> getSuppressedGoals()
	{
		return suppressedGoals;
	}

	void setSuppressedGoals(@Nullable List<WrappedGoal> goals)
	{
		suppressedGoals = goals;
	}

	/** Who last raised this entity's Freedom via {@code TechBreathLiberate} - see this class's own doc comment. */
	@Nullable
	public UUID getLastLiberatedBy()
	{
		return lastLiberatedBy;
	}

	public void setLastLiberatedBy(@Nullable UUID id)
	{
		lastLiberatedBy = id;
	}

	/** Who this entity has willingly chosen to follow, if anyone - see this class's own doc comment. */
	@Nullable
	public UUID getFollowing()
	{
		return following;
	}

	public void setFollowing(@Nullable UUID id)
	{
		following = id;
	}

	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider provider)
	{
		CompoundTag nbt = new CompoundTag();
		nbt.putFloat("Freedom", freedom);
		if(lastLiberatedBy != null)
			nbt.putUUID("LastLiberatedBy", lastLiberatedBy);
		if(following != null)
			nbt.putUUID("Following", following);
		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt)
	{
		freedom = nbt.contains("Freedom") ? nbt.getFloat("Freedom") : DEFAULT;
		lastLiberatedBy = nbt.hasUUID("LastLiberatedBy") ? nbt.getUUID("LastLiberatedBy") : null;
		following = nbt.hasUUID("Following") ? nbt.getUUID("Following") : null;
	}
}
