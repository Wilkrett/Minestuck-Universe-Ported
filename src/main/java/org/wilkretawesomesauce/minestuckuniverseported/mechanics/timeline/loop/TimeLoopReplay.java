package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.loop;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.EntitySnapshot;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.WorldTickSnapshot;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The radius-filtered reset/forward-replay logic a {@link TimeLoopZone} needs every tick - parallel to
 * {@code mechanics.timeline.TimelineManager#applySnapshot} (which this deliberately doesn't reuse directly: that
 * method applies a snapshot's {@code oldState} level-wide with no radius filter and no puppeted-entity
 * restriction, neither of which fits a zone effect).
 */
final class TimeLoopReplay
{
	private TimeLoopReplay()
	{
	}

	/**
	 * Start of a new pass: walks the whole window newest-to-oldest applying each in-radius block's
	 * {@code oldState} (so multi-tick changes to the same block fully unwind to its state at the very
	 * start of the window, same cumulative-undo idiom {@code TimelineManager#travelBackwards} already
	 * uses), then snaps every puppeted entity to the earliest snapshot of it found in the window.
	 */
	static void resetToWindowStart(ServerLevel level, TimeLoopZone zone)
	{
		List<WorldTickSnapshot> window = zone.getWindow();
		for(int i = window.size() - 1; i >= 0; i--)
			applyBlockChanges(level, zone, window.get(i), true);

		for(UUID entityId : zone.getPuppetedEntityIds())
		{
			EntitySnapshot earliest = earliestSnapshotOf(window, entityId);
			if(earliest != null && puppetableEntity(level, entityId) instanceof LivingEntity entity)
				earliest.applyTo(entity);
		}
	}

	/** One tick forward within a pass: applies this step's in-radius block {@code newState}s and puppeted-entity forward positions. */
	static void replayStepForward(ServerLevel level, TimeLoopZone zone, WorldTickSnapshot step)
	{
		applyBlockChanges(level, zone, step, false);

		for(Map.Entry<UUID, EntitySnapshot> entry : step.entitySnapshots().entrySet())
		{
			if(!zone.getPuppetedEntityIds().contains(entry.getKey()))
				continue;
			if(puppetableEntity(level, entry.getKey()) instanceof LivingEntity entity)
				entry.getValue().applyTo(entity);
		}
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
			level.setBlock(pos, useOldState ? record.oldState() : record.newState(), Block.UPDATE_ALL);
		}
	}

	private static EntitySnapshot earliestSnapshotOf(List<WorldTickSnapshot> window, UUID entityId)
	{
		for(WorldTickSnapshot step : window)
		{
			EntitySnapshot snapshot = step.entitySnapshots().get(entityId);
			if(snapshot != null)
				return snapshot;
		}
		return null;
	}
}
