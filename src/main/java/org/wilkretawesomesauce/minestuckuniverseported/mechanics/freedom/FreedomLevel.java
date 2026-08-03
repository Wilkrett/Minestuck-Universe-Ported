package org.wilkretawesomesauce.minestuckuniverseported.mechanics.freedom;

/**
 * The four behavior brackets a {@link FreedomData#getFreedom()} value falls into - see that class's own
 * doc comment for the whole system. Boundaries follow the user's own design doc as closely as a
 * continuous 0-100 value allows: the doc explicitly calls out 70-100 ("High"), 30-50 ("Low") and 0-20
 * ("Extremely Low"), leaving two unlabeled gaps (21-29 and 51-69). 21-29 is folded into {@link #LOW}
 * (a low-but-not-extreme value reads as more "routine" than "neutral") and 51-69 becomes {@link #NEUTRAL}
 * (closer to the stated 50 baseline than to either named bracket) - a reasonable interpretation, not a
 * literal transcription, since the source doc never actually assigns those two ranges anywhere.
 */
public enum FreedomLevel
{
	EXTREME_LOW,
	LOW,
	NEUTRAL,
	HIGH;

	public static FreedomLevel of(float freedom)
	{
		if(freedom <= 20.0F)
			return EXTREME_LOW;
		if(freedom <= 50.0F)
			return LOW;
		if(freedom < 70.0F)
			return NEUTRAL;
		return HIGH;
	}
}
