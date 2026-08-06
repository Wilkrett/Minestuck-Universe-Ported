package org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

import java.util.UUID;

/**
 * Real, project-original, entity-agnostic enforcement of what a positive real {@link RelationshipManager}
 * relationship <i>means</i> for combat, no 1.12.2 counterpart. Deliberately generic - a direct user
 * correction after an earlier version of this hand-rolled the same three behaviors individually into
 * {@code entity.GolemEntity} (its own owner field, a {@code setTarget} override, two owner-aware target
 * goals) plus bespoke {@code ownerId} exclusion fields on {@code entity.GolemBoulderEntity}/
 * {@code entity.GolemFallingBlockEntity}: "this should be ENFORCED by relationships in default", not a
 * bespoke copy per entity class. This class is that single enforcement point - anything with a real
 * positive relationship (see {@link RelationshipManager#isPositive}) to another {@link LivingEntity}
 * automatically gets these behaviors, with zero code of its own, the moment
 * {@link RelationshipManager#getOrCreate}/{@code #onAnimalTame}/{@code #ensureNaturalRelationship} records
 * one - {@code entity.GolemEntity} itself carries no owner-related code at all any more.
 * <p>
 * <b>Never target/damage a positively-related party</b>: {@link #onChangeTarget} cancels
 * {@link LivingChangeTargetEvent} outright whenever the prospective target has a positive relationship
 * with the entity retargeting (covers every path that could set a target - natural AI retargeting, another
 * mod's code, this project's own goals); {@link #onDamagePre} zeroes ({@code LivingDamageEvent.Pre#setNewDamage})
 * any damage whose {@link net.minecraft.world.damagesource.DamageSource#getEntity()} attribution and
 * victim have a positive relationship - this is why {@code GolemBoulderEntity}/{@code GolemFallingBlockEntity}
 * now attribute their damage to a real {@link LivingEntity} (the golem itself) rather than an
 * unattributed/generic source: without a real attacker entity in the {@code DamageSource}, this check has
 * nothing to look up.
 * <p>
 * <b>Defend/assist, matching a real vanilla {@code Wolf}</b> (a direct user report: "when I summon the
 * golem + attack something else, it doesn't attack that target too - it should behave as if it were a
 * wolf"): {@link #onEntityTick} - for any idle ({@code getTarget() == null}) {@link Mob} with a real
 * {@link RelationshipType#OWNERSHIP} relationship to some other {@link LivingEntity} ("the owner"),
 * retargets onto whoever last hurt the owner, or failing that whatever the owner is currently attacking.
 * <b>Two deliberate simplifications</b> versus vanilla {@code Wolf}'s own real target-selector goals (and
 * this project's own earlier, since-reverted {@code GolemEntity}-specific goal pair, which did have both of
 * these): no per-mob "did this actually just change" edge-detector (vanilla/the reverted version compared
 * against a cached timestamp field to avoid re-triggering off a stale memory) - the {@code getTarget() ==
 * null} gate alone throttles this well enough in practice, generically, without needing a side-table of
 * per-mob-UUID state; and this never <i>interrupts</i> an already-active target to defend the owner (only
 * fires while idle), unlike real vanilla target-selector goal priority, which would preempt a lower-priority
 * goal. Both are real, stated trade-offs for staying a simple, universal tick check rather than a second
 * generic goal-injection system.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class RelationshipCombatEvents
{
	private static final int CHECK_INTERVAL_TICKS = 20;

	private RelationshipCombatEvents()
	{
	}

	@SubscribeEvent
	private static void onChangeTarget(LivingChangeTargetEvent event)
	{
		LivingEntity newTarget = event.getNewAboutToBeSetTarget();
		if(newTarget != null && isPositivelyRelated(event.getEntity(), newTarget))
			event.setCanceled(true);
	}

	@SubscribeEvent
	private static void onDamagePre(LivingDamageEvent.Pre event)
	{
		if(event.getSource().getEntity() instanceof LivingEntity attacker && isPositivelyRelated(attacker, event.getEntity()))
			event.setNewDamage(0F);
	}

	/**
	 * Public so callers that apply non-damage side effects (knockback, invulnerability-timer resets, etc.)
	 * alongside a hit can pre-filter a positively-related target out entirely, rather than relying only on
	 * {@link #onDamagePre} to zero the HP loss after those other side effects already happened - see
	 * {@code entity.GolemFallingBlockEntity#hurtNearbyEntities}/{@code entity.GolemEntity#kickEntities}'s
	 * own real fix for exactly that gap.
	 */
	public static boolean isPositivelyRelated(LivingEntity a, LivingEntity b)
	{
		if(a == b)
			return false;

		Relationship rel = RelationshipManager.get(a.getUUID(), b.getUUID());
		return rel != null && RelationshipManager.isPositive(rel.type);
	}

	@SubscribeEvent
	private static void onEntityTick(EntityTickEvent.Post event)
	{
		if(!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel serverLevel))
			return;
		if(mob.getTarget() != null || mob.tickCount % CHECK_INTERVAL_TICKS != 0)
			return;

		for(Relationship rel : RelationshipManager.getAllFor(mob.getUUID()))
		{
			if(rel.type != RelationshipType.OWNERSHIP)
				continue;

			UUID ownerId = rel.other(mob.getUUID());
			if(!(serverLevel.getEntity(ownerId) instanceof LivingEntity owner) || !owner.isAlive())
				continue;

			LivingEntity attacker = owner.getLastHurtByMob();
			if(attacker != null && attacker.isAlive())
			{
				mob.setTarget(attacker);
				return;
			}

			LivingEntity ownerTarget = owner.getLastHurtMob();
			if(ownerTarget != null && ownerTarget.isAlive())
			{
				mob.setTarget(ownerTarget);
				return;
			}
		}
	}
}
