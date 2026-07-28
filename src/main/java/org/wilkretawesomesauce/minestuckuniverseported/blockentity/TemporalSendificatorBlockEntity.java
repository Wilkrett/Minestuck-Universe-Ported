package org.wilkretawesomesauce.minestuckuniverseported.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.wilkretawesomesauce.minestuckuniverseported.MSUBlockEntities;
import org.wilkretawesomesauce.minestuckuniverseported.inventory.TemporalSendificatorMenu;

import javax.annotation.Nullable;

/**
 * The Temporal Sendificator's block entity - a new machine for the Time Request / Doom System (see
 * {@code CLAUDE.md}), not an extension of Minestuck's real {@code SendificatorBlockEntity} (whose
 * destination-{@code BlockPos} recipient model doesn't fit paradox repayment - see that class's own doc
 * comment on {@code block.TemporalSendificatorBlock} for why).
 * <p>
 * Holds a single-slot {@link SimpleContainer} that's never persisted to NBT - the input slot is purely
 * transient, resolved-or-returned every time the menu is open (see {@code TemporalSendificatorMenu#broadcastChanges}/
 * {@code #removed}), so there's nothing meaningful to save between sessions.
 */
public class TemporalSendificatorBlockEntity extends BlockEntity implements MenuProvider
{
	public final SimpleContainer container = new SimpleContainer(1);

	public TemporalSendificatorBlockEntity(BlockPos pos, BlockState state)
	{
		super(MSUBlockEntities.TEMPORAL_SENDIFICATOR.get(), pos, state);
	}

	@Override
	public Component getDisplayName()
	{
		return Component.translatable("gui.temporalSendificator");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player)
	{
		return new TemporalSendificatorMenu(containerId, inventory, container, ContainerLevelAccess.create(level, getBlockPos()));
	}
}
