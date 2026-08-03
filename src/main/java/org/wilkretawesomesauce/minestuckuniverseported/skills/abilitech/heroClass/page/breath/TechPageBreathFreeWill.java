package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.page.breath;

import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.Relationship;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.RelationshipManager;
import org.wilkretawesomesauce.minestuckuniverseported.network.WindBurstPacket;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAspectColors;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.breath.WindEngine;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;

import java.util.List;

/**
 * "Free Will" - new tech, no 1.12.2 counterpart, ported from the "Minestuck Relationship System
 * Interaction: Breath Aspect" design doc's own "Ability Concept: Free Will" ([Page] [Breath] [Utility]).
 * Page of Breath's real class+aspect tech (matching {@code page.doom.TechPageDoomReservoir}'s own
 * {@code EnumClass.PAGE} + {@code requiredAspect = EnumAspect.BREATH} shape for this exact combo).
 * <p>
 * <b>Passive</b> ("nearby entities slowly gain Freedom... entities with relationships to the Page or
 * allies gain increased effects"): every {@link #PASSIVE_INTERVAL_TICKS}, every real {@link LivingEntity}
 * within {@link #RADIUS} gains a small Freedom trickle, {@link #RELATIONSHIP_MULTIPLIER}&times; for
 * anyone who already has a real relationship with the Page ({@link RelationshipManager#get} - never
 * created, see {@code TechBreathLiberate}'s own doc comment for why "Breath does not create
 * relationships" is a real, enforced rule in this project now, not just flavor text). "...or allies"
 * (a second-order check - friends of the Page's own friends) is deliberately not implemented; walking that
 * whole relationship graph every pulse for every nearby entity has real, unbounded cost for a passive
 * tick handler, and the doc's own primary case is "relationships to the Page" - the second-order case is
 * left as an unbuilt refinement, not silently dropped.
 * <p>
 * <b>Activation</b> ("remove artificial restrictions... reduces relationship manipulation effects...
 * allows entities to leave forced situations"): a real instant burst on key-press - grants every nearby
 * {@link LivingEntity} a real chunk of Freedom outright, snaps any nearby {@link Mob}'s leash (real
 * {@code Leashable#dropLeash}, the concrete reading of "allows entities to leave forced situations"), and
 * reduces Instability on every relationship touching a nearby entity - this project's own real
 * "relationship manipulation" mechanic is Crimson Discord's Instability system
 * ({@code heroClass.bard.blood.TechBardBloodCrimsonDiscord}), so "reduces relationship manipulation
 * effects" is read as a direct, concrete counter to that specific existing mechanic, not new abstract
 * infrastructure invented to match the phrase. Also implements the doc's own <b>"Shared Freedom"</b>
 * relationship event for real ("escaping confinement together... shared freedom creates shared
 * experiences"): any two entities caught in the same burst that already have a relationship with
 * <i>each other</i> (never created, same rule as above) gain Familiarity/Trust/Affinity from it - a real
 * between-targets effect, not between either target and the Page.
 * <p>
 * <b>Deliberately not modeled</b>: "makes loyalty based more on Trust than dependency" has no concrete
 * mechanical anchor in this project's real {@code Relationship} fields - Trust and Strength ("dependency")
 * already move somewhat independently via existing gains elsewhere, and there's no principled way to
 * selectively boost one at the other's expense as a burst effect without arbitrarily reweighting every
 * relationship in range. Left as flavor text, not built, same honesty convention as this project's other
 * stated gaps (e.g. {@code mechanics.freedom.FreedomEvents}' own "more varied AI decisions" gap) rather
 * than a forced, arbitrary implementation.
 * <p>
 * <b>Real visuals</b>, from the "Breath Wind Engine Visualizer Design" doc's own explicit spec for this
 * exact activation ("create a large expanding pressure wave... transparent wind sphere expands
 * outward... dust is pushed outward... leashes visibly snap"): three real
 * {@link WindEngine#expandingBurst} calls at increasing radii/decreasing density (a single-instant
 * approximation of an expanding wavefront, since this ability fires once rather than holding over
 * several ticks like {@code TechBreathLiberate}/{@code TechBreathConstrain} do), plus
 * {@link WindEngine#nudgeItemsOutward} for the doc's own "dust is pushed outward" line - the same real,
 * non-Mixin-reachable {@code ItemEntity} nudge {@code WindEngine}'s own doc comment explains is the one
 * real "Environmental Reaction" from that doc's list.
 * <p>
 * <b>Real primary visual now</b>, per the later "Breath Visualizer Architecture Decision" doc's own
 * stricter rule (vanilla particles demoted to secondary/atmospheric only): a {@link WindBurstPacket}
 * fired alongside the particle calls above, telling every nearby client to draw a real expanding
 * billboard-quad pressure-wave shell ({@code client.render.WindBurstRenderer}) - a genuine mesh, not more
 * particles, matching {@code TechBreathLiberate}/{@code TechBreathConstrain}'s own ribbon+vortex mesh.
 */
