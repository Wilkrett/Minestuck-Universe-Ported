package org.wilkretawesomesauce.minestuckuniverseported.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.badges.Badge;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;

/**
 * {@code /msu godtier skilllevel <value>} / {@code /msu godtier badge <id>} - permission-level-2-gated
 * ("cheats"/op, not tied to game mode) debug bridges for the two new real-but-automatic-gain-path-free
 * pieces of {@code godtier.GodTierData} this
 * project's Badge pass added (skill level, badge unlocks) - same shape and same reasoning as
 * {@code AbilitechUnlockCommand}: {@code skilllevel} just sets the real stored field directly (there's no
 * real in-game way to earn it yet, same standing gap as Karma - see {@code GodTierData}'s own doc
 * comment), while {@code badge} calls the target badge's actual real {@code canUnlock} (so a real
 * {@code badges.BadgeKarma}/{@code badges.BadgeEffectBuff} unlock through this command genuinely spends
 * the real grist/item cost, exactly like a real purchase would).
 */
public final class GodTierDebugCommand
{
	private GodTierDebugCommand()
	{
	}

	public static LiteralArgumentBuilder<CommandSourceStack> getArgumentBuilder()
	{
		return Commands.literal("godtier")
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal("skilllevel")
						.then(Commands.argument("value", IntegerArgumentType.integer(0))
								.executes(context -> setSkillLevel(context.getSource(), IntegerArgumentType.getInteger(context, "value")))))
				.then(Commands.literal("badge")
						.then(Commands.argument("badge", ResourceLocationArgument.id())
								.executes(context -> unlockBadge(context.getSource(), ResourceLocationArgument.getId(context, "badge")))));
	}

	private static int setSkillLevel(CommandSourceStack source, int value) throws CommandSyntaxException
	{
		ServerPlayer player = source.getPlayerOrException();
		player.getData(MSUAttachments.GOD_TIER).setSkillLevel(value);
		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.godtier.skill_level_set", value), true);
		return value;
	}

	private static int unlockBadge(CommandSourceStack source, ResourceLocation badgeId) throws CommandSyntaxException
	{
		ServerPlayer player = source.getPlayerOrException();

		Badge badge = null;
		for(Badge candidate : Badge.BADGES)
			if(candidate.getId().equals(badgeId))
			{
				badge = candidate;
				break;
			}

		if(badge == null)
		{
			source.sendFailure(Component.translatable("status.minestuckuniverseported.godtier.badge_not_found", badgeId.toString()));
			return 0;
		}

		GodTierData godTier = player.getData(MSUAttachments.GOD_TIER);
		if(godTier.hasBadge(badge))
		{
			source.sendFailure(Component.translatable("status.minestuckuniverseported.godtier.badge_already", badge.getDisplayName()));
			return 0;
		}

		if(!badge.canUnlock(player.level(), player))
		{
			source.sendFailure(Component.translatable("status.minestuckuniverseported.godtier.badge_cant_afford", badge.getDisplayName()));
			return 0;
		}

		godTier.unlockBadge(badge);
		badge.onBadgeUnlocked(player.level(), player);
		org.wilkretawesomesauce.minestuckuniverseported.badges.BuilderBadgeEvents.sync(player);

		Badge unlockedBadge = badge;
		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.godtier.badge_success", unlockedBadge.getDisplayName()), true);
		return 1;
	}
}
