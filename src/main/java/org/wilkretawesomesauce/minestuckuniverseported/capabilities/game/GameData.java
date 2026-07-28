package org.wilkretawesomesauce.minestuckuniverseported.capabilities.game;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.HashSet;
import java.util.Set;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code gui.container.InventoryItemVoid} +
 * {@code capabilities.game.GameData}/{@code IGameData} in full - real name match now (this class used to
 * be called {@code ItemVoidData}; renamed for real, matching the original's own class name exactly).
 * Holds both the item-void inventory below and, via {@link #hasJujuSpawned}/{@link #setJujuSpawned}, the
 * original's {@code jujuSpawns} tracking (consulted by {@code juju.JujuLootCondition} so a given Juju item
 * only ever drops once per world). Kept in one class rather than split across two attachments, matching
 * the original bundling both concerns into one {@code GameData} capability - {@link IGameData} itself is
 * empty in the original (just an {@code IMSUCapabilityBase} marker, no real declared methods), matched
 * here the same way. A real per-{@code Level}
 * attachment (see {@link org.wilkretawesomesauce.minestuckuniverseported.MSUAttachments#ITEM_VOID}), but
 * only ever fetched from the Overworld specifically (mirroring the original's own single-instance,
 * dimension-0-only capability) - see {@code itemvoid.ItemVoidCommand} for the one real caller.
 * <p>
 * 27 slots. {@link #addItem} tries to merge into an existing matching stack first; failing that, it fills
 * the first empty slot; failing <i>that</i> (void full of 27 distinct stacks), it shifts every slot down by
 * one and appends the new stack at the end - <b>silently discarding whatever was in slot 0</b>, faithfully
 * matching the original (things really do fall out of the Void forever once it's full).
 * <p>
 * <b>Deliberate deviation, not a bug fix on autopilot</b>: the original's real {@code removeStackFromSlot}
 * backfilled every emptied slot with a {@code minestuck:generic_object} placeholder ("the void is never
 * truly empty anywhere") - this port used to do the same via a {@code backfillIfEmptied} helper, but a real
 * player report found it reads as the game silently swapping your item for junk, and (confirmed separately
 * while investigating) the backfill was already inconsistent besides: vanilla's own
 * {@code AbstractContainerMenu#moveItemStackTo} (the shift-click path) mutates a slot's {@link ItemStack}
 * count directly rather than calling {@link #removeItem}/{@link #removeItemNoUpdate}, so shift-click
 * extraction never triggered the backfill even before it was removed, while a normal click did - two
 * different visible behaviors depending on click type. Removed outright rather than made consistent, per
 * explicit direction: slots are genuinely empty once taken from, full stop, no placeholder either way.
 * <p>
 * <b>Known gap, same category as {@code godtier.MediumData}'s own Quest Bed one</b>: the original's real
 * caller was {@code TechVoidGrasp}, a Void-aspect ability that isn't ported anywhere in this project yet
 * (Void is one of the ten still-unstarted {@code heroAspect} packages). {@link #addItem} is real, ready
 * infrastructure for whenever that ability gets built - nothing currently calls it.
 */
public class GameData implements IGameData, Container, INBTSerializable<CompoundTag>
{
	private static final int SLOT_COUNT = 27;

	private final ItemStack[] slots = new ItemStack[SLOT_COUNT];
	private final Set<Item> jujuSpawns = new HashSet<>();

	public GameData()
	{
		java.util.Arrays.fill(slots, ItemStack.EMPTY);
	}

	/** Mirrors the original's static {@code GameData.hasJujuSpawned}/{@code setJujuSpawned}. */
	public boolean hasJujuSpawned(Item juju)
	{
		return jujuSpawns.contains(juju);
	}

	public void setJujuSpawned(Item juju, boolean spawned)
	{
		if(spawned)
			jujuSpawns.add(juju);
		else
			jujuSpawns.remove(juju);
	}

	/** Mirrors the original's {@code InventoryItemVoid#addItem}. */
	public void addItem(ItemStack stack)
	{
		ItemStack remaining = stack.copy();

		for(int i = 0; i < SLOT_COUNT; i++)
		{
			ItemStack existing = slots[i];

			if(existing.isEmpty())
			{
				slots[i] = remaining;
				setChanged();
				return;
			}

			if(ItemStack.isSameItemSameComponents(existing, remaining))
			{
				int room = Math.min(existing.getMaxStackSize(), getMaxStackSize()) - existing.getCount();
				int move = Math.min(remaining.getCount(), Math.max(0, room));
				if(move > 0)
				{
					existing.grow(move);
					remaining.shrink(move);
					if(remaining.isEmpty())
					{
						setChanged();
						return;
					}
				}
			}
		}

		// Full of 27 distinct stacks - the oldest (slot 0) falls out of the Void forever.
		System.arraycopy(slots, 1, slots, 0, SLOT_COUNT - 1);
		slots[SLOT_COUNT - 1] = remaining;
		setChanged();
	}

	@Override
	public int getContainerSize()
	{
		return SLOT_COUNT;
	}

	@Override
	public boolean isEmpty()
	{
		for(ItemStack stack : slots)
			if(!stack.isEmpty())
				return false;
		return true;
	}

	@Override
	public ItemStack getItem(int slot)
	{
		return slots[slot];
	}

	@Override
	public ItemStack removeItem(int slot, int amount)
	{
		ItemStack result = slots[slot].split(amount);
		setChanged();
		return result;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot)
	{
		ItemStack result = slots[slot];
		slots[slot] = ItemStack.EMPTY;
		setChanged();
		return result;
	}

	@Override
	public void setItem(int slot, ItemStack stack)
	{
		slots[slot] = stack;
		setChanged();
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
		java.util.Arrays.fill(slots, ItemStack.EMPTY);
	}

	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider provider)
	{
		CompoundTag nbt = new CompoundTag();
		ListTag list = new ListTag();
		for(int i = 0; i < SLOT_COUNT; i++)
		{
			if(slots[i].isEmpty())
				continue;

			CompoundTag slotNbt = new CompoundTag();
			slotNbt.putByte("Slot", (byte) i);
			slotNbt.put("Item", slots[i].save(provider));
			list.add(slotNbt);
		}
		nbt.put("Items", list);

		ListTag jujuList = new ListTag();
		for(Item juju : jujuSpawns)
		{
			ResourceLocation id = BuiltInRegistries.ITEM.getKey(juju);
			jujuList.add(StringTag.valueOf(id.toString()));
		}
		nbt.put("JujuSpawns", jujuList);

		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt)
	{
		java.util.Arrays.fill(slots, ItemStack.EMPTY);
		ListTag list = nbt.getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND);
		for(int i = 0; i < list.size(); i++)
		{
			CompoundTag slotNbt = list.getCompound(i);
			int slot = slotNbt.getByte("Slot") & 255;
			if(slot < SLOT_COUNT)
				slots[slot] = ItemStack.parseOptional(provider, slotNbt.getCompound("Item"));
		}

		jujuSpawns.clear();
		ListTag jujuList = nbt.getList("JujuSpawns", net.minecraft.nbt.Tag.TAG_STRING);
		for(int i = 0; i < jujuList.size(); i++)
			jujuSpawns.add(BuiltInRegistries.ITEM.get(ResourceLocation.parse(jujuList.getString(i))));
	}
}
