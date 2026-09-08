package org.wilkretawesomesauce.minestuckuniverseported.mechanics.soul;

import javax.annotation.Nullable;
import java.util.UUID;

/** See {@link PlayerSoul}'s own doc comment for the whole system this backs. */
public interface ISoulData
{
	@Nullable
	UUID getCurrentHostUUID();

	void setCurrentHostUUID(@Nullable UUID id);

	/**
	 * Whether the Soul is currently hostless, freely "flying" as itself - see {@link PlayerSoul}'s own doc
	 * comment for why this is a plain boolean and not a separate tracked entity's UUID (a real, direct
	 * user correction from an earlier version of this system that used one).
	 */
	boolean isFreeSpirit();

	void setFreeSpirit(boolean freeSpirit);

	/** {@code Abilities#mayfly}'s own value from just before {@link #isFreeSpirit()} last granted it - so ending free-spirit restores exactly what the player had (e.g. a Creative player's own legitimate flight), not a hardcoded false. Meaningless while {@link #isFreeSpirit()} is false. */
	boolean getPreviousMayFly();

	void setPreviousMayFly(boolean previousMayFly);

	/** True whenever either relationship is set - the soul is not in plain, unremarkable control of its own real body. */
	boolean isProjecting();
}
