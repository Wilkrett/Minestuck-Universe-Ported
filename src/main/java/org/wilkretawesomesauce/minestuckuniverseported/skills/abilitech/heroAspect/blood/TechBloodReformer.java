package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.blood;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.MSUSkills;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

import java.util.EnumSet;
import java.util.List;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.blood.TechBloodReformer}
 * ("Reformer's Reach") - a passive tech: toggle it on and nearby hostile mobs stop noticing you, wild
 * animals wander over to you instead, and you're generally harder for anything to detect at all.
 * <p>
 * All three of the original's real behaviors are ported:
 * <ol>
 *     <li>Immediately clears any {@link Mob}'s attack target if it's the player - generalized to any
 *     {@code Mob} rather than the original's {@code EntityCreature}-only check, nothing in this project
 *     needs a narrower distinction.</li>
 *     <li>{@link LivingEvent.LivingVisibilityEvent} - makes the player effectively undetectable
 *     ({@code modifyVisibility(0)}, same as the original) to anything checking visibility, not just
 *     entities that have already targeted them.</li>
 *     <li>Two AI-affecting hooks, both intentionally more general than the original's own type-specific
 *     versions: {@link FollowReformerGoal} is injected onto every newly-joined ground-pathfinding
 *     {@link Animal} (matching "wild animals follow the reformer"), and {@link LivingChangeTargetEvent}
 *     cancels <i>any</i> mob's attempt to newly target a Reformer's-Reach player - the original only
 *     specifically reworked Minestuck's own hostile "Underling" mobs' targeting goal for this; this
 *     project has no clean way to splice a replacement goal into an already-constructed Minestuck mob's
 *     own goal selector, but {@code LivingChangeTargetEvent} achieves the same real outcome (underlings,
 *     and everything else, simply never lock onto a Reformer's-Reach player in the first place) for any
 *     mob type at once, which is arguably a strict improvement over the original's narrower scope.</li>
 * </ol>
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public class TechBloodReformer extends TechHeroAspect
{
	private static final double REACH_RADIUS = 32.0;
	private static final double ANIMAL_FOLLOW_RADIUS = 16.0;

	public TechBloodReformer()
	{
		super(Minestuckuniverseported.id("reformers_reach"), EnumAspect.BLOOD, 510, MSUTechType.PASSIVE);
	}

	@Override
	public boolean onPassiveTick(Level level, Player player, int techSlot)
	{
		if(!(level instanceof ServerLevel serverLevel))
			return false;

		for(Mob mob : serverLevel.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(REACH_RADIUS), m -> m.getTarget() == player))
			mob.setTarget(null);

		MSUAbilitechParticles.aura(level, player, EnumAspect.BLOOD, 6);

		return true;
	}

	@Override
	public void onPassiveToggle(Level level, Player player, boolean active)
	{
		super.onPassiveToggle(level, player, active);
		sendToggleMessage(player, active);
	}

	private static boolean hasReformerActive(Player player)
	{
		return player.getData(MSUAttachments.GOD_TIER).isPassiveEnabledFor(MSUSkills.BLOOD_REFORMER);
	}

	@SubscribeEvent
	private static void onVisibilityCheck(LivingEvent.LivingVisibilityEvent event)
	{
		if(event.getEntity() instanceof Player player && hasReformerActive(player))
			event.modifyVisibility(0.0);
	}

	@SubscribeEvent
	private static void onChangeTarget(LivingChangeTargetEvent event)
	{
		if(event.getNewAboutToBeSetTarget() instanceof Player player && hasReformerActive(player))
			event.setCanceled(true);
	}

	@SubscribeEvent
	private static void onEntityJoinLevel(EntityJoinLevelEvent event)
	{
		if(event.getEntity() instanceof Animal animal && animal.getNavigation() instanceof GroundPathNavigation)
			animal.goalSelector.addGoal(3, new FollowReformerGoal(animal, 1.1));
	}

	/** Ported from the original's {@code entity.ai.EntityAIFollowReformer} - moves nearby ground animals toward whichever Reformer's-Reach player is closest. */
	private static class FollowReformerGoal extends Goal
	{
		private final Animal animal;
		private final double speed;
		private int timeToRecalcPath;
		private Player target;

		FollowReformerGoal(Animal animal, double speed)
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
			return target != null && target.isAlive() && hasReformerActive(target) && animal.distanceToSqr(target) <= ANIMAL_FOLLOW_RADIUS * ANIMAL_FOLLOW_RADIUS;
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
}
