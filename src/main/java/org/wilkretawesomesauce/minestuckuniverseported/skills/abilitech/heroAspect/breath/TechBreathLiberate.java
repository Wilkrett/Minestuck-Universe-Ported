package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.breath;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.freedom.FreedomData;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.freedom.FreedomLevel;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.Relationship;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.RelationshipManager;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.RelationshipType;
import org.wilkretawesomesauce.minestuckuniverseported.network.WindRibbonSyncPacket;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.AbilitechLoadout;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAspectColors;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;

/**
 * New tech for this project ("Liberating Zephyr") - no 1.12.2 counterpart, no original cost to port (see
 * this class's own cost comment below). The player-facing half of {@code mechanics.freedom.FreedomData}
 * for the Breath aspect: hold and aim at a target (locks on immediately, same raytrace-and-tether idiom
 * {@code heart.TechHeartBond} already established) to gradually raise their hidden Freedom - more
 * autonomy, more resistance to control, never direct command of the target, matching the source design
 * doc's own "Breath users manipulate Freedom instead of directly controlling entities" framing exactly.
 * Every successful tick also records this caster as {@link FreedomData#setLastLiberatedBy} - see
 * {@code mechanics.freedom.FreedomRelationshipEvents} for what that eventually leads to ("they increase
 * its Freedom until it chooses to follow").
 * <p>
 * <b>Relationship-scaled potency</b> - the "Minestuck Systems Overview" doc's own "Potential Relationship
 * Effects" section ("High Friendship: allies gain stronger shared Freedom effects", "Low Trust: Breath
 * support abilities become weaker", "High Fear: entities may obey but gain reduced Freedom"), all folded
 * into one {@link #freedomMultiplier} lookup rather than three separate checks: a positive relationship
 * (Friendship/Loyalty/Family/Ownership/Kinship) boosts the gain rate by up to 50% at full trust; a
 * standing {@link RelationshipType#HOSTILE} relationship - the closest existing analog to "Fear" this
 * project's real {@code Relationship} class has, see {@code FreedomRelationshipEvents}' own doc comment
 * for why no dedicated Fear field was added - heavily dampens it instead (obeying out of fear doesn't
 * grant much real freedom); anything else with low trust is mildly weaker, matching "support abilities
 * become weaker" without a relationship to actually back them yet.
 * <p>
 * <b>Real "Liberation" and "Forced Freedom" relationship events</b>, from the later "Minestuck
 * Relationship System Interaction: Breath Aspect" design doc's own named event list (distinct from
 * Blood's own relationship events - "Breath should create different types of Relationship events than
 * Blood"). Both only ever touch an <i>already-existing</i> relationship ({@link RelationshipManager#get},
 * never {@code getOrCreate}) - see {@code FreedomRelationshipEvents}' own doc comment for why creating
 * one here would violate that doc's central rule.
 * <ul>
 *     <li><b>Liberation</b> ("freeing an entity from captivity... the entity remembers who gave them a
 *     choice"): fires once, the tick a target's Freedom actually crosses from Low/Extremely Low up into
 *     High as a direct result of this ability - a real threshold-crossing event, not a per-tick trickle -
 *     granting {@link #LIBERATION_TRUST_GAIN}/{@link #LIBERATION_AFFINITY_GAIN}/{@link #LIBERATION_STABILITY_GAIN}
 *     on top of the ordinary per-tick gain above.</li>
 *     <li><b>Forced Freedom</b> ("removing restrictions against an entity's will... Freedom cannot be
 *     forced"): a small per-check chance, only while the relationship is {@link RelationshipType#HOSTILE},
 *     of the opposite outcome - {@code -}Trust and {@code +}Conflict instead of any gain, "no guaranteed
 *     Affinity increase" per the doc's own wording (this project's implementation guarantees the loss
 *     roll can happen at all specifically <i>because</i> the target is unwilling, i.e. Hostile - there's
 *     no attempt to detect "against an entity's will" more generally than that).</li>
 * </ul>
 * <p>
 * <b>Real visuals</b> - a later "Breath Visualizer Architecture Decision" doc superseded an even-later
 * "Breath Wind Engine Visualizer Design" pass's own particle-only approach with a stricter rule: "do not
 * implement Breath visuals primarily through vanilla particle spawning... the primary Breath visual should
 * be a custom renderer." The real primary system now is {@code client.render.WindRibbonRenderer} - a
 * genuine procedural ribbon mesh + orbiting vortex, driven by {@link WindRibbonSyncPacket} (sent once on
 * lock-on, then re-sent every {@link #RIBBON_RESYNC_INTERVAL_TICKS} while held so the vortex can visibly
 * grow as the target's Freedom actually climbs, and once more on release to clear it - never every tick,
 * unlike the particle calls below, since a Photon/custom-mesh effect is meant to run its own animation
 * loop client-side once told "this is active", not be re-spawned constantly). {@link WindEngine#ribbon}/
 * {@link WindEngine#spiralAroundTarget} are kept too, exactly matching that doc's own "correct" formula
 * (custom ribbon + particles as secondary atmospheric decoration + environmental reactions) - not removed,
 * just demoted to the decoration layer they were always going to end up as.
 */
