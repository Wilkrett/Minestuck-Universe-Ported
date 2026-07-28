package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.blood;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code potions.PotionBleeding} - a plain periodic
 * damage-over-time effect, same shape as vanilla Poison, applied by {@link TechBloodBleeding}.
 * <p>
 * The interval formula ({@link #shouldApplyEffectTickThisTick}) mirrors vanilla Poison's own
 * ({@code 25 >> amplifier}, floored at every 10 ticks so a high amplifier can't tick every single frame).
 * Chip damage is a flat 1 HP per tick and won't finish off the last point of health (matching vanilla
 * Poison's own "can't kill, only bring to 1 HP" rule) - the original didn't have this guard, but every
 * other damage-over-time effect in this project's dependency tree behaves this way and diverging felt
 * like an oversight rather than an intentional design choice worth preserving.
 */
public class BleedingEffect extends MobEffect
{
	private static final float DAMAGE_PER_TICK = 1.0F;

	public BleedingEffect()
	{
		super(MobEffectCategory.HARMFUL, 0x8B0000);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier)
	{
		int interval = Math.max(10, 25 >> amplifier);
		return duration % interval == 0;
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier)
	{
		if(entity.getHealth() > 1.0F)
			entity.hurt(entity.damageSources().magic(), DAMAGE_PER_TICK);
		return true;
	}
}
