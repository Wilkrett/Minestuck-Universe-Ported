package org.wilkretawesomesauce.minestuckuniverseported.mechanics.doom;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * The universal Doom value's manipulation API - see {@link DoomData}'s own doc comment for what
 * Doom actually represents. Original design for this project - no MinestuckUniverse (1.12.2)
 * counterpart exists to port, unlike almost everything else in this project.
 * <p>
 * Every method here corresponds to one of the manipulation verbs the design doc lists (accumulate,
 * store, transfer, harvest, consume, redirect, destroy, create, seal, release) - "store" has no
 * dedicated method since it's just the bound value itself, and "harvest" lives on
 * {@link DoomReleasePool} instead (it moves Doom from the pool into a living entity, not between two
 * {@code IDoomData} instances).
 */
public interface IDoomData
{
	double getDoom();

	/** Accumulate/create. Applies {@link #getMarkAccrualMultiplier()} to positive amounts if marked. No-op while {@link #isSealed()}. */
	void addDoom(double amount);

	/**
	 * Same as {@link #addDoom(double)} but never applies the mark multiplier - used internally by
	 * {@link #transferTo} and by the Dead Shuffle death-redirect (see {@code DoomReleaseEvents}) so a
	 * marked recipient doesn't get their own mark multiplier applied to Doom they're merely receiving
	 * from someone else. Still no-op while {@link #isSealed()}.
	 */
	void addDoomRaw(double amount);

	/** Consume/destroy. No-op while {@link #isSealed()}. */
	void removeDoom(double amount);

	/** Absolute create/destroy - debug/tech use. No-op while {@link #isSealed()}. */
	void setDoom(double amount);

	/** Transfer - moves up to {@code amount} from this instance to {@code other}, returns the actual amount moved. */
	double transferTo(IDoomData other, double amount);

	/** Seal/release verb pair - while sealed, every mutating method above is a no-op (a ward against manipulation while alive, not a death-survival exploit - see {@code DoomReleaseEvents} for what happens to sealed Doom on death). */
	boolean isSealed();

	void setSealed(boolean sealed);

	boolean isMarked();

	@Nullable
	DoomMarkType getMarkType();

	@Nullable
	UUID getMarkCasterId();

	double getMarkAccrualMultiplier();

	/** Redirect setup - see {@code DoomMarks#applyDeadShuffleMark} for the real entry point future techs should call. */
	void applyMark(UUID casterId, DoomMarkType type, double accrualMultiplier);

	void clearMark();
}
