package org.wilkretawesomesauce.minestuckuniverseported.timeline;

import net.commoble.infiniverse.api.InfiniverseAPI;
import net.commoble.infiniverse.api.UnregisterDimensionEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Deletes a {@link TimelineBranch} - both the still-dormant case (folder can go immediately, nothing's
 * touching it) and the still-registered case (only reachable from an explicit manual delete - the
 * idle auto-prune sweep in {@link BranchPruneSweep} only ever targets already-dormant branches).
 * <p>
 * The still-registered path can't delete files the instant {@code markDimensionForUnregistration} is
 * called: disassembling Infiniverse's {@code DimensionManager} confirmed the actual removal happens on
 * a <i>later</i> server tick, and its own {@code getLevelsPendingUnregistration()} isn't a reliable
 * "did it finish" signal (the pending set is cleared unconditionally regardless of success). The
 * reliable signal is {@link UnregisterDimensionEvent} itself - this class listens for it and only
 * deletes files once it's seen (not cancelled) for the branch in question, with a small fallback-tick
 * buffer (and a logged warning) in case something else cancels it.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class BranchDeleter
{
	private static final long UNREGISTER_FALLBACK_TICKS = 5;

	private record PendingDeletion(TimelineBranch branch, long fallbackDeadlineGameTime)
	{
	}

	private static final Set<ResourceKey<Level>> CONFIRMED_UNREGISTERED = new HashSet<>();
	private static final List<PendingDeletion> PENDING = new ArrayList<>();

	private BranchDeleter()
	{
	}

	/**
	 * Deletes {@code branch}. If {@code recursive}, every existing descendant is deleted too (each via
	 * this same method, so a still-registered descendant is evicted/unregistered properly rather than
	 * having its files ripped out from under a live level).
	 */
	public static void delete(MinecraftServer server, TimelineBranch branch, boolean recursive)
	{
		TimelineBranchRegistry registry = server.overworld().getData(MSUAttachments.TIMELINE_BRANCHES);

		if(recursive)
			for(TimelineBranch child : List.copyOf(registry.childrenOf(branch.getId())))
				delete(server, child, true);

		registry.remove(branch.getId());

		if(!branch.isRegistered())
		{
			deleteFolder(server, branch);
			return;
		}

		ServerLevel level = server.getLevel(branch.getDimensionKey());
		if(level != null)
		{
			ServerLevel fallback = BranchForker.resolveParentLevel(server, branch);
			for(ServerPlayer player : List.copyOf(level.players()))
				player.teleportTo(fallback, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());

			InfiniverseAPI.get().markDimensionForUnregistration(server, branch.getDimensionKey());
		}

		PENDING.add(new PendingDeletion(branch, server.overworld().getGameTime() + UNREGISTER_FALLBACK_TICKS));
	}

	private static void deleteFolder(MinecraftServer server, TimelineBranch branch)
	{
		Path folder = DimensionType.getStorageFolder(branch.getDimensionKey(), server.getWorldPath(LevelResource.ROOT));
		if(Files.notExists(folder))
			return;

		try
		{
			Files.walkFileTree(folder, new SimpleFileVisitor<>()
			{
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
				{
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
				{
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
			});
		}
		catch(IOException e)
		{
			throw new UncheckedIOException("Failed to delete timeline branch folder " + folder, e);
		}
	}

	@SubscribeEvent
	private static void onUnregistered(UnregisterDimensionEvent event)
	{
		if(!event.isCanceled())
			CONFIRMED_UNREGISTERED.add(event.getLevel().dimension());
	}

	@SubscribeEvent
	private static void onServerTick(ServerTickEvent.Post event)
	{
		if(PENDING.isEmpty())
			return;

		MinecraftServer server = event.getServer();
		long now = server.overworld().getGameTime();

		PENDING.removeIf(pending ->
		{
			ResourceKey<Level> key = pending.branch().getDimensionKey();
			boolean confirmed = CONFIRMED_UNREGISTERED.remove(key);
			if(!confirmed && now < pending.fallbackDeadlineGameTime())
				return false;

			deleteFolder(server, pending.branch());
			return true;
		});
	}
}
