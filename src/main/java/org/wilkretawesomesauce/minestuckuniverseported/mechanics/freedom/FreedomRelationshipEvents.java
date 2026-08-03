package org.wilkretawesomesauce.minestuckuniverseported.mechanics.freedom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.Relationship;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.RelationshipManager;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.RelationshipType;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;

import java.util.UUID;

/**
 * "Relationship and Breath Interaction" from the "Minestuck Systems Overview" design doc - original
 * design for this project, no 1.12.2 counterpart. Registers itself against {@code RelationshipManager}'s
 * own public API rather than that class knowing anything about Freedom, the same one-way-dependency
 * shape {@code mechanics.doom.RelationshipDoomEvents} already established (Relationship stays generic
 * infrastructure; the newer system reaches into it, never the reverse).
 * <p>
 * <b>The flagship mechanic, and the doc's own literal example</b>: "A Page of Breath does not force a
 * mob to follow them. They increase its Freedom until it chooses to follow." A {@link Mob} that's both
 * currently {@link FreedomLevel#HIGH} and was last raised there by a specific player
 * ({@link FreedomData#getLastLiberatedBy}, set by {@code heroAspect.breath.TechBreathLiberate}) gets a
 * real trust/affinity boost toward that player every {@link #CHECK_INTERVAL_TICKS}; only if that's
 * enough to (re)derive the relationship into {@link RelationshipType#FRIENDSHIP}/{@code LOYALTY} - never
 * forced, never guaranteed on the first check, and never at all against a standing
 * {@link RelationshipType#HOSTILE} relationship - does the mob actually start following
 * ({@link FreedomData#setFollowing}, consumed by {@link FreedomFollowGoal}). Dropping back to
 * {@link FreedomLevel#LOW}/{@code EXTREME_LOW} (via {@code TechBreathConstrain}, or anything else that
 * lowers Freedom) breaks an existing willing-follow bond - "based on choice" cuts both ways, taking the
 * choice away ends the following.
 * <p>
 * <b>Real correction, from a later, separate "Minestuck Relationship System Interaction: Breath Aspect"
 * design doc's own explicit Core Design Rule</b>: "Blood creates the relationship. Breath determines
 * whether the relationship is chosen." An earlier version of {@link #tryFormWillingFollowership} used
 * {@link RelationshipManager#getOrCreate} here, which meant a Page of Breath could conjure real
 * followership out of a mob with zero prior connection to them at all - directly contradicting that
 * later doc's own opening line, "Breath does not create relationships." Fixed to use
 * {@link RelationshipManager#get} instead (never creates) - a mob only ever converts an <i>already
 * existing</i> relationship (real vanilla taming's own {@code OWNERSHIP}, an organically-formed
 * {@code FORMING}/{@code RIVALRY} from some earlier interaction, {@code KINSHIP}, etc.) into a chosen
 * one; a total stranger mob liberated to High Freedom gains nothing from this method at all, no matter
 * how long it's held - which is the correct reading of the doc's own wolf example ("follows because it
 * chooses to, not because it is unable to leave") - that wolf already has a real bond (taming) before
 * Breath ever touches it.
 * <p>
 * <b>Ambient relationship fragility</b>: "High Freedom relationships: based on choice, stronger... Low
 * Freedom relationships: based on control, more fragile" - a slow stability drift for every relationship
 * a {@link FreedomLevel#HIGH} or {@code LOW}/{@code EXTREME_LOW} entity is party to, the same ambient-tick
 * shape {@code RelationshipManager}'s own "Spending Time Together" familiarity gain already uses. Only
 * processed from the lexicographically-first side of each relationship ({@link Relationship#entityA}) so
 * a pair whose both sides independently tick this handler doesn't double-apply the drift.
 * <p>
 * <b>The doc's other two named relationship values are deliberately NOT new fields</b>, mapped onto what
 * this project's real, already-built {@code Relationship} class actually tracks instead of growing it for
 * one flavor doc (same restraint {@code RelationshipDoomEvents}/{@code RelationshipManager} itself already
 * shows toward their own source docs): "Respect... entities follow leadership more easily" needs no new
 * code at all - {@link RelationshipType#LOYALTY} ("Devotion/allegiance", trust&gt;70 and strength&gt;60)
 * is already this project's own closest existing analog, and already qualifies for the following
 * conversion above the moment it's derived, same as Friendship. "Fear... entities may obey but gain
 * reduced Freedom" has no dedicated numeric field to hook either - approximated in
 * {@code TechBreathLiberate} itself (not here) by checking for a standing {@link RelationshipType#HOSTILE}
 * relationship and dampening the Freedom gained per tick, rather than inventing a seventh
 * {@code Relationship} value that nothing else in this project would ever read.
 * <p>
 * <b>The plain Freedom/Doom four-quadrant matrix from the same doc is intentionally NOT implemented as
 * its own interaction class</b> - three of its four quadrants ("highly adaptable, few consequences",
 * "can escape... but every choice carries risk", "safe but limited") already emerge for free just from
 * running {@code FreedomEvents} and {@code mechanics.doom.DoomDamageEvents} side by side, nothing new to
 * build. Only the fourth ("trapped by circumstances, events feel inevitable" - Low Freedom + High Doom)
 * needed real code, since nothing already makes Doom's consequences hit <i>harder</i> specifically when
 * an entity has no room to escape them - see {@code mechanics.doom.FreedomDoomEvents} for that one piece.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class FreedomRelationshipEvents
{
	private static final int CHECK_INTERVAL_TICKS = 100;
	private static final float STABILITY_DRIFT_PER_CHECK = 1.0F;
	private static final float FOLLOW_TRUST_GAIN = 8.0F;
	private static final float FOLLOW_AFFINITY_GAIN = 8.0F;
	private static final double FOLLOW_SPEED = 1.0;

	private FreedomRelationshipEvents()
	{
	}

	@SubscribeEvent
	private static void onEntityTick(EntityTickEvent.Post event)
	{
		if(!(event.getEntity() instanceof LivingEntity entity) || entity.level().isClientSide())
			return;
		if(entity.tickCount % CHECK_INTERVAL_TICKS != 0)
			return;

		FreedomLevel level = entity.getData(MSUAttachments.FREEDOM_DATA).getLevel();
		applyStabilityDrift(entity, level);

		if(entity instanceof Mob mob)
		{
			if(level == FreedomLevel.HIGH)
				tryFormWillingFollowership(mob);
			else if(level == FreedomLevel.LOW || level == FreedomLevel.EXTREME_LOW)
				breakWillingFollowershipIfPresent(mob);
		}
	}

	private static void applyStabilityDrift(LivingEntity entity, FreedomLevel level)
	{
		if(level == FreedomLevel.NEUTRAL)
			return;

		float delta = level == FreedomLevel.HIGH ? STABILITY_DRIFT_PER_CHECK : -STABILITY_DRIFT_PER_CHECK;

		for(Relationship rel : RelationshipManager.getAllFor(entity.getUUID()))
			if(rel.entityA.equals(entity.getUUID()))
				RelationshipManager.adjustStability(rel, delta);
	}

	private static void tryFormWillingFollowership(Mob mob)
	{
		FreedomData data = mob.getData(MSUAttachments.FREEDOM_DATA);
		UUID liberator = data.getLastLiberatedBy();
		if(data.getFollowing() != null || liberator == null)
			return;

		if(!(mob.level() instanceof ServerLevel level) || !(level.getEntity(liberator) instanceof LivingEntity))
			return;

		// Never creates - see this class's own doc comment on the "Breath does not create relationships"
		// correction. A mob with no prior relationship to the liberator at all just doesn't qualify.
		Relationship rel = RelationshipManager.get(mob.getUUID(), liberator);
		if(rel == null || rel.type == RelationshipType.HOSTILE)
			return;

		long now = level.getGameTime();
		RelationshipManager.adjustTrust(rel, FOLLOW_TRUST_GAIN);
		RelationshipManager.adjustAffinity(rel, FOLLOW_AFFINITY_GAIN);
		RelationshipManager.deriveType(rel);

		if(rel.type == RelationshipType.FRIENDSHIP || rel.type == RelationshipType.LOYALTY)
		{
			data.setFollowing(liberator);
			RelationshipManager.recordEvent(rel, "Chose to follow after being freed", now);
			ensureFollowGoal(mob);
		}
	}

	private static void breakWillingFollowershipIfPresent(Mob mob)
	{
		FreedomData data = mob.getData(MSUAttachments.FREEDOM_DATA);
		UUID liberator = data.getFollowing();
		if(liberator == null)
			return;

		data.setFollowing(null);

		if(mob.level() instanceof ServerLevel level && RelationshipManager.get(mob.getUUID(), liberator) instanceof Relationship rel)
			RelationshipManager.recordEvent(rel, "Stopped following as Freedom was taken away", level.getGameTime());
	}

	/** Re-injects {@link FreedomFollowGoal} whenever a mob whose Freedom data still has {@link FreedomData#getFollowing} set (re)loads - goals aren't part of vanilla's own NBT persistence, same reasoning as {@code heroAspect.rage.RageMobEvents#onEntityJoinLevel}. */
	@SubscribeEvent
	private static void onEntityJoinLevel(EntityJoinLevelEvent event)
	{
		if(event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob))
			return;

		if(mob.getData(MSUAttachments.FREEDOM_DATA).getFollowing() != null)
			ensureFollowGoal(mob);
	}

	private static void ensureFollowGoal(Mob mob)
	{
		boolean alreadyPresent = mob.goalSelector.getAvailableGoals().stream()
				.anyMatch(wrapped -> wrapped.getGoal() instanceof FreedomFollowGoal);
		if(!alreadyPresent)
			mob.goalSelector.addGoal(6, new FreedomFollowGoal(mob, FOLLOW_SPEED));
	}
}
