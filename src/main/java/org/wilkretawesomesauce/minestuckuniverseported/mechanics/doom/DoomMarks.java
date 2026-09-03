package org.wilkretawesomesauce.minestuckuniverseported.mechanics.doom;

import net.minecraft.world.entity.LivingEntity;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;

import java.util.UUID;

/**
 * Static convenience API for applying {@link DoomMarkType}s - original design for this project, no
 * 1.12.2 counterpart. Infra only: this pass doesn't add any real tech that calls
 * {@link #applyDeadShuffleMark}, it just builds the mark itself (the accumulation-rate multiplier and
 * the death-time redirect, both real - see {@link DoomData#addDoom} and
 * {@code DoomReleaseEvents#onDeath}) as a clean entry point for a future tech to call.
 */
public final class DoomMarks
{
	/** Default Doom-accumulation-rate multiplier applied by {@link #applyDeadShuffleMark}. */
	private static final double MARK_ACCRUAL_MULTIPLIER = 2.0;

	private DoomMarks()
	{
	}

	/**
	 * "Dead Shuffle" - the design doc's own named example. The marked target's Doom accumulates faster
	 * (see {@link DoomData#addDoom}); on death, instead of releasing into the world's
	 * {@link DoomReleasePool}, it transfers directly to the caster (see {@code DoomReleaseEvents#onDeath}).
	 */
	public static void applyDeadShuffleMark(LivingEntity target, UUID casterId)
	{
		target.getData(MSUAttachments.DOOM_DATA).applyMark(casterId, DoomMarkType.DEAD_SHUFFLE, MARK_ACCRUAL_MULTIPLIER);
	}
}
