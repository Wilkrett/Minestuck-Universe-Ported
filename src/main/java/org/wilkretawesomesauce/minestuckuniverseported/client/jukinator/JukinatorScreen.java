package org.wilkretawesomesauce.minestuckuniverseported.client.jukinator;

import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import org.wilkretawesomesauce.minestuckuniverseported.client.MSUJukinatorKeyMappings;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The Jukinator-3000's real 4-lane rhythm minigame - original design for this project, no 1.12.2
 * counterpart. Opened via {@code network.OpenJukinatorPacket} (see that class's own doc comment for why a
 * packet, not the zero-network {@code AbilitechnosynthBlock.open()} pattern, is used here: the chart mode
 * is resolved from a server-only gamerule and has to be handed down explicitly).
 * <p>
 * Once open, this runs entirely client-side and locally - the chart itself was already decided by the
 * time {@link #create} is called, so there's no further need for server involvement in the actual
 * scrolling/judging loop, timed off the client's own wall clock ({@link Util#getMillis()}) rather than
 * server ticks.
 * <p>
 * <b>Known, stated gaps</b>: no texture/model exists for the Jukinator item itself yet, and this screen
 * deliberately draws plain filled rectangles rather than inventing note/lane art to go with it - see
 * {@code item.JukinatorItem}'s own doc comment. Score is session-only, no persistence or reward hook -
 * not requested, purely a for-fun minigame.
 * <p>
 * <b>Hold notes, DDR/beatmania-style</b>: a note's head must be pressed within the normal hit window like
 * any tap, but a {@link JukinatorChart.Note#isHold()} note then transitions to {@link #STATE_HOLDING}
 * instead of resolving immediately - the player must keep the lane key down continuously until the hold's
 * own {@code endTimeMs} (a small {@link #HOLD_RELEASE_TOLERANCE_MS} grace window on release timing,
 * matching typical rhythm-game leniency); releasing early breaks it (combo reset, no completion bonus),
 * holding through to the tail (or slightly past it) completes it for bonus score. Rendered the same way
 * DDR's own "freeze arrows" read: once grabbed, the head visually pins to the hit line while the tail
 * keeps approaching, so the remaining bar visibly shrinks as the required hold time runs out.
 * <p>
 * <b>Real bug found and fixed while building this</b>: {@code KeyboardHandler}'s real source (read
 * directly, not guessed) calls {@code Screen#keyPressed} for both {@code GLFW_PRESS} <i>and</i>
 * {@code GLFW_REPEAT} - so a naive {@code keyPressed} that re-judges on every call would spam repeated
 * miss/combo-reset judgements for as long as a lane key is held down with no note nearby (OS key-repeat
 * fires it dozens of times a second), and would make holding an already-grabbed hold's key down
 * indistinguishable from spamming fresh presses into it. {@link #laneKeyDown} tracks each lane's actual
 * up/down edge so judging only happens once per real press - necessary infrastructure for hold notes to
 * work at all, and also a real latent bug fix for the plain tap-note case that predates this feature.
 */
public class JukinatorScreen extends Screen
{
	private static final int LANE_COUNT = JukinatorChart.LANE_COUNT;
	private static final int LANE_WIDTH = 40;
	private static final int LANE_GAP = 10;
	private static final int NOTE_HEIGHT = 10;
	private static final int LANE_TOP_MARGIN = 40;
	private static final int HIT_LINE_BOTTOM_MARGIN = 50;

	/** How long (ms) a note takes to travel from the top of the lane down to the hit line at 1.0x scroll
	 *  speed - see {@link #scrollSpeed} for how this actually gets scaled at render time. */
	private static final int APPROACH_MS = 1500;
	private static final int HIT_WINDOW_GOOD_MS = 150;
	private static final int HIT_WINDOW_PERFECT_MS = 75;
	/** How early a hold's key may be released and still count as a full completion. */
	private static final int HOLD_RELEASE_TOLERANCE_MS = 120;
	private static final int HOLD_COMPLETE_BONUS = 150;

	private static final float MIN_SCROLL_SPEED = 0.5F;
	private static final float MAX_SCROLL_SPEED = 3.0F;
	private static final float SCROLL_SPEED_STEP = 0.25F;

	private static final int STATE_PENDING = 0;
	private static final int STATE_HIT = 1;
	private static final int STATE_MISSED = 2;
	/** The note's head was grabbed and it's a hold - key must stay down until its tail arrives. */
	private static final int STATE_HOLDING = 3;

	/** Real DDR/beatmania "speed mod" - purely visual, never touches judging (that's always real elapsed
	 *  time vs. a note's own {@code hitTimeMs}/{@code endTimeMs}, completely independent of this). Higher
	 *  values make notes cross the lane faster, spreading them out more on screen for readability on dense
	 *  charts; lower values give more time to react on sparse ones. Static so it persists across screen
	 *  instances (closing and reopening the minigame keeps whatever speed you last set), same real-game
	 *  convention as remembering a player's speed mod between songs - deliberately not saved to disk
	 *  though, same "session-only" scope as this screen's score already has.
	 *  Adjusted via {@link #mouseScrolled}. */
	private static float scrollSpeed = 1.0F;

	private final ItemStack disc;
	private final boolean seededChart;

	private JukinatorChart chart;
	private int[] noteState;
	/** Real per-lane up/down edge tracking - see this class's own doc comment for why this exists. */
	private boolean[] laneKeyDown;
	/** Index into {@code chart.notes()} of the hold currently being held in each lane, or -1. */
	private int[] activeHoldNoteIndex;
	private long startTimeMs;
	private SoundInstance soundInstance;

	private int score;
	private int combo;
	private Component lastJudgement = Component.empty();
	private long lastJudgementTimeMs;

	private JukinatorScreen(ItemStack disc, boolean seededChart)
	{
		super(Component.translatable("gui.jukinator.title"));
		this.disc = disc;
		this.seededChart = seededChart;
	}

	/** Entry point for {@code network.OpenJukinatorPacket#execute()} - already client-only code by the
	 *  time it runs, so there's no dedicated-server class-loading concern to route around here. */
	public static JukinatorScreen create(ItemStack disc, boolean seededChart)
	{
		return new JukinatorScreen(disc, seededChart);
	}

	@Override
	protected void init()
	{
		super.init();

		Optional<Holder<JukeboxSong>> songHolder =
				JukeboxSong.fromStack(Minecraft.getInstance().level.registryAccess(), disc);

		if(songHolder.isPresent())
		{
			JukeboxSong song = songHolder.get().value();
			RandomSource random = seededChart
					? RandomSource.create(songHolder.get().unwrapKey().map(key -> key.location().hashCode()).orElse(disc.getItem().hashCode()))
					: RandomSource.create();

			chart = JukinatorChart.generate(random, song);
			soundInstance = SimpleSoundInstance.forUI(song.soundEvent(), 1.0F);
			Minecraft.getInstance().getSoundManager().play(soundInstance);
		}
		else
		{
			// The disc lost its JukeboxPlayable data somehow (e.g. a modded disc removed after being
			// loaded) - fall back to a fixed-length chart with no audio rather than crashing.
			RandomSource random = seededChart ? RandomSource.create(disc.getItem().hashCode()) : RandomSource.create();
			chart = JukinatorChart.generate(random, 30_000);
			soundInstance = null;
		}

		noteState = new int[chart.notes().size()];
		laneKeyDown = new boolean[LANE_COUNT];
		activeHoldNoteIndex = new int[LANE_COUNT];
		Arrays.fill(activeHoldNoteIndex, -1);
		startTimeMs = Util.getMillis();
		score = 0;
		combo = 0;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
	{
		guiGraphics.fill(0, 0, width, height, 0xC0101010);
		super.render(guiGraphics, mouseX, mouseY, partialTick);

		long elapsed = Util.getMillis() - startTimeMs;

		guiGraphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
		guiGraphics.drawCenteredString(font, disc.getHoverName(), width / 2, 24, 0xAAAAAA);

		int hitLineY = height - HIT_LINE_BOTTOM_MARGIN;
		int totalWidth = LANE_COUNT * LANE_WIDTH + (LANE_COUNT - 1) * LANE_GAP;
		int startX = width / 2 - totalWidth / 2;

		for(int lane = 0; lane < LANE_COUNT; lane++)
		{
			int laneX = startX + lane * (LANE_WIDTH + LANE_GAP);
			guiGraphics.fill(laneX, LANE_TOP_MARGIN, laneX + LANE_WIDTH, hitLineY + NOTE_HEIGHT, 0x40FFFFFF);
			guiGraphics.fill(laneX, hitLineY, laneX + LANE_WIDTH, hitLineY + 2, 0xFFFFFFFF);
		}

		List<JukinatorChart.Note> notes = chart.notes();
		for(int i = 0; i < notes.size(); i++)
		{
			JukinatorChart.Note note = notes.get(i);

			if(noteState[i] == STATE_PENDING && elapsed > note.hitTimeMs() + HIT_WINDOW_GOOD_MS)
			{
				noteState[i] = STATE_MISSED;
				combo = 0;
			}

			if(noteState[i] == STATE_HOLDING && elapsed >= note.endTimeMs())
				completeHold(i, note);

			if(noteState[i] != STATE_PENDING && noteState[i] != STATE_HOLDING)
				continue;

			int laneX = startX + note.lane() * (LANE_WIDTH + LANE_GAP);
			int color = laneColor(note.lane());

			if(note.isHold())
			{
				renderHoldNote(guiGraphics, note, noteState[i], elapsed, laneX, hitLineY, color);
			}
			else
			{
				long timeUntilHit = note.hitTimeMs() - elapsed;
				if(timeUntilHit > approachMs() || timeUntilHit < -HIT_WINDOW_GOOD_MS)
					continue;

				int noteY = noteY(elapsed, note.hitTimeMs(), hitLineY);
				guiGraphics.fill(laneX, noteY, laneX + LANE_WIDTH, noteY + NOTE_HEIGHT, color);
			}
		}

		guiGraphics.drawCenteredString(font, Component.translatable("gui.jukinator.score", score, combo), width / 2, hitLineY + 20, 0xFFFFFF);
		guiGraphics.drawString(font, Component.translatable("gui.jukinator.speed", String.format("%.2f", scrollSpeed)), 6, height - 14, 0xAAAAAA);

		if(Util.getMillis() - lastJudgementTimeMs < 500)
			guiGraphics.drawCenteredString(font, lastJudgement, width / 2, hitLineY - 20, 0xFFFF55);

		if(elapsed > chart.durationMs() + 500)
			guiGraphics.drawCenteredString(font, Component.translatable("gui.jukinator.complete", score), width / 2, height / 2, 0xFFFFFF);
	}

	private static int laneColor(int lane)
	{
		return switch(lane)
		{
			case 0 -> 0xFFE63946;
			case 1 -> 0xFFF1C40F;
			case 2 -> 0xFF2ECC71;
			default -> 0xFF3498DB;
		};
	}

	/** The real, current approach time after applying {@link #scrollSpeed} - notes become visible this
	 *  many ms before their own {@code hitTimeMs}/{@code endTimeMs} and cross the whole lane in that span.
	 *  Purely a rendering value; nothing judging-related ever reads this. */
	private float approachMs()
	{
		return APPROACH_MS / scrollSpeed;
	}

	private int noteY(long elapsed, int timeMs, int hitLineY)
	{
		long timeUntil = timeMs - elapsed;
		float progress = 1.0F - (float) timeUntil / approachMs();
		return (int) (LANE_TOP_MARGIN + progress * (hitLineY - LANE_TOP_MARGIN));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
	{
		if(scrollY != 0)
		{
			scrollSpeed = Mth.clamp(scrollSpeed + (float) scrollY * SCROLL_SPEED_STEP, MIN_SCROLL_SPEED, MAX_SCROLL_SPEED);
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	/** DDR-style "freeze arrow" rendering: while {@link #STATE_HOLDING}, the head is pinned to the hit
	 *  line (it's already been grabbed and isn't going anywhere) and only the tail keeps approaching, so
	 *  the visible bar between them visibly shrinks as the required hold time runs out. While still
	 *  {@link #STATE_PENDING} (not yet grabbed), head and tail scroll down together like a rigid bar. */
	private void renderHoldNote(GuiGraphics guiGraphics, JukinatorChart.Note note, int state, long elapsed, int laneX, int hitLineY, int color)
	{
		int headY = state == STATE_HOLDING ? hitLineY : Math.min(noteY(elapsed, note.hitTimeMs(), hitLineY), hitLineY);
		int tailY = Math.min(noteY(elapsed, note.endTimeMs(), hitLineY), headY);

		int bodyAlpha = state == STATE_HOLDING ? 0xB0 : 0x70;
		int bodyColor = (bodyAlpha << 24) | (color & 0xFFFFFF);
		guiGraphics.fill(laneX, tailY, laneX + LANE_WIDTH, headY, bodyColor);
		guiGraphics.fill(laneX, headY, laneX + LANE_WIDTH, headY + NOTE_HEIGHT, color);
	}

	/** A hold whose key stayed down all the way to (or past) its tail - completes it for bonus score.
	 *  Called both from the per-frame {@link #render} sweep (the common case: the player kept holding
	 *  and the tail simply arrived) and from {@link #keyReleased} (the player released right at/after the
	 *  tail rather than holding past it - same outcome either way). Safe to call twice for the same note
	 *  since it's guarded by the {@code STATE_HOLDING} check at each call site. No dedicated popup text -
	 *  the score bump and combo continuing are feedback enough. */
	private void completeHold(int index, JukinatorChart.Note note)
	{
		noteState[index] = STATE_HIT;
		combo++;
		score += HOLD_COMPLETE_BONUS + combo;
		if(activeHoldNoteIndex[note.lane()] == index)
			activeHoldNoteIndex[note.lane()] = -1;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers)
	{
		if(chart != null)
		{
			for(int lane = 0; lane < LANE_COUNT; lane++)
			{
				if(laneKey(lane).matches(keyCode, scanCode))
				{
					// GLFW_REPEAT also calls keyPressed while a key is held (confirmed in
					// KeyboardHandler's real source) - only judge on the genuine up->down edge, both to
					// avoid spamming miss judgements against thin air and because a hold note's key stays
					// physically down for its whole duration.
					if(!laneKeyDown[lane])
					{
						laneKeyDown[lane] = true;
						// Anti-mash: charts here never require more than 2 simultaneous keys (a single
						// note, or a jump/jump+hold pair - see JukinatorChart's own doc comment for the
						// generation-side guarantee), so a 3rd simultaneously-held key can only ever be
						// cheesing, not a legitimate chart requirement. Penalize it as a miss instead of
						// letting it grab a note.
						if(keysDown() > 2)
							registerMashMiss();
						else
							judgeLane(lane);
					}
					return true;
				}
			}
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers)
	{
		if(chart != null)
		{
			for(int lane = 0; lane < LANE_COUNT; lane++)
			{
				if(laneKey(lane).matches(keyCode, scanCode))
				{
					laneKeyDown[lane] = false;
					releaseHold(lane);
					return true;
				}
			}
		}

		return super.keyReleased(keyCode, scanCode, modifiers);
	}

	private void releaseHold(int lane)
	{
		int index = activeHoldNoteIndex[lane];
		if(index < 0)
			return;

		activeHoldNoteIndex[lane] = -1;
		if(noteState[index] != STATE_HOLDING)
			return;

		JukinatorChart.Note note = chart.notes().get(index);
		long elapsed = Util.getMillis() - startTimeMs;

		if(elapsed >= note.endTimeMs() - HOLD_RELEASE_TOLERANCE_MS)
		{
			// Same as completeHold() - no popup text, the score bump speaks for itself.
			noteState[index] = STATE_HIT;
			combo++;
			score += HOLD_COMPLETE_BONUS + combo;
		}
		else
		{
			noteState[index] = STATE_MISSED;
			combo = 0;
			lastJudgement = Component.translatable("gui.jukinator.released");
			lastJudgementTimeMs = Util.getMillis();
		}
	}

	private static KeyMapping laneKey(int lane)
	{
		return switch(lane)
		{
			case 0 -> MSUJukinatorKeyMappings.lane0;
			case 1 -> MSUJukinatorKeyMappings.lane1;
			case 2 -> MSUJukinatorKeyMappings.lane2;
			default -> MSUJukinatorKeyMappings.lane3;
		};
	}

	private int keysDown()
	{
		int count = 0;
		for(boolean down : laneKeyDown)
			if(down)
				count++;
		return count;
	}

	private void registerMashMiss()
	{
		combo = 0;
		lastJudgement = Component.translatable("gui.jukinator.miss");
		lastJudgementTimeMs = Util.getMillis();
	}

	private void judgeLane(int lane)
	{
		long elapsed = Util.getMillis() - startTimeMs;
		List<JukinatorChart.Note> notes = chart.notes();

		int bestIndex = -1;
		long bestDelta = Long.MAX_VALUE;
		for(int i = 0; i < notes.size(); i++)
		{
			if(noteState[i] != STATE_PENDING || notes.get(i).lane() != lane)
				continue;

			long delta = Math.abs(elapsed - notes.get(i).hitTimeMs());
			if(delta < bestDelta)
			{
				bestDelta = delta;
				bestIndex = i;
			}
		}

		lastJudgementTimeMs = Util.getMillis();
		if(bestIndex >= 0 && bestDelta <= HIT_WINDOW_GOOD_MS)
		{
			JukinatorChart.Note note = notes.get(bestIndex);
			boolean perfect = bestDelta <= HIT_WINDOW_PERFECT_MS;
			combo++;
			score += (perfect ? 100 : 50) + combo;
			lastJudgement = Component.translatable(perfect ? "gui.jukinator.perfect" : "gui.jukinator.good");

			if(note.isHold())
			{
				noteState[bestIndex] = STATE_HOLDING;
				activeHoldNoteIndex[lane] = bestIndex;
			}
			else
			{
				noteState[bestIndex] = STATE_HIT;
			}
		}
		else
		{
			combo = 0;
			lastJudgement = Component.translatable("gui.jukinator.miss");
		}
	}

	@Override
	public void onClose()
	{
		if(soundInstance != null)
			Minecraft.getInstance().getSoundManager().stop(soundInstance);
		super.onClose();
	}

	@Override
	public boolean isPauseScreen()
	{
		return false;
	}
}
