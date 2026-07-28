package org.wilkretawesomesauce.minestuckuniverseported.mechanics.doom;

/**
 * Doom Mark variants - see the design doc's own "Dead Shuffle" example, ported here as
 * {@link #DEAD_SHUFFLE}. Original design for this project, no 1.12.2 counterpart.
 * <p>
 * Scope note: future mark types (e.g. one that seals a target's Doom on death instead of releasing
 * it, or one that splits a release between multiple casters) are plausible extensions of this enum -
 * not attempted this pass since no tech exists yet to apply any mark at all (this pass is
 * infrastructure only, see {@code DoomMarks}'s own doc comment).
 */
public enum DoomMarkType
{
	/**
	 * The marked target's bound Doom accumulates faster (see {@code DoomData#addDoom}); on death,
	 * instead of releasing into the {@link DoomReleasePool}, it transfers directly to whoever applied
	 * the mark - see {@code DoomReleaseEvents#onDeath} for the actual redirect logic.
	 */
	DEAD_SHUFFLE
}
