package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.doom;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;

/**
 * Real port of MinestuckUniverse (1.12.2)'s {@code potions.PotionCounter}'s other in-scope
 * instantiation, {@code MSUPotions.DECAYPROOF} - previously stated as "zero real producer or consumer
 * anywhere in already-ported code" (see {@code mind.MindFortitudeEffect}'s own doc comment) back when
 * that was true. It's real now: {@code badges.BadgeEffectBuff} became a real producer once the Badge
 * hierarchy pass wired up {@code heroClass.MSUAspectAmbientEffects}'s own real
 * {@code EFFECT_BUFF}-gated HOPE special case. Every tick, cures {@link MSUMobEffects#DECAY} if present,
 * same "hardcode the one real instantiation, skip the generic varargs base" idiom
 * {@code MindFortitudeEffect} already established.
 */
public class DecayproofEffect extends MobEffect
{
	public DecayproofEffect()
	{
		super(MobEffectCategory.BENEFICIAL, 0xFFDE55);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier)
	{
		return true;
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier)
	{
		entity.removeEffect(MSUMobEffects.DECAY);
		return true;
	}
}
