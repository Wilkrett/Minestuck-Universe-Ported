package org.wilkretawesomesauce.minestuckuniverseported.mechanics.mind;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Applies {@link DecisionData}'s four hidden attributes to real, hookable {@link Mob} AI behavior - see
 * that class's own doc comment for the stat itself, and {@link DecisionManager}'s own doc comment for
 * the deliberate split (this class alone touches {@code goalSelector}/vanilla target state; the manager
 * only ever mutates plain {@code DecisionData} fields). Original design for this project ("Mind Aspect
 * System Design"), no 1.12.2 counterpart. Player entities are deliberately not processed here - none of
 * these hooks (vanilla AI target-switching, goal suppression) apply to a real connected player, unlike
 * {@code mechanics.freedom.FreedomEvents} which has real player-relevant hooks (attribute modifiers) of
 * its own.
 * <p>
 * <b>Certainty vs. natural retargeting</b> ("less target switching... more commitment" at high Certainty):
 * every {@link #CHECK_INTERVAL_TICKS}, compares the {@link Mob}'s own real vanilla
 * {@link Mob#getTarget()} against {@link DecisionData#getCurrentDecisionTarget()}. A first target
 * (gaining or losing one entirely) always syncs freely - Certainty only ever resists a genuine
 * <i>swap</i> between two different real targets. Above-neutral Certainty gets a real chance to reject
 * the swap outright by forcibly re-setting {@link Mob#setTarget} back to the old decision target -
 * "harder to redirect" made literal, without touching {@link DecisionData#getResolve()} (Resolve is
 * reserved for resisting <i>Mind-ability</i> redirects specifically - see {@link DecisionManager#tryRedirect} -
 * not ordinary vanilla AI churn).
 * <p>
 * <b>Relationship-aware override</b> (the source doc's own "Relationships provide context. They do not
 * determine the decision"): before rolling Certainty resistance, {@link DecisionManager#evaluatePriority}
 * is consulted for both the old and prospective new target - if the new one is dramatically more
 * relevant (a real live implementation of the doc's own worked example: a Hostile threat should override
 * a merely-committed decision even for a high-Certainty entity), the swap goes through unconditionally,
 * no resistance roll at all. This is the one place {@code RelationshipManager} data actually reaches a
 * live AI decision in this pass - see {@link DecisionManager}'s own doc comment for why that stays a
 * modest, single consult point rather than a full replacement targeting AI.
 * <p>
 * <b>Adaptability vs. stale-target clearing</b> ("better responses to changing circumstances" at high
 * Adaptability, "tunnel vision" at low): once the tracked decision target is no longer alive/present, how
 * quickly {@link DecisionData#getCurrentDecisionTarget}/{@code getCurrentDecision} actually clear
 * (letting the entity naturally re-evaluate) is itself scaled by Adaptability - checked at
 * {@link #ADAPTABILITY_MIN_INTERVAL_TICKS} (high Adaptability) up to {@link #ADAPTABILITY_MAX_INTERVAL_TICKS}
 * (low Adaptability), rather than a fixed cadence, so a low-Adaptability entity visibly keeps "believing"
 * a dead/gone target still matters for longer.
 * <p>
 * <b>Hesitation vs. attack goals</b> ("brief delays before attacks... this is not a stun effect, it
 * represents uncertainty"): {@link DecisionManager#delay} schedules
 * {@link DecisionData#getHesitationResumeTick}; every tick (not throttled by {@link #CHECK_INTERVAL_TICKS} -
 * a pause needs to start and end precisely, not on a coarse polling cadence), this class splices real
 * {@link MeleeAttackGoal} instances out of the mob's own {@code goalSelector} for the pause's duration and
 * restores them (at their original priority) once it elapses - the same real goal-splicing idiom
 * {@code heroAspect.rage.TechRageManagement}/{@code mechanics.freedom.FreedomEvents} already established. Only the
 * attack goal is touched, deliberately: movement/look/flee/wander goals are untouched, so a hesitating
 * entity can still react to its surroundings in every way except actually committing to a swing - a real,
 * literal distinction from a stun (which the source doc explicitly says this isn't), not just a renamed
 * one. <b>A heuristic, not exhaustive</b>: matches {@link MeleeAttackGoal} by exact vanilla class only,
 * same stated limitation as {@code FreedomEvents}' own dodge/flee/wander goal-class heuristic - a modded
 * mob's own custom attack goal won't be recognized.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class DecisionEvents
{
	private static final int CHECK_INTERVAL_TICKS = 20;
	private static final float CERTAINTY_RESIST_MAX = 0.7F;
	private static final float RELATIONSHIP_OVERRIDE_THRESHOLD = 25F;
	private static final int ADAPTABILITY_MIN_INTERVAL_TICKS = 20;
	private static final int ADAPTABILITY_MAX_INTERVAL_TICKS = 200;

	private DecisionEvents()
	{
	}

	@SubscribeEvent
	private static void onEntityTick(EntityTickEvent.Post event)
	{
		if(!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide())
			return;

		DecisionData data = mob.getData(MSUAttachments.DECISION_DATA);

		processHesitationPause(mob, data);

		if(mob.tickCount % CHECK_INTERVAL_TICKS != 0)
			return;

		processTargetTracking(mob, data);
		processStaleTargetClearing(mob, data);
	}

	private static void processTargetTracking(Mob mob, DecisionData data)
	{
		if(!(mob.level() instanceof ServerLevel serverLevel))
			return;

		LivingEntity naturalTarget = mob.getTarget();
		UUID naturalTargetId = naturalTarget != null ? naturalTarget.getUUID() : null;
		UUID decisionTargetId = data.getCurrentDecisionTarget();

		if(Objects.equals(naturalTargetId, decisionTargetId))
			return;

		// A first target (gaining or losing one entirely) always syncs freely - Certainty only resists
		// an actual swap between two different real targets.
		if(decisionTargetId == null || naturalTargetId == null)
		{
			syncTarget(data, naturalTargetId);
			return;
		}

		if(!(serverLevel.getEntity(decisionTargetId) instanceof LivingEntity decisionTargetLive) || !decisionTargetLive.isAlive())
		{
			syncTarget(data, naturalTargetId);
			return;
		}

		float oldPriority = DecisionManager.evaluatePriority(mob, decisionTargetLive);
		float newPriority = DecisionManager.evaluatePriority(mob, naturalTarget);
		if(newPriority - oldPriority >= RELATIONSHIP_OVERRIDE_THRESHOLD)
		{
			syncTarget(data, naturalTargetId);
			return;
		}

		float resistChance = Math.max(0F, (data.getCertainty() - DecisionData.DEFAULT) / DecisionData.DEFAULT) * CERTAINTY_RESIST_MAX;
		if(mob.getRandom().nextFloat() < resistChance)
		{
			mob.setTarget(decisionTargetLive);
			return;
		}

		syncTarget(data, naturalTargetId);
	}

	/**
	 * Syncs {@link DecisionData#getCurrentDecisionTarget()} to a mob's own real vanilla combat target,
	 * and labels it {@link DecisionType#ATTACK} to match - {@code Mob#getTarget()} <i>is</i> vanilla's own
	 * attack target, so that label is always accurate when a target is actually present, and clearing it
	 * to {@code null} alongside a lost target keeps the two fields from ever silently disagreeing. This is
	 * what lets an entity that's simply hunting something via ordinary vanilla AI - never touched by any
	 * {@link DecisionManager#commit}/{@code tryRedirect} call - still show up correctly in a real client
	 * as "Currently: ATTACK targeting &lt;name&gt;" via {@code mage.mind.TechMageMindInsight}, rather than
	 * silently having a target tracked with no visible label at all (a real, reported gap in an earlier
	 * version of that tech, since fixed there too).
	 * <p>
	 * <b>Known limitation, not fully solved here</b>: {@link DecisionData#getCurrentDecisionTarget()} is
	 * genuinely overloaded - it doubles as both "what vanilla's own attack target is" (this method's own
	 * job) and "who a deliberately committed non-combat decision, like {@code PROTECT}, is about" (
	 * {@link DecisionManager#commit}'s job). Nothing in this project currently calls {@code commit} with a
	 * non-{@code ATTACK} decision that also sets a target, so the two roles have never actually collided in
	 * practice - but a future caller that does would have its own committed target silently overwritten
	 * the next time this method runs and finds a real (unrelated) vanilla combat target. Worth revisiting
	 * with a second, dedicated field if/when a real non-combat "protect this specific ally" caller exists.
	 */
	private static void syncTarget(DecisionData data, @Nullable UUID naturalTargetId)
	{
		data.setCurrentDecisionTarget(naturalTargetId);
		data.setCurrentDecision(naturalTargetId != null ? DecisionType.ATTACK : null);
	}

	private static void processStaleTargetClearing(Mob mob, DecisionData data)
	{
		UUID decisionTargetId = data.getCurrentDecisionTarget();
		if(decisionTargetId == null || !(mob.level() instanceof ServerLevel serverLevel))
			return;

		if(serverLevel.getEntity(decisionTargetId) instanceof LivingEntity live && live.isAlive())
			return;

		int interval = (int) Mth.lerp(data.getAdaptability() / 100F, ADAPTABILITY_MAX_INTERVAL_TICKS, ADAPTABILITY_MIN_INTERVAL_TICKS);
		if(mob.tickCount % interval == 0)
		{
			data.setCurrentDecisionTarget(null);
			data.setCurrentDecision(null);
		}
	}

	private static void processHesitationPause(Mob mob, DecisionData data)
	{
		long resumeTick = data.getHesitationResumeTick();
		if(resumeTick == 0)
			return;

		long now = mob.level().getGameTime();
		List<WrappedGoal> suppressed = data.getSuppressedAttackGoals();

		if(suppressed == null && now < resumeTick)
		{
			List<WrappedGoal> removed = new ArrayList<>();
			for(WrappedGoal wrapped : List.copyOf(mob.goalSelector.getAvailableGoals()))
			{
				if(wrapped.getGoal() instanceof MeleeAttackGoal)
				{
					removed.add(wrapped);
					mob.goalSelector.removeGoal(wrapped.getGoal());
				}
			}
			data.setSuppressedAttackGoals(removed);
		}
		else if(suppressed != null && now >= resumeTick)
		{
			for(WrappedGoal wrapped : suppressed)
				mob.goalSelector.addGoal(wrapped.getPriority(), wrapped.getGoal());
			data.setSuppressedAttackGoals(null);
			data.setHesitationResumeTick(0);
		}
	}
}
