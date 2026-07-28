package org.wilkretawesomesauce.minestuckuniverseported.inventory;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.MSUBlocks;
import org.wilkretawesomesauce.minestuckuniverseported.MSUItemComponents;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMenuTypes;
import org.wilkretawesomesauce.minestuckuniverseported.network.TimeRequestSyncPacket;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.request.TimeRequest;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.request.TimeRequestData;

import java.util.List;

/**
 * The Temporal Sendificator's real, server-authoritative container menu - see
 * {@code block.TemporalSendificatorBlock}'s doc comment for why this is a genuine
 * {@link AbstractContainerMenu} rather than this project's more common plain-{@code Screen} GUI pattern.
 * <p>
 * Two constructors, the standard vanilla machine-menu shape (e.g. {@code FurnaceMenu}): the client-side one
 * (registered directly as this menu type's supplier) builds a throwaway local {@link SimpleContainer} whose
 * single slot gets kept in sync with the real one automatically by vanilla's own container-sync networking;
 * the server-side one (called from {@code TemporalSendificatorBlockEntity#createMenu}) wires the real
 * block entity's container in directly.
 * <p>
 * There's no "select a request" step - {@link #tryResolveInsertedItem} runs every {@link #broadcastChanges()}
 * (server tick while open) and matches the inserted stack against *any* open request for the opening player,
 * resolving the first match by base {@link Item}. Deliberately does not track a "target" request client-side;
 * the full open-request list itself is read directly by {@code client.gui.TemporalSendificatorScreen} from
 * the player's already-synced {@code timeline.request.TimeRequestData} attachment (see
 * {@code network.TimeRequestSyncPacket}), not carried through this menu at all.
 * <p>
 * The input slot's {@link Slot#mayPlace} rejects any stack carrying {@code MSUItemComponents.BORROWED_REQUEST_ID}
 * outright - see that component's own doc comment for why tagging, not provenance-checking, is how "must be
 * a new copy" is enforced. Unconsumed contents are returned to the player on close ({@link #removed}) rather
 * than persisted - the slot is intentionally transient, see {@code TemporalSendificatorBlockEntity}'s doc
 * comment.
 */
public class TemporalSendificatorMenu extends AbstractContainerMenu
{
	private static final int SLOT_X = 80, SLOT_Y = 20;

	private final Container container;
	private final ContainerLevelAccess access;
	private final Player player;

	/** Client-side constructor - matches {@link net.minecraft.world.inventory.MenuType}'s plain supplier shape. */
	public TemporalSendificatorMenu(int containerId, Inventory playerInventory)
	{
		this(containerId, playerInventory, new SimpleContainer(1), ContainerLevelAccess.NULL);
	}

	/** Server-side constructor - called from {@code TemporalSendificatorBlockEntity#createMenu} with the real container. */
	public TemporalSendificatorMenu(int containerId, Inventory playerInventory, Container container, ContainerLevelAccess access)
	{
		super(MSUMenuTypes.TEMPORAL_SENDIFICATOR.get(), containerId);
		checkContainerSize(container, 1);
		this.container = container;
		this.access = access;
		this.player = playerInventory.player;
		container.startOpen(playerInventory.player);

		this.addSlot(new Slot(container, 0, SLOT_X, SLOT_Y)
		{
			@Override
			public boolean mayPlace(ItemStack stack)
			{
				return !stack.has(MSUItemComponents.BORROWED_REQUEST_ID);
			}
		});

		addPlayerInventorySlots(playerInventory);
	}

	private void addPlayerInventorySlots(Inventory playerInventory)
	{
		for(int row = 0; row < 3; row++)
			for(int col = 0; col < 9; col++)
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));

		for(int col = 0; col < 9; col++)
			this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
	}

	@Override
	public void broadcastChanges()
	{
		super.broadcastChanges();
		access.execute((level, pos) ->
		{
			if(!level.isClientSide() && player instanceof ServerPlayer serverPlayer)
				tryResolveInsertedItem(serverPlayer);
		});
	}

	private void tryResolveInsertedItem(ServerPlayer serverPlayer)
	{
		ItemStack inserted = container.getItem(0);
		if(inserted.isEmpty() || inserted.has(MSUItemComponents.BORROWED_REQUEST_ID))
			return;

		TimeRequestData data = serverPlayer.getData(MSUAttachments.TIME_REQUEST_DATA);
		for(TimeRequest request : List.copyOf(data.getOpenRequests()))
		{
			if(!BuiltInRegistries.ITEM.getKey(inserted.getItem()).equals(request.getItem()))
				continue;

			inserted.shrink(1);
			container.setItem(0, inserted);
			data.removeRequest(request.getId());

			PacketDistributor.sendToPlayer(serverPlayer, TimeRequestSyncPacket.create(serverPlayer));
			Item item = BuiltInRegistries.ITEM.get(request.getItem());
			serverPlayer.displayClientMessage(Component.translatable("status.minestuckuniverseported.timeRequest.resolved",
					Component.translatable(item.getDescriptionId())), true);
			return;
		}
	}

	@Override
	public void removed(Player player)
	{
		super.removed(player);
		access.execute((level, pos) ->
		{
			if(!level.isClientSide())
				clearContainer(player, container);
		});
	}

	@Override
	public boolean stillValid(Player player)
	{
		return stillValid(access, player, MSUBlocks.TEMPORAL_SENDIFICATOR.get());
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index)
	{
		ItemStack result = ItemStack.EMPTY;
		Slot slot = slots.get(index);
		if(slot != null && slot.hasItem())
		{
			ItemStack stackInSlot = slot.getItem();
			result = stackInSlot.copy();

			if(index == 0)
			{
				if(!moveItemStackTo(stackInSlot, 1, slots.size(), true))
					return ItemStack.EMPTY;
				slot.onQuickCraft(stackInSlot, result);
			}
			else if(!moveItemStackTo(stackInSlot, 0, 1, false))
				return ItemStack.EMPTY;

			if(stackInSlot.isEmpty())
				slot.set(ItemStack.EMPTY);
			else
				slot.setChanged();
		}
		return result;
	}
}
