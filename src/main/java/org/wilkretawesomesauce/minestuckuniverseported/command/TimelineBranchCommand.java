package org.wilkretawesomesauce.minestuckuniverseported.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.BranchDeleter;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.BranchForker;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.BranchPruneSweep;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.TimelineBranch;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.TimelineBranchRegistry;

import javax.annotation.Nullable;
import java.util.List;

/**
 * {@code /msutimeline branch ...} - a permission-level-2-gated ("cheats"/op, not tied to game mode)
 * debug/testing surface for the parallel timeline branch tree (see
 * {@code mechanics.timeline.TimelineBranch}/{@code mechanics.timeline.BranchForker}). The real
 * player-facing way to fork/return between branches is the {@code TechTimelineBranch} Abilitech; this
 * command tree exists for testing it directly and for operations (like naming a branch, or browsing to
 * an arbitrary one anywhere in the tree) that tech has no in-game UI for yet.
 */
public final class TimelineBranchCommand
{
	private TimelineBranchCommand()
	{
	}

	/** Returns the "branch" branch to attach under the shared "msutimeline" root - see {@code MSUCommands}. */
	public static LiteralArgumentBuilder<CommandSourceStack> getArgumentBuilder()
	{
		return Commands.literal("branch")
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal("create")
						.executes(context -> create(context.getSource(), null))
						.then(Commands.argument("name", StringArgumentType.greedyString())
								.executes(context -> create(context.getSource(), StringArgumentType.getString(context, "name")))))
				.then(Commands.literal("list")
						.executes(context -> list(context.getSource())))
				.then(Commands.literal("travel")
						.then(Commands.argument("branch", StringArgumentType.greedyString())
								.executes(context -> travel(context.getSource(), StringArgumentType.getString(context, "branch")))))
				.then(Commands.literal("delete")
						.then(Commands.argument("branch", StringArgumentType.string())
								.executes(context -> delete(context.getSource(), StringArgumentType.getString(context, "branch"), false))
								.then(Commands.literal("confirm")
										.executes(context -> delete(context.getSource(), StringArgumentType.getString(context, "branch"), true)))))
				.then(Commands.literal("prune")
						.executes(context -> prune(context.getSource())))
				.then(Commands.literal("home")
						.executes(context -> home(context.getSource())));
	}

	private static int create(CommandSourceStack source, @Nullable String requestedName) throws CommandSyntaxException
	{
		ServerPlayer player = source.getPlayerOrException();
		if(!(player.level() instanceof ServerLevel level))
			return 0;

		String name = (requestedName == null || requestedName.isBlank())
				? BranchForker.autoName(player, registryOf(player.getServer()))
				: requestedName;

		TimelineBranch branch = BranchForker.fork(player, level, name);
		if(branch == null)
		{
			source.sendFailure(Component.translatable("status.minestuckuniverseported.timeline.branch_fork_failed"));
			return 0;
		}

		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.timeline.branch_created", branch.getDisplayName()), true);
		return 1;
	}

	private static int list(CommandSourceStack source)
	{
		TimelineBranchRegistry registry = registryOf(source.getServer());
		StringBuilder sb = new StringBuilder("Timeline branches:\nAlpha (Overworld)\n");
		appendChildren(registry, null, sb, 1);
		source.sendSuccess(() -> Component.literal(sb.toString()), false);
		return 1;
	}

	private static void appendChildren(TimelineBranchRegistry registry, @Nullable java.util.UUID parentId, StringBuilder sb, int depth)
	{
		for(TimelineBranch branch : registry.childrenOf(parentId))
		{
			sb.append("  ".repeat(depth))
					.append("- ").append(branch.getDisplayName())
					.append(branch.isRegistered() ? " [active]" : " [dormant]")
					.append('\n');
			appendChildren(registry, branch.getId(), sb, depth + 1);
		}
	}

	private static int travel(CommandSourceStack source, String idOrName) throws CommandSyntaxException
	{
		ServerPlayer player = source.getPlayerOrException();
		MinecraftServer server = player.getServer();
		TimelineBranchRegistry registry = registryOf(server);

		TimelineBranch branch = registry.findByIdOrName(idOrName);
		if(branch == null)
		{
			source.sendFailure(Component.translatable("status.minestuckuniverseported.timeline.branch_not_found", idOrName));
			return 0;
		}

		ServerLevel destination = BranchForker.travelTo(server, branch);
		player.teleportTo(destination, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());

		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.timeline.branch_traveled", branch.getDisplayName()), true);
		return 1;
	}

	private static int delete(CommandSourceStack source, String idOrName, boolean confirmed)
	{
		TimelineBranchRegistry registry = registryOf(source.getServer());
		TimelineBranch branch = registry.findByIdOrName(idOrName);
		if(branch == null)
		{
			source.sendFailure(Component.translatable("status.minestuckuniverseported.timeline.branch_not_found", idOrName));
			return 0;
		}

		List<TimelineBranch> children = registry.childrenOf(branch.getId());
		if(!children.isEmpty() && !confirmed)
		{
			source.sendFailure(Component.translatable("status.minestuckuniverseported.timeline.branch_has_children", children.size()));
			return 0;
		}

		BranchDeleter.delete(source.getServer(), branch, true);
		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.timeline.branch_deleted", branch.getDisplayName()), true);
		return 1;
	}

	private static int prune(CommandSourceStack source)
	{
		BranchPruneSweep.runNow(source.getServer());
		source.sendSuccess(() -> Component.literal("Ran an idle-branch prune sweep."), true);
		return 1;
	}

	/** Jumps straight to Alpha regardless of how deep in the branch tree the sender is - the command-line counterpart to {@code TechTimelineBranch}'s long-hold action. */
	private static int home(CommandSourceStack source) throws CommandSyntaxException
	{
		ServerPlayer player = source.getPlayerOrException();
		ServerLevel overworld = player.getServer().overworld();
		if(player.level() == overworld)
		{
			source.sendFailure(Component.translatable("status.minestuckuniverseported.timeline.branch_already_alpha"));
			return 0;
		}

		player.teleportTo(overworld, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.timeline.branch_returned_to_alpha"), true);
		return 1;
	}

	private static TimelineBranchRegistry registryOf(MinecraftServer server)
	{
		return server.overworld().getData(MSUAttachments.TIMELINE_BRANCHES);
	}
}
