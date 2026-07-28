package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.rage;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code entity.ai.AIRageFrenzyTarget} - unlike
 * {@code TechRageManagement}'s "attack players/iron golems" (a real, direct
 * {@code NearestAttackableTargetGoal} use), a frenzied creature attacks <i>anything</i> living nearby,
 * itself included in the search pool minus itself - no vanilla goal does that indiscriminate a search,
 * so this is a small, real, hand-written {@link Goal.Flag#TARGET} goal instead.
 */
public class FrenzyTargetGoal extends Goal
{
	private static final double RADIUS = 16;

	private final Mob mob;

	public FrenzyTargetGoal(Mob mob)
	{
		this.mob = mob;
		setFlags(EnumSet.of(Flag.TARGET));
	}

	@Override
	public boolean canUse()
	{
		if(mob.getTarget() != null)
			return false;

		List<LivingEntity> nearby = mob.level().getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(RADIUS),
				e -> e != mob && e.isAlive() && mob.hasLineOfSight(e));

		if(nearby.isEmpty())
			return false;

		mob.setTarget(nearby.get(mob.getRandom().nextInt(nearby.size())));
		return false;
	}
}
