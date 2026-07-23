package org.wilkretawesomesauce.minestuckuniverseported;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Map;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code ItemGTArmor.MATERIAL}: a Forge
 * {@code EnumHelper.addArmorMaterial} with all-zero defense values ({@code new int[] {0,0,0,0}}), i.e.
 * purely cosmetic armor. Modern NeoForge/Minecraft's unified {@link ArmorMaterial} record replaces
 * {@code EnumHelper}; the registration pattern here mirrors Minestuck's own (see
 * {@code MSItemTypes#registerArmorMaterial}), notably its own zero-defense "cloth" material as a direct
 * precedent for this exact use case.
 */
public final class MSUArmorMaterials
{
	public static final DeferredRegister<ArmorMaterial> REGISTER = DeferredRegister.create(Registries.ARMOR_MATERIAL, Minestuckuniverseported.MODID);

	public static final Holder<ArmorMaterial> GOD_TIER = REGISTER.register("god_tier", () -> new ArmorMaterial(
			Map.of(ArmorItem.Type.BOOTS, 0, ArmorItem.Type.LEGGINGS, 0, ArmorItem.Type.CHESTPLATE, 0, ArmorItem.Type.HELMET, 0),
			0,
			SoundEvents.ARMOR_EQUIP_LEATHER,
			() -> Ingredient.EMPTY,
			List.of(new ArmorMaterial.Layer(Minestuckuniverseported.id("god_tier"))),
			0.0F,
			0.0F
	));

	// Cosmetic, zero-defense materials for consort.ConsortHatItems - ported from MinestuckUniverse
	// (1.12.2)'s shared "materialCloth" (also all-zero defense). Each hat gets its own Layer since,
	// unlike GOD_TIER's per-class texture-swap trick, these are just distinct physical hats.
	public static final Holder<ArmorMaterial> WIZARD_HAT = REGISTER.register("wizard_hat", () -> new ArmorMaterial(
			Map.of(ArmorItem.Type.HELMET, 0),
			0,
			SoundEvents.ARMOR_EQUIP_LEATHER,
			() -> Ingredient.EMPTY,
			List.of(new ArmorMaterial.Layer(Minestuckuniverseported.id("wizard_hat"))),
			0.0F,
			0.0F
	));

	public static final Holder<ArmorMaterial> FROG_HAT = REGISTER.register("frog_hat", () -> new ArmorMaterial(
			Map.of(ArmorItem.Type.HELMET, 0),
			0,
			SoundEvents.ARMOR_EQUIP_LEATHER,
			() -> Ingredient.EMPTY,
			List.of(new ArmorMaterial.Layer(Minestuckuniverseported.id("frog_hat"))),
			0.0F,
			0.0F
	));

	private MSUArmorMaterials()
	{
	}
}
