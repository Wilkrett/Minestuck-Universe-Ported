package org.wilkretawesomesauce.minestuckuniverseported.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.doom.DoomData;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.doom.DoomMarkType;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.doom.DoomMarks;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.doom.DoomReleasePool;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;

/**
 * {@code /msu debug doom ...} - permission-level-2-gated ("cheats"/op, not tied to game mode, same bar
 * every other debug command in this project uses) manual-testing bridge for the universal Doom value
 * system ({@code mechanics.doom.DoomData}/{@code mechanics.doom.DoomReleasePool}). Original design for this project, no
 * 1.12.2 counterpart.
 * <p>
 * {@code pool peek}/{@code pool harvest} are centered on the executing player's own position - a real
 * future harvesting tech would center on its caster instead, this is just a manual-test bridge.
 */
public final class DoomDebugCommand
{
	private DoomDebugCommand()
	{
	}

	public static LiteralArgumentBuilder<CommandSourceStack> getArgumentBuilder()
	{
		return Commands.literal("doom")
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal("get")
						.then(Commands.argument("target", EntityArgument.entity())
								.executes(ctx -> get(ctx.getSource(), EntityArgument.getEntity(ctx, "target")))))
				.then(Commands.literal("set")
						.then(Commands.argument("target", EntityArgument.entity())
								.then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
										.executes(ctx -> set(ctx.getSource(), EntityArgument.getEntity(ctx, "target"), DoubleArgumentType.getDouble(ctx, "amount"))))))
				.then(Commands.literal("add")
						.then(Commands.argument("target", EntityArgument.entity())
								.then(Commands.argument("amount", DoubleArgumentType.doubleArg())
										.executes(ctx -> add(ctx.getSource(), EntityArgument.getEntity(ctx, "target"), DoubleArgumentType.getDouble(ctx, "amount"))))))
				.then(Commands.literal("seal")
						.then(Commands.argument("target", EntityArgument.entity())
								.then(Commands.argument("value", BoolArgumentType.bool())
										.executes(ctx -> seal(ctx.getSource(), EntityArgument.getEntity(ctx, "target"), BoolArgumentType.getBool(ctx, "value"))))))
				.then(Commands.literal("mark")
						.then(Commands.literal("deadshuffle")
								.then(Commands.argument("target", EntityArgument.entity())
										.then(Commands.argument("caster", EntityArgument.entity())
												.executes(ctx -> markDeadShuffle(ctx.getSource(),
														EntityArgument.getEntity(ctx, "target"), EntityArgument.getEntity(ctx, "caster"))))))
						.then(Commands.literal("clear")
								.then(Commands.argument("target", EntityArgument.entity())
										.executes(ctx -> clearMark(ctx.getSource(), EntityArgument.getEntity(ctx, "target"))))))
				.then(Commands.literal("pool")
						.then(Commands.literal("peek")
								.then(Commands.argument("radius", DoubleArgumentType.doubleArg(0))
										.executes(ctx -> peek(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "radius")))))
						.then(Commands.literal("harvest")
								.then(Commands.argument("radius", DoubleArgumentType.doubleArg(0))
										.executes(ctx -> harvest(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "radius"))))));
	}

	private static LivingEntity asLiving(CommandSourceStack source, Entity entity) throws CommandSyntaxException
	{
		if(!(entity instanceof LivingEntity living))
		{
			source.sendFailure(Component.translatable("status.minestuckuniverseported.doom.not_living", entity.getName()));
			return null;
		}
		return living;
	}

	private static int get(CommandSourceStack source, Entity target) throws CommandSyntaxException
	{
		LivingEntity living = asLiving(source, target);
		if(living == null)
			return 0;

		double doom = living.getData(MSUAttachments.DOOM_DATA).getDoom();
		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.doom.get", living.getName(), doom), false);
		return (int)doom;
	}

	private static int set(CommandSourceStack source, Entity target, double amount) throws CommandSyntaxException
	{
		LivingEntity living = asLiving(source, target);
		if(living == null)
			return 0;

		living.getData(MSUAttachments.DOOM_DATA).setDoom(amount);
		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.doom.set", living.getName(), amount), true);
		return (int)amount;
	}

	private static int add(CommandSourceStack source, Entity target, double amount) throws CommandSyntaxException
	{
		LivingEntity living = asLiving(source, target);
		if(living == null)
			return 0;

		DoomData data = living.getData(MSUAttachments.DOOM_DATA);
		if(amount >= 0)
			data.addDoom(amount);
		else
			data.removeDoom(-amount);

		double result = data.getDoom();
		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.doom.add", amount, living.getName(), result), true);
		return (int)result;
	}

	private static int seal(CommandSourceStack source, Entity target, boolean value) throws CommandSyntaxException
	{
		LivingEntity living = asLiving(source, target);
		if(living == null)
			return 0;

		living.getData(MSUAttachments.DOOM_DATA).setSealed(value);
		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.doom.seal", living.getName(), value), true);
		return 1;
	}

	private static int markDeadShuffle(CommandSourceStack source, Entity target, Entity caster) throws CommandSyntaxException
	{
		LivingEntity livingTarget = asLiving(source, target);
		if(livingTarget == null)
			return 0;
		LivingEntity livingCaster = asLiving(source, caster);
		if(livingCaster == null)
			return 0;

		DoomMarks.applyDeadShuffleMark(livingTarget, livingCaster.getUUID());
		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.doom.mark_deadshuffle", livingTarget.getName(), livingCaster.getName()), true);
		return 1;
	}

	private static int clearMark(CommandSourceStack source, Entity target) throws CommandSyntaxException
	{
		LivingEntity living = asLiving(source, target);
		if(living == null)
			return 0;

		living.getData(MSUAttachments.DOOM_DATA).clearMark();
		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.doom.mark_clear", living.getName()), true);
		return 1;
	}

	private static int peek(CommandSourceStack source, double radius) throws CommandSyntaxException
	{
		ServerPlayer player = source.getPlayerOrException();
		DoomReleasePool pool = player.level().getData(MSUAttachments.DOOM_RELEASE_POOL);
		double available = pool.peekAvailable(BlockPos.containing(player.position()), radius);

		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.doom.pool_peek", available, radius), false);
		return (int)available;
	}

	private static int harvest(CommandSourceStack source, double radius) throws CommandSyntaxException
	{
		ServerPlayer player = source.getPlayerOrException();
		DoomReleasePool pool = player.level().getData(MSUAttachments.DOOM_RELEASE_POOL);
		double harvested = pool.harvest(BlockPos.containing(player.position()), radius, Double.MAX_VALUE);
		player.getData(MSUAttachments.DOOM_DATA).addDoomRaw(harvested);

		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.doom.pool_harvest", harvested, radius), true);
		return (int)harvested;
	}
}
