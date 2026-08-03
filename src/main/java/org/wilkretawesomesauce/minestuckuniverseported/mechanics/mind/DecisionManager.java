package org.wilkretawesomesauce.minestuckuniverseported.mechanics.mind;

import net.minecraft.world.entity.LivingEntity;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.Relationship;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.RelationshipManager;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.RelationshipType;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * "Owns behavioral choices" - the source doc's own named class, real, project-original infrastructure
 * (no 1.12.2 counterpart) operating on the per-entity {@link DecisionData} attachment
 * ({@code MSUAttachments#DECISION_DATA}). Mirrors {@code mechanics.relationship.RelationshipManager}'s own
 * static-utility shape, but doesn't need that class's separate pairwise registry - a decision is a fact
 * about <i>one</i> entity, so it's a plain NeoForge attachment like {@code FreedomData}/{@code DoomData}
 * rather than a second static map.
 * <p>
 * <b>Architectural dependency is real and one-directional, exactly as the source doc's own diagram</b>
 * (<code>RelationshipManager -&gt; DecisionManager</code>): this class freely reads
 * {@link RelationshipManager} ({@link #evaluatePriority}), but never writes to it - no method here calls
 * {@code adjustTrust}/{@code adjustAffinity}/etc. Enforcing the doc's own explicit Design Boundary
 * ("Mind should never directly modify relationship values... Avoid +Trust/+Affinity/+Relationship
 * Strength - those belong to Blood") is done by simple omission: nothing in this file has a code path to
 * do that, matching the doc's own "the relationship itself remains unchanged, only the decision changes."
 * {@code RelationshipManager} itself has zero knowledge of this class or package, same one-way shape
 * {@code mechanics.doom.RelationshipDoomEvents}/{@code mechanics.freedom.FreedomRelationshipEvents}
 * already established for Doom/Freedom reaching into Relationship.
 * <p>
 * The doc's own "Possible operations" list (Reconsider/Commit/Redirect/Delay/Predict/Weaken confidence/
 * Reinforce confidence) map onto real methods here: {@link #commit}, {@link #reconsider}, {@link #tryRedirect}
 * (Resolve-resisted, the one operation the doc explicitly ties to being "harder to redirect... less
 * vulnerable to Mind abilities" at high Resolve), {@link #delay}, {@link #reinforceConfidence}/
 * {@link #weakenConfidence} (both just {@link #adjustCertainty} under the hood - the doc doesn't
 * distinguish a separate mechanic for either direction). "Predict" has no dedicated method of its own -
 * {@link IDecisionData}'s own plain getters already are the read-only "observe this entity's decision"
 * operation; {@code mage.mind.TechMageMindInsight} (the fourth sibling to
 * {@code mage.breath.TechMageBreathInsight}/{@code mage.blood.TechMageBloodInsight}/
 * {@code mage.doom.TechMageDoomInsight}) reads those directly, the same way {@code TechMageBloodInsight}
 * reads {@code Relationship}'s own plain fields rather than going through a dedicated "predict" method on
 * {@code RelationshipManager}.
 */
public final class DecisionManager
{
	private static final float RESOLVE_RESIST_MAX = 0.75F;

	private DecisionManager()
	{
	}

	private static DecisionData data(LivingEntity entity)
	{
		return entity.getData(MSUAttachments.DECISION_DATA);
	}

	public static void adjustCertainty(LivingEntity entity, float delta)
	{
		DecisionData data = data(entity);
		data.setCertainty(data.getCertainty() + delta);
	}

	public static void adjustHesitation(LivingEntity entity, float delta)
	{
		DecisionData data = data(entity);
		data.setHesitation(data.getHesitation() + delta);
	}

	public static void adjustAdaptability(LivingEntity entity, float delta)
	{
		DecisionData data = data(entity);
		data.setAdaptability(data.getAdaptability() + delta);
	}

	public static void adjustResolve(LivingEntity entity, float delta)
	{
		DecisionData data = data(entity);
		data.setResolve(data.getResolve() + delta);
	}

	/** "Reinforce confidence" - the doc's own named operation. */
	public static void reinforceConfidence(LivingEntity entity, float amount)
	{
		adjustCertainty(entity, Math.abs(amount));
	}

	/** "Weaken confidence" - the doc's own named operation. */
	public static void weakenConfidence(LivingEntity entity, float amount)
	{
		adjustCertainty(entity, -Math.abs(amount));
	}

	/** "Commit" - directly sets the current decision, no resistance check (the entity's own choice, not something imposed on it). Slightly reinforces Certainty, matching "commitment" being self-reinforcing. */
	public static void commit(LivingEntity entity, DecisionType decision, @Nullable UUID target, long tick)
	{
		DecisionData data = data(entity);
		data.setCurrentDecision(decision);
		data.setCurrentDecisionTarget(target);
		data.recordDecision("Committed to " + decision, tick);
		adjustCertainty(entity, 2F);
	}

	/** "Reconsider" - clears the current decision outright, letting it be naturally re-evaluated (by vanilla AI, or a later {@link #commit}). */
	public static void reconsider(LivingEntity entity, long tick)
	{
		DecisionData data = data(entity);
		data.setCurrentDecision(null);
		data.setCurrentDecisionTarget(null);
		data.recordDecision("Reconsidered", tick);
	}

	/** "Delay" - schedules a brief hesitation pause, consumed by {@code DecisionEvents}' own tick handler (never touches goal selectors directly - see this class's own doc comment on why goal-selector mutation stays centralized there). */
	public static void delay(LivingEntity entity, long now, int ticks)
	{
		data(entity).setHesitationResumeTick(now + ticks);
	}

	/**
	 * "Redirect" - the one operation the source doc explicitly ties to Resolve ("harder to redirect...
	 * less vulnerable to Mind abilities" at high Resolve). {@code pressure} is the caster/ability's own
	 * strength (0-100, higher overcomes more Resolve); returns whether the redirect actually took hold.
	 * Always records an attempt in history, win or lose - "Freedom cannot be forced" (the sibling Breath
	 * doc's own words) applies here too: resisting is a real, visible outcome, not a guaranteed success
	 * dressed up as a coin flip.
	 */
	public static boolean tryRedirect(LivingEntity entity, DecisionType decision, @Nullable UUID target, float pressure, long tick)
	{
		DecisionData data = data(entity);
		float resolve = data.getResolve();
		float resistChance = Math.max(0F, (resolve - DecisionData.DEFAULT) / DecisionData.DEFAULT) * RESOLVE_RESIST_MAX;

		if(entity.getRandom().nextFloat() < resistChance - pressure / 200F)
		{
			data.recordDecision("Resisted a redirect toward " + decision, tick);
			return false;
		}

		commit(entity, decision, target, tick);
		return true;
	}

	/**
	 * A flat resistance roll against Resolve alone, above-neutral only (same "only resists above the 50
	 * baseline" shape {@code mechanics.freedom.FreedomEvents} already established for its own resistance
	 * checks) - for Mind abilities that don't fit {@link #tryRedirect}'s decision-commit shape (e.g. a
	 * possession attempt, a confusion effect). Real callers: {@code heroAspect.mind.TechMindControl}/
	 * {@code TechMindConfusion}. Shares {@link #RESOLVE_RESIST_MAX} with {@link #tryRedirect} rather than
	 * a separate constant - both are "how hard is it to impose something on this entity's decisions."
	 */
	public static boolean resistsInfluence(LivingEntity target)
	{
		float resolve = data(target).getResolve();
		float resistChance = Math.max(0F, (resolve - DecisionData.DEFAULT) / DecisionData.DEFAULT) * RESOLVE_RESIST_MAX;
		return target.getRandom().nextFloat() < resistChance;
	}

	/**
	 * "The Decision system may consult the Relationship system when evaluating choices... Relationships
	 * provide context. They do not determine the decision" - the one real, live consult point in this
	 * pass (see this class's own doc comment for the full reasoning). Returns a rough 0-100 relevance
	 * score for how much {@code subject} should matter to {@code entity} right now: positive-type
	 * relationships (Ownership/Friendship/Loyalty/Family/Kinship) score by Trust, Hostile/Rivalry score
	 * by Conflict, anything else scores low by Familiarity alone. Read-only - never mutates
	 * {@code Relationship} itself, same one-way-dependency shape as this whole class.
	 */
	public static float evaluatePriority(LivingEntity entity, LivingEntity subject)
	{
		Relationship rel = RelationshipManager.get(entity.getUUID(), subject.getUUID());
		if(rel == null)
			return 0F;

		boolean positive = rel.type == RelationshipType.OWNERSHIP || rel.type == RelationshipType.FRIENDSHIP
				|| rel.type == RelationshipType.LOYALTY || rel.type == RelationshipType.FAMILY
				|| rel.type == RelationshipType.KINSHIP;
		if(positive)
			return rel.trust;

		boolean negative = rel.type == RelationshipType.HOSTILE || rel.type == RelationshipType.RIVALRY
				|| rel.type == RelationshipType.HOSTILE_ATTACHMENT;
		if(negative)
			return rel.conflict;

		return rel.familiarity * 0.3F;
	}
}
