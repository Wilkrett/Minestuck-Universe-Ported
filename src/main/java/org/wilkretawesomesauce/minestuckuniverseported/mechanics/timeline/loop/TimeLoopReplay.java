package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.loop;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.EntitySnapshot;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.RewindVisuals;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.TimelineTags;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.WorldTickSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The radius-filtered reset/forward-replay logic a {@link TimeLoopZone} needs every tick - parallel to
 * {@code mechanics.timeline.TimelineManager#applySnapshot} (which this deliberately doesn't reuse directly: that
 * method applies a snapshot's {@code oldState} level-wide with no radius filter and no puppeted-entity
 * restriction, neither of which fits a zone effect).
 * <p>
 * <b>Entities now really walk backward, user-requested</b> - the old {@code resetToWindowStart} snapped
 * every puppeted entity straight to its earliest recorded snapshot in one instant jump, which (correctly)
 * read back as "that's not rewinding, that's simply resetting". {@link #resetBlocksToWindowStart} still
 * does an instant jump for blocks (they have no smooth "animate" concept - a block is one state or another),
 * but entities are now moved by {@link #reverseStep}, called once per tick across
 * {@link TimeLoopZone#getReverseTicks()} real ticks by {@code TimeLoopPlayback}, each call sampling one step
 * further back along the entity's own real recorded path via {@code mechanics.timeline.RewindVisuals#sampleReversePath} -
 * the same shared math {@code skills.abilitech.heroAspect.time.TechTimeLoopBeta}'s own dedicated player tick
 * driver uses (real players are never part of {@link TimeLoopZone#getPuppetedEntityIds()}, so they need
 * their own mover, walking the identical formula against the identical path shape).
 */
final class TimeLoopReplay
{
	private TimeLoopReplay()
	{
	}

	/** The instant block-undo half of a pass's reset - see this class's own doc comment for why blocks stay instant while entities don't. Called once per pass, at {@link TimeLoopZone#isReverseStart()}. */
	static void resetBlocksToWindowStart(ServerLevel level, TimeLoopZone zone)
	{
		List<WorldTickSnapshot> window = zone.getWindow();
		for(int i = window.size() - 1; i >= 0; i--)
			applyBlockChanges(level, zone, window.get(i), true);
	}

	/**
	 * Fires the user-requested gray doppelganger comet ({@code RewindVisuals#showRewindGhost}) for every
	 * puppeted entity's own real path, once per pass at {@link TimeLoopZone#isReverseStart()} - alongside
	 * {@link #resetBlocksToWindowStart}, timed to cover the exact {@link TimeLoopZone#getReverseTicks()}
	 * window {@link #reverseStep} is about to walk the real entity through.
	 */
	static void fireRewindGhosts(ServerLevel level, TimeLoopZone zone)
	{
		List<WorldTickSnapshot> window = zone.getWindow();
		for(UUID entityId : zone.getPuppetedEntityIds())
		{
			if(!(puppetableEntity(level, entityId) instanceof LivingEntity entity) || isImmune(entity))
				continue;

			List<EntitySnapshot> path = pathOf(window, entityId);
			RewindVisuals.showRewindGhost(entity, path);
		}
	}

	/**
	 * One tick of the real reverse-walk lead-in: moves every puppeted entity to wherever
	 * {@code RewindVisuals#sampleReversePath} says they should be {@code reverseTick} ticks into the
	 * {@link TimeLoopZone#getReverseTicks()}-long walk through their own real recorded path - genuine
	 * movement (health/equipment/pose included, same as any other {@link EntitySnapshot#applyTo} call),
	 * not a visual-only effect.
	 */
	static void reverseStep(ServerLevel level, TimeLoopZone zone, int reverseTick)
	{
		List<WorldTickSnapshot> window = zone.getWindow();
		for(UUID entityId : zone.getPuppetedEntityIds())
		{
			if(!(puppetableEntity(level, entityId) instanceof LivingEntity entity) || isImmune(entity))
				continue;

			List<EntitySnapshot> path = pathOf(window, entityId);
			if(path.isEmpty())
				continue;

			RewindVisuals.sampleReversePath(path, reverseTick, zone.getReverseTicks()).applyTo(entity);
		}
	}

	/** Every recorded snapshot of {@code entityId} across {@code window}, chronological - the real path both {@link #reverseStep} and {@link #fireRewindGhosts} walk/sweep through, not just its oldest endpoint. */
	private static List<EntitySnapshot> pathOf(List<WorldTickSnapshot> window, UUID entityId)
	{
		List<EntitySnapshot> path = new ArrayList<>();
		for(WorldTickSnapshot step : window)
		{
			EntitySnapshot snapshot = step.entitySnapshots().get(entityId);
			if(snapshot != null)
				path.add(snapshot);
		}
		return path;
	}

	/** One tick forward within a pass: applies this step's in-radius block {@code newState}s and puppeted-entity forward positions. */
	static void replayStepForward(ServerLevel level, TimeLoopZone zone, WorldTickSnapshot step)
	{
		applyBlockChanges(level, zone, step, false);

		for(Map.Entry<UUID, EntitySnapshot> entry : step.entitySnapshots().entrySet())
		{
			if(!zone.getPuppetedEntityIds().contains(entry.getKey()))
				continue;
			if(puppetableEntity(level, entry.getKey()) instanceof LivingEntity entity && !isImmune(entity))
				entry.getValue().applyTo(entity);
		}
	}

	/** Braid-style "this doesn't rewind" exclusion - see {@code mechanics.timeline.TimelineTags}'s own doc comment. */
	private static boolean isImmune(LivingEntity entity)
	{
		return entity.getType().is(TimelineTags.IMMUNE_ENTITY_TYPES) || entity.getTags().contains(TimelineTags.IMMUNE_ENTITY_TAG);
	}

	/**
	 * {@code TimeLoopZone#getPuppetedEntityIds} already excludes the zone's own caster, but a snapshot
	 * carries no entity-type information - some *other* real player could still end up in that set if they
	 * happened to be within radius during the window. Real players are never puppeted directly here,
	 * matching {@code mechanics.timeline.TimelineManager#applySnapshot}'s existing exclusion - only the caster gets a
	 * (separate, dedicated) ghost, via {@code TimeLoopZone#getClone}.
	 */
	private static net.minecraft.world.entity.Entity puppetableEntity(ServerLevel level, UUID entityId)
	{
		net.minecraft.world.entity.Entity entity = level.getEntity(entityId);
		return entity instanceof Player ? null : entity;
	}

	private static void applyBlockChanges(ServerLevel level, TimeLoopZone zone, WorldTickSnapshot step, boolean useOldState)
	{
		Vec3 center = zone.getCenter();
		double radiusSqr = zone.getRadius() * zone.getRadius();

		for(Map.Entry<BlockPos, WorldTickSnapshot.BlockChangeRecord> entry : step.blockChanges().entrySet())
		{
			BlockPos pos = entry.getKey();
			if(center.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > radiusSqr)
				continue;

			var record = entry.getValue();
			if(record.oldState().is(TimelineTags.IMMUNE_BLOCKS) || record.newState().is(TimelineTags.IMMUNE_BLOCKS))
				continue;

			level.setBlock(pos, useOldState ? record.oldState() : record.newState(), Block.UPDATE_ALL);
		}
	}
}
