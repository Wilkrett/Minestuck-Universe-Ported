package org.wilkretawesomesauce.minestuckuniverseported;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.wilkretawesomesauce.minestuckuniverseported.items.StrifeCardItem;

/**
 * Items registry for this addon. Starts with just what the strife specibus system needs; more of
 * MinestuckUniverse's 1.12.2 item list (com.cibernet.minestuckuniverse.items.MinestuckUniverseItems)
 * will land here as further subsystems get ported.
 */
public final class MSUItems
{
	public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(Minestuckuniverseported.MODID);

	public static final DeferredItem<Item> STRIFE_CARD = REGISTER.register("strife_card",
			() -> new StrifeCardItem(new Item.Properties()));

	public static final DeferredItem<Item> GOD_TIER_HOOD = REGISTER.register("god_tier_hood",
			() -> new org.wilkretawesomesauce.minestuckuniverseported.items.GodTierArmorItem(MSUArmorMaterials.GOD_TIER, ArmorItem.Type.HELMET, new Item.Properties()));
	public static final DeferredItem<Item> GOD_TIER_SHIRT = REGISTER.register("god_tier_shirt",
			() -> new org.wilkretawesomesauce.minestuckuniverseported.items.GodTierArmorItem(MSUArmorMaterials.GOD_TIER, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
	public static final DeferredItem<Item> GOD_TIER_PANTS = REGISTER.register("god_tier_pants",
			() -> new org.wilkretawesomesauce.minestuckuniverseported.items.GodTierArmorItem(MSUArmorMaterials.GOD_TIER, ArmorItem.Type.LEGGINGS, new Item.Properties()));
	public static final DeferredItem<Item> GOD_TIER_SHOES = REGISTER.register("god_tier_shoes",
			() -> new org.wilkretawesomesauce.minestuckuniverseported.items.GodTierArmorItem(MSUArmorMaterials.GOD_TIER, ArmorItem.Type.BOOTS, new Item.Properties()));

	// consort.ConsortHatEvents' hat-spawn pool - see that class's own doc comment. crumply_hat isn't
	// registered here at all: it's a real, already-ported Minestuck item (com.mraof.minestuck.item.MSItems
	// .CRUMPLY_HAT) reused directly rather than duplicated, per this project's established "check
	// Minestuck's own dependency jar before building from scratch" methodology.
	public static final DeferredItem<Item> WIZARD_HAT = REGISTER.register("wizard_hat",
			() -> new ArmorItem(MSUArmorMaterials.WIZARD_HAT, ArmorItem.Type.HELMET, new Item.Properties()));
	public static final DeferredItem<Item> FROG_HAT = REGISTER.register("frog_hat",
			() -> new ArmorItem(MSUArmorMaterials.FROG_HAT, ArmorItem.Type.HELMET, new Item.Properties()));

	// Ported from MinestuckUniverse (1.12.2)'s "Needlewand" (items.MinestuckUniverseItems#needlewands) -
	// see beam.BeamWeaponItem's own doc comment for why this is the only one of several original beam
	// weapon items ported this pass.
	public static final DeferredItem<Item> NEEDLEWAND = REGISTER.register("needlewand",
			() -> new org.wilkretawesomesauce.minestuckuniverseported.beam.BeamWeaponItem(
					new Item.Properties().durability(488), 0.05f, 10f, 1f, 60, 15, "needlewand"));

	// Ported from MinestuckUniverse (1.12.2)'s items.ItemManipulatedMatter - see that class's own doc
	// comment (abilitech.heroAspect.space.TechSpaceManipulator captures the region that fills it).
	public static final DeferredItem<Item> MANIPULATED_MATTER = REGISTER.register("manipulated_matter",
			() -> new org.wilkretawesomesauce.minestuckuniverseported.items.ManipulatedMatterItem(new Item.Properties().stacksTo(1)));

	public static final DeferredItem<net.minecraft.world.item.BlockItem> ABILITECHNOSYNTH = REGISTER.register("abilitechnosynth",
			() -> new org.wilkretawesomesauce.minestuckuniverseported.items.AbilitechnosynthItem(MSUBlocks.ABILITECHNOSYNTH.get(), new Item.Properties()));
	public static final DeferredItem<net.minecraft.world.item.BlockItem> TEMPORAL_SENDIFICATOR = REGISTER.registerSimpleBlockItem("temporal_sendificator", MSUBlocks.TEMPORAL_SENDIFICATOR);

	// Deliberately no BlockItem for Chloroball - confirmed via the real original source
	// (blocks.MinestuckUniverseBlocks#registerBlock(registry, chloroball, null), whose null ItemBlock
	// argument skips adding it to MinestuckUniverseItems.itemBlocks entirely): the original never gave
	// this block a placeable/holdable item form either. It only ever exists via
	// heroAspect.life.TechLifeChloroball's own direct level.setBlockAndUpdate(...) call - never given,
	// dropped, or manually placed.

	// Real icon/model assets were already sitting in this project's resources unused (same dangling
	// minestuckuniverse: model-texture-namespace bug already fixed for other bulk-imported items) - wired
	// for real now that badges.BadgeKarma needs it for its own real unlock cost.
	public static final DeferredItem<Item> MOONSTONE = REGISTER.registerSimpleItem("moonstone");

	private MSUItems()
	{
	}
}
