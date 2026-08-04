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
 * New tech for this project ("Tailwind", renamed from "Liberating Zephyr") - no 1.12.2 counterpart, no original cost to port (see
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
 * grow as the target's Freedom actually climbs, and once more on release to clear it - never every tick).
 * <p>
 * <b>{@code WindEngine} particles reintroduced, tracing the trail curve - two direct later user requests</b>.
 * First, "remove the particles from LiberatingZephyr and use the trail instead" dropped the old
 * {@code WindEngine#ribbon}/{@code WindEngine#spiralAroundTarget} calls entirely (the mesh's own lightning
 * trail was judged sufficient on its own). Then, from a live screenshot ("it only shows 1 measly wind
 * effect"), a follow-up request asked to bring {@code WindEngine} back specifically wired to "the trail"
 * rather than its own old independent path: {@link #onUseTick} now calls {@code WindEngine#ribbon} every
 * active tick again, but that method's own curve was reworked (see its own doc comment) to trace the exact
 * same tapered curve {@code client.render.WindRibbonRenderer}'s lightning tube animates along, so the
 * particle stream now visually hugs the mesh's own glowing core instead of an unrelated separate line -
 * denser, fuller "wind" layered directly on the thin tube rather than atmospheric decoration off to the
 * side. {@code spiralAroundTarget} is deliberately still not re-added (not asked for). The same
 * {@code WindEngine#ribbon} call was also added to {@code TechBreathConstrain} (a direct user confirmation,
 * both abilities should get it, not just this one).
 * <p>
 * <b>{@code WindEngine#windSwirl} added, a genuine technique pivot from a later reference-screenshot
 * request</b> ("I want something like this [soft, blurred, curling smoke-ring wisps]... though keep the
 * color blue"): the mesh's precise line geometry was never going to read as "natural wind" no matter how
 * its thickness/flatness was retuned - see {@code client.particles.WindWispParticle}'s own doc comment for
 * the full investigation (including why Photon wasn't reintroduced). {@link #onUseTick} now also calls
 * {@code WindEngine#windSwirl} around the target every tick, radius/intensity scaled by the same
 * {@code freedomFraction} the mesh's own vortex already uses, so the two stay visually in sync.
 */
public class TechBreathLiberate extends TechHeroAspect
{
	private static final float FREEDOM_PER_TICK = 0.5F;
	private static final float LOW_TRUST_THRESHOLD = 25F;

	private static final float LIBERATION_TRUST_GAIN = 15F;
	private static final float LIBERATION_AFFINITY_GAIN = 15F;
	private static final float LIBERATION_STABILITY_GAIN = 10F;

	private static final float FORCED_FREEDOM_CHANCE_PER_CHECK = 0.1F;
	private static final float FORCED_FREEDOM_TRUST_LOSS = 5F;
	private static final float FORCED_FREEDOM_CONFLICT_GAIN = 5F;

	private static final int RIBBON_RESYNC_INTERVAL_TICKS = 10;

	private static final double SWIRL_RADIUS_MIN = 0.5;
	private static final double SWIRL_RADIUS_MAX = 1.3;

	public TechBreathLiberate()
	{
		super(Minestuckuniverseported.id("tailwind"), EnumAspect.BREATH, 32000, MSUTechType.UTILITY); // new tech, no original cost to port - picked to fit this aspect's own cost spread
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

		float freedomFraction = data.getFreedom() / 100F;

		int color = MSUAspectColors.get(EnumAspect.BREATH)[0];
		WindEngine.ribbon(level, player.position().add(0, player.getEyeHeight() * 0.8, 0),
				target.position().add(0, target.getBbHeight() * 0.5, 0),
				level.getGameTime() / 20F, color, freedomFraction);

		WindEngine.windSwirl(level, target.position().add(0, target.getBbHeight() * 0.5, 0),
				SWIRL_RADIUS_MIN + freedomFraction * (SWIRL_RADIUS_MAX - SWIRL_RADIUS_MIN),
				level.getGameTime() / 20F, color, Math.max(0.3F, freedomFraction));

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
