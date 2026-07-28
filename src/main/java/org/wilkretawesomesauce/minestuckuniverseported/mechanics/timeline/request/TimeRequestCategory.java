package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.request;

/**
 * The 5 categories a player can borrow from their future self via {@code TechFutureRequest}, matching
 * the user/friend design doc's own category list 1:1. Which concrete item a category resolves to is
 * data-driven per {@link com.mraof.minestuck.player.Echeladder} rung - see {@link TimeRequestTierRegistry}.
 */
public enum TimeRequestCategory
{
	WEAPON,
	ARMOR,
	FOOD,
	UTILITY,
	RESOURCE
}
