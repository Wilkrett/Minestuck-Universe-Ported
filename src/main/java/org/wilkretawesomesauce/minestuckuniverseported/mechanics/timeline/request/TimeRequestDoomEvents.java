package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.request;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The "Doom Points do something" mechanism for the Time Request / Doom System - the per-player
 * counterpart to {@code mechanics.timeline.TimelineDebtEvents} (which stayed disabled/placeholder for the
 * rewind/branch system's own, unrelated DP). Every {@link #DOOM_CHECK_INTERVAL} ticks,
 * each open {@link TimeRequest} accrues DP (faster the more a player has open at once, capped by
 * {@link #DOOM_MULTIPLIER_CAP}), then a weighted-random selection of {@link DoomEventPool}
 * entries affordable within the player's current total DP fires.
 * <p>
 * DP itself is <b>not</b> spent down by firing events - per the design doc, it only disappears when a
 * request actually resolves (see {@code TimeRequestData#removeRequest}). Events are capped at
 * {@link #MAX_EVENTS_PER_CHECK} per check and respect a per-event-id cooldown
 * ({@link #EVENT_COOLDOWN_TICKS}) so a steady high DP total doesn't just repeat the same
 * severe event every check.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class TimeRequestDoomEvents
{
	private static final int MAX_EVENTS_PER_CHECK = 3;
	/** How often (in ticks) each open request accrues DP and a spend check runs. */
	private static final int DOOM_CHECK_INTERVAL = 200;
	/** DP accrued per open request per tick, before the multiplier below. */
	private static final double DOOM_PER_TICK_BASE = 0.02;
	/** Max simultaneous-open-requests multiplier applied to DP accrual. */
	private static final double DOOM_MULTIPLIER_CAP = 4.0;
	/** Minimum ticks between two firings of the same event id for one player. */
	private static final int EVENT_COOLDOWN_TICKS = 400;

	// Transient, in-memory only - doesn't need to survive a restart, same reasoning as
	// AbilitechLoadout's own transient per-slot scratch state.
	private static final Map<UUID, Map<ResourceLocation, Long>> LAST_FIRED = new HashMap<>();

	private TimeRequestDoomEvents()
	{
	}

	@SubscribeEvent
	private static void onLevelTick(LevelTickEvent.Post event)
	{
		if(!(event.getLevel() instanceof ServerLevel level))
			return;
		if(level.getGameTime() % DOOM_CHECK_INTERVAL != 0)
			return;

		for(ServerPlayer player : level.players())
			tickPlayer(level, player);
	}

	private static void tickPlayer(ServerLevel level, ServerPlayer player)
	{
		TimeRequestData data = player.getData(MSUAttachments.TIME_REQUEST_DATA);
		if(data.getOpenRequests().isEmpty())
			return;

		double multiplier = Math.min(data.getOpenRequests().size(), DOOM_MULTIPLIER_CAP);
		double gain = DOOM_PER_TICK_BASE * multiplier * DOOM_CHECK_INTERVAL;
		for(TimeRequest request : data.getOpenRequests())
			request.addDoomPoints(gain);

		spendBudget(level, player, data.getTotalDoomPoints());
	}

	private static void spendBudget(ServerLevel level, ServerPlayer player, double budget)
	{
		int cheapestCost = DoomEventPool.ALL.stream().mapToInt(DoomEvent::cost).min().orElse(Integer.MAX_VALUE);
		if(budget < cheapestCost)
			return;

		Map<ResourceLocation, Long> cooldowns = LAST_FIRED.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
		long now = level.getGameTime();

		List<DoomEvent> candidates = new ArrayList<>(DoomEventPool.ALL);
		Collections.shuffle(candidates);

		int fired = 0;
		for(DoomEvent candidate : candidates)
		{
			if(fired >= MAX_EVENTS_PER_CHECK || budget < cheapestCost)
				break;
			if(candidate.cost() > budget)
				continue;

			Long last = cooldowns.get(candidate.id());
			if(last != null && now - last < EVENT_COOLDOWN_TICKS)
				continue;

			candidate.apply().accept(player);
			cooldowns.put(candidate.id(), now);
			budget -= candidate.cost();
			fired++;
		}
	}
}
