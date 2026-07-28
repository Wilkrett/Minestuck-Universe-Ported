package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline;

import net.commoble.infiniverse.api.InfiniverseAPI;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

/**
 * Makes empty parallel timeline branches go dormant (unregistered, not ticking) - see
 * {@link TimelineBranch}'s doc comment and this feature's design in {@code CLAUDE.md}. Alpha (the
 * Overworld) never goes dormant - Infiniverse itself already refuses to unregister it.
 * <p>
 * Two events matter here: {@link PlayerEvent.PlayerChangedDimensionEvent} (confirmed, by reading
 * {@code ServerPlayer#changeDimension}'s source, to fire as the very last step - the departing player
 * is already removed from the old level's player list by the time this fires) and
 * {@link PlayerEvent.PlayerLoggedOutEvent} (confirmed, by reading {@code PlayerList#remove}, to fire
 * <i>before</i> the player is actually removed from their level's player list - the opposite order).
 * Both handlers share one dormancy check that explicitly excludes the player in question from the
 * "is anyone still here" count, which is correct regardless of which firing order applies.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class BranchLifecycleEvents
{
	private BranchLifecycleEvents()
	{
	}

	@SubscribeEvent
	private static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event)
	{
		if(event.getEntity() instanceof ServerPlayer player)
			checkDormancy(player.getServer(), event.getFrom(), player);
	}

	@SubscribeEvent
	private static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
	{
		if(event.getEntity() instanceof ServerPlayer player)
			checkDormancy(player.getServer(), player.level().dimension(), player);
	}

	private static void checkDormancy(MinecraftServer server, ResourceKey<Level> dimensionKey, ServerPlayer excluding)
	{
		if(dimensionKey == Level.OVERWORLD)
			return;

		ServerLevel level = server.getLevel(dimensionKey);
		if(level == null)
			return;

		TimelineBranchRegistry registry = server.overworld().getData(MSUAttachments.TIMELINE_BRANCHES);
		TimelineBranch branch = registry.findByDimension(dimensionKey);
		if(branch == null || !branch.isRegistered())
			return;

		for(ServerPlayer remaining : level.players())
			if(remaining != excluding)
				return;

		InfiniverseAPI.get().markDimensionForUnregistration(server, dimensionKey);
		branch.setRegistered(false);
		branch.setLastVisitedGameTime(server.overworld().getGameTime());
	}
}
