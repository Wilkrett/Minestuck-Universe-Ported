package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.rage;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;

/**
 * Shared AI-manipulation helpers for {@code TechRageFrenzy} ("Frenzied Mayhem") and
 * {@code TechRageManagement} ("Anger Management") - both originals leaned on the same
 * {@code resetAI}/goal-list-splicing idiom. Modern {@link net.minecraft.world.entity.ai.goal.GoalSelector}
 * exposes {@code addGoal}/{@code removeGoal}/{@code getAvailableGoals()} as plain public API (confirmed
 * via {@code javap}) - the original's whole {@code ObfuscationReflectionHelper}/{@code resetTask()}
 * reflection dance existed only to work around 1.12.2 not exposing that, and has no modern equivalent
 * to port because it's no longer needed at all, not because it was dropped.
 */
final class RageAI
{
	private RageAI()
	{
	}

	static void enableFrenzy(Mob mob)
	{
		mob.addEffect(new MobEffectInstance(MSUMobEffects.FRENZIED, -1, 0, true, false));
		mob.targetSelector.addGoal(1, new FrenzyTargetGoal(mob));
		ensureAttackGoal(mob);
	}

	static void enableRageShift(Mob mob)
	{
		mob.addEffect(new MobEffectInstance(MSUMobEffects.RAGE_SHIFTED, -1, 0, true, false));
		mob.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(mob, Player.class, true));
		mob.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(mob, IronGolem.class, true));
		ensureAttackGoal(mob);
	}

	/** Matches the original's own drastic {@code disableRageShift} - wipes every goal outright, not just the ones this class added. */
	static void clearAllGoals(Mob mob)
	{
		for(WrappedGoal goal : java.util.List.copyOf(mob.targetSelector.getAvailableGoals()))
			mob.targetSelector.removeGoal(goal.getGoal());
		for(WrappedGoal goal : java.util.List.copyOf(mob.goalSelector.getAvailableGoals()))
			mob.goalSelector.removeGoal(goal.getGoal());
	}

	private static void ensureAttackGoal(Mob mob)
	{
		boolean hasAttackGoal = mob.goalSelector.getAvailableGoals().stream()
				.anyMatch(goal -> goal.getGoal() instanceof MeleeAttackGoal);

		if(!hasAttackGoal && mob instanceof PathfinderMob pathfinder)
			mob.goalSelector.addGoal(2, new MeleeAttackGoal(pathfinder, 1.5, false));
	}
}
