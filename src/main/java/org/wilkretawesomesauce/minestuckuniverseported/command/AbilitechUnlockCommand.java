package org.wilkretawesomesauce.minestuckuniverseported.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;
import org.wilkretawesomesauce.minestuckuniverseported.network.MSUAbilitechPackets;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.Abilitech;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRegistry;

/**
 * {@code /msu debug unlock ...} - a permission-level-2-gated ("cheats"/op, same bar vanilla's own
 * `/give`-style commands use - not tied to game mode, so a survival-mode player with cheats enabled can
 * still reach it) debug bridge for the real unlock economy (see {@code abilitech.TechBoondollarCost}),
 * same gating shape as {@code TimelineBranchCommand}. Merges what used to be two separate command
 * classes under two unrelated roots ({@code /msu debug unlock ...} for the commanding player's own
 * unlocks, {@code /msu abilitech grant|revoke user <player>} for managing another player's) - both were
 * always the same real {@code GodTierData} unlock bookkeeping, a real reported inconsistency, not a
 * deliberate split.
 * <p>
 * {@code /msu debug unlock <tech>} is an interim testing tool, not the real player-facing unlock
 * trigger - that's {@code client.gui.SkillShopScreen} (reached via {@code /msu debug shop} or real
 * Consort dialogue). Still real: it calls the tech's actual {@code canUnlock}/{@code onUnlock} (so it
 * genuinely spends the real boondollar balance/required items exactly like a real purchase would), it
 * just skips needing the Consort-triggered shop UI to reach that call.
 * <p>
 * {@code /msu debug unlock all} is a separate, more blunt dev shortcut - it force-marks every registered
 * tech unlocked directly (bypassing {@code canUnlock}/{@code onUnlock} entirely, so it spends nothing),
 * for testing content without needing to grind or repeatedly simulate ~65 individual real purchases. Not
 * a real purchase in any sense - a debug bypass, stated plainly.
 * <p>
 * {@code /msu debug unlock user <player> grant} is {@code all}'s exact equivalent targeted at another
 * player instead of the commander. {@code /msu debug unlock user <player> revoke} is the real inverse
 * (not present anywhere else): clears the target's entire unlocked set, then unequips any slot whose
 * tech is no longer unlocked (a bare clear alone would leave an already-equipped-but-now-locked tech
 * sitting in its slot, since this project's own unlock gate only ever runs at equip time, not
 * continuously).
 */
public final class AbilitechUnlockCommand
{
	private AbilitechUnlockCommand()
	{
	}

	public static LiteralArgumentBuilder<CommandSourceStack> getArgumentBuilder()
	{
		return Commands.literal("unlock")
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal("all").executes(context -> unlockAll(context.getSource())))
				.then(Commands.argument("tech", ResourceLocationArgument.id())
						.executes(context -> unlock(context.getSource(), ResourceLocationArgument.getId(context, "tech"))))
				.then(Commands.literal("user")
						.then(Commands.argument("player", EntityArgument.player())
								.then(Commands.literal("grant")
										.executes(context -> grantUser(context.getSource(), EntityArgument.getPlayer(context, "player"))))
								.then(Commands.literal("revoke")
										.executes(context -> revokeUser(context.getSource(), EntityArgument.getPlayer(context, "player"))))));
	}

	private static int unlockAll(CommandSourceStack source) throws CommandSyntaxException
	{
		ServerPlayer player = source.getPlayerOrException();
		GodTierData godTier = player.getData(MSUAttachments.GOD_TIER);

		int count = 0;
		for(Abilitech tech : MSUAbilitechRegistry.getAll())
		{
			if(godTier.isUnlocked(tech))
				continue;
			godTier.markUnlocked(tech);
			count++;
		}

		int unlockedCount = count;
		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.abilitech.unlock_all_success", unlockedCount), true);
		return count;
	}

	private static int unlock(CommandSourceStack source, ResourceLocation techId) throws CommandSyntaxException
	{
		ServerPlayer player = source.getPlayerOrException();

		Abilitech tech = MSUAbilitechRegistry.get(techId);
		if(tech == null)
		{
			source.sendFailure(Component.translatable("status.minestuckuniverseported.abilitech.unlock_not_found", techId.toString()));
			return 0;
		}

		GodTierData godTier = player.getData(MSUAttachments.GOD_TIER);
		if(godTier.isUnlocked(tech))
		{
			source.sendFailure(Component.translatable("status.minestuckuniverseported.abilitech.unlock_already", tech.getDisplayName()));
			return 0;
		}

		if(!tech.canUnlock(player.level(), player))
		{
			source.sendFailure(Component.translatable("status.minestuckuniverseported.abilitech.unlock_cant_afford", tech.getUnlockRequirements()));
			return 0;
		}

		tech.onUnlock(player.level(), player);
		godTier.markUnlocked(tech);

		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.abilitech.unlock_success", tech.getDisplayName()), true);
		return 1;
	}

	private static int grantUser(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException
	{
		GodTierData godTier = target.getData(MSUAttachments.GOD_TIER);

		int count = 0;
		for(Abilitech tech : MSUAbilitechRegistry.getAll())
		{
			if(godTier.isUnlocked(tech))
				continue;
			godTier.markUnlocked(tech);
			count++;
		}

		MSUAbilitechPackets.sendLoadoutSync(target);

		int unlockedCount = count;
		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.abilitech.user_unlock_all_success", unlockedCount, target.getName()), true);
		return count;
	}

	private static int revokeUser(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException
	{
		GodTierData godTier = target.getData(MSUAttachments.GOD_TIER);
		godTier.clearUnlockedTechs();

		for(int slot = 0; slot < GodTierData.TECH_SLOTS; slot++)
		{
			Abilitech equipped = godTier.getTech(slot);
			if(equipped != null && !godTier.isUnlocked(equipped))
				godTier.unequipTech(target.level(), target, slot);
		}

		MSUAbilitechPackets.sendLoadoutSync(target);

		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.abilitech.user_revoke_success", target.getName()), true);
		return 1;
	}
}
