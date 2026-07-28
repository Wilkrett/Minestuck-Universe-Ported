package org.wilkretawesomesauce.minestuckuniverseported.item;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

import java.util.function.Supplier;

/**
 * Registration hub for {@link SkaianScrollChestLootModifier} - same pattern as
 * {@code juju.MSUJujuRegistry}'s own {@code CHEST_LOOT_MODIFIER}.
 */
public final class MSUSkaianScrollRegistry
{
	public static final DeferredRegister<MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS =
			DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Minestuckuniverseported.MODID);

	public static final Supplier<MapCodec<SkaianScrollChestLootModifier>> CHEST_LOOT_MODIFIER =
			LOOT_MODIFIER_SERIALIZERS.register("chest_skaian_scroll", () -> SkaianScrollChestLootModifier.CODEC);

	private MSUSkaianScrollRegistry()
	{
	}
}
