package org.wilkretawesomesauce.minestuckuniverseported.capabilities.beam;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code items.weapons.IBeamStats}. The original's mutable
 * {@code setBeamTexture}/{@code setCustomBeamTexture} builder-style setters aren't kept - this port's one
 * implementer ({@link BeamWeaponItem}) just takes its texture as a constructor argument, a final field
 * being simpler than a settable one for something that never actually changes after construction.
 */
public interface IBeamStats
{
	float getBeamRadius(ItemStack stack);

	int getBeamHurtTime(ItemStack stack);

	ResourceLocation getBeamTexture();
}
