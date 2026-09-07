package org.wilkretawesomesauce.minestuckuniverseported.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class MSUCommands
{
	private MSUCommands()
	{
	}

	@SubscribeEvent
	private static void onRegisterCommands(RegisterCommandsEvent event)
	{
		CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

		// Every command this project registers now lives under one shared /msu root, rather than each as
		// its own top-level literal (/msutimeline, /msuitemvoid, /msustreak) - a real
		// user-requested restructure. Each command's own getArgumentBuilder() previously baked its
		// top-level name directly into its own Commands.literal(...) call, so those were changed to bare
		// sub-literals ("timeline", "itemvoid", "streak") to nest cleanly here instead.
		//
		// itemvoid/shop/streak/unlock moved a second time, under a real "debug" sub-literal - another
		// explicit user-requested restructure, separating the clearly debug/testing-only commands from the
		// two that stayed direct /msu children (abilitech, godtier - neither was named in that request).
		// The Juju Modus link/unlink/stash command (formerly registered here too) was removed entirely
		// along with the rest of the Juju Modus item/mechanic - see juju.MSUJujuRegistry's own doc comment.
		dispatcher.register(Commands.literal("msu")
				.then(Commands.literal("timeline")
						.then(TimelineRewindCommand.getArgumentBuilder())
						.then(TimelineTravelCommand.getArgumentBuilder())
						.then(TimelineBranchCommand.getArgumentBuilder()))
				.then(Commands.literal("debug")
						.then(ItemVoidCommand.getArgumentBuilder())
						.then(StreakCommand.getArgumentBuilder())
						.then(AbilitechUnlockCommand.getArgumentBuilder())
						.then(SkillShopCommand.getArgumentBuilder())
						.then(DoomDebugCommand.getArgumentBuilder())
						.then(TornadoDebugCommand.getArgumentBuilder()))
				.then(GodTierDebugCommand.getArgumentBuilder())
				.then(AbilitechUserCommand.getArgumentBuilder()));
	}
}
