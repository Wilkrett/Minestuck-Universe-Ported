package org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship;

/**
 * The four "Helping" event categories from the "Relationship Helping System" design document - passed to
 * {@link RelationshipManager#recordPositiveInteraction}, which applies each type's own relative mix of
 * Trust/Affinity/Strength/Familiarity gains (see that method's own doc comment for the exact proportions
 * and why Rescue outweighs the others - "one of the strongest positive relationship events" per the doc).
 */
public enum RelationshipEventType
{
	/** Restoring health - real caller: {@link RelationshipManager#onLivingDamage}'s "Protection" detection wires attacks, not this; this type has no current real caller (see that method's own doc comment for why "Healing" needs a known-caster push from a specific ability, not a blanket listener). */
	HEALING,
	/** Fighting off a threat that was targeting an ally - real caller: {@link RelationshipManager#onLivingDamage}. The doc's own priority-1 pick: "most reliable, easy to detect through damage events". */
	PROTECTION,
	/** Giving an item/resource - real caller: {@link RelationshipManager#onEntityInteract} (feeding a tame animal). */
	SHARING,
	/** Reviving, saving from death, or curing a dangerous effect - "one of the strongest positive relationship events" per the doc. Real caller: {@code heroAspect.life.SavingGraceEvents} (Saving Grace's own real cancel-death-and-heal handler credits whoever cast the ward, at the moment it actually triggers). */
	RESCUE
}
