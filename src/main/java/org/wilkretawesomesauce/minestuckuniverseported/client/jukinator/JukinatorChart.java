package org.wilkretawesomesauce.minestuckuniverseported.client.jukinator;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.JukeboxSong;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A generated note chart for {@code JukinatorScreen}'s rhythm minigame - real, original design for this
 * project, no 1.12.2 counterpart. "Based on the inserted disc" is read from the disc's own real vanilla
 * {@link JukeboxSong} data: chart length matches {@link JukeboxSong#lengthInTicks()} exactly, and note
 * density is loosely scaled by {@link JukeboxSong#comparatorOutput()} (0-15) so louder/more "energetic"
 * discs feel busier. Exact note timing/lane placement is generated fresh from whatever
 * {@link RandomSource} the caller passes in - {@code JukinatorScreen} is the one that decides whether
 * that source is genuinely random or seeded from the disc's identity, per
 * {@code MSUGameRules#JUKINATOR_RANDOM_CHARTS}; this class itself doesn't know or care which.
 * <p>
 * <b>Jumpstreams</b>: at each step, in addition to the normal single note, there's a chance of a second
 * note landing in a different lane at the exact same {@code hitTimeMs} (a "jump" - the rhythm-game term
 * for two simultaneous notes, requiring both lane keys to be pressed together) - stringing these through
 * an otherwise-regular stream of single notes is what makes it read as a "jumpstream" rather than
 * isolated jumps. No changes needed anywhere else: {@code JukinatorScreen}'s judging is already per-lane
 * and per-note independent, so two notes sharing a timestamp in different lanes just work.
 * <p>
 * <b>Hold notes</b> (DDR "freeze arrows" / beatmania long notes): either note in a step (the lead note or
 * its jump partner) can independently roll into a hold instead of a tap, given a {@link Note#holdDurationMs()}
 * &gt; 0. {@code laneFreeAt} is advanced to the hold's own *tail* time (not its head), so - same as a
 * regular note's own min-gap rule - nothing else is ever scheduled in that lane until
 * {@link #MIN_SAME_LANE_GAP_MS} after the hold actually ends, guaranteeing holds never overlap another
 * note in the same lane (cross-lane overlap, e.g. tapping lane 2 while holding lane 0, is normal and
 * expected, same as real DDR/beatmania). A hold rolled too close to the chart's own end is clamped
 * (shrunk, or dropped back to a plain tap if there's no room left) rather than allowed to run past
 * {@code durationMs}.
 * <p>
 * <b>Never more than 2 simultaneous keys</b>: {@code JukinatorScreen} treats a 3rd simultaneously-held
 * lane key as an automatic miss (anti-mash), so this generator guarantees it never actually asks for one
 * - a step's jump partner is only ever rolled while no hold from an earlier step is still active
 * ({@code holdActiveUntilMs}), and a new hold likewise only ever starts while no other hold is still
 * running. A single jump (2 notes, tap or hold, in the same step) is always exactly 2 keys and is never
 * restricted - only an *older* still-running hold overlapping a *new* step's jump would push the
 * requirement to 3, and that's the one case suppressed here.
 */
public final class JukinatorChart
{
	public static final int LANE_COUNT = 4;

	/** A lead-in before the first note, giving the player a moment to get ready once the chart starts. */
	private static final int LEAD_IN_MS = 1500;
	/** Minimum time between two notes landing in the *same* lane, so every note is actually hittable. */
	private static final int MIN_SAME_LANE_GAP_MS = 220;
	/** Jump chance at comparatorOutput=0 (quiet/short discs) vs =15 (loud/long ones) - busier discs get
	 *  noticeably more jumps, same "based on the disc" scaling the note density itself already uses. */
	private static final float JUMP_CHANCE_MIN = 0.10F;
	private static final float JUMP_CHANCE_MAX = 0.35F;
	/** Same disc-based scaling as jumps, for how often a given note becomes a hold instead of a tap. */
	private static final float HOLD_CHANCE_MIN = 0.08F;
	private static final float HOLD_CHANCE_MAX = 0.22F;
	private static final int HOLD_DURATION_MIN_MS = 300;
	private static final int HOLD_DURATION_MAX_MS = 900;
	private static final int NO_PREVIOUS_HIT = -1_000_000;

	private final List<Note> notes;
	private final int durationMs;

	private JukinatorChart(List<Note> notes, int durationMs)
	{
		this.notes = notes;
		this.durationMs = durationMs;
	}

	public List<Note> notes()
	{
		return notes;
	}

	public int durationMs()
	{
		return durationMs;
	}

	public static JukinatorChart generate(RandomSource random, JukeboxSong song)
	{
		return generate(random, song.lengthInTicks() * 50, song.comparatorOutput());
	}

	/** Fallback used when the loaded disc's {@link JukeboxSong} data couldn't be resolved. */
	public static JukinatorChart generate(RandomSource random, int durationMs)
	{
		return generate(random, durationMs, 8);
	}

	private static JukinatorChart generate(RandomSource random, int durationMs, int comparatorOutput)
	{
		// comparatorOutput ranges 0-15 (vanilla discs); higher -> smaller average gap between notes.
		int averageGapMs = Math.max(250, 900 - comparatorOutput * 40);
		float jumpChance = JUMP_CHANCE_MIN + (JUMP_CHANCE_MAX - JUMP_CHANCE_MIN) * (comparatorOutput / 15.0F);
		float holdChance = HOLD_CHANCE_MIN + (HOLD_CHANCE_MAX - HOLD_CHANCE_MIN) * (comparatorOutput / 15.0F);

		List<Note> notes = new ArrayList<>();
		int[] laneFreeAt = new int[LANE_COUNT];
		Arrays.fill(laneFreeAt, NO_PREVIOUS_HIT);
		// Tracks the furthest-out end time of any currently-running hold, across all lanes - see this
		// class's own doc comment ("Never more than 2 simultaneous keys") for why this is needed.
		int holdActiveUntilMs = NO_PREVIOUS_HIT;

		int time = LEAD_IN_MS;
		while(time < durationMs - 500)
		{
			boolean holdCurrentlyActive = time < holdActiveUntilMs;

			int firstLane = random.nextInt(LANE_COUNT);
			int firstHoldMs = holdCurrentlyActive ? 0 : rollHoldDuration(random, holdChance, durationMs, time);
			tryAddNote(notes, laneFreeAt, firstLane, time, firstHoldMs);
			if(firstHoldMs > 0)
				holdActiveUntilMs = Math.max(holdActiveUntilMs, time + firstHoldMs);

			// A jump (2 notes in one step) is always exactly 2 keys on its own - only suppressed here if
			// an older hold from an earlier step is still running, which would push the total to 3.
			if(!holdCurrentlyActive && random.nextFloat() < jumpChance)
			{
				int secondLane = pickDifferentLane(random, firstLane);
				int secondHoldMs = rollHoldDuration(random, holdChance, durationMs, time);
				tryAddNote(notes, laneFreeAt, secondLane, time, secondHoldMs);
				if(secondHoldMs > 0)
					holdActiveUntilMs = Math.max(holdActiveUntilMs, time + secondHoldMs);
			}

			time += averageGapMs / 2 + random.nextInt(averageGapMs);
		}

		return new JukinatorChart(notes, durationMs);
	}

	/** Returns 0 (a plain tap) unless the roll succeeds *and* there's actually room left before the
	 *  chart's own end for a hold of at least {@link #HOLD_DURATION_MIN_MS} to fit. */
	private static int rollHoldDuration(RandomSource random, float holdChance, int durationMs, int time)
	{
		if(random.nextFloat() >= holdChance)
			return 0;

		int roomLeft = (durationMs - 500) - time;
		int maxHold = Math.min(HOLD_DURATION_MAX_MS, roomLeft);
		if(maxHold < HOLD_DURATION_MIN_MS)
			return 0;

		return HOLD_DURATION_MIN_MS + random.nextInt(maxHold - HOLD_DURATION_MIN_MS + 1);
	}

	private static void tryAddNote(List<Note> notes, int[] laneFreeAt, int lane, int time, int holdDurationMs)
	{
		if(time - laneFreeAt[lane] >= MIN_SAME_LANE_GAP_MS)
		{
			notes.add(new Note(lane, time, holdDurationMs));
			laneFreeAt[lane] = time + holdDurationMs;
		}
	}

	private static int pickDifferentLane(RandomSource random, int excludedLane)
	{
		int lane = random.nextInt(LANE_COUNT - 1);
		return lane >= excludedLane ? lane + 1 : lane;
	}

	/** {@code holdDurationMs == 0} is a plain tap note; {@code > 0} is a hold ("freeze arrow"/long note)
	 *  that must be held down continuously from {@code hitTimeMs} through {@link #endTimeMs()}. */
	public record Note(int lane, int hitTimeMs, int holdDurationMs)
	{
		public boolean isHold()
		{
			return holdDurationMs > 0;
		}

		public int endTimeMs()
		{
			return hitTimeMs + holdDurationMs;
		}
	}
}
