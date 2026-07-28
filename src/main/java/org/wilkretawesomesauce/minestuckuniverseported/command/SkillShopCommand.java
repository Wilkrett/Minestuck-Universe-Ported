package org.wilkretawesomesauce.minestuckuniverseported.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.network.OpenSkillShopPacket;

/**
 * {@code /msu debug shop} - opens the real Skill Shop screen for the commanding player (moved under a real
 * {@code debug} sub-literal alongside itemvoid/juju/streak/unlock - a later, separate user-requested
 * restructure from the one described below). Real trigger point for Consort dialogue: {@code Trigger}
 * (Minestuck's own dialogue-trigger type) is a <b>sealed</b> interface (confirmed the hard way -
 * {@code javac} rejects any external implementation, since only the variants listed in its own
 * {@code permits} clause are allowed), so a custom {@code Trigger} implementation isn't possible without
 * Mixin. Minestuck's own real {@code Trigger.Command} variant (confirmed via `javap` - runs arbitrary
 * command text as the triggering player via {@code Commands#performPrefixedCommand}) already covers
 * exactly this case: the Consort dialogue response in
 * {@code data/minestuckuniverseported/minestuck/dialogue/skill_shop_offer.json} runs
 * {@code "msu debug shop"} through it instead. This command has no gating of its own - any player can run
 * it directly too, which is a real, low-risk bonus (a manual way to reach the shop, not just via dialogue).
 */
public final class SkillShopCommand
{
	private SkillShopCommand()
	{
	}

	public static LiteralArgumentBuilder<CommandSourceStack> getArgumentBuilder()
	{
		return Commands.literal("shop").executes(context -> open(context.getSource()));
	}

	private static int open(CommandSourceStack source) throws CommandSyntaxException
	{
		ServerPlayer player = source.getPlayerOrException();
		PacketDistributor.sendToPlayer(player, new OpenSkillShopPacket());
		return 1;
	}
}
