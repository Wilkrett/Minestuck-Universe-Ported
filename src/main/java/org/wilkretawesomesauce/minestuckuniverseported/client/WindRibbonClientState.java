package org.wilkretawesomesauce.minestuckuniverseported.client;

import net.minecraft.client.Minecraft;
import org.wilkretawesomesauce.minestuckuniverseported.client.streak.StreakClientState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side cache of active Breath wind ribbons (caster entity id -&gt; target entity id + style),
 * populated entirely from {@code network.WindRibbonSyncPacket}. Consumed by
 * {@code client.render.WindRibbonRenderer} every frame - same "packet-fed cache, separate renderer reads
 * it" split this project already uses for {@code TetherBondClientState}/{@code TetherBondRenderer}.
 * <p>
 * <b>Live/fading-out split, a direct later user request</b> ("instead of instantly making the trail
 * disappear it should slowly fade out"): mirrors {@link StreakClientState}'s own {@code active}/
 * {@code fadingOut} map split (built for its ghost afterimages, not its ribbon trail - this is the first
 * reuse of that exact pattern for a different feature). {@link #clearRibbon} used to just remove the entry
 * outright, so the renderer stopped seeing it the very next frame; now it moves the last known
 * {@link Ribbon} into {@link #fadingRibbons} with a start tick instead, and {@link #getRenderRibbons()}
 * (the renderer's only read path now) hands back a decaying {@code fadeMultiplier} for
 * {@link #FADE_OUT_TICKS} more ticks before self-pruning it, the same "self-prunes on access" shape as
 * {@code StreakClientState#getGhostRenderState}.
 * <p>
 * <b>{@code spawnTick}, a direct user correction</b> ("this is only random on game launch... should be
 * done when spawning in the wind engine"): {@code client.render.WindRibbonRenderer}'s per-strand random
 * vertical offset used to seed off caster id alone, which is stable for that caster's whole session
 * rather than re-randomizing per cast. {@link Ribbon} now carries the real game tick the ribbon actually
 * started on, so the renderer can seed off <i>that</i> instead - {@link #setRibbon} only assigns a fresh
 * one when there wasn't already a live ribbon for this caster+target (a brand new cast), and preserves the
 * existing one across the periodic resyncs {@code TechBreathLiberate}/{@code TechBreathConstrain} already
 * send while the same target stays held, so the random offset doesn't drift mid-cast either.
 */
public final class WindRibbonClientState
{
	private static final int FADE_OUT_TICKS = 20;

	private static final Map<Integer, Ribbon> ribbons = new ConcurrentHashMap<>();
	private static final Map<Integer, FadingRibbon> fadingRibbons = new ConcurrentHashMap<>();

	private WindRibbonClientState()
	{
	}

	public static void setRibbon(int casterId, int targetId, boolean inward, float intensity)
	{
		fadingRibbons.remove(casterId);
		Ribbon existing = ribbons.get(casterId);
		long spawnTick = (existing != null && existing.targetId() == targetId) ? existing.spawnTick() : currentGameTime();
		ribbons.put(casterId, new Ribbon(targetId, inward, intensity, spawnTick));
	}

	public static void clearRibbon(int casterId)
	{
		Ribbon previous = ribbons.remove(casterId);
		if(previous != null)
			fadingRibbons.put(casterId, new FadingRibbon(previous, currentGameTime()));
	}

	/**
	 * What {@code WindRibbonRenderer} should draw right now - live ribbons (a full-strength
	 * {@code fadeMultiplier} of 1) plus any still-fading-out ones (a decaying multiplier, self-pruned once
	 * their {@link #FADE_OUT_TICKS} window elapses). Builds a fresh combined map per call - call once per
	 * frame and reuse the result across every render pass, the same way the single old {@code getRibbons()}
	 * call used to be reused.
	 */
	public static Map<Integer, RenderRibbon> getRenderRibbons()
	{
		Map<Integer, RenderRibbon> result = new ConcurrentHashMap<>();
		for(Map.Entry<Integer, Ribbon> entry : ribbons.entrySet())
		{
			Ribbon ribbon = entry.getValue();
			result.put(entry.getKey(), new RenderRibbon(ribbon.targetId(), ribbon.inward(), ribbon.intensity(), 1F, ribbon.spawnTick()));
		}

		long now = currentGameTime();
		for(Map.Entry<Integer, FadingRibbon> entry : fadingRibbons.entrySet())
		{
			if(result.containsKey(entry.getKey()))
				continue;

			long elapsed = now - entry.getValue().fadeStartTick();
			if(elapsed >= FADE_OUT_TICKS)
			{
				fadingRibbons.remove(entry.getKey());
				continue;
			}

			Ribbon ribbon = entry.getValue().ribbon();
			float fadeMultiplier = 1F - (float) elapsed / FADE_OUT_TICKS;
			result.put(entry.getKey(), new RenderRibbon(ribbon.targetId(), ribbon.inward(), ribbon.intensity(), fadeMultiplier, ribbon.spawnTick()));
		}

		return result;
	}

	private static long currentGameTime()
	{
		var level = Minecraft.getInstance().level;
		return level == null ? 0L : level.getGameTime();
	}

	private record Ribbon(int targetId, boolean inward, float intensity, long spawnTick)
	{
	}

	private record FadingRibbon(Ribbon ribbon, long fadeStartTick)
	{
	}

	/** A render-ready snapshot of one ribbon - see this class's own doc comment for {@code fadeMultiplier}/{@code spawnTick}. */
	public record RenderRibbon(int targetId, boolean inward, float intensity, float fadeMultiplier, long spawnTick)
	{
	}
}
