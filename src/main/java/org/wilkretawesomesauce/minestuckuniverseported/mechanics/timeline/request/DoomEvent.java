package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.request;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

/**
 * One entry in {@link DoomEventPool} - a negative consequence {@link TimeRequestDoomEvents} can "buy"
 * with a player's current total Doom Points, per the design doc's cost-tiered budget system (5/10/20 DP
 * examples). {@link #id} is used purely for the per-event-id cooldown ({@code TimeRequestDoomEvents#EVENT_COOLDOWN_TICKS}),
 * so the same high-severity event can't fire back to back once a player's DP plateaus.
 */
public record DoomEvent(ResourceLocation id, int cost, Consumer<ServerPlayer> apply)
{
}
