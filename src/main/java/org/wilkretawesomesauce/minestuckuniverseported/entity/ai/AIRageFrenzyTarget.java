package org.wilkretawesomesauce.minestuckuniverseported.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

public class AIRageFrenzyTarget extends NearestAttackableTargetGoal<Mob>
{
    public AIRageFrenzyTarget(Mob mob, Class<Mob> targetType, boolean mustSee) {
        super(mob, targetType, mustSee);
    }
//    public AIRageFrenzyTarget(Mob mob)
//    {
//        super(mob, Mob.class, true, AIRageFrenzyTarget::canTarget);
//    }

//    private static boolean canTarget(LivingEntity target)
//    {
//        return target instanceof RageFrenzyMob;
//    }
}
