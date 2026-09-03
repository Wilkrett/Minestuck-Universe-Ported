package org.wilkretawesomesauce.minestuckuniverseported.mechanics.doom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.Relationship;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.RelationshipManager;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.RelationshipType;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * "Doom does not create separate relationship mechanics - it manipulates Doom generated through the
 * existing Relationship System" - the design doc's own central rule. Original design for this project,
 * no 1.12.2 counterpart. Registers itself with {@code mechanics.relationship.RelationshipManager}'s two
 * existing extension points ({@code addDeathListener}/{@code addCollapseListener}) rather than that
 * class knowing anything about Doom - the exact same one-way-callback shape
 * {@code heroClass.witch.blood.CultOfPersonalityManager} already uses for its own Family-relationship
 * collapse handling.
 * <p>
 * Four sources, matching the design doc's own sections: death of a connected entity (the surviving
 * party gains Doom, scaled by the relationship's strength/type), betrayal (killing your own positive
 * relationship partner grants that killer bonus Doom on top, scaled by trust/strength/stability - "more
 * significant when trust is high, strength is high, stability is high"), severance (a Stage-4
 * Instability collapse grants Doom to both surviving parties, since neither necessarily died), and
 * isolation (a player with very few current relationships slowly accrues Doom over time - "the absence
 * of relationships can also generate Doom").
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class RelationshipDoomEvents
{
	static
	{
		RelationshipManager.addDeathListener(RelationshipDoomEvents::onRelationshipEndedByDeath);
		RelationshipManager.addCollapseListener(RelationshipDoomEvents::onRelationshipSevered);
	}

	/**
	 * How much Doom a relationship of this {@link RelationshipType} would generate if it ended, before
	 * scaling by strength - the design doc's own "Doom Contribution" concept, computed on the fly rather
	 * than stored as a new persisted field ({@code Relationship} isn't persisted at all today, and
	 * recomputing from its existing strength/type is cheap). Not user-configurable per type (like
	 * {@code RelationshipManager}'s own hardcoded {@code isPositive}/{@code SPECIAL_ORIGIN_TYPES} sets) -
	 * only the overall scale/cap constants below are config-driven.
	 */
	private static final java.util.Map<RelationshipType, Double> TYPE_WEIGHT = java.util.Map.of(
			RelationshipType.FAMILY, 1.0,
			RelationshipType.LOYALTY, 0.75,
			RelationshipType.FRIENDSHIP, 0.75,
			RelationshipType.OBLIGATION, 0.75,
			RelationshipType.OWNERSHIP, 0.75,
			RelationshipType.KINSHIP, 0.5,
			RelationshipType.RIVALRY, 0.5,
			RelationshipType.HOSTILE_ATTACHMENT, 0.5,
			RelationshipType.HOSTILE, 0.3
	);
	private static final double FORMING_WEIGHT = 0.15;

	/** Scales {@link #contributionOf} into the Doom a survivor gains when the other side of a relationship dies. */
	private static final double DEATH_SCALE = 0.5;
	/** Max Doom a single relationship's death-of-connected-entity trigger can ever grant the survivor. */
	private static final double DEATH_CAP = 20.0;
	/** Scales {@link #contributionOf} into the Doom both surviving parties gain when a relationship severs. */
	private static final double SEVERANCE_SCALE = 0.4;
	/** Max Doom a single relationship's severance can ever grant each surviving party. */
	private static final double SEVERANCE_CAP = 15.0;
	/** Base betrayal-bonus Doom, scaled by trust/strength/stability before {@link #BETRAYAL_CAP} applies. */
	private static final double BETRAYAL_BASE = 10.0;
	/** Max betrayal-bonus Doom a single kill can ever grant. */
	private static final double BETRAYAL_CAP = 25.0;
	/** How often (in ticks) each online player's relationship count is checked for isolation Doom. 1200 = once a minute. */
	private static final int ISOLATION_CHECK_INTERVAL_TICKS = 1200;
	/** A player with this many or fewer current relationships accrues isolation Doom each check. */
	private static final int ISOLATION_RELATIONSHIP_THRESHOLD = 0;
	/** How much Doom accrues per {@link #ISOLATION_CHECK_INTERVAL_TICKS} while at/under the isolation threshold. */
	private static final double ISOLATION_PER_INTERVAL = 0.05;

	/** Duplicated from {@code RelationshipManager#isPositive} (private there) - a 5-entry constant unlikely to change, not worth exposing a new public method for this one caller. */
	private static final Set<RelationshipType> POSITIVE_TYPES = EnumSet.of(
			RelationshipType.LOYALTY, RelationshipType.FRIENDSHIP, RelationshipType.FAMILY,
			RelationshipType.OWNERSHIP, RelationshipType.KINSHIP);

	private RelationshipDoomEvents()
	{
	}

	public static double contributionOf(Relationship rel)
	{
		return TYPE_WEIGHT.getOrDefault(rel.type, FORMING_WEIGHT) * (rel.strength / 100.0);
	}

	/**
	 * Death of a connected entity: the surviving side of {@code relationship} gains Doom scaled by
	 * {@link #contributionOf}. If the survivor is also {@code killer} and {@code relationship} was a
	 * positive type, they additionally gain a betrayal bonus scaled by trust/strength/stability as they
	 * stood at the moment of death (this listener fires before {@code RelationshipManager}'s own betrayal
	 * handling zeroes strength - see that class's own doc comment on {@code addDeathListener}).
	 */
	private static void onRelationshipEndedByDeath(ServerLevel level, Relationship rel, UUID deadEntityId, @Nullable LivingEntity killer)
	{
		UUID survivorId = rel.other(deadEntityId);
		if(level.getEntity(survivorId) instanceof LivingEntity survivor)
		{
			double gain = Math.min(DEATH_CAP, contributionOf(rel) * DEATH_SCALE);
			survivor.getData(MSUAttachments.DOOM_DATA).addDoom(gain);

			if(killer != null && killer.getUUID().equals(survivorId) && POSITIVE_TYPES.contains(rel.type))
			{
				double betrayalBonus = Math.min(BETRAYAL_CAP, BETRAYAL_BASE
						* (rel.trust / 100.0) * (rel.strength / 100.0) * (rel.stability / 100.0));
				killer.getData(MSUAttachments.DOOM_DATA).addDoom(betrayalBonus);
			}
		}
	}

	/** Severance (Stage-4 Instability collapse) - both surviving parties gain Doom, scaled by {@link #contributionOf} as the relationship stood right before it broke. */
	private static void onRelationshipSevered(ServerLevel level, Relationship rel)
	{
		double gain = Math.min(SEVERANCE_CAP, contributionOf(rel) * SEVERANCE_SCALE);
		for(UUID id : List.of(rel.entityA, rel.entityB))
			if(level.getEntity(id) instanceof LivingEntity entity)
				entity.getData(MSUAttachments.DOOM_DATA).addDoom(gain);
	}

	/** Isolation - a player with very few current relationships slowly accrues Doom, same throttled-tick shape as {@code mechanics.doom.DoomPassiveAccrualEvents}. Scoped to players only, matching the design doc's own "losing all allies"/"being forgotten" framing. */
	@SubscribeEvent
	private static void onLevelTick(LevelTickEvent.Post event)
	{
		if(!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % ISOLATION_CHECK_INTERVAL_TICKS != 0)
			return;

		for(ServerPlayer player : level.players())
		{
			RelationshipManager.ensureNaturalRelationship(player, level.getGameTime());
			if(RelationshipManager.getAllFor(player.getUUID()).size() <= ISOLATION_RELATIONSHIP_THRESHOLD)
				player.getData(MSUAttachments.DOOM_DATA).addDoom(ISOLATION_PER_INTERVAL);
		}
	}
}
