package org.wilkretawesomesauce.minestuckuniverseported.mechanics.soul;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Marker effect applied to a Soul's real {@code ServerPlayer} while it's a free Spirit (see
 * {@link PlayerSoul}'s own doc comment for why this is now just the real player, not a separate proxy
 * entity) - carries no attribute modifiers, exists purely so every observing client (including the
 * player's own, in third person) can tell to render them translucently
 * ({@code client.SoulSpiritRenderEvents}), via the free network sync every potion effect already gets.
 * <p>
 * Deliberately separate from {@link SoulProjectionEffect} - that one is scoped to occupying a host
 * (camera-swap + input relay, still genuinely needed there since a host really is a different entity),
 * while free-spirit needs no input handling at all anymore, only this render cue.
 */
public class SoulSpiritEffect extends MobEffect
{
	public SoulSpiritEffect()
	{
		super(MobEffectCategory.NEUTRAL, 0x87CEFA);
	}
}
