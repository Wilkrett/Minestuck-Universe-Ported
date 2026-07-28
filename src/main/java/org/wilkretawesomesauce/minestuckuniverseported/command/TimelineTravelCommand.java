package org.wilkretawesomesauce.minestuckuniverseported.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.TimelineManager;

/**
 * {@code /msutimeline travel backwards <seconds>} - the instant counterpart to
 * {@link TimelineRewindCommand}'s {@code rewind}: applies the exact same undo via
 * {@code mechanics.timeline.TimelineManager#travelBackwards}, but all at once instead of played out over real
 * ticks, and without spawning a {@code mechanics.timeline.DoomedTimelineClone} (there's nothing gradual to show
 * one alongside). You're instantly set to the beginning of the timeline snapshot.
 */
public final class TimelineTravelCommand
{
	private TimelineTravelCommand()
	{
	}

	/** Returns the "travel" branch to attach under the shared "msutimeline" root - see {@code MSUCommands}. */
	public static LiteralArgumentBuilder<CommandSourceStack> getArgumentBuilder()
	{
		return Commands.literal("travel")
				.then(Commands.literal("backwards")
						.then(Commands.argument("seconds", IntegerArgumentType.integer(1, 300))
								.executes(context -> execute(context.getSource(), IntegerArgumentType.getInteger(context, "seconds")))));
	}

	private static int execute(CommandSourceStack source, int seconds) throws CommandSyntaxException
	{
		ServerPlayer player = source.getPlayerOrException();
		if(!(player.level() instanceof ServerLevel level))
			return 0;

		int actualTicks = TimelineManager.travelBackwards(level, player, seconds * 20);

		if(actualTicks <= 0)
		{
			source.sendFailure(Component.translatable("status.minestuckuniverseported.timeline.no_history"));
			return 0;
		}

		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.timeline.traveled", actualTicks / 20F), true);
		return actualTicks;
	}
}
