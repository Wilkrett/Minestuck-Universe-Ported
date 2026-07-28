package org.wilkretawesomesauce.minestuckuniverseported.juju;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMenuTypes;

/**
 * Real GUI for viewing/withdrawing from a linked Juju partner's stash - see {@code juju.JujuModus}'s own
 * doc comment for why this is a new, purpose-built {@link AbstractContainerMenu} (this project's own
 * established real-menu pattern, already used by {@code inventory.TemporalSendificatorMenu}/
 * {@code itemvoid.ItemVoidMenu}) rather than Minestuck's own internal {@code ArraySylladexScreen}: that
 * screen displays whatever {@code Modus#getItems()} returns, which for a faithful {@code JujuModus} has to
 * stay "my own contributed items" (dropped on death, saved to disk, etc. all rely on that meaning), not the
 * partner's withdrawable stash the original's own custom {@code JujuGuiHandler} specifically showed instead
 * - reusing the generic screen unmodified would have shown players the wrong grid.
 * <p>
 * {@link PartnerStashContainer} is a real, live display, not a random copy: {@link #broadcastChanges()}
 * refreshes it from {@link JujuModus#getPartnerItems} every server tick the menu is open (same polling
 * shape {@code TemporalSendificatorMenu} already uses), and clicking a slot to take a stack routes through
 * {@link JujuModus#getItem} for the real redirect/cross-sync logic - not a raw vanilla slot removal, which
 * would only mutate this display copy and desync from the actual partner stash. Putting items in doesn't
 * need any UI here at all: that already happens through Minestuck's own normal item-captchalogue action,
 * which calls into this modus's inherited {@code ArrayModus#putItemStack} on its own.
 */
public class JujuMenu extends AbstractContainerMenu
{
	private static final int ROWS = 3, COLS = 9;

	private final PartnerStashContainer container;

	public JujuMenu(int containerId, Inventory playerInventory)
	{
		this(containerId, playerInventory, new PartnerStashContainer(null, null));
	}

	public JujuMenu(int containerId, Inventory playerInventory, PartnerStashContainer container)
	{
		super(MSUMenuTypes.JUJU.get(), containerId);
		this.container = container;

		for(int row = 0; row < ROWS; row++)
			for(int col = 0; col < COLS; col++)
				this.addSlot(new Slot(container, col + row * COLS, 8 + col * 18, 18 + row * 18)
				{
					@Override
					public boolean mayPlace(ItemStack stack)
					{
						return false;
					}
				});

		for(int row = 0; row < 3; row++)
			for(int col = 0; col < 9; col++)
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));

		for(int col = 0; col < 9; col++)
			this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
	}

	@Override
	public void broadcastChanges()
	{
		container.refresh();
		super.broadcastChanges();
	}

	@Override
	public boolean stillValid(Player player)
	{
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index)
	{
		int stashSlots = ROWS * COLS;
		if(index < stashSlots)
		{
			Slot slot = slots.get(index);
			if(!slot.hasItem())
				return ItemStack.EMPTY;

			ItemStack taken = slot.remove(slot.getItem().getCount());
			if(!taken.isEmpty() && !player.getInventory().add(taken))
				player.drop(taken, false);
		}
		return ItemStack.EMPTY;
	}

	/** Backing {@link Container} for {@link #container} - real live partner-stash view, see this class's own doc comment. */
	public static class PartnerStashContainer implements Container
	{
		private final JujuModus modus;
		private final ServerPlayer player;
		private ItemStack[] display = new ItemStack[27];

		public PartnerStashContainer(JujuModus modus, ServerPlayer player)
		{
			this.modus = modus;
			this.player = player;
			java.util.Arrays.fill(display, ItemStack.EMPTY);
		}

		void refresh()
		{
			if(modus == null || player == null)
				return;

			NonNullList<ItemStack> partnerItems = modus.getPartnerItems(player.server);
			java.util.Arrays.fill(display, ItemStack.EMPTY);
			for(int i = 0; i < Math.min(display.length, partnerItems.size()); i++)
				display[i] = partnerItems.get(i);
		}

		@Override
		public int getContainerSize()
		{
			return display.length;
		}

		@Override
		public boolean isEmpty()
		{
			for(ItemStack stack : display)
				if(!stack.isEmpty())
					return false;
			return true;
		}

		@Override
		public ItemStack getItem(int slot)
		{
			return display[slot];
		}

		@Override
		public ItemStack removeItem(int slot, int amount)
		{
			if(modus == null || player == null || display[slot].isEmpty())
				return ItemStack.EMPTY;

			ItemStack taken = modus.getItem(player, slot, false);
			refresh();
			return taken;
		}

		@Override
		public ItemStack removeItemNoUpdate(int slot)
		{
			return removeItem(slot, Integer.MAX_VALUE);
		}

		@Override
		public void setItem(int slot, ItemStack stack)
		{
		}

		@Override
		public void setChanged()
		{
		}

		@Override
		public boolean stillValid(Player player)
		{
			return true;
		}

		@Override
		public void clearContent()
		{
		}
	}
}
