package org.wilkretawesomesauce.minestuckuniverseported.juju;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

import java.util.function.Supplier;

/**
 * Registration hub for the Juju collectible item ("Cue Ball", ported from the original's real
 * {@code MinestuckUniverseItems.cueBall}) and the {@link JujuLootCondition} type. The Juju Modus item/
 * {@code ModusType} and its link/unlink/stash mechanic (formerly {@code juju.JujuModus}/{@code JujuMenu}/
 * {@code command.JujuCommand}) were removed by explicit request - Cue Ball and its drop-once-per-world
 * loot condition are unrelated to that mechanic and stay.
 */
public final class MSUJujuRegistry
{
	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Minestuckuniverseported.MODID);

	public static final DeferredHolder<Item, Item> CUE_BALL = ITEMS.register("cue_ball", () -> new Item(new Item.Properties()));

	public static final DeferredRegister<LootItemConditionType> LOOT_CONDITIONS =
			DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, Minestuckuniverseported.MODID);

	public static final Supplier<LootItemConditionType> JUJU_CONDITION =
			LOOT_CONDITIONS.register("juju", () -> new LootItemConditionType(JujuLootCondition.CODEC));

	public static final DeferredRegister<MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS =
			DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Minestuckuniverseported.MODID);

	public static final Supplier<MapCodec<JujuChestLootModifier>> CHEST_LOOT_MODIFIER =
			LOOT_MODIFIER_SERIALIZERS.register("chest_juju", () -> JujuChestLootModifier.CODEC);

	private MSUJujuRegistry()
	{
	}
}
