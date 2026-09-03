package org.wilkretawesomesauce.minestuckuniverseported.entity.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code entity.ai.EntityAIMindflayerTarget} - added to/removed
 * from a possessed {@link Mob}'s real {@link net.minecraft.world.entity.ai.goal.GoalSelector} by
 * {@code TechMindControl} ("Mindflayer's Spell") for as long as the tether holds.
 * <p>
 * Deliberately as simple as the original: holds a raw world-relative movement offset ({@link #setMove},
 * called every tick by {@code network.MindflayerMovementInputPacket} as the controller's own forwarded WASD
 * input arrives) and nudges the mob toward a point offset from its own current position by that vector -
 * the original's own {@code updateTask()} intent ({@code entity.getPositionVector().addVector(moveStrafe, 0,
 * moveForward)}), not a raytrace/attack system (an earlier version of this port invented one that had no
 * real correspondence to the original and never actually received any movement input at all, since nothing
 * forwarded the controller's WASD for a non-player target - see this class's own git history).
 * <p>
 * <b>Real, deliberate deviation from the original's literal API call</b>: the original drove this via
 * {@code EntityLiving#getNavigator()#tryMoveToXYZ(...)} (a full pathfind), called fresh every tick toward a
 * target barely a block away from the mob's current position. A first pass here ported that literally via
 * {@link net.minecraft.world.entity.ai.navigation.PathNavigation#moveTo}, but that produced visibly jittery,
 * barely-moving mobs in practice (a real, reported symptom, not guessed) - modern {@code PathNavigation}
 * recomputes a full A* search on every call, and discarding/replacing that search 20 times a second before
 * the mob can meaningfully advance along any single one of them starves real movement. Switched to
 * {@link net.minecraft.world.entity.ai.control.MoveControl#setWantedPosition}, which every {@link Mob}
 * already ticks unconditionally every tick regardless of goal state, does no pathfinding at all (just steers
 * straight toward the given point, turning the mob to face it as it goes) - the right tool for "a human is
 * providing fresh directional input every tick," which needs no route-planning, only immediate response.
 * <p>
 * Declaring every {@link Flag} is the modern equivalent of the original's {@code setMutexBits(255)}, letting
 * vanilla's own goal-conflict resolution suppress every other goal (including the mob's own vanilla combat
 * AI) while this one is active - matching the original, which never gave mind-controlled mobs any attack
 * behavior of their own either.
 */
public class EntityAIMindflayerTarget extends Goal
{
	private final Mob mob;
	private final float speed;
	private float moveStrafe;
	private float moveForward;

	public EntityAIMindflayerTarget(Mob mob, float speed)
	{
		this.mob = mob;
		this.speed = speed;
		setFlags(EnumSet.allOf(Flag.class));
	}

	/** Called every tick a {@code MindflayerMovementInputPacket} arrives for this mob - see this class's own doc comment. */
	public void setMove(float moveStrafe, float moveForward)
	{
		this.moveStrafe = moveStrafe;
		this.moveForward = moveForward;
	}

	@Override
	public boolean canUse()
	{
		return true;
	}

	@Override
	public boolean requiresUpdateEveryTick()
	{
		return true;
	}

	@Override
	public void tick()
	{
		if(moveStrafe == 0 && moveForward == 0)
			return;

		Vec3 pos = mob.position();
		mob.getMoveControl().setWantedPosition(pos.x + moveStrafe, pos.y, pos.z + moveForward, speed);
	}
}
