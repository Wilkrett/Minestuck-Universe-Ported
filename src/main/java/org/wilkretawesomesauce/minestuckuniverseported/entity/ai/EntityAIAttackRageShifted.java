package org.wilkretawesomesauce.minestuckuniverseported.entity.ai;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class EntityAIAttackRageShifted extends MeleeAttackGoal
{
    public EntityAIAttackRageShifted(PathfinderMob mob, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(mob, speedModifier, followingTargetEvenIfNotSeen);
    }
//    public EntityAIAttackRageShifted(Mob mob, double speedModifier, boolean followingTargetEvenIfNotSeen)
//    {
//        super((PathfinderMob) mob, speedModifier, followingTargetEvenIfNotSeen);
//    }
//
//    @Override
//    protected void checkAndPerformAttack(LivingEntity target)
//    {
//        double reach = this.getAttackReachSqr(target);
//        double distance = this.mob.distanceToSqr(target);
//
//        if (distance <= reach && this.isTimeToAttack())
//        {
//            this.resetAttackCooldown();
//            this.mob.swing(this.mob.getUsedItemHand());
//
//            if (!this.mob.doHurtTarget(target))
//            {
//                DamageSource source = this.mob.damageSources().mobAttack(this.mob);
//
//                target.hurt(
//                        source,
//                        this.mob.getMaxHealth() * 0.2F *
//                                (float)Math.sqrt(this.mob.getBbWidth() * this.mob.getBbHeight())
//                );
//            }
//        }
//    }
}
