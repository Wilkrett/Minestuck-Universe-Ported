package org.wilkretawesomesauce.minestuckuniverseported.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.wilkretawesomesauce.minestuckuniverseported.entity.TornadoEntity;

/**
 * {@code /msu debug tornado spawn} - permission-level-2-gated ("cheats"/op, not tied to game mode,
 * same bar every other debug command in this project uses), modeled on {@code StreakCommand}. Lets
 * the small cosmetic {@link TornadoEntity} (see its own doc comment - visual-only, no gameplay
 * effect, not yet wired to any Tech) be dropped at the executing player's own position so its look
 * can be iterated on at different locations before it's ever hooked into a real move.
 */
public final class TornadoDebugCommand
{
	private static final float DEFAULT_SIZE = 1.0F;
	private static final int DEFAULT_COLOR = 0x47E2FA;
	private static final int DEFAULT_LIFESPAN_TICKS = 200;

	private TornadoDebugCommand()
	{
	}

	public static LiteralArgumentBuilder<CommandSourceStack> getArgumentBuilder()
	{
		return Commands.literal("tornado")
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal("spawn")
						.executes(context -> spawn(context.getSource())));
	}

	private static int spawn(CommandSourceStack source) throws CommandSyntaxException
	{
		ServerPlayer player = source.getPlayerOrException();
		ServerLevel level = player.serverLevel();

		TornadoEntity tornado = TornadoEntity.create(level, DEFAULT_SIZE, DEFAULT_COLOR, DEFAULT_LIFESPAN_TICKS);
		tornado.setPos(player.getX(), player.getY(), player.getZ());
		level.addFreshEntity(tornado);

		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.tornado.spawned"), true);
		return 1;
	}
}
