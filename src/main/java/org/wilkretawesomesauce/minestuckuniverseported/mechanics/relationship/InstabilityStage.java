package org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship;

/**
 * The four bands from "Crimson Discord"'s own "Instability Stages" design document section - every
 * consumer of {@link Relationship#instability} (see {@link RelationshipManager#stageOf}) reads one of
 * these rather than re-deriving the {@code 0-25/26-50/51-75/76-100} thresholds itself.
 */
public enum InstabilityStage
{
	/** 0-25, "Minor tension" - a small extra reduction to Blood Bond sharing effectiveness. */
	MINOR,
	/** 26-50, "Noticeable strain" - Blood Vengeance (Cult retaliation) starts occasionally failing to fire. */
	NOTICEABLE,
	/** 51-75, "Relationships begin collapsing" - Blood Bonds lose sharing effectiveness rapidly, retaliation fails often. */
	COLLAPSING,
	/** 76-100, "Complete social collapse" - the relationship (and any Blood Bond built on it) automatically breaks - see {@link RelationshipManager#checkForCollapse}. */
	COLLAPSED
}
