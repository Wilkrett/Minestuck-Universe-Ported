package org.wilkretawesomesauce.minestuckuniverseported.mechanics.soul;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Marker effect applied to a Soul's real hidden {@code ServerPlayer} anchor whenever it's projecting
 * anywhere (a host or a Spirit) - carries no attribute modifiers, exists purely so the anchor's own
 * client can tell (via the free network sync every potion effect already gets) whether to start
 * capturing/relaying its own movement input instead of applying it locally. Same marker-effect idiom
 * this project already uses for {@code TechMindControl.MindControllingEffect}/
 * {@code TechVoidStep}'s own marker effect - see {@code SoulManager}'s doc comment for the whole
 * "hidden anchor" design this backs.
 */
public class SoulProjectionEffect extends MobEffect
{
	public SoulProjectionEffect()
	{
		super(MobEffectCategory.NEUTRAL, 0x87CEFA);
	}
}
