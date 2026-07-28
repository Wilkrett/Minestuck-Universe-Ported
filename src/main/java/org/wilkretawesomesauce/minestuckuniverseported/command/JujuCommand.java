package org.wilkretawesomesauce.minestuckuniverseported.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mraof.minestuck.inventory.captchalogue.CaptchaDeckHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import org.wilkretawesomesauce.minestuckuniverseported.juju.JujuMenu;
import org.wilkretawesomesauce.minestuckuniverseported.juju.JujuModus;

/**
 * Real trigger for {@code juju.JujuModus}'s link/unlink/withdraw actions - the original exposed these
 * through a button inside its own custom {@code JujuGuiHandler} and (for withdrawing) clicking cards
 * directly in that same screen. This project reuses a real, separate {@code AbstractContainerMenu}
 * ({@code juju.JujuMenu}) for withdrawing (see that class's own doc comment for why), so a command is the
 * real equivalent trigger for link/unlink specifically - same reasoning
 * {@code itemvoid.ItemVoidCommand} already used for its own GUI, which also had no in-world block/item to
 * hang a right-click interaction off of.
 */
public final class JujuCommand
{
	private JujuCommand()
	{
	}

	public static LiteralArgumentBuilder<CommandSourceStack> getArgumentBuilder()
	{
		return Commands.literal("juju")
				.then(Commands.literal("link").executes(context ->
				{
					if(!(context.getSource().getEntity() instanceof ServerPlayer player))
						return 0;
					return JujuModus.link(player) ? 1 : 0;
				}))
				.then(Commands.literal("unlink").executes(context ->
				{
					if(!(context.getSource().getEntity() instanceof ServerPlayer player))
						return 0;
					return JujuModus.unlink(player) ? 1 : 0;
				}))
				.then(Commands.literal("stash").executes(context ->
				{
					if(!(context.getSource().getEntity() instanceof ServerPlayer player))
						return 0;

					if(!(CaptchaDeckHandler.getModus(player) instanceof JujuModus modus))
					{
						player.displayClientMessage(Component.translatable("status.minestuckuniverseported.jujuModusRequired"), false);
						return 0;
					}

					player.openMenu(new SimpleMenuProvider(
							(containerId, inventory, p) -> new JujuMenu(containerId, inventory, new JujuMenu.PartnerStashContainer(modus, player)),
							Component.translatable("gui.minestuckuniverseported.juju.title")));
					return 1;
				}));
	}
}