public class TechPageBreathFreeWill extends TechHeroClass
{
	private static final double RADIUS = 16.0;
	private static final int PASSIVE_INTERVAL_TICKS = 40;
	private static final float PASSIVE_FREEDOM_GAIN = 0.5F;
	private static final float RELATIONSHIP_MULTIPLIER = 2.0F;

	private static final float ACTIVATION_FREEDOM_BURST = 15.0F;
	private static final float ACTIVATION_INSTABILITY_REDUCTION = 15.0F;
	private static final float SHARED_FREEDOM_GAIN = 5.0F;

	public TechPageBreathFreeWill()
	{
		super(Minestuckuniverseported.id("free_will"), EnumClass.PAGE, EnumAspect.BREATH, 220000, MSUTechType.PASSIVE, MSUTechType.UTILITY);
	}

	@Override
	public boolean onPassiveTick(Level level, Player player, int techSlot)
	{
		if(!(player instanceof ServerPlayer) || !(level instanceof ServerLevel serverLevel))
			return false;

		if(serverLevel.getGameTime() % PASSIVE_INTERVAL_TICKS != 0)
			return false;

		for(LivingEntity nearby : serverLevel.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RADIUS), e -> e != player))
		{
			float gain = PASSIVE_FREEDOM_GAIN;
			if(RelationshipManager.get(player.getUUID(), nearby.getUUID()) != null)
				gain *= RELATIONSHIP_MULTIPLIER;

			nearby.getData(MSUAttachments.FREEDOM_DATA).addFreedom(gain);
		}

		MSUAbilitechParticles.aura(level, player, EnumAspect.BREATH, 3);
		return false;
	}

	@Override
	public void onPassiveToggle(Level level, Player player, boolean active)
	{
		sendToggleMessage(player, active);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.PRESS || !(level instanceof ServerLevel serverLevel))
			return false;

		List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RADIUS), e -> e != player);
		long now = serverLevel.getGameTime();

		for(LivingEntity entity : nearby)
		{
			entity.getData(MSUAttachments.FREEDOM_DATA).addFreedom(ACTIVATION_FREEDOM_BURST);

			if(entity instanceof Mob mob && mob.isLeashed())
				mob.dropLeash(true, true);

			for(Relationship rel : RelationshipManager.getAllFor(entity.getUUID()))
				if(rel.entityA.equals(entity.getUUID()) && rel.instability > 0F)
					RelationshipManager.adjustInstability(rel, -ACTIVATION_INSTABILITY_REDUCTION, now);
		}

		applySharedFreedom(now, nearby);

		int color = MSUAspectColors.get(EnumAspect.BREATH)[0];
		WindEngine.expandingBurst(level, player.position(), RADIUS * 0.35, color, 12);
		WindEngine.expandingBurst(level, player.position(), RADIUS * 0.65, color, 10);
		WindEngine.expandingBurst(level, player.position(), RADIUS, color, 8);
		WindEngine.nudgeItemsOutward(level, player.position(), RADIUS, 0.25);

		if(player instanceof ServerPlayer serverPlayer)
			PacketDistributor.sendToPlayersTrackingEntityAndSelf(serverPlayer, new WindBurstPacket(serverPlayer.getId()));

		return true;
	}

	/** "Shared Freedom" - see this class's own doc comment. */
	private static void applySharedFreedom(long now, List<LivingEntity> freedTogether)
	{
		for(int i = 0; i < freedTogether.size(); i++)
		{
			for(int j = i + 1; j < freedTogether.size(); j++)
			{
				Relationship rel = RelationshipManager.get(freedTogether.get(i).getUUID(), freedTogether.get(j).getUUID());
				if(rel == null)
					continue;

				RelationshipManager.adjustFamiliarity(rel, SHARED_FREEDOM_GAIN);
				RelationshipManager.adjustTrust(rel, SHARED_FREEDOM_GAIN);
				RelationshipManager.adjustAffinity(rel, SHARED_FREEDOM_GAIN);
				RelationshipManager.recordEvent(rel, "Shared a moment of freedom", now);
				RelationshipManager.deriveType(rel);
			}
		}
	}
}
