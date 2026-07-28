package org.wilkretawesomesauce.minestuckuniverseported.client.util;

/**
 * Developer-only tuning constants for the streak ribbon + sprint-ghost effect (ported from iChun's
 * Streak, see {@code streak.StreakFlavours}'s own doc comment). Deliberately <b>not</b> wired to
 * {@code Config.java} or any in-game GUI - the whole feature is a creative-only debug/demo command
 * (see {@code command.StreakCommand}), so these are edited directly in source rather than exposed as
 * player-facing options. Lives under {@code client} (not a common-code {@code streak.util} package)
 * because {@link StreakRibbonUtils}/{@link StreakGhostUtils} alongside it reference client-only
 * rendering types - keeping all three together here avoids the exact "client-only class referenced
 * from common code" dedicated-server crash pattern already documented elsewhere in this project (see
 * this project's CLAUDE.md, {@code AbilitechnosynthBlock}/{@code StrifeCardItem}).
 */
public final class StreakSettings
{
	/** How many ticks of ribbon trail are kept/rendered behind a tracked entity. Mirrors the original's {@code streakTime} default. */
	public static final int TRAIL_LENGTH_TICKS = 100;

	/** Base opacity (0..1) the ribbon is rendered at, before the per-segment fade-in/fade-out ramp. */
	public static final float TRAIL_OPACITY = 1.0F;

	/** How many sprint-ghost afterimage copies are drawn along the recent path while sprinting. Mirrors the original's {@code sprintTrail} default. */
	public static final int SPRINT_GHOST_COUNT = 6;

	/** Tick spacing between consecutive sprint-ghost copies along the recorded path. */
	public static final int SPRINT_GHOST_SPACING_TICKS = 4;

	/** Whether the local player's own ribbon renders while they're in first person. */
	public static final boolean RENDER_IN_FIRST_PERSON = false;

	/** How many ticks the sprint-ghost afterimages take to fade out to nothing after the streak effect is
	 * toggled off, instead of vanishing the instant it is - consumed by
	 * {@code client.StreakClientState#getGhostRenderState}, see that class's own doc comment. */
	public static final int GHOST_FADE_OUT_TICKS = 15;

	private StreakSettings()
	{
	}
}
