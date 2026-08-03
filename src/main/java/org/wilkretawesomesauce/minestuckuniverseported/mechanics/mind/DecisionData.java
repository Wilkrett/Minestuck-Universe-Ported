package org.wilkretawesomesauce.minestuckuniverseported.mechanics.mind;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * The "Decision State" every {@code LivingEntity} carries (see {@code MSUAttachments#DECISION_DATA}) -
 * original design for this project, described directly by the user (a standalone design doc, "Mind
 * Aspect System Design") rather than ported from any real MinestuckUniverse (1.12.2) source, same
 * category as {@code mechanics.doom.DoomData}/{@code mechanics.freedom.FreedomData}.
 * <p>
 * Mind governs <b>decisions</b>, not thoughts/personality/knowledge/relationships (those belong to
 * Heart/Light/Blood respectively, per the source doc's own explicit boundary) - {@link #certainty}/
 * {@link #hesitation}/{@link #adaptability}/{@link #resolve} describe <i>how</i> an entity decides, not
 * <i>who</i> it is, and {@link #currentDecision}/{@link #currentDecisionTarget} are the actual decision
 * itself (what it's currently doing, and about whom/what). All four attribute fields are {@code 0-100}
 * with {@code 50} as neutral, matching {@code FreedomData}'s own convention for consistency across this
 * project's "hidden stat" systems - the source doc gives no explicit numeric scale of its own, this is a
 * deliberate interpretation, not a transcription.
 * <p>
 * <b>Deliberately NOT {@code copyOnDeath()}</b>, matching {@code DoomData}/{@code FreedomData}'s own
 * precedent: a fresh, neutral decision state on a respawned player's new instance is correct, not a bug -
 * nothing about "how you make decisions" should persist through death here any more than Freedom does.
 */
public class DecisionData implements IDecisionData, INBTSerializable<CompoundTag>
{
	public static final float DEFAULT = 50.0F;
	static final int MAX_HISTORY_ENTRIES = 8;

	private float certainty = DEFAULT;
	private float hesitation = DEFAULT;
	private float adaptability = DEFAULT;
	private float resolve = DEFAULT;

	@Nullable
	private DecisionType currentDecision;
	@Nullable
	private UUID currentDecisionTarget;
	private final Deque<DecisionRecord> history = new ArrayDeque<>();

	// Runtime-only bookkeeping for DecisionEvents' hesitation-pause pass - same never-persisted reasoning
	// as FreedomData's own suppressedGoals: a WrappedGoal only means anything against the live Mob
	// instance's own GoalSelector it came from. A mob that reloads mid-pause simply resumes immediately -
	// harmless, and not worth persisting a resume tick across a save/load boundary for.
	private long hesitationResumeTick;
	@Nullable
	private List<WrappedGoal> suppressedAttackGoals;

	@Override
	public float getCertainty()
	{
		return certainty;
	}

	public void setCertainty(float value)
	{
		certainty = clamp(value);
	}

	@Override
	public float getHesitation()
	{
		return hesitation;
	}

	public void setHesitation(float value)
	{
		hesitation = clamp(value);
	}

	@Override
	public float getAdaptability()
	{
		return adaptability;
	}

	public void setAdaptability(float value)
	{
		adaptability = clamp(value);
	}

	@Override
	public float getResolve()
	{
		return resolve;
	}

	public void setResolve(float value)
	{
		resolve = clamp(value);
	}

	@Override
	@Nullable
	public DecisionType getCurrentDecision()
	{
		return currentDecision;
	}

	public void setCurrentDecision(@Nullable DecisionType decision)
	{
		currentDecision = decision;
	}

	@Override
	@Nullable
	public UUID getCurrentDecisionTarget()
	{
		return currentDecisionTarget;
	}

	public void setCurrentDecisionTarget(@Nullable UUID target)
	{
		currentDecisionTarget = target;
	}

	public Deque<DecisionRecord> getHistory()
	{
		return history;
	}

	/** Appends to this entity's decision history, dropping the oldest entry past {@link #MAX_HISTORY_ENTRIES} - same shape as {@code Relationship#history}. */
	public void recordDecision(String description, long tick)
	{
		history.addLast(new DecisionRecord(description, tick));
		while(history.size() > MAX_HISTORY_ENTRIES)
			history.pollFirst();
	}

	long getHesitationResumeTick()
	{
		return hesitationResumeTick;
	}

	void setHesitationResumeTick(long tick)
	{
		hesitationResumeTick = tick;
	}

	@Nullable
	List<WrappedGoal> getSuppressedAttackGoals()
	{
		return suppressedAttackGoals;
	}

	void setSuppressedAttackGoals(@Nullable List<WrappedGoal> goals)
	{
		suppressedAttackGoals = goals;
	}

	private static float clamp(float value)
	{
		return Math.max(0.0F, Math.min(100.0F, value));
	}

	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider provider)
	{
		CompoundTag nbt = new CompoundTag();
		nbt.putFloat("Certainty", certainty);
		nbt.putFloat("Hesitation", hesitation);
		nbt.putFloat("Adaptability", adaptability);
		nbt.putFloat("Resolve", resolve);
		if(currentDecision != null)
			nbt.putString("CurrentDecision", currentDecision.name());
		if(currentDecisionTarget != null)
			nbt.putUUID("CurrentDecisionTarget", currentDecisionTarget);

		ListTag historyTag = new ListTag();
		for(DecisionRecord record : history)
		{
			CompoundTag entry = new CompoundTag();
			entry.putString("Description", record.description());
			entry.putLong("Tick", record.tick());
			historyTag.add(entry);
		}
		nbt.put("History", historyTag);

		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt)
	{
		certainty = nbt.contains("Certainty") ? nbt.getFloat("Certainty") : DEFAULT;
		hesitation = nbt.contains("Hesitation") ? nbt.getFloat("Hesitation") : DEFAULT;
		adaptability = nbt.contains("Adaptability") ? nbt.getFloat("Adaptability") : DEFAULT;
		resolve = nbt.contains("Resolve") ? nbt.getFloat("Resolve") : DEFAULT;
		currentDecision = nbt.contains("CurrentDecision") ? DecisionType.valueOf(nbt.getString("CurrentDecision")) : null;
		currentDecisionTarget = nbt.hasUUID("CurrentDecisionTarget") ? nbt.getUUID("CurrentDecisionTarget") : null;

		history.clear();
		for(int i = 0; i < nbt.getList("History", 10).size(); i++)
		{
			CompoundTag entry = nbt.getList("History", 10).getCompound(i);
			history.addLast(new DecisionRecord(entry.getString("Description"), entry.getLong("Tick")));
		}
	}

	/** One entry in {@link #history} - same shape as {@code Relationship.RelationshipEvent}. */
	public record DecisionRecord(String description, long tick)
	{
	}
}
