package org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/**
 * One relationship between exactly two entities - the design doc's own "Core Relationship Data". Real
 * mutable data object owned entirely by {@link RelationshipManager} (constructed only by
 * {@link RelationshipManager#getOrCreate}, never directly) - not a NeoForge data attachment on either
 * entity, matching {@code heroClass.witch.blood.CultOfPersonalityManager}'s own plain-static-registry
 * approach rather than this project's usual per-entity {@code capabilities} pattern, since a relationship
 * is inherently a fact about a <i>pair</i>, not either entity alone.
 * <p>
 * {@link #strength}/{@link #stability} are both {@code 0-100} per the doc's own ranges. {@link #history}
 * is real but deliberately capped (see {@link RelationshipManager#MAX_HISTORY_ENTRIES}) rather than an
 * unbounded log - "Relationship History" was listed as Core Data with no size limit specified, but an
 * ever-growing per-relationship list is exactly the kind of unbounded-memory-growth this project's other
 * systems (e.g. Cult of Personality's own bond bookkeeping) are careful to avoid.
 * <p>
 * {@link #instability}/{@link #instabilityRate} back {@code heroClass.bard.blood.TechBardBloodCrimsonDiscord}
 * ("Crimson Discord") - a real, separate {@code 0-100} value per its own "Instability - Core Data" design
 * document ("stored independently of Relationship Strength... can be both strong and unstable at the same
 * time"), not derived from {@link #strength}/{@link #stability} at all.
 * <p>
 * {@link #affinity}/{@link #trust}/{@link #familiarity}/{@link #conflict} are the "Relationship System
 * Foundation"/"...merge" design documents' own finer-grained values - a real <i>addition</i> to
 * {@link #strength}/{@link #stability}, not a replacement (both design docs list all six as Core Data
 * together). {@link #type} is <b>derived</b> from these four via {@link RelationshipManager#deriveType}
 * for every "ordinary" relationship (Loyalty/Friendship/Rivalry), <i>except</i> the "special origin" types
 * ({@link RelationshipType#FAMILY}/{@code OWNERSHIP}/{@code OBLIGATION}/{@code HOSTILE_ATTACHMENT}) - those
 * are set once at creation by whichever system created them and never auto-reassigned, per the doc's own
 * "created through ownership events"/"created through oaths, contracts, Blood Pacts" wording for those
 * specific types.
 */
public final class Relationship
{
	public final UUID id = UUID.randomUUID();
	public final UUID entityA;
	public final UUID entityB;
	public RelationshipType type;
	public float strength;
	public float stability;
	public final long createdTick;
	public long lastInteractionTick;
	public boolean corrupted;
	public final Deque<RelationshipEvent> history = new ArrayDeque<>();

	public float instability = 0F;
	/** How fast {@link #instability} naturally decays back toward 0 (points/real-tick-sweep-interval) absent Bard influence, and a multiplier on how fast the Bard's own influence raises it. */
	public float instabilityRate = 1F;
	public long lastInstabilityUpdateTick;

	/** Emotional preference, {@code -100 to 100} - positive: friendship/loyalty/attachment; negative: dislike/hatred/rivalry. */
	public float affinity = 0F;
	/** Willingness to rely on the other party, {@code 0-100}. */
	public float trust = 0F;
	/** How well the two know each other, {@code 0-100} - deliberately independent of {@link #affinity}: "an enemy can be highly familiar". */
	public float familiarity = 0F;
	/** Negative interaction, {@code 0-100} - fighting, competition, repeated hostility. */
	public float conflict = 0F;

	Relationship(UUID entityA, UUID entityB, RelationshipType type, float strength, float stability, long createdTick)
	{
		this.entityA = entityA;
		this.entityB = entityB;
		this.type = type;
		this.strength = strength;
		this.stability = stability;
		this.createdTick = createdTick;
		this.lastInteractionTick = createdTick;
		this.lastInstabilityUpdateTick = createdTick;
	}

	/** The other party of this relationship, given one side of it. */
	public UUID other(UUID knownSide)
	{
		return entityA.equals(knownSide) ? entityB : entityA;
	}

	public record RelationshipEvent(String description, long tick)
	{
	}
}