public class TechBreathLiberate extends TechHeroAspect
{
	private static final float FREEDOM_PER_TICK = 0.5F;
	private static final float LOW_TRUST_THRESHOLD = 25F;

	private static final double SPIRAL_RADIUS_MIN = 0.3;
	private static final double SPIRAL_RADIUS_MAX = 1.2;
	private static final float SPIRAL_INTENSITY_MIN = 0.4F;
	private static final float SPIRAL_INTENSITY_MAX = 2.0F;

	private static final float LIBERATION_TRUST_GAIN = 15F;
	private static final float LIBERATION_AFFINITY_GAIN = 15F;
	private static final float LIBERATION_STABILITY_GAIN = 10F;

	private static final float FORCED_FREEDOM_CHANCE_PER_CHECK = 0.1F;
	private static final float FORCED_FREEDOM_TRUST_LOSS = 5F;
	private static final float FORCED_FREEDOM_CONFLICT_GAIN = 5F;

	private static final int RIBBON_RESYNC_INTERVAL_TICKS = 10;

	public TechBreathLiberate()
	{
		super(Minestuckuniverseported.id("liberating_zephyr"), EnumAspect.BREATH, 32000, MSUTechType.UTILITY); // new tech, no original cost to port - picked to fit this aspect's own cost spread
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		AbilitechLoadout badgeEffects = player.getData(MSUAttachments.ABILITECH_LOADOUT);

		if(state == AbilitechKeyState.NONE || state == AbilitechKeyState.RELEASED)
		{
			badgeEffects.setTether(techSlot, null);
			clearRibbon(player);
			return false;
		}

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 1)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			badgeEffects.setTether(techSlot, null);
			clearRibbon(player);
			return false;
		}

		Entity tether = badgeEffects.getTether(techSlot);
		LivingEntity target = tether instanceof LivingEntity livingTether && livingTether.isAlive() ? livingTether : null;

		if(target == null && state == AbilitechKeyState.PRESS)
		{
			LivingEntity raytraced = MSUAbilitechRayTrace.getTargetEntity(player);
			if(raytraced != null)
			{
				badgeEffects.setTether(techSlot, raytraced);
				target = raytraced;
			}
		}

		if(target == null)
			return false;

		FreedomData data = target.getData(MSUAttachments.FREEDOM_DATA);
		FreedomLevel before = data.getLevel();
		data.addFreedom(FREEDOM_PER_TICK * freedomMultiplier(player, target));
		data.setLastLiberatedBy(player.getUUID());

		Relationship rel = RelationshipManager.get(player.getUUID(), target.getUUID());
		if(rel != null)
		{
			if((before == FreedomLevel.LOW || before == FreedomLevel.EXTREME_LOW) && data.getLevel() == FreedomLevel.HIGH)
				applyLiberationEvent(rel, level.getGameTime());
			else if(rel.type == RelationshipType.HOSTILE && time % 20 == 0)
				rollForcedFreedom(player, rel, level.getGameTime());
		}

		if(time % 20 == 0 && !player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);

		int color = MSUAspectColors.get(EnumAspect.BREATH)[0];
		float freedomFraction = data.getFreedom() / 100F;

		WindEngine.ribbon(level, player.getEyePosition(1.0F), target.position(), color, 1.0F);
		WindEngine.spiralAroundTarget(level, target.position().add(0, target.getBbHeight() * 0.5, 0),
				SPIRAL_RADIUS_MIN + freedomFraction * (SPIRAL_RADIUS_MAX - SPIRAL_RADIUS_MIN), color,
				SPIRAL_INTENSITY_MIN + freedomFraction * (SPIRAL_INTENSITY_MAX - SPIRAL_INTENSITY_MIN));

		if(time == 0 || time % RIBBON_RESYNC_INTERVAL_TICKS == 0)
			syncRibbon(player, target, freedomFraction);

		return true;
	}

	@Override
	public void onUnequipped(Level level, Player player, int techSlot)
	{
		player.getData(MSUAttachments.ABILITECH_LOADOUT).setTether(techSlot, null);
		clearRibbon(player);
	}

	private static void syncRibbon(Player player, LivingEntity target, float intensity)
	{
		if(player instanceof ServerPlayer serverPlayer)
			PacketDistributor.sendToPlayersTrackingEntityAndSelf(serverPlayer, new WindRibbonSyncPacket(serverPlayer.getId(), target.getId(), false, intensity));
	}

	private static void clearRibbon(Player player)
	{
		if(player instanceof ServerPlayer serverPlayer)
			PacketDistributor.sendToPlayersTrackingEntityAndSelf(serverPlayer, new WindRibbonSyncPacket(serverPlayer.getId(), -1, false, 0F));
	}

	/** See this class's own doc comment's "Relationship-scaled potency" section. */
	private static float freedomMultiplier(Player caster, LivingEntity target)
	{
		Relationship rel = RelationshipManager.get(caster.getUUID(), target.getUUID());
		if(rel == null)
			return 1.0F;

		if(rel.type == RelationshipType.HOSTILE)
			return 0.3F;

		boolean positive = rel.type == RelationshipType.FRIENDSHIP || rel.type == RelationshipType.LOYALTY
				|| rel.type == RelationshipType.FAMILY || rel.type == RelationshipType.OWNERSHIP
				|| rel.type == RelationshipType.KINSHIP;
		if(positive)
			return 1.0F + (rel.trust / 100F) * 0.5F;

		return rel.trust < LOW_TRUST_THRESHOLD ? 0.5F : 1.0F;
	}

	/** "Liberation" - see this class's own doc comment. */
	private static void applyLiberationEvent(Relationship rel, long now)
	{
		RelationshipManager.adjustTrust(rel, LIBERATION_TRUST_GAIN);
		RelationshipManager.adjustAffinity(rel, LIBERATION_AFFINITY_GAIN);
		RelationshipManager.adjustStability(rel, LIBERATION_STABILITY_GAIN);
		RelationshipManager.recordEvent(rel, "Liberated from a restricted state", now);
		RelationshipManager.deriveType(rel);
	}

	/** "Forced Freedom" - see this class's own doc comment. */
	private static void rollForcedFreedom(Player caster, Relationship rel, long now)
	{
		if(caster.getRandom().nextFloat() >= FORCED_FREEDOM_CHANCE_PER_CHECK)
			return;

		RelationshipManager.adjustTrust(rel, -FORCED_FREEDOM_TRUST_LOSS);
		RelationshipManager.adjustConflict(rel, FORCED_FREEDOM_CONFLICT_GAIN);
		RelationshipManager.recordEvent(rel, "Had freedom forced upon it against its will", now);
	}
}
