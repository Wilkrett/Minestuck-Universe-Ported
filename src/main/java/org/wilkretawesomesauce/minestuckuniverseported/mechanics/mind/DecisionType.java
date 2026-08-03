package org.wilkretawesomesauce.minestuckuniverseported.mechanics.mind;

/**
 * The "Decision Objects" from the "Mind Aspect System Design" doc - what an entity is currently doing,
 * not how it feels about it. Deliberately a closed, small set matching the doc's own worked list rather
 * than an open-ended free-text goal name - a real {@link DecisionManager#commit} caller always means one
 * of these, and nothing in this project needs more granularity than this yet.
 */
public enum DecisionType
{
	ATTACK,
	PROTECT,
	FLEE,
	WANDER,
	FOLLOW,
	BREED,
	SEARCH,
	HARVEST,
	GUARD
}
