package org.wilkretawesomesauce.minestuckuniverseported;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.wilkretawesomesauce.minestuckuniverseported.item.GolemSpawnEggItem;
import org.wilkretawesomesauce.minestuckuniverseported.item.StrifeCardItem;

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

	// Real bug fix: the original's own 1.12.2 ItemArmor unconditionally called setMaxStackSize(1) in its
	// constructor regardless of durability - modern ArmorItem's constructor does no such thing (confirmed
	// via javap: it just forwards Properties to Item unmodified), so every armor/hat item registered with a
	// bare `new Item.Properties()` here was silently stacking to 64 (a real, reported bug - "why is all my
	// armor stackable"). The original's real GOD_TIER material used EnumHelper's maxDamageFactor=-1 trick
	// (read directly from ItemGTArmor.MATERIAL) to make it unbreakable while still being non-stackable via
	// that same constructor - .stacksTo(1) alone reproduces both real facts here (unbreakable, non-stacking)
	// without inventing a durability number the original never had.
	public static final DeferredItem<Item> GOD_TIER_HOOD = REGISTER.register("god_tier_hood",
			() -> new org.wilkretawesomesauce.minestuckuniverseported.item.GodTierArmorItem(MSUArmorMaterials.GOD_TIER, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1)));
	public static final DeferredItem<Item> GOD_TIER_SHIRT = REGISTER.register("god_tier_shirt",
			() -> new org.wilkretawesomesauce.minestuckuniverseported.item.GodTierArmorItem(MSUArmorMaterials.GOD_TIER, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1)));
	public static final DeferredItem<Item> GOD_TIER_PANTS = REGISTER.register("god_tier_pants",
			() -> new org.wilkretawesomesauce.minestuckuniverseported.item.GodTierArmorItem(MSUArmorMaterials.GOD_TIER, ArmorItem.Type.LEGGINGS, new Item.Properties().stacksTo(1)));
	public static final DeferredItem<Item> GOD_TIER_SHOES = REGISTER.register("god_tier_shoes",
			() -> new org.wilkretawesomesauce.minestuckuniverseported.item.GodTierArmorItem(MSUArmorMaterials.GOD_TIER, ArmorItem.Type.BOOTS, new Item.Properties().stacksTo(1)));

	// capabilities.consortCosmetics.ConsortHatsData's hat-spawn pool - see that class's own doc comment. crumply_hat isn't
	// registered here at all: it's a real, already-ported Minestuck item (com.mraof.minestuck.item.MSItems
	// .CRUMPLY_HAT) reused directly rather than duplicated, per this project's established "check
	// Minestuck's own dependency jar before building from scratch" methodology.
	// Real conical multi-box hat model (client.model.WizardHatModel), not vanilla's plain flat helmet
	// shape - see items.WizardHatItem's own doc comment for the rendering bug this fixes.
	// Real bug fix, same category as GOD_TIER_HOOD etc. above: the original's own real MSUArmorBase(int
	// maxUses, ...) overload (items.MinestuckUniverseItems#wizardHat, read directly) gave this a real 40-use
	// durability - .durability(40) reproduces that exactly, and (unlike GOD_TIER's .stacksTo(1)-only fix)
	// also fixes the stacking bug as a side effect, since modern Item.Properties#durability(int) still
	// implies stacksTo(1) (confirmed via javap - unchanged from 1.12.2's own ItemArmor behavior here).
	public static final DeferredItem<Item> WIZARD_HAT = REGISTER.register("wizard_hat",
			() -> new org.wilkretawesomesauce.minestuckuniverseported.item.WizardHatItem(MSUArmorMaterials.WIZARD_HAT, new Item.Properties().durability(40)));
	// Real flat multi-box hat model (client.model.FrogHatModel), not vanilla's plain flat helmet shape -
	// see items.WizardHatItem's own doc comment for the rendering bug this fixes. Real bug fix: the
	// original's own frogHat registration (items.MinestuckUniverseItems, read directly) used the plain
	// no-maxUses MSUArmorBase constructor, i.e. unbreakable via materialCloth's own real maxDamageFactor=-1
	// trick, same as GOD_TIER_HOOD etc. above - .stacksTo(1) alone, no durability() call, matches that.
	public static final DeferredItem<Item> FROG_HAT = REGISTER.register("frog_hat",
			() -> new org.wilkretawesomesauce.minestuckuniverseported.item.FrogHatItem(MSUArmorMaterials.FROG_HAT, new Item.Properties().stacksTo(1)));
	// Not part of ConsortHatsData's random HAT_SPAWN_POOL - matches the original, where this was reserved
	// exclusively for util.MSUConsorts' force-equipped skill-shop-seller Consorts, never rolled randomly.
	// Real conical multi-box hat model (client.model.ArchmageHatModel) - see items.WizardHatItem's own doc
	// comment for the rendering bug this fixes. Real bug fix: the original's own real archmageHat
	// registration (items.MinestuckUniverseItems, read directly) gave this a real 500-use durability via the
	// same MSUArmorBase(int maxUses, ...) overload wizardHat uses above - .durability(500) matches exactly.
	public static final DeferredItem<Item> ARCHMAGE_HAT = REGISTER.register("archmage_hat",
			() -> new org.wilkretawesomesauce.minestuckuniverseported.item.ArchmageHatItem(MSUArmorMaterials.ARCHMAGE_HAT, new Item.Properties().durability(500)));

	// Ported from MinestuckUniverse (1.12.2)'s "Needlewand" (items.MinestuckUniverseItems#needlewands) -
	// see beam.BeamWeaponItem's own doc comment for why this is the only one of several original beam
	// weapon items ported this pass.
	public static final DeferredItem<Item> NEEDLEWAND = REGISTER.register("needlewand",
			() -> new org.wilkretawesomesauce.minestuckuniverseported.capabilities.beam.BeamWeaponItem(
					new Item.Properties().durability(488), 0.05f, 10f, 1f, 60, 15, "needlewand"));

	// Ported from MinestuckUniverse (1.12.2)'s items.ItemManipulatedMatter - see that class's own doc
	// comment (abilitech.heroAspect.space.TechSpaceManipulator captures the region that fills it).
	public static final DeferredItem<Item> MANIPULATED_MATTER = REGISTER.register("manipulated_matter",
			() -> new org.wilkretawesomesauce.minestuckuniverseported.item.ManipulatedMatterItem(new Item.Properties().stacksTo(1)));

	public static final DeferredItem<net.minecraft.world.item.BlockItem> ABILITECHNOSYNTH = REGISTER.register("abilitechnosynth",
			() -> new org.wilkretawesomesauce.minestuckuniverseported.item.AbilitechnosynthItem(MSUBlocks.ABILITECHNOSYNTH.get(), new Item.Properties()));
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

	// Real unlock-cost item for skills.abilitech.TechDragonAura ("Draconic Aura") - the first tech in this
	// project's port to actually use TechBoondollarCost#requiredStacks. Resource assets (model/texture) were
	// already sitting in this project's resources unused, same bulk-import-but-never-registered situation as
	// moonstone above.
	public static final DeferredItem<Item> DRAGON_GEL = REGISTER.registerSimpleItem("dragon_gel");

	// Real unlock-cost item for skills.abilitech.TechReturn ("Homeward Bound"). The original's own item
	// (items.ItemWarpMedallion, RETURN variant) also had a real held-item teleport-charm use case of its own
	// - not ported here, this is deliberately scoped to just the unlock-gate role TechReturn itself needs
	// (same "don't build more than the current consumer needs" call already made for other required-stack
	// items in this project).
	public static final DeferredItem<Item> RETURN_MEDALLION = REGISTER.registerSimpleItem("return_medallion");

	// Real unlock-cost item for badges.BadgeBuilder - see MSUToolTiers' own doc comment for why this is a
	// real functional pickaxe (using this project's own custom MSUToolTiers.BATTLEPICK_OF_ZILLYDEW tier)
	// rather than a plain unusable item, without chasing exact 1.12.2 numeric parity.
	public static final DeferredItem<Item> BATTLEPICK_OF_ZILLYDEW = REGISTER.register("battlepick_of_zillydew",
			() -> new net.minecraft.world.item.PickaxeItem(MSUToolTiers.BATTLEPICK_OF_ZILLYDEW,
					new Item.Properties().attributes(net.minecraft.world.item.PickaxeItem.createAttributes(MSUToolTiers.BATTLEPICK_OF_ZILLYDEW, 16.0F, -2.8F))));

	// Ported from MinestuckUniverse (1.12.2)'s items.ItemSkaianScroll - see that class's own doc comment
	// for the real, new per-player Aspect/Class eligibility gate the original never actually had.
	public static final DeferredItem<Item> SKAIAN_SCROLL = REGISTER.register("skaian_scroll",
			() -> new org.wilkretawesomesauce.minestuckuniverseported.item.SkaianScrollItem(new Item.Properties()));

	// Ported from ModularBosses (1.8)'s items.ItemCustomEgg - a real throwable spawn egg (see
	// GolemSpawnEggItem's own doc comment), not vanilla's own instant-place SpawnEggItem. No spawn egg of
	// any kind existed in this project before this (see MSUEntityTypes' own doc comment on that gap).
	public static final DeferredItem<Item> GOLEM_SPAWN_EGG = REGISTER.register("golem_spawn_egg",
			() -> new GolemSpawnEggItem(new Item.Properties()));

	// The "Jukinator-3000" - real, original design for this project, no 1.12.2 counterpart. See
	// item.JukinatorItem's own doc comment. Known gap, stated plainly: no texture/model yet.
	public static final DeferredItem<Item> JUKINATOR = REGISTER.register("jukinator",
			() -> new org.wilkretawesomesauce.minestuckuniverseported.item.JukinatorItem(new Item.Properties()));

	private MSUItems()
	{
	}
}
