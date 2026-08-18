package org.wilkretawesomesauce.minestuckuniverseported.entity.ai;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code entity.ai.EntityAIAttackRageShifted} - the real melee
 * attack goal a frenzied/rage-shifted creature gets (see {@code heroAspect.rage.TechRageFrenzy}/
 * {@code TechRageManagement}), not vanilla's plain {@link MeleeAttackGoal}: on a failed
 * {@link net.minecraft.world.entity.Mob#doHurtTarget} (e.g. blocked), it lands a real bonus hit anyway -
 * {@code maxHealth * 0.2 * sqrt(width * height)} - matching the original's own formula exactly.
 * <p>
 * {@code checkAndPerformAttack(LivingEntity)} (confirmed via {@code javap} against this project's pinned
 * client jar, not guessed) is the one method {@link MeleeAttackGoal} actually exposes for overriding this -
 * unlike the original's 1.12.2 {@code EntityAIAttackMelee}, the modern base class has no separate
 * {@code getAttackReachSqr}; {@link #canPerformAttack} already does that same range check internally (it's
 * the same method {@code tick()} itself calls to decide whether to invoke this at all), so it's reused
 * directly instead of reimplementing reach math by hand.
 */
public class EntityAIAttackRageShifted extends MeleeAttackGoal
{
	public EntityAIAttackRageShifted(PathfinderMob mob, double speedModifier, boolean followingTargetEvenIfNotSeen)
	{
		super(mob, speedModifier, followingTargetEvenIfNotSeen);
	}

	@Override
	protected void checkAndPerformAttack(LivingEntity target)
	{
		if(!canPerformAttack(target) || !isTimeToAttack())
			return;

		resetAttackCooldown();
		mob.swing(InteractionHand.MAIN_HAND);

		if(!mob.doHurtTarget(target))
		{
			DamageSource source = mob.damageSources().mobAttack(mob);
			target.hurt(source, mob.getMaxHealth() * 0.2F * (float) Math.sqrt(mob.getBbWidth() * mob.getBbHeight()));
		}
	}
}
