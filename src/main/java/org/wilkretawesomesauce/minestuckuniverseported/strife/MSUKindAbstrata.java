package org.wilkretawesomesauce.minestuckuniverseported.strife;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code com.cibernet.minestuckuniverse.strife.MSUKindAbstrata}.
 * <p>
 * <b>This is a deliberately trimmed first pass.</b> The original file wired dozens of kinds up to the
 * addon's own tool items (toolHammer, toolClub, toolSickle, ...) and to items from a couple dozen other
 * 1.12.2 mods (IC2, Botania, Thaumcraft, Immersive Engineering, etc.). Neither of those exist yet in this
 * port: the addon's own items subsystem hasn't been ported yet, and those other mods either don't have a
 * 1.21.1 release or haven't been evaluated for this project.
 * <p>
 * <b>Two complementary ways every kind here is data-driven, not just code-driven:</b>
 * <ul>
 *     <li>Every kind registered via {@link #kind} automatically gets a same-named tag
 *     ({@code #minestuckuniverseported:kind/<path>}), checked in {@link KindAbstratus#isStackCompatible}.
 *     Standard tag JSON ({@code values}/{@code remove}/{@code replace}) already gives datapacks full
 *     control over what's in it, so extending (or - for the still-empty placeholders below like
 *     {@code hammerkind} - populating from scratch) any kind's item list needs zero Java changes and no
 *     dependency on this mod's classes. This is genuinely the preferred way to fill in the TODOs below
 *     now, including for cross-mod compat that used to be hardcoded per-mod in the 1.12.2 original.</li>
 *     <li>{@link MSUKindAbstrataDataLoader} additionally loads
 *     {@code data/<namespace>/minestuckuniverseported/strife_kinds/*.json} files ({@link StrifeKindData}),
 *     which can extend a kind's keyword matching, or - since a tag alone can't invent a kind's identity -
 *     define a brand new kind (id, hidden flag, items, keywords) entirely from data when no Java code for
 *     it exists at all.</li>
 * </ul>
 * Because of the tag, {@link KindAbstratus#isEmpty()} now always returns {@code false} for kinds
 * registered here (they always have at least their own tag) - so all of them, including the currently
 * inert placeholders, show up as selectable rather than being hidden until code catches up. That trades a
 * small UX blemish (an option that currently accepts nothing) for actually being discoverable/extensible.
 */
public final class MSUKindAbstrata
{
	private MSUKindAbstrata()
	{
	}

	// Vanilla-tool-backed kinds - fully functional now
	public static final KindAbstratus BLADEKIND = kind("sword")
			.addItemClasses(SwordItem.class)
			.addKeywords("sword", "katana", "kukiri", "saber", "rapier", "excalibur");
	public static final KindAbstratus PICKAXEKIND = kind("pickaxe").addItemClasses(PickaxeItem.class);
	public static final KindAbstratus AXEKIND = kind("axe").addItemClasses(AxeItem.class).addKeywords("battleaxe", "halberd");
	public static final KindAbstratus SHOVELKIND = kind("shovel").addItemClasses(ShovelItem.class);
	public static final KindAbstratus HOEKIND = kind("hoe").addItemClasses(HoeItem.class);
	public static final KindAbstratus FISHING_RODKIND = kind("fishing_rod").addItemClasses(FishingRodItem.class);
	public static final KindAbstratus POTIONKIND = kind("potion").addItemClasses(PotionItem.class);
	public static final KindAbstratus SHIELDKIND = kind("shield").addItemClasses(ShieldItem.class);
	public static final KindAbstratus BOWKIND = kind("bow")
			.setPreventRightClick(true)
			.addItemClasses(BowItem.class, CrossbowItem.class);
	public static final KindAbstratus TRIDENTKIND = kind("trident").addItemClasses(TridentItem.class).addKeywords("trident");
	public static final KindAbstratus SHEARSKIND = kind("shears").addItemClasses(ShearsItem.class).addKeywords("scissor");

	public static final KindAbstratus THROWKIND = kind("projectile")
			.setPreventRightClick(true)
			.addKeywords("shuriken");
	// ^ default contents (snowball, egg, ender pearl, etc.) now live in
	//   data/minestuckuniverseported/tags/item/kind/projectile.json instead of here

	public static final KindAbstratus ROCKKIND = kind("rock");
	// ^ default contents (cobblestone, stone, mossy cobblestone) now live in .../kind/rock.json
	public static final KindAbstratus PAPERKIND = kind("paper").addKeywords("paper");
	// ^ default contents (paper, map, filled map) now live in .../kind/paper.json
	public static final KindAbstratus BUNNYKIND = kind("bunny")
			.setHidden(true)
			.addKeywords("bunny", "rabbit", "hare");
	// ^ default contents (rabbit, cooked rabbit, rabbit foot/hide, rabbit stew) now live in .../kind/bunny.json

	public static final KindAbstratus FISTKIND = kind("fist").setFist(true);
	public static final KindAbstratus JOKERKIND = kind("joker")
			.setHidden(true)
			.setConditional((item, stack, res) -> res || !stack.isEmpty());

	// Keyword-only kinds - work today for any item whose registry path matches, and are ready to accept
	// addon items directly once they exist.
	public static final KindAbstratus WRENCHKIND = kind("wrench").addKeywords("wrench");
	public static final KindAbstratus WANDKIND = kind("wand").addKeywords("wand");
	public static final KindAbstratus LANCEKIND = kind("lance").addKeywords("lance");
	public static final KindAbstratus SPEARKIND = kind("spear").addKeywords("spear");
	public static final KindAbstratus CHAINSAWKIND = kind("chainsaw").addKeywords("chainsaw");
	public static final KindAbstratus MAKEUPKIND = kind("makeup").addKeywords("lipstick", "lip_stick");
	public static final KindAbstratus UMBRELLAKIND = kind("umbrella").addKeywords("umbrella");
	public static final KindAbstratus BROOMKIND = kind("broom").addKeywords("broom");
	public static final KindAbstratus FLASHLIGHTKIND = kind("flashlight").addKeywords("flashlight", "laser_pointer", "laserpointer");
	public static final KindAbstratus BATONKIND = kind("baton").addKeywords("baton");
	public static final KindAbstratus KNIFEKIND = kind("knife").addKeywords("knife", "dagger", "katar", "knive", "kunai", "sai");
	public static final KindAbstratus CLUBKIND = kind("club").addKeywords("mace", "club");
	public static final KindAbstratus CLAWKIND = kind("claw").addKeywords("katar");
	public static final KindAbstratus GLOVEKIND = kind("gauntlet").addKeywords("glove", "gauntlet", "fist");
	public static final KindAbstratus NEEDLEKIND = kind("needles").addKeywords("needle");
	public static final KindAbstratus SICKLEKIND = kind("sickle").addKeywords("sickle", "scythe");

	// TODO(items subsystem): empty until the addon's own tools exist. See MinestuckUniverse 1.12.2's
	// com.cibernet.minestuckuniverse.strife.MSUKindAbstrata for the original defaults, e.g.:
	//   hammerkind  -> MinestuckUniverseItems.toolHammer
	//   canekind    -> MinestuckUniverseItems.toolCane
	//   spoonkind   -> MinestuckUniverseItems.toolSpoon, conditional vs. MinestuckItems.crockerSpork
	//   forkkind    -> MinestuckUniverseItems.toolFork, conditional vs. MinestuckItems.crockerSpork
	//   dicekind    -> MinestuckUniverseItems.dice, fluoriteOctet
	//   keykind     -> MinestuckUniverseItems.dungeonKey
	public static final KindAbstratus HAMMERKIND = kind("hammer");
	public static final KindAbstratus CANEKIND = kind("cane");
	public static final KindAbstratus SPOONKIND = kind("spoon").addKeywords("spoon");
	public static final KindAbstratus FORKKIND = kind("fork").addKeywords("fork");
	public static final KindAbstratus DICEKIND = kind("dice");
	public static final KindAbstratus KEYKIND = kind("key");
	public static final KindAbstratus DRILLKIND = kind("drill");

	// TODO(mod compat): the 1.12.2 source additionally cross-registered dozens of items from other mods
	// (IC2, Botania, Thaumcraft, Immersive Engineering, Mowzie's Mobs, etc.) into most of the kinds above,
	// plus mod-specific integration blocks registerArsenalApi()/registerVariedCommoditiesApi() for the
	// MinestuckArsenal and VariedCommodities addons. Revisit once/if 1.21.1 ports of those mods are
	// targeted by this project.

	// TODO(half-weapon mechanic): halfBladekind/halfBowkind relied on PropertyBreakableItem (a 1.12.2
	// addon mechanic marking weapons as "broken"/half-strength via NBT). Not ported yet.

	// TODO(sbahj joke items): sbahjkind was tied to a set of joke items (sbahjWhip, unrealAir, sord,
	// sbahjBedrock, ...). Not ported yet, low priority.

	private static KindAbstratus kind(String path)
	{
		KindAbstratus kindAbstratus = new KindAbstratus(id(path)).addTag(tag(path));
		return MSUKindAbstrataRegistry.register(kindAbstratus);
	}

	/**
	 * Every kind gets a same-named tag ({@code #minestuckuniverseported:kind/<path>}) for free, matched
	 * in {@link KindAbstratus#isStackCompatible}. Unlike everything else on {@link KindAbstratus}, tags
	 * need zero extra plumbing to be datapack-extensible: standard tag JSON already supports
	 * {@code values}/{@code remove}/{@code replace}, so a pack (including this mod's own, or a third
	 * party's compat pack) can add or remove items from any kind - including the ones below with
	 * hardcoded {@code addItems(...)} calls, or the currently-empty placeholders - by shipping
	 * {@code data/<namespace>/tags/item/kind/<path>.json}, with no Java changes
	 * and no dependency on this mod's classes.
	 */
	private static TagKey<Item> tag(String path)
	{
		return TagKey.create(Registries.ITEM, id("kind/" + path));
	}

	private static ResourceLocation id(String path)
	{
		return Minestuckuniverseported.id(path);
	}

	/** Call once during mod setup to make sure this class is loaded and all kinds above are registered. */
	public static void init()
	{
	}
}
