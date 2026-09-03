package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

import java.util.List;

/**
 * Periodically deletes parallel timeline branches that have sat dormant (see {@link BranchLifecycleEvents})
 * for longer than {@link #IDLE_PRUNE_TICKS}, via {@link BranchDeleter}.
 * <p>
 * <b>Deletion policy: recursive, not reparenting</b> - a doomed branch collapsing takes its own forks
 * with it, rather than promoting orphans nobody asked to keep (see {@code CLAUDE.md}/the design plan
 * for this feature for the full reasoning).
 * <p>
 * <b>Protecting active descendants:</b> a branch is only ever swept if its <i>entire</i> subtree is
 * simultaneously dormant and idle past the threshold - a still-visited descendant (or one whose own
 * idle timer hasn't elapsed) blocks deletion of its whole ancestor chain, not just of itself. This is
 * why {@link #subtreeFullyQualifies} recurses down from each candidate before anything gets deleted,
 * and why the outer loop skips any branch whose parent already qualifies (its ancestor's own pass -
 * the topmost qualifying branch in the chain - is the one that actually triggers the recursive delete,
 * so a chain is never deleted piecemeal from multiple starting points in the same sweep).
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class BranchPruneSweep
{
	/** How long (in ticks) a dormant, unregistered branch's whole subtree must sit idle before it's pruned. Default 72000 = 1 hour. */
	private static final int IDLE_PRUNE_TICKS = 72000;
	/** How often (in ticks) a sweep pass runs. Default 1200 = 1 minute. */
	private static final int PRUNE_SWEEP_INTERVAL_TICKS = 1200;

	private BranchPruneSweep()
	{
	}

	@SubscribeEvent
	private static void onServerTick(ServerTickEvent.Post event)
	{
		MinecraftServer server = event.getServer();
		if(server.overworld().getGameTime() % PRUNE_SWEEP_INTERVAL_TICKS == 0)
			runNow(server);
	}

	/** Runs one sweep pass immediately - shared by the scheduled tick handler and {@code /msutimeline branch prune}. */
	public static void runNow(MinecraftServer server)
	{
		long now = server.overworld().getGameTime();
		TimelineBranchRegistry registry = server.overworld().getData(MSUAttachments.TIMELINE_BRANCHES);

		for(TimelineBranch branch : List.copyOf(registry.getAll()))
		{
			if(!qualifies(branch, now))
				continue;

			TimelineBranch parent = branch.getParentBranchId() != null ? registry.get(branch.getParentBranchId()) : null;
			if(parent != null && qualifies(parent, now))
				continue; // the topmost qualifying ancestor in this chain handles the whole subtree instead

			if(subtreeFullyQualifies(registry, branch, now))
				BranchDeleter.delete(server, branch, true);
		}
	}

	private static boolean qualifies(TimelineBranch branch, long now)
	{
		return !branch.isRegistered() && (now - branch.getLastVisitedGameTime()) >= IDLE_PRUNE_TICKS;
	}

	private static boolean subtreeFullyQualifies(TimelineBranchRegistry registry, TimelineBranch branch, long now)
	{
		if(!qualifies(branch, now))
			return false;

		for(TimelineBranch child : registry.childrenOf(branch.getId()))
			if(!subtreeFullyQualifies(registry, child, now))
				return false;

		return true;
	}
}
