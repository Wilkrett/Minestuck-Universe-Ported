package org.wilkretawesomesauce.minestuckuniverseported.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.wilkretawesomesauce.minestuckuniverseported.timeline.TimelineManager;

/**
 * {@code /msutimeline rewind <seconds>} - the "just type a number" way to trigger a real world rewind
 * that plays out over time (see {@code timeline.TimelineManager#rewind}), for testing/using the timeline
 * system directly rather than through an equipped Abilitech's charge-and-release input. See
 * {@link TimelineTravelCommand} for the instant counterpart. Still adds Doom Points like any other
 * rewind - this is a different way to trigger the same operation, not a way around its cost.
 */
public final class TimelineRewindCommand
{
	private TimelineRewindCommand()
	{
	}

	/** Returns the "rewind" branch to attach under the shared "msutimeline" root - see {@code MSUCommands}. */
	public static LiteralArgumentBuilder<CommandSourceStack> getArgumentBuilder()
	{
		return Commands.literal("rewind")
				.then(Commands.argument("seconds", IntegerArgumentType.integer(1, 300))
						.executes(context -> execute(context.getSource(), IntegerArgumentType.getInteger(context, "seconds"))));
	}

	private static int execute(CommandSourceStack source, int seconds) throws CommandSyntaxException
	{
		ServerPlayer player = source.getPlayerOrException();
		if(!(player.level() instanceof ServerLevel level))
			return 0;

		int actualTicks = TimelineManager.rewind(level, player, seconds * 20);

		if(actualTicks <= 0)
		{
			source.sendFailure(Component.translatable("status.minestuckuniverseported.timeline.no_history"));
			return 0;
		}

		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.timeline.rewinding", actualTicks / 20F), true);
		return actualTicks;
	}
}
