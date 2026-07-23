package org.wilkretawesomesauce.minestuckuniverseported.strife;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code com.cibernet.minestuckuniverse.strife.StrifeSpecibus}.
 * <p>
 * One slot in an entity's strife portfolio: a {@link KindAbstratus} plus the stack(s) of matching items
 * currently assigned to it.
 * <p>
 * Difference from the original: the old code stamped a {@code "StrifeAssigned"} boolean directly onto
 * an item stack's NBT to mark it as "currently equipped from a specibus" (used elsewhere to stop players
 * dragging equipped weapons around). That's replaced in this port by tracking assignment purely through
 * portfolio state (which specibus/slot is selected) rather than mutating the item stack &mdash; a cleaner
 * fit for the modern data-component item model, and something {@link StrifePortfolioHandler} (Phase 2)
 * will need to account for.
 */
public class StrifeSpecibus implements INBTSerializable<CompoundTag>
{
	private final LinkedList<ItemStack> items = new LinkedList<>();
	@Nullable
	private KindAbstratus kindAbstratus;
	private String customName = "";

	public StrifeSpecibus(@Nullable KindAbstratus kindAbstratus)
	{
		this.kindAbstratus = kindAbstratus;
	}

	public static StrifeSpecibus empty()
	{
		return new StrifeSpecibus(null);
	}

	public boolean putItemStack(ItemStack stack)
	{
		return putItemStack(stack, -1);
	}

	public boolean putItemStack(ItemStack stack, int slot)
	{
		if(stack.isEmpty() || kindAbstratus == null || !kindAbstratus.isStackCompatible(stack))
			return false;
		if(org.wilkretawesomesauce.minestuckuniverseported.Config.strifeDeckMaxSize >= 0
				&& items.size() >= org.wilkretawesomesauce.minestuckuniverseported.Config.strifeDeckMaxSize)
			return false;

		if(slot == -1)
			items.add(stack);
		else
			items.add(slot, stack);
		return true;
	}

	public boolean unassign(ItemStack stack)
	{
		int index = indexOfSame(stack);
		return index >= 0 && unassign(index);
	}

	public boolean unassign(int index)
	{
		if(index < 0 || index >= items.size())
			return false;
		items.remove(index);
		return true;
	}

	public ItemStack retrieveStack(ItemStack stack)
	{
		int index = indexOfSame(stack);
		return index < 0 ? ItemStack.EMPTY : retrieveStack(index);
	}

	public ItemStack retrieveStack(int index)
	{
		if(index < 0 || index >= items.size())
			return ItemStack.EMPTY;
		return items.get(index).copy();
	}

	/**
	 * Looser than {@link ItemStack#matches}: same item type only, ignoring components like durability.
	 * <p>
	 * Durability changes constantly while a weapon is actually being used in combat - that's the whole
	 * point of durability - so comparing full component equality against a stack still sitting untouched
	 * in the deck was causing the currently-armed weapon to spuriously stop "matching" the instant it took
	 * damage, which made {@code StrifePortfolioEvents#checkArmed} think it had left the player's hand and
	 * unassign it mid-fight. With {@code restrictedStrife} on, that then blocked all further attacks since
	 * nothing was allocated anymore - "hit once, unhittable after that".
	 */
	public static boolean sameWeapon(ItemStack a, ItemStack b)
	{
		return !a.isEmpty() && !b.isEmpty() && ItemStack.isSameItem(a, b);
	}

	private int indexOfSame(ItemStack stack)
	{
		for(int i = 0; i < items.size(); i++)
			if(sameWeapon(items.get(i), stack))
				return i;
		return -1;
	}

	/** Reassigns this specibus to a different kind, kicking out any contents that no longer match. */
	public void switchKindAbstratus(@Nullable KindAbstratus abstratus, @Nullable java.util.function.Consumer<ItemStack> onEjected)
	{
		if(abstratus == kindAbstratus)
			return;
		kindAbstratus = abstratus;

		for(ItemStack stack : new ArrayList<>(items))
			if(abstratus == null || !abstratus.isStackCompatible(stack))
			{
				items.remove(stack);
				if(onEjected != null)
					onEjected.accept(stack);
			}
	}

	public LinkedList<ItemStack> getContents()
	{
		return items;
	}

	@Nullable
	public KindAbstratus getKindAbstratus()
	{
		return kindAbstratus;
	}

	public boolean isAssigned()
	{
		return kindAbstratus != null;
	}

	public String getCustomName()
	{
		return customName;
	}

	public void setCustomName(String customName)
	{
		this.customName = customName.trim();
	}

	public boolean hasCustomName()
	{
		return customName != null && !customName.isEmpty();
	}

	/** Full display name: the custom name if set, otherwise the kind's localized name, else "". */
	public String getDisplayName()
	{
		if(hasCustomName())
			return customName;
		return kindAbstratus == null ? "" : kindAbstratus.getDisplayName().getString();
	}

	/** Same as {@link #getDisplayName()} but lowercased and truncated to fit on the card art. */
	public String getDisplayNameForCard()
	{
		String name = getDisplayName().toLowerCase();
		if(name.length() > 12)
			name = name.substring(0, 9) + "...";
		return name;
	}

	@Override
	public String toString()
	{
		return kindAbstratus + " " + items;
	}

	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider provider)
	{
		CompoundTag nbt = new CompoundTag();
		if(isAssigned())
		{
			nbt.putString("KindAbstratus", kindAbstratus.getRegistryName().toString());

			ListTag contents = new ListTag();
			for(ItemStack stack : items)
				contents.add(stack.save(provider));
			nbt.put("Contents", contents);
		}
		if(hasCustomName())
			nbt.putString("CustomName", customName);
		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt)
	{
		items.clear();
		kindAbstratus = null;

		if(nbt.contains("KindAbstratus"))
			kindAbstratus = MSUKindAbstrataRegistry.get(ResourceLocation.parse(nbt.getString("KindAbstratus")));

		if(isAssigned() && nbt.contains("Contents"))
		{
			ListTag contents = nbt.getList("Contents", Tag.TAG_COMPOUND);
			for(int i = 0; i < contents.size(); i++)
			{
				ItemStack stack = ItemStack.parseOptional(provider, contents.getCompound(i));
				if(!stack.isEmpty())
					items.add(stack);
			}
		}

		customName = nbt.contains("CustomName") ? nbt.getString("CustomName") : "";
	}
}
