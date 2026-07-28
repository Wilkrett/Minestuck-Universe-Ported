package org.wilkretawesomesauce.minestuckuniverseported.juju;

import com.mojang.serialization.MapCodec;
import com.mraof.minestuck.inventory.captchalogue.ModusType;
import com.mraof.minestuck.inventory.captchalogue.ModusTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

import java.util.function.Supplier;

/**
 * Registration hub for everything {@code juju.JujuModus} needs: the {@link ModusType} itself (registered
 * into Minestuck's own real {@code ModusTypes.REGISTER} - confirmed a real {@code DeferredRegister} an
 * addon can register into, not something exclusive to Minestuck's own built-in moduses), its physical card
 * item, the Juju collectible item ("Cue Ball", ported from the original's real
 * {@code MinestuckUniverseItems.cueBall}), and the {@link JujuLootCondition} type.
 */
public final class MSUJujuRegistry
{
	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Minestuckuniverseported.MODID);

	public static final DeferredHolder<Item, Item> JUJU_MODUS_ITEM = ITEMS.register("juju_modus", () -> new Item(new Item.Properties()));
	public static final DeferredHolder<Item, Item> CUE_BALL = ITEMS.register("cue_ball", () -> new Item(new Item.Properties()));

	public static final DeferredHolder<ModusType<?>, ModusType<JujuModus>> JUJU_MODUS_TYPE =
			ModusTypes.REGISTER.register("juju", () -> new ModusType<>(JujuModus::new, JUJU_MODUS_ITEM::get));

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
