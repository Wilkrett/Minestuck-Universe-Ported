package org.wilkretawesomesauce.minestuckuniverseported;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.SimpleTier;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code items.MinestuckUniverseItems#battlepickOfZillydew} -
 * {@code MSUWeaponBase(780, 16, -2.8, 40, ...).setTool(toolPickaxe, 5, 10)}, a real functional pickaxe with
 * unusually large combat stats layered on top. The original's own weapon-stat system (raw
 * durability/attack-damage/attack-speed fields on a bespoke {@code MSUWeaponBase} item class) has no 1:1
 * modern equivalent - {@link net.neoforged.neoforge.common.SimpleTier}/{@code PickaxeItem.createAttributes}
 * is the real modern substitute for defining a custom tool tier's stats, same idea the vanilla pickaxes
 * themselves use. This is a deliberately scoped-down port: the item exists as a real, usable pickaxe with
 * comparable-magnitude stats (not integrated into this project's real Strife weapon/specibus system, which
 * the original never plugged {@code battlepickOfZillydew} into either) - see {@code badges.BadgeBuilder}'s
 * own doc comment for why exact 1.12.2 numeric parity isn't the point here, it's this item's only real
 * remaining role (a real held-item unlock-cost check for that badge).
 */
public final class MSUToolTiers
{
	public static final SimpleTier BATTLEPICK_OF_ZILLYDEW = new SimpleTier(
			BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 780, 10.0F, 5.0F, 40,
			() -> Ingredient.of(Items.NETHERITE_INGOT));

	private MSUToolTiers()
	{
	}
}
