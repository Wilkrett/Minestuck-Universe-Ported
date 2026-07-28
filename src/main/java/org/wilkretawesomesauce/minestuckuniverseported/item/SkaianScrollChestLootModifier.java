package org.wilkretawesomesauce.minestuckuniverseported.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code world.storage.loot.MSULoot#onLootInject} "General" chest
 * branch, which pulled a Skaian Scroll entry (rolled via {@code SetRandomSkill}) out of
 * {@code inject/medium_loot.json} into the {@code minestuck:chests/medium_basic/general} table's "misc"
 * pool. Modern equivalent, same pattern as {@code juju.JujuChestLootModifier}: a data-driven
 * {@link net.neoforged.neoforge.common.loot.IGlobalLootModifier}, gated to that one specific vanilla/
 * Minestuck loot table id rather than every table whose path merely contains "chests" (the original
 * scoped this injection to the General table specifically, unlike its sibling Cue Ball injection).
 * <p>
 * See {@code data/minestuckuniverseported/neoforge/loot_modifiers/skaian_scroll.json} for the actual
 * instance (chance, matching the original's 0.05) and
 * {@code data/neoforge/loot_modifiers/global_loot_modifiers.json} for how it's globally activated.
 */
public class SkaianScrollChestLootModifier extends LootModifier
{
	private static final ResourceLocation GENERAL_TABLE = ResourceLocation.fromNamespaceAndPath("minestuck", "chests/medium_basic/general");

	public static final MapCodec<SkaianScrollChestLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
			codecStart(instance).apply(instance, SkaianScrollChestLootModifier::new));

	public SkaianScrollChestLootModifier(LootItemCondition[] conditions)
	{
		super(conditions);
	}

	@Override
	protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context)
	{
		if(GENERAL_TABLE.equals(context.getQueriedLootTableId()))
		{
			ItemStack stack = new ItemStack(org.wilkretawesomesauce.minestuckuniverseported.MSUItems.SKAIAN_SCROLL.get());
			SkaianScrollItem.storeRandomTech(stack, context.getRandom());
			generatedLoot.add(stack);
		}

		return generatedLoot;
	}

	@Override
	public MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier> codec()
	{
		return org.wilkretawesomesauce.minestuckuniverseported.item.MSUSkaianScrollRegistry.CHEST_LOOT_MODIFIER.get();
	}
}
