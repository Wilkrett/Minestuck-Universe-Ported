package org.wilkretawesomesauce.minestuckuniverseported.client;

import net.minecraft.client.Minecraft;
import org.wilkretawesomesauce.minestuckuniverseported.client.util.StreakSettings;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side cache of which entities currently have the streak effect toggled on and how it should
 * currently render for each - populated entirely from {@code network.StreakStateSyncPacket}. An entity
 * id is "active" exactly when present in {@link #active}; there's no local random fallback assignment
 * here (unlike a first draft of this feature) because every active id always arrives with an explicit
 * flavour from the sync packet.
 * <p>
 * When an id is toggled off, its last known {@link ActiveState} moves into {@link #fadingOut} instead of
 * being discarded outright - {@link #getGhostRenderState} keeps returning it (with a decaying
 * {@code fadeMultiplier}) for {@link StreakSettings#GHOST_FADE_OUT_TICKS} more ticks so
 * {@code client.render.StreakGhostRenderer} can fade the sprint-ghost afterimages out smoothly instead of
 * cutting them off instantly - per direct user request after the earlier hard on/off read as an abrupt
 * pop. The ribbon trail ({@code client.render.StreakRibbonRenderer}) is untouched by this and still stops
 * instantly on toggle-off - only the ghosts were asked for.
 */
public final class StreakClientState
{
	/**
	 * @param flavour the ribbon texture name (irrelevant while {@code hideTrail} is true)
	 * @param hideTrail skip the ribbon entirely - real gameplay reuse (e.g. Accelerate) wants ghosts only
	 * @param ghostsIgnoreSprint show ghost afterimages for every recent sample, not just sprinting ones
	 * @param ghostTint packed RGB tint multiplied into the ghosts' own fade alpha, {@code 0xFFFFFF} = none
	 */
	public record ActiveState(String flavour, boolean hideTrail, boolean ghostsIgnoreSprint, int ghostTint)
	{
	}

	/** A ghost-fade-eligible state to render right now - {@code fadeMultiplier} is 1 while still actively
	 * toggled on, decaying to 0 over the toggle-off fade-out window. */
	public record GhostRenderState(ActiveState state, float fadeMultiplier)
	{
	}

	private record FadingState(ActiveState state, long fadeStartTick)
	{
	}

	private static final Map<Integer, ActiveState> active = new ConcurrentHashMap<>();
	private static final Map<Integer, FadingState> fadingOut = new ConcurrentHashMap<>();

	private StreakClientState()
	{
	}

	public static void setState(int entityId, String flavourName, boolean hideTrail, boolean ghostsIgnoreSprint, int ghostTint)
	{
		fadingOut.remove(entityId);
		active.put(entityId, new ActiveState(flavourName, hideTrail, ghostsIgnoreSprint, ghostTint));
	}

	public static void clearState(int entityId)
	{
		ActiveState previous = active.remove(entityId);
		if(previous != null)
			fadingOut.put(entityId, new FadingState(previous, currentGameTime()));
	}

	public static String getFlavourFor(int entityId)
	{
		ActiveState state = active.get(entityId);
		return state == null ? null : state.flavour();
	}

	public static ActiveState getState(int entityId)
	{
		return active.get(entityId);
	}

	public static Map<Integer, ActiveState> getActive()
	{
		return active;
	}

	/** True while {@code entityId}'s sample history should still be recorded/kept around - either
	 * actively toggled on, or still within its post-toggle-off ghost fade-out window. Self-prunes expired
	 * {@link #fadingOut} entries as a side effect of checking them. */
	public static boolean isTracked(int entityId)
	{
		if(active.containsKey(entityId))
			return true;

		FadingState fading = fadingOut.get(entityId);
		if(fading == null)
			return false;

		if(currentGameTime() - fading.fadeStartTick() >= StreakSettings.GHOST_FADE_OUT_TICKS)
		{
			fadingOut.remove(entityId);
			return false;
		}

		return true;
	}

	/** Every id {@link StreakTracker} should currently be recording samples for - active ids plus
	 * still-fading-out ones (see {@link #isTracked}). */
	public static Set<Integer> trackedIds()
	{
		Set<Integer> ids = new HashSet<>(active.keySet());
		for(Integer id : fadingOut.keySet())
			if(isTracked(id))
				ids.add(id);
		return ids;
	}

	/** What {@code StreakGhostRenderer} should render right now for {@code entityId}, or {@code null} if
	 * nothing should render at all (never toggled on, or fully faded out). */
	public static GhostRenderState getGhostRenderState(int entityId)
	{
		ActiveState liveState = active.get(entityId);
		if(liveState != null)
			return new GhostRenderState(liveState, 1F);

		FadingState fading = fadingOut.get(entityId);
		if(fading == null)
			return null;

		long elapsed = currentGameTime() - fading.fadeStartTick();
		if(elapsed >= StreakSettings.GHOST_FADE_OUT_TICKS)
		{
			fadingOut.remove(entityId);
			return null;
		}

		float fadeMultiplier = 1F - (float) elapsed / StreakSettings.GHOST_FADE_OUT_TICKS;
		return new GhostRenderState(fading.state(), fadeMultiplier);
	}

	private static long currentGameTime()
	{
		var level = Minecraft.getInstance().level;
		return level == null ? 0L : level.getGameTime();
	}
}
