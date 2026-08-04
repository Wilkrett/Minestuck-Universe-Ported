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
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.Abilitech;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRegistry;
import org.wilkretawesomesauce.minestuckuniverseported.network.MSUAbilitechPackets;

/**
 * {@code /msu abilitech grant <player> <abilitech>} / {@code /msu abilitech grant <player> all} /
 * {@code /msu abilitech revoke <player> <abilitech>} / {@code /msu abilitech revoke <player> all} -
 * permission-level-2-gated ("cheats"/op, not tied to game mode) debug bridges for managing <i>another</i>
 * player's real unlock state, same shape/gating as {@code AbilitechUnlockCommand} (which only ever
 * targets the commanding player themselves). {@code <player>} comes before the tech/{@code all} choice
 * (not nested under a {@code user} sub-literal) - a direct user correction, this command's real shape.
 * <p>
 * A single-tech {@code grant} force-marks that one tech unlocked for the target, bypassing
 * {@code canUnlock}/{@code onUnlock} entirely (spends nothing) - same real debug-bypass semantics as
 * {@code /msu debug unlock all}'s self-targeted equivalent. {@code grant ... all} does the same for every
 * registered tech at once. {@code revoke} is the real inverse for both shapes: a single-tech revoke clears
 * just that one tech's unlocked flag, {@code revoke ... all} clears the whole set - either way, any slot
 * left equipped with a tech that's no longer unlocked is force-unequipped afterward (a bare unlock-flag
 * clear alone would leave an already-equipped-but-now-locked tech sitting in its slot, since this
 * project's own unlock gate only ever runs at equip time, not continuously).
 */
public final class AbilitechUserCommand
{
	private AbilitechUserCommand()
	{
	}

	public static LiteralArgumentBuilder<CommandSourceStack> getArgumentBuilder()
	{
		return Commands.literal("abilitech")
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal("grant")
						.then(Commands.argument("player", EntityArgument.player())
								.then(Commands.literal("all")
										.executes(context -> grantAll(context.getSource(), EntityArgument.getPlayer(context, "player"))))
								.then(Commands.argument("tech", ResourceLocationArgument.id())
										.executes(context -> grant(context.getSource(), EntityArgument.getPlayer(context, "player"), ResourceLocationArgument.getId(context, "tech"))))))
				.then(Commands.literal("revoke")
						.then(Commands.argument("player", EntityArgument.player())
								.then(Commands.literal("all")
										.executes(context -> revokeAll(context.getSource(), EntityArgument.getPlayer(context, "player"))))
								.then(Commands.argument("tech", ResourceLocationArgument.id())
										.executes(context -> revoke(context.getSource(), EntityArgument.getPlayer(context, "player"), ResourceLocationArgument.getId(context, "tech"))))));
	}

	private static int grant(CommandSourceStack source, ServerPlayer target, ResourceLocation techId) throws CommandSyntaxException
	{
		Abilitech tech = MSUAbilitechRegistry.get(techId);
		if(tech == null)
		{
			source.sendFailure(Component.translatable("status.minestuckuniverseported.abilitech.unlock_not_found", techId.toString()));
			return 0;
		}

		GodTierData godTier = target.getData(MSUAttachments.GOD_TIER);
		if(godTier.isUnlocked(tech))
		{
			source.sendFailure(Component.translatable("status.minestuckuniverseported.abilitech.grant_already", target.getName(), tech.getDisplayName()));
			return 0;
		}

		godTier.markUnlocked(tech);
		MSUAbilitechPackets.sendLoadoutSync(target);

		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.abilitech.grant_success", tech.getDisplayName(), target.getName()), true);
		return 1;
	}

	private static int grantAll(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException
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

	private static int revoke(CommandSourceStack source, ServerPlayer target, ResourceLocation techId) throws CommandSyntaxException
	{
		Abilitech tech = MSUAbilitechRegistry.get(techId);
		if(tech == null)
		{
			source.sendFailure(Component.translatable("status.minestuckuniverseported.abilitech.unlock_not_found", techId.toString()));
			return 0;
		}

		GodTierData godTier = target.getData(MSUAttachments.GOD_TIER);
		if(!godTier.isUnlocked(tech))
		{
			source.sendFailure(Component.translatable("status.minestuckuniverseported.abilitech.revoke_not_unlocked", target.getName(), tech.getDisplayName()));
			return 0;
		}

		godTier.revokeUnlocked(tech);
		unequipIfNowLocked(godTier, target);
		MSUAbilitechPackets.sendLoadoutSync(target);

		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.abilitech.revoke_success", tech.getDisplayName(), target.getName()), true);
		return 1;
	}

	private static int revokeAll(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException
	{
		GodTierData godTier = target.getData(MSUAttachments.GOD_TIER);
		godTier.clearUnlockedTechs();
		unequipIfNowLocked(godTier, target);
		MSUAbilitechPackets.sendLoadoutSync(target);

		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.abilitech.user_revoke_success", target.getName()), true);
		return 1;
	}

	private static void unequipIfNowLocked(GodTierData godTier, ServerPlayer target)
	{
		for(int slot = 0; slot < GodTierData.TECH_SLOTS; slot++)
		{
			Abilitech equipped = godTier.getTech(slot);
			if(equipped != null && !godTier.isUnlocked(equipped))
				godTier.unequipTech(target.level(), target, slot);
		}
	}
}
