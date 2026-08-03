package org.wilkretawesomesauce.minestuckuniverseported.mechanics.freedom;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies {@link FreedomData}'s hidden 0-100 value to real, hookable game behavior - see that class's own
 * doc comment for the stat itself. Original design for this project ("Minestuck - Breath Aspect
 * Mechanic"), no 1.12.2 counterpart.
 * <p>
 * Every effect below is a real, verified NeoForge/vanilla hook (confirmed against this project's pinned
 * decompiled source, not guessed) - <b>not</b> every line of the source doc got one. Two categories of
 * the doc's own wording have no generic engine hook to attach to and are deliberately left unmodeled
 * rather than faked:
 * <ul>
 *     <li>"More varied AI decisions" / "improvised alternate routes" / "the entity appears creative" -
 *     vanilla's A* pathfinder always computes the objectively shortest viable path to whatever goal a
 *     goal picked; there is no "path diversity" knob anywhere in it to turn up. {@link #TARGET_REROLL}
 *     and the periodic {@code recomputePath()} nudge below are the closest real approximation (forcing
 *     an opportunistic re-evaluation of target/route more often at {@link FreedomLevel#HIGH}), not a
 *     literal implementation of "alternate routes."</li>
 *     <li>"Webs" resistance (the doc's own "Strong resistance to Slowness, webs, knockback reduction")
 *     - {@code Entity#stuckSpeedMultiplier} is a protected field, re-set every tick from inside
 *     {@code Block#entityInside} while the entity remains inside a cobweb, with no public mutator this
 *     project's no-Mixin policy can reach before {@code Entity#move} consumes it the same tick. Left out
 *     entirely, same category as {@code TechBreathWindVessel}'s own documented collision-phasing gap.</li>
 * </ul>
 * Everything else below is real:
 * <ul>
 *     <li>Movement speed / jump height scale continuously with Freedom's distance from the 50 baseline,
 *     via a real {@link AttributeModifier} on {@link Attributes#MOVEMENT_SPEED}/{@link Attributes#JUMP_STRENGTH}
 *     (the latter confirmed - despite its stale "for horses" doc comment in vanilla's own source - to be
 *     a base attribute on every {@code LivingEntity}, not horse-specific, and to be what
 *     {@code LivingEntity#getJumpPower()} actually reads for every entity's jump height).</li>
 *     <li>Incoming knockback is reduced at {@link FreedomLevel#HIGH} via {@link LivingKnockBackEvent}.</li>
 *     <li>Incoming Slowness has a chance to be denied outright at {@link FreedomLevel#HIGH}, via
 *     {@link MobEffectEvent.Applicable}, the same real hook {@code mechanics.doom.DoomEffectDurationEvents}
 *     already established (here used for outright denial, not a duration rewrite, so no
 *     cancel-and-reapply dance is needed).</li>
 *     <li>Leashing a {@link FreedomLevel#HIGH} mob has a chance to fail outright ({@link PlayerInteractEvent.EntityInteract},
 *     canceled before {@code Entity#interact} ever runs), and an already-leashed {@link FreedomLevel#HIGH}
 *     mob has a small periodic chance to snap its own leash via {@link net.minecraft.world.entity.Leashable#dropLeash}
 *     (which {@link Mob} implements directly).</li>
 *     <li>At {@link FreedomLevel#EXTREME_LOW}, a {@link Mob}'s own dodge/flee/wander goals
 *     ({@link AvoidEntityGoal}, {@link PanicGoal}, {@link RandomStrollGoal}, {@link WaterAvoidingRandomStrollGoal})
 *     are spliced out of its {@code goalSelector} on bracket entry and spliced back in (at their original
 *     priority) on bracket exit - the same real {@code GoalSelector#addGoal}/{@code removeGoal} splicing
 *     idiom {@code heroAspect.rage.RageAI} already established in this project. <b>A heuristic, not
 *     exhaustive</b>: matches by exact vanilla class only, so a modded/custom mob's own equivalent goal
 *     class won't be recognized - stated plainly rather than silently incomplete.</li>
 * </ul>
 * {@link FreedomLevel#LOW}/{@link FreedomLevel#NEUTRAL} get no bespoke hooks beyond the continuous
 * movement/jump scaling above - the doc's own "less wandering"/"more direct pathfinding"/"rarely
 * recalculate navigation" language at those brackets is realized by simply <i>not</i> adding any of the
 * {@link FreedomLevel#HIGH}-only nudges, i.e. plain unmodified vanilla behavior stands in for "routine" -
 * there is no public API to make vanilla's own path-cache timer run any less often than its already-fairly
 * conservative default.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class FreedomEvents
{
	private static final ResourceLocation MOVEMENT_SPEED_MODIFIER_ID = Minestuckuniverseported.id("freedom_movement_speed");
	private static final ResourceLocation JUMP_STRENGTH_MODIFIER_ID = Minestuckuniverseported.id("freedom_jump_strength");
	private static final float MOVEMENT_SPEED_SCALE = 0.35F;
	private static final float JUMP_STRENGTH_SCALE = 0.5F;
	private static final int ATTRIBUTE_UPDATE_INTERVAL_TICKS = 5;

	private static final float HIGH_THRESHOLD = 70.0F;
	private static final float KNOCKBACK_RESISTANCE_MAX = 0.5F;
	private static final float SLOWNESS_RESIST_CHANCE_MAX = 0.6F;
	private static final float LEASH_BLOCK_CHANCE_MAX = 0.75F;
	private static final float LEASH_BREAK_CHANCE_PER_CHECK_MAX = 0.05F;

	private static final int HIGH_FREEDOM_CHECK_INTERVAL_TICKS = 20;
	private static final int TARGET_REROLL_INTERVAL_TICKS = 100;
	private static final float TARGET_REROLL_CHANCE_MAX = 0.5F;
	private static final int PATH_RECOMPUTE_INTERVAL_TICKS = 60;

	private FreedomEvents()
	{
	}

	@SubscribeEvent
	private static void onEntityTick(EntityTickEvent.Post event)
	{
		if(!(event.getEntity() instanceof LivingEntity entity) || entity.level().isClientSide())
			return;

		FreedomData data = entity.getData(MSUAttachments.FREEDOM_DATA);
		float freedom = data.getFreedom();

		if(entity.tickCount % ATTRIBUTE_UPDATE_INTERVAL_TICKS == 0)
			applyAttributeModifiers(entity, freedom);

		if(entity instanceof Mob mob)
		{
			updateGoalSuppression(mob, data, FreedomLevel.of(freedom));

			if(freedom >= HIGH_THRESHOLD && mob.tickCount % HIGH_FREEDOM_CHECK_INTERVAL_TICKS == 0)
				applyHighFreedomMobBehavior(mob, freedom);
		}
	}

	private static void applyAttributeModifiers(LivingEntity entity, float freedom)
	{
		double scale = (freedom - FreedomData.DEFAULT) / FreedomData.DEFAULT;

		AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
		if(speed != null)
			speed.addOrUpdateTransientModifier(new AttributeModifier(MOVEMENT_SPEED_MODIFIER_ID,
					scale * MOVEMENT_SPEED_SCALE, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

		AttributeInstance jump = entity.getAttribute(Attributes.JUMP_STRENGTH);
		if(jump != null)
			jump.addOrUpdateTransientModifier(new AttributeModifier(JUMP_STRENGTH_MODIFIER_ID,
					scale * JUMP_STRENGTH_SCALE, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	}

	private static void applyHighFreedomMobBehavior(Mob mob, float freedom)
	{
		float aboveThreshold = (freedom - HIGH_THRESHOLD) / (100.0F - HIGH_THRESHOLD);

		if(mob.isLeashed() && mob.getRandom().nextFloat() < aboveThreshold * LEASH_BREAK_CHANCE_PER_CHECK_MAX)
			mob.dropLeash(true, true);

		if(mob.getTarget() != null && mob.tickCount % TARGET_REROLL_INTERVAL_TICKS == 0
				&& mob.getRandom().nextFloat() < aboveThreshold * TARGET_REROLL_CHANCE_MAX)
			mob.setTarget(null);

		if(mob.tickCount % PATH_RECOMPUTE_INTERVAL_TICKS == 0 && mob.getNavigation().isInProgress())
			mob.getNavigation().recomputePath();
	}

	private static void updateGoalSuppression(Mob mob, FreedomData data, FreedomLevel level)
	{
		if(level == data.getLastAppliedLevel())
			return;

		if(level == FreedomLevel.EXTREME_LOW && data.getSuppressedGoals() == null)
		{
			List<WrappedGoal> removed = new ArrayList<>();
			for(WrappedGoal wrapped : List.copyOf(mob.goalSelector.getAvailableGoals()))
			{
				if(isDodgeFleeOrWanderGoal(wrapped.getGoal()))
				{
					removed.add(wrapped);
					mob.goalSelector.removeGoal(wrapped.getGoal());
				}
			}
			data.setSuppressedGoals(removed);
		}
		else if(level != FreedomLevel.EXTREME_LOW && data.getSuppressedGoals() != null)
		{
			for(WrappedGoal wrapped : data.getSuppressedGoals())
				mob.goalSelector.addGoal(wrapped.getPriority(), wrapped.getGoal());
			data.setSuppressedGoals(null);
		}

		data.setLastAppliedLevel(level);
	}

	private static boolean isDodgeFleeOrWanderGoal(Goal goal)
	{
		return goal instanceof AvoidEntityGoal<?> || goal instanceof PanicGoal
				|| goal instanceof RandomStrollGoal || goal instanceof WaterAvoidingRandomStrollGoal;
	}

	@SubscribeEvent
	private static void onKnockback(LivingKnockBackEvent event)
	{
		LivingEntity entity = event.getEntity();
		if(entity.level().isClientSide())
			return;

		float freedom = entity.getData(MSUAttachments.FREEDOM_DATA).getFreedom();
		if(freedom <= HIGH_THRESHOLD)
			return;

		float reduction = (freedom - HIGH_THRESHOLD) / (100.0F - HIGH_THRESHOLD) * KNOCKBACK_RESISTANCE_MAX;
		event.setStrength(event.getStrength() * (1.0F - reduction));
	}

	@SubscribeEvent
	private static void onMobEffectApplicable(MobEffectEvent.Applicable event)
	{
		LivingEntity entity = event.getEntity();
		if(entity.level().isClientSide())
			return;

		MobEffectInstance instance = event.getEffectInstance();
		if(instance.getEffect() != MobEffects.MOVEMENT_SLOWDOWN)
			return;

		float freedom = entity.getData(MSUAttachments.FREEDOM_DATA).getFreedom();
		if(freedom <= HIGH_THRESHOLD)
			return;

		float chance = (freedom - HIGH_THRESHOLD) / (100.0F - HIGH_THRESHOLD) * SLOWNESS_RESIST_CHANCE_MAX;
		if(entity.getRandom().nextFloat() < chance)
			event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
	}

	@SubscribeEvent
	private static void onEntityInteract(PlayerInteractEvent.EntityInteract event)
	{
		if(event.getLevel().isClientSide())
			return;
		if(!event.getItemStack().is(Items.LEAD))
			return;
		if(!(event.getTarget() instanceof Mob mob) || !mob.canBeLeashed())
			return;

		float freedom = mob.getData(MSUAttachments.FREEDOM_DATA).getFreedom();
		if(freedom <= HIGH_THRESHOLD)
			return;

		float chance = (freedom - HIGH_THRESHOLD) / (100.0F - HIGH_THRESHOLD) * LEASH_BLOCK_CHANCE_MAX;
		if(mob.getRandom().nextFloat() < chance)
			event.setCanceled(true);
	}
}
