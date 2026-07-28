package org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship;

/**
 * The relationship categories from the "Relationship System" design documents (the original, and the
 * later "...Foundation"/"...merge" revisions that added {@link #HOSTILE_ATTACHMENT}). {@link #LOYALTY}/
 * {@link #FRIENDSHIP}/{@link #RIVALRY} are "ordinary" types - {@link RelationshipManager#deriveType}
 * actively computes and reassigns them from a relationship's own affinity/trust/familiarity/conflict
 * values. {@link #FAMILY}/{@link #OWNERSHIP}/{@link #OBLIGATION}/{@link #HOSTILE_ATTACHMENT}/
 * {@link #HOSTILE}/{@link #KINSHIP} are "special origin" types per the merge doc's own wording ("created
 * through ownership events"/"created through oaths, contracts, Blood Pacts") - set once by whichever
 * system creates them and never overwritten by {@code deriveType}. {@link RelationshipManager#onLivingDamage}/
 * {@code onLivingDeath} only ever weaken the positive types (Loyalty/Friendship/Family/Ownership/Kinship)
 * on betrayal - damaging a Rival is expected, not a betrayal.
 */
public enum RelationshipType
{
	/**asdasd
	 * Not from either design document by name - a real, project-original addition needed to make
	 * {@link RelationshipManager#deriveType} correct: the default type for a relationship organically
	 * created by a single event (one hit, one instance of fighting alongside someone) that hasn't yet
	 * crossed any real threshold. Without this, a freshly-created relationship would need to start
	 * <i>as</i> {@link #RIVALRY} or {@link #FRIENDSHIP} just to have some type at all, which would
	 * misrepresent a single low-conflict hit as an already-full-blown rivalry. Auto-derived like the three
	 * ordinary types (never a special origin), and the natural type for anything that hasn't earned a real
	 * one yet.
	 */
	FORMING,
	/** Devotion/allegiance - player and summoned creature, leader and follower. Auto-derived: trust &gt; 70 and strength &gt; 60. */
	LOYALTY,
	/** Positive personal connection - player friendships, animal bonds, trusted allies. Auto-derived: affinity &gt; 40 and trust &gt; 50. */
	FRIENDSHIP,
	/** Strong natural Blood connections - villager families, pack animals, Blood Bonds (this project's own Cult of Personality links - see {@code heroClass.witch.blood.CultOfPersonalityManager#link}). Special origin, never auto-derived. */
	FAMILY,
	/** Creator/creation - player and pet, summoner and summon. Special origin (real vanilla taming/this project's own {@code entity.HopeGolemEntity}), never auto-derived. */
	OWNERSHIP,
	/**
	 * A same-species mob's baseline goodwill toward other members of its own species - real,
	 * project-original addition (not from either design document by name, which never named a positive
	 * counterpart to {@link #HOSTILE}). Unlike {@link #FRIENDSHIP} (earned gradually through
	 * trust/affinity) this is a special origin, assigned immediately just from two real vanilla
	 * {@code Mob}s sharing the same {@code EntityType} - see
	 * {@link RelationshipManager#ensureNaturalRelationship}'s own doc comment.
	 */
	KINSHIP,
	/** A negative relationship - increased aggression, competition, target priority. Auto-derived: conflict &gt; 60 and familiarity &gt; 40. */
	RIVALRY,
	/**
	 * A hostile mob's baseline hostility toward players and whatever it's currently targeting - real,
	 * project-original addition (not from either design document by name, which only ever named
	 * {@link #RIVALRY}/{@link #HOSTILE_ATTACHMENT} for negative relationships). Unlike {@link #RIVALRY}
	 * (earned gradually through repeated conflict) this is a special origin, assigned immediately just from
	 * a mob being a real vanilla {@code monster.Enemy} - see
	 * {@link RelationshipManager#ensureNaturalRelationship}'s own doc comment.
	 */
	HOSTILE,
	/** A forced/required relationship - oaths, contracts, Blood Pacts. Special origin; <b>no real producer exists yet</b> - nothing in this project creates oaths/contracts/Blood Pacts, so this is ready infrastructure with no current caller, same category as this project's other documented "real but unused" gaps. */
	OBLIGATION,
	/** "Pain has become a connection" - the merge doc's own "Masochistic Tendencies" example. Special origin, requires a specific ability actively flagging it; <b>no real producer exists yet</b> for the same reason as {@link #OBLIGATION} - no such ability has been built in this project. */
	HOSTILE_ATTACHMENT
}
