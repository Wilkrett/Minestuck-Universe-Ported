package org.wilkretawesomesauce.minestuckuniverseported.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.game.GameData;
import org.wilkretawesomesauce.minestuckuniverseported.gui.itemvoid.ItemVoidMenu;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code ITEM_VOID_UI} GUI handler entry - the original opened
 * this GUI from a keybind/menu action with no block or item tied to it (unlike e.g. the Temporal
 * Sendificator, which is a real placed block), so a command is this port's real equivalent trigger rather
 * than a stand-in. Always fetches {@link GameData} off the Overworld specifically, regardless of which
 * dimension the executing player is actually in - matching the original's single, dimension-0-only capability
 * instance (see that class's own doc comment).
 */
public final class ItemVoidCommand
{
	private ItemVoidCommand()
	{
	}

	public static LiteralArgumentBuilder<CommandSourceStack> getArgumentBuilder()
	{
		return Commands.literal("itemvoid").executes(context ->
		{
			if(!(context.getSource().getEntity() instanceof ServerPlayer player))
				return 0;

			GameData data = player.server.overworld().getData(MSUAttachments.ITEM_VOID);
			player.openMenu(new SimpleMenuProvider(
					(containerId, inventory, p) -> new ItemVoidMenu(containerId, inventory, data),
					Component.translatable("gui.minestuckuniverseported.itemVoid.title")));
			return 1;
		});
	}
}
