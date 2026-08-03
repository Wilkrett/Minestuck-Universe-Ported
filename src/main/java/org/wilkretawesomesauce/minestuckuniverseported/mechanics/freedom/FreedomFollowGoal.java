package org.wilkretawesomesauce.minestuckuniverseported.mechanics.freedom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Generic equivalent of {@code entity.HopeGolemEntity}'s own private {@code FollowOwnerGoal}, but driven
 * by an arbitrary UUID ({@link FreedomData#getFollowing()}) instead of a fixed owner field - this has to
 * attach to any real vanilla {@link Mob}, not just one bespoke entity type with its own owner field.
 * Backs {@code FreedomRelationshipEvents}' "chooses to follow" conversion - see that class's own doc
 * comment for the design doc quote this implements ("A Page of Breath does not force a mob to follow
 * them. They increase its Freedom until it chooses to follow.").
 * <p>
 * Reads {@link FreedomData#getFollowing()} fresh every {@link #canUse()}/{@link #canContinueToUse()}
 * check rather than caching it - clearing that field elsewhere (the mob's Freedom dropping back down,
 * see {@code FreedomRelationshipEvents} again) is all that's needed to make this goal stop selecting
 * itself, no extra bookkeeping required here. Movement/look logic is copied from {@code FollowOwnerGoal}'s
 * own proven values (10-block trigger distance, 4-block stop distance, 12-block teleport-catchup) rather
 * than re-derived.
 */
public class FreedomFollowGoal extends Goal
{
	private final Mob mob;
	private final double speed;
	private final PathNavigation navigation;
	private LivingEntity leader;
	private int timeToRecalcPath;

	public FreedomFollowGoal(Mob mob, double speed)
	{
		this.mob = mob;
		this.speed = speed;
		this.navigation = mob.getNavigation();
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse()
	{
		if(!(mob.level() instanceof ServerLevel serverLevel))
			return false;

		UUID followingId = mob.getData(MSUAttachments.FREEDOM_DATA).getFollowing();
		if(followingId == null)
			return false;

		if(!(serverLevel.getEntity(followingId) instanceof LivingEntity target) || !target.isAlive()
				|| (target instanceof Player player && player.isSpectator()))
			return false;

		if(mob.distanceToSqr(target) < 10.0 * 10.0)
			return false;

		leader = target;
		return true;
	}

	@Override
	public boolean canContinueToUse()
	{
		if(leader == null || !leader.isAlive())
			return false;

		UUID followingId = mob.getData(MSUAttachments.FREEDOM_DATA).getFollowing();
		return leader.getUUID().equals(followingId) && !navigation.isDone() && mob.distanceToSqr(leader) > 4.0 * 4.0;
	}

	@Override
	public void start()
	{
		timeToRecalcPath = 0;
	}

	@Override
	public void stop()
	{
		leader = null;
		navigation.stop();
	}

	@Override
	public void tick()
	{
		LookControl lookControl = mob.getLookControl();
		lookControl.setLookAt(leader, 10.0F, mob.getMaxHeadXRot());

		if(--timeToRecalcPath <= 0)
		{
			timeToRecalcPath = 10;
			if(mob.getLeashData() == null && !mob.isPassenger())
			{
				if(mob.distanceToSqr(leader) >= 144.0)
					mob.teleportTo(leader.getX(), leader.getY(), leader.getZ());
				else
					navigation.moveTo(leader, speed);
			}
		}
	}
}
