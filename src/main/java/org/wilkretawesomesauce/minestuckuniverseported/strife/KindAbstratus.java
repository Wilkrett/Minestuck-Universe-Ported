package org.wilkretawesomesauce.minestuckuniverseported.strife;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Ported from MinestuckUniverse (Forge 1.12.2)'s {@code com.cibernet.minestuckuniverse.strife.KindAbstratus}.
 * <p>
 * Represents a "weapon kind" (bladekind, hammerkind, etc.) that a {@link StrifeSpecibus} can be assigned to.
 * An entity may only hold items compatible with a specibus' assigned kind in that specibus.
 * <p>
 * Differences from the 1.12.2 original, and why:
 * <ul>
 *     <li>No longer a Forge {@code IForgeRegistryEntry}. Registered into {@link MSUKindAbstrataRegistry}
 *     instead of a true NeoForge {@code Registry<KindAbstratus>}. A real registry (via
 *     {@code NewRegistryEvent}/{@code RegistryBuilder}) could replace this later if datapack or
 *     cross-mod extensibility becomes a requirement, but a simple registrar is lower risk to get
 *     compiling first.</li>
 *     <li>Tool-class matching (the old {@code MSUToolClass}/{@code IClassedTool}, built on 1.12.2's
 *     {@code ItemTool#getToolMaterialName()}) has been removed outright &mdash; modern items don't expose
 *     tool materials that way. Matching now relies on item classes, explicit item references, keyword
 *     search against the item's registry path, and item tags.</li>
 *     <li>OreDictionary-based loose matching has been replaced by item tags ({@link #addTag}), which is
 *     the direct modern equivalent and - unlike everything else here - is genuinely datapack-editable: a
 *     pack maker (or another mod's compat datapack) can add items to a kind by shipping a tag JSON, with
 *     no code changes and no dependency on this mod's classes. See {@link MSUKindAbstrata} for how the
 *     default kinds use this.</li>
 * </ul>
 */
public class KindAbstratus implements Comparable<KindAbstratus>
{
	private static int idAt = 0;

	private final ResourceLocation registryName;
	private final int id;

	private boolean hidden = false;
	private boolean isFist = false;
	private boolean preventRightClick = false;

	private final List<Class<? extends Item>> itemClasses = new ArrayList<>();
	private final List<Supplier<? extends Item>> toolItems = new ArrayList<>();
	private final List<Supplier<? extends Item>> itemBlacklist = new ArrayList<>();
	private final List<String> keywords = new ArrayList<>();
	private final List<TagKey<Item>> tags = new ArrayList<>();

	@Nullable
	private Conditional conditional;

	public KindAbstratus(ResourceLocation registryName)
	{
		this.registryName = registryName;
		this.id = idAt++;
	}

	public ResourceLocation getRegistryName()
	{
		return registryName;
	}

	public boolean isHidden()
	{
		return hidden;
	}

	public KindAbstratus setHidden(boolean hidden)
	{
		this.hidden = hidden;
		return this;
	}

	public boolean isFist()
	{
		return isFist;
	}

	public KindAbstratus setFist(boolean isFist)
	{
		this.isFist = isFist;
		return this;
	}

	public boolean preventsRightClick()
	{
		return preventRightClick;
	}

	public KindAbstratus setPreventRightClick(boolean prevents)
	{
		this.preventRightClick = prevents;
		return this;
	}

	public KindAbstratus setConditional(Conditional conditional)
	{
		this.conditional = conditional;
		return this;
	}

	@Nullable
	public Conditional getConditional()
	{
		return conditional;
	}

	@SafeVarargs
	public final KindAbstratus addItemClasses(Class<? extends Item>... itemClasses)
	{
		for(Class<? extends Item> ic : itemClasses)
			if(ic != null && !this.itemClasses.contains(ic))
				this.itemClasses.add(ic);
		return this;
	}

	@SafeVarargs
	public final KindAbstratus addItems(Supplier<? extends Item>... items)
	{
		for(Supplier<? extends Item> i : items)
			if(i != null)
				this.toolItems.add(i);
		return this;
	}

	public KindAbstratus addItem(Item item)
	{
		return addItems(() -> item);
	}

	public KindAbstratus addItems(Item... items)
	{
		for(Item item : items)
			addItem(item);
		return this;
	}

	/**
	 * Clears everything a datapack {@code strife_kinds} JSON file can control (item references and
	 * keywords), leaving item-class matching and conditionals (Java-only concepts) untouched. Used by
	 * the data loader when a JSON file sets {@code "replace": true}, so re-applying that file after a
	 * datapack change doesn't just keep accumulating old entries.
	 */
	public KindAbstratus clearDataDrivenMatchers()
	{
		toolItems.clear();
		keywords.clear();
		return this;
	}

	public KindAbstratus addKeywords(String... keys)
	{
		for(String i : keys)
			if(i != null && !i.isEmpty() && !this.keywords.contains(i))
				this.keywords.add(i);
		return this;
	}

	public KindAbstratus addTag(TagKey<Item> tag)
	{
		this.tags.add(tag);
		return this;
	}

	@SafeVarargs
	public final KindAbstratus addTags(TagKey<Item>... tags)
	{
		for(TagKey<Item> tag : tags)
			addTag(tag);
		return this;
	}

	public List<TagKey<Item>> getTags()
	{
		return tags;
	}

	public List<Class<? extends Item>> getItemClasses()
	{
		return itemClasses;
	}

	public List<Supplier<? extends Item>> getToolItems()
	{
		return toolItems;
	}

	public List<String> getKeywords()
	{
		return keywords;
	}

	public boolean isStackCompatible(ItemStack stack)
	{
		if(stack.isEmpty())
			return false;

		Item item = stack.getItem();

		for(Supplier<? extends Item> blacklisted : itemBlacklist)
			if(blacklisted.get() == item)
				return applyConditional(item, stack, false);

		boolean result = false;

		for(Class<? extends Item> clzz : itemClasses)
			if(clzz.isInstance(item))
			{
				result = true;
				break;
			}

		if(!result)
			for(Supplier<? extends Item> toolItem : toolItems)
				if(toolItem.get() == item)
				{
					result = true;
					break;
				}

		if(!result)
			for(TagKey<Item> tag : tags)
				if(stack.is(tag))
				{
					result = true;
					break;
				}

		if(!result)
		{
			ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
			if(key != null)
				for(String str : keywords)
					if(key.getPath().contains(str))
					{
						result = true;
						break;
					}
		}

		return applyConditional(item, stack, result);
	}

	private boolean applyConditional(Item item, ItemStack stack, boolean result)
	{
		if(conditional != null)
			return conditional.test(item, stack, result);
		return result;
	}

	public String getTranslationKey()
	{
		return "strife." + registryName.getNamespace() + "." + registryName.getPath();
	}

	public Component getDisplayName()
	{
		return Component.translatable(getTranslationKey());
	}

	@Override
	public int compareTo(KindAbstratus o)
	{
		return this.id - o.id;
	}

	/** True if this kind has no way to ever match an item - i.e. it isn't usable yet. */
	public boolean isEmpty()
	{
		return !isFist() && conditional == null && toolItems.isEmpty() && itemClasses.isEmpty()
				&& keywords.isEmpty() && tags.isEmpty();
	}

	public boolean canSelect()
	{
		return !isHidden() && !isEmpty();
	}

	@Override
	public String toString()
	{
		return registryName.toString();
	}

	@FunctionalInterface
	public interface Conditional
	{
		/** Mirrors the 1.12.2 {@code IAbstratusConditional}: lets a kind override the base match result. */
		boolean test(Item item, ItemStack stack, boolean originalResult);
	}
}
