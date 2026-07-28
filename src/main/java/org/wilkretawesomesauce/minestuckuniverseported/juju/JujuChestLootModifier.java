package org.wilkretawesomesauce.minestuckuniverseported.juju;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code world.storage.loot.MSULoot#onLootInject} "Inject Cue
 * Ball" branch - the original fired on every {@code LootTableLoadEvent} and appended a Cue Ball pool
 * (gated by {@code JujuLootCondition}) into any loot table whose resource path contained "chests". Modern
 * NeoForge's real equivalent of ad hoc loot-table-load injection is a data-driven
 * {@link net.neoforged.neoforge.common.loot.IGlobalLootModifier} - the "table path contains chests" check
 * itself stays plain Java here (not a registered, reusable condition), matching the original: that check
 * was ad hoc code in the event handler, not one of the original's own registered
 * {@code LootCondition.Serializer} types either (only the chance/spawn-tracking piece,
 * {@link JujuLootCondition}, was a real reusable condition in the original).
 * <p>
 * See {@code data/minestuckuniverseported/neoforge/loot_modifiers/cue_ball.json} for the actual instance
 * (chance 0.05, matching the original exactly) and
 * {@code data/neoforge/loot_modifiers/global_loot_modifiers.json} for how it's globally activated.
 */
public class JujuChestLootModifier extends LootModifier
{
	public static final MapCodec<JujuChestLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
			codecStart(instance).apply(instance, JujuChestLootModifier::new));

	public JujuChestLootModifier(LootItemCondition[] conditions)
	{
		super(conditions);
	}

	@Override
	protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context)
	{
		if(context.getQueriedLootTableId().getPath().toLowerCase().contains("chests"))
			generatedLoot.add(new ItemStack(MSUJujuRegistry.CUE_BALL.get()));

		return generatedLoot;
	}

	@Override
	public MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier> codec()
	{
		return MSUJujuRegistry.CHEST_LOOT_MODIFIER.get();
	}
}
