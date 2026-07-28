package org.wilkretawesomesauce.minestuckuniverseported.capabilities.beam;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code items.properties.beams.IPropertyBeam} - the original had
 * this as one {@code WeaponProperty} implementation among a much larger, mostly-unrelated generic
 * item-behavior-modifier framework (attack damage/speed, tooltips, right-click, block-break, durability -
 * dozens of hooks serving many non-beam weapons never touched by this task). That whole framework isn't
 * ported; a beam weapon {@link Item} implements this interface <i>directly</i> instead of going through a
 * {@code getProperties(stack)} indirection list - this port only ever builds one beam weapon at a time
 * anyway, so the list-of-properties layer buys nothing here. Real extension point for future beam weapons
 * with different impact behavior, same as the original's actual purpose.
 */
public interface IPropertyBeam
{
	default void onBeamTick(ItemStack stack, Beam beam)
	{
	}

	default DamageSource onEntityImpact(ItemStack stack, Beam beam, Entity entity, DamageSource damageSource)
	{
		return damageSource;
	}

	default void onBlockImpact(ItemStack stack, Beam beam, BlockPos pos)
	{
	}
}
