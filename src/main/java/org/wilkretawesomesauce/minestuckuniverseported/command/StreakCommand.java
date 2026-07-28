package org.wilkretawesomesauce.minestuckuniverseported.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.network.StreakStateSyncPacket;
import org.wilkretawesomesauce.minestuckuniverseported.client.streak.StreakFlavours;
import org.wilkretawesomesauce.minestuckuniverseported.client.streak.StreakPreference;

/**
 * {@code /msustreak ...} - a permission-level-2-gated ("cheats"/op, not tied to game mode) debug/demo
 * command for the streak ribbon + sprint-ghost effect ported from iChun's Streak (Forge 1.16.3, LGPL-3.0 -
 * see {@code streak.StreakFlavours}'s own doc comment). Same gating shape as {@code TimelineBranchCommand}:
 * this isn't a general player-facing feature, it exists to toggle and verify the port works on the
 * commanding player.
 */
public final class StreakCommand
{
	private StreakCommand()
	{
	}

	public static LiteralArgumentBuilder<CommandSourceStack> getArgumentBuilder()
	{
		return Commands.literal("streak")
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal("toggle")
						.executes(context -> toggle(context.getSource()))
						.then(Commands.argument("name", StringArgumentType.string())
								.executes(context -> toggleNamed(context.getSource(), StringArgumentType.getString(context, "name")))))
				.then(Commands.literal("flavour")
						.then(Commands.literal("list").executes(context -> list(context.getSource())))
						.then(Commands.argument("name", StringArgumentType.string())
								.executes(context -> setFlavour(context.getSource(), StringArgumentType.getString(context, "name")))));
	}

	private static int toggle(CommandSourceStack source)
	{
		ServerPlayer player = source.getPlayer();
		if(player == null)
			return 0;

		StreakPreference preference = player.getData(MSUAttachments.STREAK_PREFERENCE);
		boolean nowEnabled = !preference.isEnabled();
		preference.setEnabled(nowEnabled);

		broadcast(player, preference);

		source.sendSuccess(() -> Component.translatable(nowEnabled
				? "status.minestuckuniverseported.streak.enabled"
				: "status.minestuckuniverseported.streak.disabled"), true);
		return 1;
	}

	/**
	 * {@code /msu streak toggle <name>} - a shortcut combining the plain no-arg toggle with
	 * {@code /msu streak flavour <name>}: picking a flavour that's already the active, enabled one turns
	 * the effect off entirely (a real toggle); picking any other name switches to it and turns the effect
	 * on. Doesn't replace the no-arg {@link #toggle(CommandSourceStack)} or {@link #setFlavour} - both stay
	 * available for on/off and flavour-only changes.
	 */
	private static int toggleNamed(CommandSourceStack source, String name)
	{
		ServerPlayer player = source.getPlayer();
		if(player == null)
			return 0;

		if(!StreakFlavours.isValid(name))
		{
			source.sendFailure(Component.translatable("status.minestuckuniverseported.streak.invalid_flavour", name));
			return 0;
		}

		StreakPreference preference = player.getData(MSUAttachments.STREAK_PREFERENCE);
		boolean turningOff = preference.isEnabled() && name.equals(preference.resolveFlavour());

		if(turningOff)
			preference.setEnabled(false);
		else
		{
			preference.setFavouriteFlavour(name);
			preference.setEnabled(true);
		}

		broadcast(player, preference);

		source.sendSuccess(() -> Component.translatable(turningOff
				? "status.minestuckuniverseported.streak.disabled"
				: "status.minestuckuniverseported.streak.enabled_flavour", name), true);
		return 1;
	}

	private static int setFlavour(CommandSourceStack source, String name)
	{
		ServerPlayer player = source.getPlayer();
		if(player == null)
			return 0;

		if(!StreakFlavours.isValid(name))
		{
			source.sendFailure(Component.translatable("status.minestuckuniverseported.streak.invalid_flavour", name));
			return 0;
		}

		StreakPreference preference = player.getData(MSUAttachments.STREAK_PREFERENCE);
		preference.setFavouriteFlavour(name);

		if(preference.isEnabled())
			broadcast(player, preference);

		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.streak.flavour_set", name), true);
		return 1;
	}

	private static int list(CommandSourceStack source)
	{
		source.sendSuccess(() -> Component.literal("Streak flavours: " + String.join(", ", StreakFlavours.NAMES)), false);
		return 1;
	}

	private static void broadcast(ServerPlayer player, StreakPreference preference)
	{
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
				new StreakStateSyncPacket(player.getId(), preference.isEnabled(), preference.resolveFlavour(),
						preference.isHideTrail(), preference.isGhostsIgnoreSprint(), preference.getGhostTint()));
	}
}
