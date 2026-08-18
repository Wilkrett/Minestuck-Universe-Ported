package org.wilkretawesomesauce.minestuckuniverseported.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechRayTrace;

import java.util.EnumSet;

/**
 * Puppets a possessed {@link Mob} on behalf of the controlling {@link Player}, added to/removed from the
 * mob's real {@link net.minecraft.world.entity.ai.goal.GoalSelector} by {@code TechMindControl}
 * ("Mindflayer's Spell") for as long as the tether holds. Every tick, it raytraces the controller's own
 * aim (the closest real modern equivalent of the original's raw client mouse-forwarding
 * {@code InputUpdateEvent}/{@code MINDFLAYER_MOVEMENT_INPUT} packet pair) and either walks the mob
 * towards a targeted block or attacks a targeted living entity, using the mob's own real
 * {@link net.minecraft.world.entity.ai.navigation.PathNavigation} - genuine pathfinding, unlike this
 * project's other puppeted-entity movement (e.g. {@code TechTimeParallelAction}'s clone), which has none.
 * Declaring {@link Flag#MOVE}/{@link Flag#LOOK}/{@link Flag#TARGET} lets vanilla's own goal-conflict
 * resolution naturally suppress the mob's other movement/look/target goals while this one is active,
 * without needing to touch them directly.
 */
public class EntityAIMindflayerTarget extends Goal
{
	private static final double ATTACK_REACH = 3.0;
	private static final int ATTACK_COOLDOWN_TICKS = 20;

	private final Mob mob;
	private final Player controller;
	private int attackCooldown;

	public EntityAIMindflayerTarget(Mob mob, Player controller)
	{
		this.mob = mob;
		this.controller = controller;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.TARGET));
	}

	public Player getController()
	{
		return controller;
	}

	@Override
	public boolean canUse()
	{
		return controller.isAlive() && !controller.isRemoved() && mob.isAlive();
	}

	@Override
	public boolean requiresUpdateEveryTick()
	{
		return true;
	}

	@Override
	public void tick()
	{
		if(attackCooldown > 0)
			attackCooldown--;

		HitResult hit = MSUAbilitechRayTrace.getMouseOver(controller, controller.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE));

		if(hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target && target != mob)
		{
			mob.getLookControl().setLookAt(target);

			if(mob.distanceToSqr(target) <= ATTACK_REACH * ATTACK_REACH)
			{
				if(attackCooldown <= 0)
				{
					mob.doHurtTarget(target);
					attackCooldown = ATTACK_COOLDOWN_TICKS;
				}
			}
			else
				mob.getNavigation().moveTo(target, 1.0);
		}
		else if(hit instanceof BlockHitResult blockHit)
		{
			var pos = blockHit.getBlockPos();
			mob.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
		}
	}
}
