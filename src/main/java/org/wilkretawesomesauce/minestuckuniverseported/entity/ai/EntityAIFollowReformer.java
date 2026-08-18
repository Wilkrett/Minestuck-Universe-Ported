package org.wilkretawesomesauce.minestuckuniverseported.entity.ai;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.blood.TechBloodReformer;

import java.util.EnumSet;
import java.util.List;

public class EntityAIFollowReformer extends Goal
{
	private static final double ANIMAL_FOLLOW_RADIUS = 16.0;

	private final Animal animal;
	private final double speed;
	private int timeToRecalcPath;
	private Player target;

	public EntityAIFollowReformer(Animal animal, double speed)
	{
		this.animal = animal;
		this.speed = speed;
		setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse()
	{
		target = findNearestReformer();
		return target != null;
	}

	@Override
	public boolean canContinueToUse()
	{
		return target != null && target.isAlive() && TechBloodReformer.hasReformerActive(target) && animal.distanceToSqr(target) <= ANIMAL_FOLLOW_RADIUS * ANIMAL_FOLLOW_RADIUS;
	}

	@Override
	public void start()
	{
		timeToRecalcPath = 0;
	}

	@Override
	public void stop()
	{
		target = null;
		animal.getNavigation().stop();
	}

	@Override
	public void tick()
	{
		animal.getLookControl().setLookAt(target, 10.0F, animal.getMaxHeadXRot());
		if(--timeToRecalcPath <= 0)
		{
			timeToRecalcPath = 10;
			animal.getNavigation().moveTo(target, speed);
		}
	}

	private Player findNearestReformer()
	{
		List<Player> nearby = animal.level().getEntitiesOfClass(Player.class, animal.getBoundingBox().inflate(ANIMAL_FOLLOW_RADIUS), TechBloodReformer::hasReformerActive);
		Player closest = null;
		double closestDistSqr = Double.MAX_VALUE;
		for(Player candidate : nearby)
		{
			double distSqr = animal.distanceToSqr(candidate);
			if(distSqr < closestDistSqr)
			{
				closestDistSqr = distSqr;
				closest = candidate;
			}
		}
		return closest;
	}
}