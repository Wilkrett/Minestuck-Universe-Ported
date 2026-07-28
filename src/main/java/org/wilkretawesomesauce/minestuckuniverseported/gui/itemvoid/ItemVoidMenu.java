package org.wilkretawesomesauce.minestuckuniverseported.gui.itemvoid;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.game.GameData;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMenuTypes;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code gui.container.ContainerItemVoid} - a real
 * {@link AbstractContainerMenu} (same "genuine menu, not this project's more common plain-{@code Screen}
 * pattern" shape as {@code inventory.TemporalSendificatorMenu}, and for the same reason: real
 * server-authoritative slot contents, not just client-local state). Standard vanilla two-constructor shape:
 * the client-side one builds a throwaway local container kept in sync automatically by vanilla's own
 * container-sync networking, the server-side one (from {@code itemvoid.ItemVoidCommand}) wires in the real
 * {@link GameData} attachment directly.
 * <p>
 * The player-inventory/hotbar rows sit 10px lower than the vanilla-standard y offsets (94/112/130/152
 * instead of 84/102/120/142) - {@code textures/gui/container/item_void.png} is a real 176x176 backdrop, not
 * vanilla's usual 176x166, since its starfield "void" box (which the 27 void slots above render into,
 * unstyled, deliberately no grid) is taller than the label-only strip a normal container texture reserves up
 * there. Confirmed by sampling the actual PNG pixel boundaries, not guessed - see {@code ItemVoidScreen}'s
 * matching {@code imageHeight}.
 */
public class ItemVoidMenu extends AbstractContainerMenu
{
	private static final int ROWS = 3, COLS = 9;

	private final Container container;

	/** Client-side constructor - matches {@link net.minecraft.world.inventory.MenuType}'s plain supplier shape. */
	public ItemVoidMenu(int containerId, Inventory playerInventory)
	{
		this(containerId, playerInventory, new SimpleContainer(ROWS * COLS));
	}

	public ItemVoidMenu(int containerId, Inventory playerInventory, Container container)
	{
		super(MSUMenuTypes.ITEM_VOID.get(), containerId);
		checkContainerSize(container, ROWS * COLS);
		this.container = container;
		container.startOpen(playerInventory.player);

		for(int row = 0; row < ROWS; row++)
			for(int col = 0; col < COLS; col++)
				this.addSlot(new Slot(container, col + row * COLS, 8 + col * 18, 18 + row * 18));

		for(int row = 0; row < 3; row++)
			for(int col = 0; col < 9; col++)
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 94 + row * 18));

		for(int col = 0; col < 9; col++)
			this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 152));
	}

	@Override
	public void removed(Player player)
	{
		super.removed(player);
		container.stopOpen(player);
	}

	@Override
	public boolean stillValid(Player player)
	{
		return true;
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

			int voidSlots = ROWS * COLS;
			if(index < voidSlots)
			{
				if(!moveItemStackTo(stackInSlot, voidSlots, slots.size(), true))
					return ItemStack.EMPTY;
			}
			else if(!moveItemStackTo(stackInSlot, 0, voidSlots, false))
				return ItemStack.EMPTY;

			if(stackInSlot.isEmpty())
				slot.set(ItemStack.EMPTY);
			else
				slot.setChanged();
		}
		return result;
	}
}
