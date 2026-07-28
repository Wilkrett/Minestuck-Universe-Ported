package org.wilkretawesomesauce.minestuckuniverseported.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.wilkretawesomesauce.minestuckuniverseported.blockentity.TemporalSendificatorBlockEntity;

import javax.annotation.Nullable;

/**
 * A new machine for the Time Request / Doom System (see {@code CLAUDE.md}) - lets a player repay an open
 * {@code timeline.request.TimeRequest} with a freshly-obtained matching item. Deliberately <b>not</b> an
 * extension of Minestuck's real {@code SendificatorBlockEntity}: that class sends an item to a destination
 * {@code BlockPos} (set via {@code SetSendificatorDestinationPacket}), not to a player or a paradox
 * request, and its internals (private {@code tick()}/{@code canSend()}/{@code processContents()}) offer no
 * safe extension point anyway - this is a genuinely separate block, only thematically related.
 * <p>
 * Opens its menu via {@code ServerPlayer#openMenu(MenuProvider)} - a real {@code AbstractContainerMenu},
 * not a client-only {@code Screen} referenced from common code. That's a deliberate departure from this
 * project's existing GUI-block pattern ({@code AbilitechnosynthBlock}'s
 * {@code Minecraft.getInstance().setScreen(...)} call), which is the exact shape behind the documented
 * dedicated-server crash (see {@code CLAUDE.md}'s "Recurring bug patterns" / known gap #6) - this sidesteps
 * that bug pattern entirely rather than repeating it in a new place.
 */
public class TemporalSendificatorBlock extends Block implements EntityBlock
{
	public TemporalSendificatorBlock(Properties properties)
	{
		super(properties);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return new TemporalSendificatorBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
	{
		if(level.getBlockEntity(pos) instanceof MenuProvider provider && player instanceof ServerPlayer serverPlayer)
			serverPlayer.openMenu(provider);
		return InteractionResult.SUCCESS;
	}
}
