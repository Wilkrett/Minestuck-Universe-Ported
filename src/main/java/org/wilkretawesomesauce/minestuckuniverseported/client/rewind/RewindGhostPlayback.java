package org.wilkretawesomesauce.minestuckuniverseported.client.rewind;

import net.minecraft.client.Minecraft;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.RewindVisuals;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side active-playback state for the rewind-ghost comet effect ({@code network.RewindGhostPacket}) -
 * a deliberately separate, simpler sibling of {@code client.streak.StreakClientState}/
 * {@code client.streak.StreakTracker}, not a reuse of them: those two are built around a <i>rolling live</i>
 * per-tick position history for a currently-active effect (Accelerate's dash, the Streak debug command),
 * with no rotation data and no fixed end time. A rewind ghost is the opposite shape - a fixed, already-known
 * array of real historical samples (position <i>and</i> rotation), played once over a short fixed duration,
 * then gone. Forcing that into the rolling-history system would mean either faking per-tick "live" samples
 * for something that was never live, or bolting rotation onto a record that has no use for it anywhere else
 * - a dedicated pair (this class + {@code client.render.RewindGhostRenderer}) is simpler than either.
 * <p>
 * {@link Playback#durationTicks} arrives per-packet from the server ({@code TimeLoopZone#REVERSE_TICKS_EFFECTIVE} at
 * cast time) rather than being a fixed client-side constant, so the comet's own sweep always matches
 * however long the real entity is actually spending walking the same path backward - see
 * {@code mechanics.timeline.RewindVisuals}'s own "Real correction (2/2)" doc note for why that match
 * matters now that the real entity moves too, not just this cosmetic layer.
 */
public final class RewindGhostPlayback
{
	/** How many trailing doppelganger copies render simultaneously each frame, spaced along the sweep with tapering alpha - the "comet tail" read, same taper idiom {@code client.render.StreakGhostRenderer}'s own sprint-ghost trail already uses. */
	public static final int TRAIL_LENGTH = 4;

	public static final int TINT = 0xAAAAAA;

	/** Real, user-requested correction: the leading ghost used to reach close to full opacity, tinted gray - reading as a solid recolored duplicate rather than a translucent ghost. 0.1 = 10%, the actual read this effect wants, with the tint now a secondary cue rather than the primary one. */
	public static final float MAX_ALPHA = 0.1F;

	public record Playback(List<RewindVisuals.PathPoint> path, long startTick, int durationTicks)
	{
	}

	private static final Map<Integer, Playback> active = new ConcurrentHashMap<>();

	private RewindGhostPlayback()
	{
	}

	public static void start(int entityId, List<RewindVisuals.PathPoint> path, int durationTicks)
	{
		if(path.size() < 2 || durationTicks <= 0)
			return;
		active.put(entityId, new Playback(path, currentTick(), durationTicks));
	}

	public static Map<Integer, Playback> getActive()
	{
		return active;
	}

	/** Drops any playback whose own duration has already elapsed - called once per render frame by {@code client.render.RewindGhostRenderer} before iterating, so a finished comet doesn't linger as a static last frame. */
	public static void pruneFinished()
	{
		long now = currentTick();
		active.values().removeIf(playback -> now - playback.startTick() >= playback.durationTicks());
	}

	private static long currentTick()
	{
		var level = Minecraft.getInstance().level;
		return level == null ? 0L : level.getGameTime();
	}
}
