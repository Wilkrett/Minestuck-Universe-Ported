package org.wilkretawesomesauce.minestuckuniverseported.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.wilkretawesomesauce.minestuckuniverseported.entity.UninhabitedBodyEntity;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.soul.BodyManager;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.soul.ISoulData;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.soul.SoulManager;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;

/**
 * {@code /msu debug soul leave|return|status} - permission-level-2-gated ("cheats"/op, not tied to game
 * mode, same bar every other debug command in this project uses), modeled on
 * {@code TornadoDebugCommand}/{@code DoomDebugCommand}. Manual-testing bridge for {@code mechanics.soul}
 * (Phases 1-5 only) - always acts on the executing player, no target argument, since the Soul system has
 * no interaction-free way to point one at a specific other player's own relationships yet.
 */
public final class SoulDebugCommand
{
	private SoulDebugCommand()
	{
	}

	public static LiteralArgumentBuilder<CommandSourceStack> getArgumentBuilder()
	{
		return Commands.literal("soul")
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal("leave")
						.executes(context -> leave(context.getSource())))
				.then(Commands.literal("return")
						.executes(context -> returnHome(context.getSource())))
				.then(Commands.literal("status")
						.executes(context -> status(context.getSource())));
	}

	private static int leave(CommandSourceStack source) throws CommandSyntaxException
	{
		ServerPlayer player = source.getPlayerOrException();
		boolean success = SoulManager.leaveBody(player);
		source.sendSuccess(() -> Component.translatable(success
				? "status.minestuckuniverseported.soul.left_body"
				: "status.minestuckuniverseported.soul.already_projecting"), true);
		return success ? 1 : 0;
	}

	private static int returnHome(CommandSourceStack source) throws CommandSyntaxException
	{
		ServerPlayer player = source.getPlayerOrException();
		UninhabitedBodyEntity ownBody = BodyManager.resolveBody(player.getServer(), player.getUUID());
		if(ownBody == null)
		{
			source.sendFailure(Component.translatable("status.minestuckuniverseported.soul.no_body"));
			return 0;
		}

		boolean success = SoulManager.inhabitBody(player, ownBody);
		source.sendSuccess(() -> Component.translatable(success
				? "status.minestuckuniverseported.soul.returned"
				: "status.minestuckuniverseported.soul.cannot_return"), true);
		return success ? 1 : 0;
	}

	private static int status(CommandSourceStack source) throws CommandSyntaxException
	{
		ServerPlayer player = source.getPlayerOrException();
		ISoulData soul = player.getData(MSUAttachments.PLAYER_SOUL);

		String state;
		if(soul.getCurrentHostUUID() != null)
			state = "host " + soul.getCurrentHostUUID();
		else if(soul.isFreeSpirit())
			state = "free spirit";
		else
			state = "normal";

		String finalState = state;
		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.soul.status", finalState), false);
		return 1;
	}
}
