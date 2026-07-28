package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.mind;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;

/**
 * Real port of MinestuckUniverse (1.12.2)'s {@code potions.PotionCounter}, hardcoded to its one
 * in-scope instantiation - {@code MSUPotions.MIND_FORTITUDE} ("mental fortitude"), which the original
 * built by passing {@code MIND_CONFUSION}/{@code BLINDNESS}/{@code NAUSEA} into the generic counter
 * class. Only one real instantiation is in scope here (the original's other one, DECAYPROOF, has no
 * real producer or consumer anywhere in this project's already-ported content - see this task's own
 * planning notes), so this skips rebuilding the generic {@code Potion...} varargs base and just hardcodes
 * the one real case directly: every tick, cures {@link MSUMobEffects#MIND_CONFUSION},
 * {@link MobEffects#BLINDNESS}, and Nausea (vanilla's own internal
 * field for it is {@link MobEffects#CONFUSION}, not {@code NAUSEA}) if present.
 * <p>
 * <b>No in-scope producer.</b> The original only ever granted this through the never-ported
 * skills/badges/Echeladder Title-Aspect ambient buff system ({@code GTEventHandler#getAspectEffects}).
 * Real, correct mechanics regardless - two real in-scope <i>consumers</i> already exist:
 * {@code TechMindControl}'s possession-immunity check and {@code client.CloakRenderEvents}'s
 * "can the observer see through a disguise" check, both of which reference this effect now instead of
 * nothing. Same "ready infrastructure, no producer yet" category as {@code itemvoid.GameData#addItem}
 * before {@code TechVoidGrasp} existed.
 */
public class MindFortitudeEffect extends MobEffect
{
	public MindFortitudeEffect()
	{
		super(MobEffectCategory.BENEFICIAL, 0x070149);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier)
	{
		return true;
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier)
	{
		entity.removeEffect(MSUMobEffects.MIND_CONFUSION);
		entity.removeEffect(MobEffects.BLINDNESS);
		entity.removeEffect(MobEffects.CONFUSION);
		return true;
	}
}
