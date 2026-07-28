package org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Config;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.entity.HopeGolemEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Real, project-original implementation of the "Relationship System - Blood Aspect Framework" design
 * document - a generic, class-agnostic store of relationships between entity pairs, meant to back every
 * Blood-Title class's own abilities rather than each maintaining its own bespoke mechanism ("All future
 * Blood abilities should interact with this system rather than creating separate mechanics", the doc's
 * own words). Consumers so far: {@code heroClass.mage.blood}'s three new Mage of Blood techs (Blood
 * Insight/Memory/Guidance, read/reinforce), and {@code heroClass.witch.blood.CultOfPersonalityManager}'s
 * Schism Aura (which weakens nearby {@link RelationshipType#OWNERSHIP} relationships and can redirect a
 * disrupted pet's aggression onto a sibling summon - see that class's own {@code pulseSchismAura}).
 * <p>
 * <b>Deliberately separate from Cult of Personality/Schism's own {@code CultBond}</b>: those already had a
 * complete, working, independently-designed system (multi-member chains, corruption, tether rendering)
 * before this document existed, and retrofitting them onto generic pairwise Relationships would risk
 * breaking a working feature for no real gain - this class doesn't replace or subsume {@code CultBond},
 * it's the new, separate substrate the doc describes for classes that don't have their own bespoke system
 * yet (Mage of Blood's read-only techs, and the Schism Aura's ambient Layer-2 effects).
 * <p>
 * <b>Relationship creation is lazy, not push-based from every possible summon/tame call site</b>: rather
 * than editing every existing summon (e.g. {@code heroAspect.hope.TechHopeGolem}) to proactively register
 * with this class, {@link #ensureNaturalRelationship} auto-detects a real vanilla {@link TamableAnimal}
 * owner or this project's own {@link HopeGolemEntity} owner on demand, the first time anything actually
 * asks about that entity's relationships - called from both Blood Insight (before displaying) and the
 * Schism Aura (before deciding whether to redirect a pet's aggression). {@link #onAnimalTame} additionally
 * creates one immediately at the real moment of vanilla taming, for the "spending time nearby"/history
 * flavor of having a record from the actual tame event rather than only from the first time someone asks.
 * <p>
 * <b>Real event wiring, and one deliberately-skipped one</b>: {@link #onLivingDamage} implements the doc's
 * "Damaging an ally" negative event (only for {@link RelationshipType#LOYALTY}/{@code FRIENDSHIP}/
 * {@code FAMILY}/{@code OWNERSHIP} - a rival or obligation being hurt isn't a betrayal) and
 * {@link #onLivingDeath} implements "Killing a related entity" (a harsher one-time strength collapse,
 * plus real cleanup of every relationship that entity was part of - the design doc's own "memory leaks
 * from removed entities" concern). <b>"Healing an ally" is not wired up</b>: NeoForge's real
 * {@code LivingHealEvent} (confirmed via {@code javap}) carries only the healed entity and amount, no
 * reference to whoever/whatever did the healing, so there's no generic way to attribute an arbitrary heal
 * to a specific ally the way {@link LivingDamageEvent.Post#getSource()} attributes damage - this would
 * only be wireable from specific, known-caster healing abilities (e.g. {@code heroAspect.heart.TechHeartBond}),
 * not as a blanket listener, and none of those call into this class yet. "Fighting together"/"saving
 * another entity"/"sharing resources" have no existing event hooks in this project at all - same category
 * of gap, not attempted.
 * <p>
 * <b>Not built, matching the doc's own "Future Expansion" list</b>: NPC friendships/factions/villages,
 * player teams, romance, political systems - all explicitly framed as future ideas in the source document,
 * not core requirements.
 * <p>
 * <b>Instability</b> ({@code heroClass.bard.blood.TechBardBloodCrimsonDiscord}, "Crimson Discord", ported
 * from a later "Crimson Discord" design document - a Bard of Blood exclusive tech): a real, separate
 * {@code 0-100} value per {@link Relationship} (see that class's own doc comment for why it's independent
 * of strength/stability), raised by the Bard's own aura ({@link #pulseCrimsonDiscordAura}) or single-target
 * burst, and naturally decaying back toward 0 over time absent that influence ({@link #onLevelTick}).
 * {@link #stageOf} bands it into the doc's own four stages - real consumers of those bands live where the
 * relevant mechanic already lives, not here: {@code CultOfPersonalityManager#onLivingDamage} reads
 * instability to further reduce Blood Bond sharing effectiveness, and {@code CultOfPersonalityManager#onLivingDeath}
 * reads it to make Blood Vengeance retaliation occasionally fail to fire. At Stage 4 (76-100),
 * {@link #checkForCollapse} breaks the relationship - "the relationship collapses naturally", the doc's own
 * words, never a direct delete call from outside - and triggers the "Domino Effect"
 * ({@link #applyDominoEffect} - nearby <i>other</i> relationships gain a flat instability bump,
 * {@link Config#crimsonDiscordDominoBump}, matching the doc's own "+10" worked example). What "breaks" means
 * depends on type, not just a blanket {@link #removeRelationshipRecord}: {@link RelationshipType#HOSTILE}
 * is pacified in place ({@link #pacifyHostileRelationship}) and {@link RelationshipType#KINSHIP} is curdled
 * into outright Hostile ({@link #corruptKinshipToHostile}) - see those two types' own paragraphs below for
 * why. Every other type is still removed outright.
 * {@link #sowDiscord} is the part that doesn't require a pre-existing relationship at all: it manufactures
 * brand-new {@link RelationshipType#RIVALRY} relationships between random nearby {@link net.minecraft.world.entity.Mob}
 * pairs that had no connection whatsoever, which then climb Instability at the same rate as everything
 * else and eventually force the two into real combat once they cross {@link Config#crimsonDiscordFightThreshold} -
 * "make entities around you start to hate each other, even if there's no bond", not just decay of things
 * that already existed.
 * {@link #addCollapseListener} lets {@code CultOfPersonalityManager} react to a {@link RelationshipType#FAMILY}
 * relationship (the type its own {@code link} method creates alongside every Cult of Personality bond, so
 * Crimson Discord affects real Blood Bonds too, not just this generic system in isolation) collapsing by
 * severing the actual {@code CultBond} - a one-way callback rather than a direct import, so this
 * class stays generic infrastructure that doesn't need to know {@code CultBond} exists.
 * <p>
 * <b>Infinite Domino Effect loops are prevented by construction</b>: {@link #applyDominoEffect} only ever
 * raises other relationships' instability, it never itself calls {@link #checkForCollapse} on them - a
 * relationship pushed over the Stage 4 threshold by a domino bump is only discovered and broken on the
 * <i>next</i> real collapse check ({@link #onLevelTick}'s sweep, or the next aura pulse/burst that touches
 * it), so a cascade unfolds gradually across ticks instead of recursing synchronously within one call.
 * {@link #currentlyCollapsing} additionally guards a single relationship from re-entering {@link #checkForCollapse}
 * while its own collapse is still being processed.
 * <p>
 * <b>Class interactions, kept small and additive rather than new techs</b> (the doc's own "Interaction With
 * Other Blood Classes" section, minus Knight of Blood - no such class/tech exists anywhere in this project
 * yet, so "temporarily stabilize relationships" has nothing to attach to and isn't built): Mage of Blood's
 * existing {@code TechMageBloodInsight} now also reports instability/stage; Witch of Blood's existing
 * {@code TechBloodWitchCultOfPersonality} reduces instability as a side effect of selecting a command
 * target (an existing press branch, not a new one); Prince of Blood's existing
 * {@code TechPrinceBloodSchism} can immediately corrupt a sufficiently-unstable plain Relationship
 * (setting {@link Relationship#corrupted}) when its target isn't part of an actual Cult of Personality bond.
 * <p>
 * <b>Affinity/Trust/Familiarity/Conflict</b> ("Relationship System Foundation"/"...merge" design documents -
 * a real addition to {@link Relationship#strength}/{@code stability}, not a replacement; both design docs
 * list all six values together as Core Data): {@link #deriveType} recomputes {@link RelationshipType} from
 * these four for the three "ordinary" types (Loyalty/Friendship/Rivalry) using the merge doc's own exact
 * thresholds, but deliberately leaves the four "special origin" types
 * ({@link RelationshipType#FAMILY}/{@code OWNERSHIP}/{@code OBLIGATION}/{@code HOSTILE_ATTACHMENT}) alone -
 * see that method's own doc comment. Real event wiring for the doc's own "Relationship Formation" section:
 * {@link #onLivingDamage} now <i>also</i> applies "Damage -&gt; Conflict increases" unconditionally to
 * <i>any</i> pair (creating a relationship if none exists yet - this is what lets "an enemy that repeatedly
 * fights the same target" from the earlier Foundation doc actually happen organically, not just decay of
 * relationships some other system already created), and separately detects "Fighting Together"
 * ({@link #recentAttackersByVictim} - two different attackers hitting the same victim within
 * {@link Config#relationshipFightingTogetherWindowTicks} reinforces a relationship <i>between the
 * attackers</i>, not either of them and the victim); {@link #onLivingDeath}'s existing Betrayal handling
 * now also reduces affinity/stability and raises conflict, not just zeroing strength; {@link #onLevelTick}
 * additionally implements "Spending Time Together" (passive familiarity growth for already-related pairs
 * currently within {@link Config#relationshipNearbyRadius} of each other). <b>"Helping" (healing/saving/
 * giving resources/protecting) is still not wired up</b> - same real API gap as before (no generic
 * "who healed/saved/gave to whom" event exists in NeoForge to attribute it from), unchanged by this pass.
 * <p>
 * <b>{@link RelationshipType#HOSTILE}</b> (real, project-original addition, not named in either design
 * document): a real vanilla hostile mob ({@link net.minecraft.world.entity.monster.Enemy}) has a baseline
 * Hostile relationship with players and whatever it's currently targeting by default, not something it has
 * to earn through repeated conflict like {@link RelationshipType#RIVALRY} - see
 * {@link #ensureNaturalRelationship}'s own doc comment for the lazy on-demand creation, and
 * {@link #onLivingDamage} for why a real {@code Enemy}'s first hit already creates the relationship as
 * {@code HOSTILE} outright instead of the generic {@link RelationshipType#FORMING} default every other
 * organically-formed relationship starts as. A real user-reported bug once made a collapsed Hostile
 * relationship instantly regenerate the moment anything next touched that pair - fixed for real by
 * {@link #pacifyHostileRelationship}, which downgrades the record in place (clears the mob's own AI target,
 * resets instability low, steps the type out of Hostile so {@link #deriveType} can reclassify it) instead of
 * deleting it, since a pair whose Hostile bond just broke has real history, not the blank slate a stranger
 * pair starts from.
 * <p>
 * <b>{@link RelationshipType#KINSHIP}</b> (real, project-original addition, not named in either design
 * document - a direct user design call: "entities of the same type should always have a friendly
 * relationship", e.g. two {@code minestuck:imp}s): the positive mirror of {@code HOSTILE} above - a real
 * vanilla {@link Mob} has a baseline Kinship bond with every other {@code Mob} of its exact same
 * {@code EntityType} within {@link #KINSHIP_NEARBY_RADIUS}, a standing fact about what it <i>is</i> rather
 * than something earned like {@link RelationshipType#FRIENDSHIP}, and deliberately gated on {@code Mob} (not
 * {@code LivingEntity}) so two real players never auto-bond this way - see
 * {@link #ensureNaturalRelationship}'s own doc comment. Collapsing a Kinship relationship doesn't fade it
 * back to nothing either: another direct user design call ("Bard worsens a Kinship relationship, that
 * relationship becomes Hostile") means {@link #corruptKinshipToHostile} curdles it into outright
 * {@code HOSTILE} instead - forcing both sides to actually target each other for real, the same payoff
 * {@link #sowDiscord}'s own Rivalry escalation uses. And when <i>that</i> Hostile relationship later
 * collapses in turn, {@link #pacifyHostileRelationship} lands it back on {@code KINSHIP} directly rather than
 * the generic {@code FORMING} every other pacified Hostile relationship gets - a real fix for a second
 * user-reported bug where the pair would silently cycle "Kinship -&gt; Hostile -&gt; Forming -&gt; (deleted
 * and recreated from scratch) -&gt; Kinship" instead of a clean, stable Kinship/Hostile oscillation directly
 * driven by Crimson Discord.
 * <p>
 * <b>Helping</b> ("Relationship Helping System" design document): the doc's own central architectural
 * point is that this class should never have to <i>guess</i> intent from a generic event the way it
 * partially had to for "Damage" - "make abilities/items/actions say: I healed this entity; record a
 * Helping event" instead. {@link #recordPositiveInteraction} is that real push API (the doc's own
 * {@code RelationshipEventManager.recordPositiveInteraction} - implemented as a method here rather than a
 * genuinely separate class, since this class already <i>is</i> the single centralizing point the doc's
 * "don't modify relationships from every event directly" principle asks for, and a second class delegating
 * straight back to this one would be pure indirection). Wired for the doc's own top two priorities ("most
 * reliable"/"clear source and target"): {@link #onLivingDamage} now also detects <b>Protection</b> (attacker
 * damages something that's currently targeting one of the attacker's own positive relationships - "attacks
 * something targeting an ally"), and {@link #onEntityInteract} detects <b>Sharing</b> (feeding a real
 * vanilla {@link Animal} its breeding food). <b>Rescue</b> (priority 4, "more advanced") is also wired for
 * real: {@code heroAspect.life.SavingGraceEvents} credits whoever cast the ward the moment it actually
 * triggers (see that class's own doc comment for why a small caster-tracking map was needed - the marker
 * effect it spends has no stored caster of its own). <b>Healing has no real caller yet</b> - matching the
 * doc's own stated reason it was priority 3 ("requires attribution tracking"): no existing Blood-aspect
 * heal exists in this project to wire it to. Real, ready infrastructure via {@link #recordPositiveInteraction}
 * itself either way - any future ability can call it directly.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class RelationshipManager
{
	static final int MAX_HISTORY_ENTRIES = 8;

	private static final float DEFAULT_STRENGTH = 30F;
	private static final float DEFAULT_STABILITY = 50F;
	private static final float TAME_STARTING_STRENGTH = 40F;

	private static final Map<UUID, Relationship> byId = new HashMap<>();
	private static final Map<PairKey, Relationship> byPair = new HashMap<>();
	private static final Map<UUID, Set<UUID>> relationshipIdsByEntity = new HashMap<>();
	private static final List<BiConsumer<ServerLevel, Relationship>> collapseListeners = new ArrayList<>();
	/** See {@link #addDeathListener} - a separate list/interface from {@link #collapseListeners} since a death listener also needs to know which side actually died. */
	private static final List<DeathListener> deathListeners = new ArrayList<>();
	/** Guards a relationship from re-entering {@link #checkForCollapse} while its own collapse is still being processed - see this class's own "Infinite Domino Effect loops" doc section. */
	private static final Set<UUID> currentlyCollapsing = new HashSet<>();
	/** Victim id -> recent attackers (pruned to {@link Config#relationshipFightingTogetherWindowTicks} on each read) - backs "Fighting Together" detection in {@link #onLivingDamage}. */
	private static final Map<UUID, List<AttackRecord>> recentAttackersByVictim = new HashMap<>();
	/** Special-origin types {@link #deriveType} never auto-reassigns - see that method's own doc comment. */
	private static final Set<RelationshipType> SPECIAL_ORIGIN_TYPES = EnumSet.of(
			RelationshipType.FAMILY, RelationshipType.OWNERSHIP, RelationshipType.OBLIGATION,
			RelationshipType.HOSTILE_ATTACHMENT, RelationshipType.HOSTILE, RelationshipType.KINSHIP);
	/** How far a hostile mob with no current target will look for a nearby player to be baseline-hostile toward - see {@link #ensureNaturalRelationship}'s own doc comment. */
	private static final double HOSTILE_NEARBY_PLAYER_RADIUS = 32.0;
	/** How far a real vanilla {@code Mob} will look for other mobs of its own exact {@code EntityType} to form a baseline {@link RelationshipType#KINSHIP} bond with - see {@link #ensureNaturalRelationship}'s own doc comment. */
	private static final double KINSHIP_NEARBY_RADIUS = 16.0;

	private RelationshipManager()
	{
	}

	/** The single relationship between {@code a} and {@code b}, if one exists (order doesn't matter). */
	@Nullable
	public static Relationship get(UUID a, UUID b)
	{
		return byPair.get(new PairKey(a, b));
	}

	/** Creates a new relationship if {@code a}/{@code b} don't already have one; returns the existing one unchanged (including its type) otherwise - matches the design doc's own "Mage cannot create relationships, only understand/reinforce existing ones" by giving every other caller an idempotent creation point instead of silently overwriting. */
	public static Relationship getOrCreate(UUID a, UUID b, RelationshipType type, long tick, float strength, float stability)
	{
		PairKey key = new PairKey(a, b);
		Relationship existing = byPair.get(key);
		if(existing != null)
			return existing;

		Relationship rel = new Relationship(a, b, type, strength, stability, tick);
		byId.put(rel.id, rel);
		byPair.put(key, rel);
		relationshipIdsByEntity.computeIfAbsent(a, k -> new HashSet<>()).add(rel.id);
		relationshipIdsByEntity.computeIfAbsent(b, k -> new HashSet<>()).add(rel.id);
		return rel;
	}

	/** Every relationship {@code entityId} is currently a party to. */
	public static List<Relationship> getAllFor(UUID entityId)
	{
		Set<UUID> ids = relationshipIdsByEntity.get(entityId);
		if(ids == null || ids.isEmpty())
			return List.of();

		List<Relationship> result = new ArrayList<>(ids.size());
		for(UUID id : ids)
		{
			Relationship rel = byId.get(id);
			if(rel != null)
				result.add(rel);
		}
		return result;
	}

	/**
	 * Adjusts {@code rel}'s strength by {@code rawDelta}, scaled down by its own stability - the design
	 * doc's own "high stability = harder to manipulate": at 0 stability the full delta applies, at 100
	 * stability only half does. Clamped to the doc's own {@code 0-100} range.
	 */
	public static void adjustStrength(Relationship rel, float rawDelta, long tick)
	{
		float resistance = 1F - rel.stability / 200F;
		rel.strength = Mth.clamp(rel.strength + rawDelta * resistance, 0F, 100F);
		rel.lastInteractionTick = tick;
	}

	public static void adjustStability(Relationship rel, float rawDelta)
	{
		rel.stability = Mth.clamp(rel.stability + rawDelta, 0F, 100F);
	}

	// Affinity/Trust/Familiarity/Conflict are deliberately NOT scaled by stability like adjustStrength is -
	// stability's own doc-stated role is resisting deliberate manipulation by Blood abilities (Bard/Prince),
	// not organic gameplay events (fighting together, taking damage) - those always apply at full rate.

	/** {@code -100 to 100} - see {@link Relationship#affinity}'s own doc comment. */
	public static void adjustAffinity(Relationship rel, float rawDelta)
	{
		rel.affinity = Mth.clamp(rel.affinity + rawDelta, -100F, 100F);
	}

	public static void adjustTrust(Relationship rel, float rawDelta)
	{
		rel.trust = Mth.clamp(rel.trust + rawDelta, 0F, 100F);
	}

	public static void adjustFamiliarity(Relationship rel, float rawDelta)
	{
		rel.familiarity = Mth.clamp(rel.familiarity + rawDelta, 0F, 100F);
	}

	public static void adjustConflict(Relationship rel, float rawDelta)
	{
		rel.conflict = Mth.clamp(rel.conflict + rawDelta, 0F, 100F);
	}

	/**
	 * Recomputes {@code rel}'s {@link RelationshipType} from its own affinity/trust/familiarity/conflict,
	 * using the "Relationship System merge" design document's own exact thresholds - a no-op for any
	 * relationship already at a {@link #SPECIAL_ORIGIN_TYPES} type ("should not usually be manually
	 * assigned" cuts both ways: those <i>were</i> manually assigned, on purpose, by whichever system
	 * created them, and this never overwrites that). Checked in the doc's own listed order (Loyalty first);
	 * never reverts to "no type" if nothing currently qualifies - a relationship that already earned a real
	 * type keeps it until conditions for a <i>different</i> ordinary type are met, rather than flickering
	 * back to whatever the very first threshold-less default would be.
	 */
	public static void deriveType(Relationship rel)
	{
		if(SPECIAL_ORIGIN_TYPES.contains(rel.type))
			return;

		if(rel.trust > 70F && rel.strength > 60F)
			rel.type = RelationshipType.LOYALTY;
		else if(rel.affinity > 40F && rel.trust > 50F)
			rel.type = RelationshipType.FRIENDSHIP;
		else if(rel.conflict > 60F && rel.familiarity > 40F)
			rel.type = RelationshipType.RIVALRY;
	}

	/** Appends to {@code rel}'s history, dropping the oldest entry once past {@link #MAX_HISTORY_ENTRIES}. */
	public static void recordEvent(Relationship rel, String description, long tick)
	{
		rel.history.addLast(new Relationship.RelationshipEvent(description, tick));
		while(rel.history.size() > MAX_HISTORY_ENTRIES)
			rel.history.pollFirst();
	}

	/** Which of the doc's four Instability bands {@code rel} currently falls in - see {@link InstabilityStage}'s own doc comment. */
	public static InstabilityStage stageOf(Relationship rel)
	{
		if(rel.instability >= 76F)
			return InstabilityStage.COLLAPSED;
		if(rel.instability >= 51F)
			return InstabilityStage.COLLAPSING;
		if(rel.instability >= 26F)
			return InstabilityStage.NOTICEABLE;
		return InstabilityStage.MINOR;
	}

	/** Raises or lowers {@code rel}'s Instability, clamped {@code 0-100} - unlike {@link #adjustStrength}, not scaled by stability (the doc keeps the two values independent). Does <b>not</b> itself check for collapse - see {@link #checkForCollapse}. */
	public static void adjustInstability(Relationship rel, float rawDelta, long tick)
	{
		rel.instability = Mth.clamp(rel.instability + rawDelta * rel.instabilityRate, 0F, 100F);
		rel.lastInstabilityUpdateTick = tick;
	}

	/** Lets another system react when a relationship collapses at Stage 4 - see this class's own "Instability" doc section for why a callback instead of a direct dependency. */
	public static void addCollapseListener(BiConsumer<ServerLevel, Relationship> listener)
	{
		collapseListeners.add(listener);
	}

	/**
	 * Lets another system react when one side of a relationship dies, seeing the relationship exactly as
	 * it stood at the moment of death - fired from {@link #onLivingDeath} for every relationship the dead
	 * entity was a party to, before that method's own betrayal handling mutates anything and before
	 * {@link #remove} wipes the records entirely. Same one-way-callback shape as
	 * {@link #addCollapseListener} (e.g. {@code mechanics.doom.RelationshipDoomEvents} registers here to generate
	 * Doom from the ending of a meaningful connection) - this class stays agnostic of what a death
	 * listener actually does with the relationship.
	 */
	public static void addDeathListener(DeathListener listener)
	{
		deathListeners.add(listener);
	}

	/** @see #addDeathListener */
	@FunctionalInterface
	public interface DeathListener
	{
		/**
		 * {@code deadEntityId} is one of {@code relationship}'s own {@code entityA}/{@code entityB} - use
		 * {@link Relationship#other} to find the surviving side. {@code killer} is whoever/whatever the
		 * fatal {@code DamageSource} attributes the death to (null for environmental/unattributed deaths),
		 * the same value {@link #onLivingDeath}'s own betrayal handling reads - passed through so a
		 * listener can detect "the surviving side of this relationship is also the one who killed the
		 * other side" without needing the whole {@link LivingDeathEvent}.
		 */
		void onDeath(ServerLevel level, Relationship relationship, UUID deadEntityId, @Nullable LivingEntity killer);
	}

	/**
	 * Checks {@code rel} for Stage 4 ({@link InstabilityStage#COLLAPSED}) and, if so, breaks it for real:
	 * notifies every {@link #addCollapseListener} registrant, applies the Domino Effect to nearby other
	 * relationships, then resolves {@code rel} itself by type: a {@link RelationshipType#HOSTILE}
	 * relationship is pacified in place (see {@link #pacifyHostileRelationship}), a
	 * {@link RelationshipType#KINSHIP} relationship is curdled into outright Hostile (see
	 * {@link #corruptKinshipToHostile} - a direct user design call: "Bard worsens a Kinship relationship,
	 * that relationship becomes Hostile"), and every other type is removed outright via
	 * {@link #removeRelationshipRecord}. Safe to call after any instability change, and safe to call
	 * repeatedly on the same still-intact relationship (only actually does anything once instability has
	 * crossed the threshold).
	 */
	public static void checkForCollapse(ServerLevel level, Relationship rel)
	{
		if(rel.instability < 76F || currentlyCollapsing.contains(rel.id))
			return;

		currentlyCollapsing.add(rel.id);
		try
		{
			for(BiConsumer<ServerLevel, Relationship> listener : collapseListeners)
				listener.accept(level, rel);

			applyDominoEffect(level, rel);

			if(rel.type == RelationshipType.HOSTILE)
				pacifyHostileRelationship(level, rel);
			else if(rel.type == RelationshipType.KINSHIP)
				corruptKinshipToHostile(level, rel);
			else
				removeRelationshipRecord(rel);
		}
		finally
		{
			currentlyCollapsing.remove(rel.id);
		}
	}

	/**
	 * Real fix for a real user-reported bug: collapsing a HOSTILE relationship used to just delete its
	 * record outright via {@link #removeRelationshipRecord} - which meant the very next thing to touch this
	 * pair (a damage event, a periodic {@link #ensureNaturalRelationship} scan) saw a null {@link #get} and
	 * treated that as "no relationship yet", instantly recreating a fresh HOSTILE one from scratch. That's
	 * conceptually wrong, not just annoying: a pair whose Hostile bond just collapsed has real history (they
	 * fought, it broke down) - that's not the same blank slate a stranger pair starts from, so it shouldn't
	 * be representable as literally no relationship record at all.
	 * <p>
	 * Real fix: downgrade the record in place instead of deleting it. Clears the mob's own current AI target
	 * if it's still set to the other side of this relationship (a real, visible payoff - "the discord
	 * dissolved this mob's will to fight you", not just invisible bookkeeping), resets instability back down
	 * into {@link InstabilityStage#MINOR} range, and cuts strength/stability down (a broken bond doesn't
	 * retain its old intensity). Real fix for a second, related user-reported bug: a pair of the exact same
	 * {@code EntityType} (i.e. this Hostile relationship almost certainly originated from
	 * {@link #corruptKinshipToHostile}) lands back on {@link RelationshipType#KINSHIP} directly, not the
	 * generic {@link RelationshipType#FORMING} - the original version always fell through to FORMING
	 * regardless of species, and since {@link #deriveType} never derives {@code KINSHIP} (it's a
	 * {@link #SPECIAL_ORIGIN_TYPES} entry, only ever assigned by {@link #ensureNaturalRelationship} or here),
	 * that FORMING record had no path back to Kinship at all - it just sat there until the <i>next</i>
	 * collapse deleted it outright, and only then did {@link #ensureNaturalRelationship}'s same-species scan
	 * recreate a fresh KINSHIP record, observed as "KINSHIP -&gt; HOSTILE -&gt; FORMING -&gt; (silently
	 * deleted and recreated) -&gt; KINSHIP" - a confusing, lossy four-step cycle instead of the clean two-state
	 * KINSHIP/HOSTILE oscillation Crimson Discord is actually supposed to drive. Every other Hostile
	 * relationship (mob-vs-player, mob-vs-different-species target) still explicitly sets
	 * {@link RelationshipType#FORMING} before running it back through {@link #deriveType}, exactly as before
	 * - HOSTILE is a {@link #SPECIAL_ORIGIN_TYPES} entry deriveType never auto-reassigns on its own, so this
	 * method is still the one deliberate place that steps a relationship out of that special-origin status.
	 * Leaving a real record behind either way is also what stops {@link #ensureNaturalRelationship}/
	 * {@link #onLivingDamage} from re-creating a fresh HOSTILE relationship immediately - both only ever
	 * create a new one when {@link #get} returns null, and getOrCreate never overwrites an existing record's
	 * type.
	 */
	private static void pacifyHostileRelationship(ServerLevel level, Relationship rel)
	{
		for(UUID id : List.of(rel.entityA, rel.entityB))
		{
			UUID otherSide = rel.other(id);
			if(level.getEntity(id) instanceof Mob mob && mob instanceof Enemy
					&& mob.getTarget() != null && mob.getTarget().getUUID().equals(otherSide))
			{
				mob.setTarget(null);
			}
		}

		long now = level.getGameTime();
		rel.instability = 15F;
		rel.lastInstabilityUpdateTick = now;
		rel.strength *= 0.25F;
		rel.stability = Math.max(rel.stability * 0.5F, 10F);

		boolean sameSpeciesKin = level.getEntity(rel.entityA) instanceof Mob a && level.getEntity(rel.entityB) instanceof Mob b
				&& a.getType() == b.getType();

		if(sameSpeciesKin)
		{
			rel.type = RelationshipType.KINSHIP;
			recordEvent(rel, "Reconciled back to Kinship as Crimson Discord's hold faded", now);
		}
		else
		{
			rel.type = RelationshipType.FORMING;
			deriveType(rel);
			recordEvent(rel, "Pacified by Crimson Discord", now);
		}
	}

	/**
	 * Real, project-original addition, a direct user design call: unlike every other "ordinary" type (which
	 * just gets deleted on collapse - see {@link #removeRelationshipRecord}), a broken
	 * {@link RelationshipType#KINSHIP} bond doesn't fade back to nothing - Crimson Discord actively curdles
	 * it into outright {@link RelationshipType#HOSTILE}, the positive counterpart to how
	 * {@link #pacifyHostileRelationship} softens a broken Hostile bond rather than deleting it.
	 * <p>
	 * Sets both sides to actually target each other for real (the same real-combat payoff
	 * {@link #sowDiscord}'s own Rivalry escalation uses - this is meant to read as "the discord turned them
	 * against each other", not just a hidden number flip), resets instability into the middle of
	 * {@link InstabilityStage#NOTICEABLE} (not all the way down to {@link #pacifyHostileRelationship}'s MINOR
	 * reset - a freshly curdled Hostile relationship is meant to still read as actively volatile, not
	 * already-cooled-off), and pushes affinity/trust/conflict toward what a real Hostile pair's stats should
	 * look like - not that {@link RelationshipType#HOSTILE} is ever auto-reassigned by {@link #deriveType}
	 * (it's a {@link #SPECIAL_ORIGIN_TYPES} entry), but a later {@link #pacifyHostileRelationship} pass on
	 * this same relationship reads these numbers when it eventually derives a landing type.
	 */
	private static void corruptKinshipToHostile(ServerLevel level, Relationship rel)
	{
		long now = level.getGameTime();

		rel.type = RelationshipType.HOSTILE;
		rel.instability = 40F;
		rel.lastInstabilityUpdateTick = now;
		adjustAffinity(rel, -60F);
		adjustTrust(rel, -40F);
		adjustConflict(rel, 60F);

		recordEvent(rel, "Kinship curdled into Hostility by Crimson Discord", now);

		for(UUID id : List.of(rel.entityA, rel.entityB))
		{
			UUID otherSide = rel.other(id);
			if(level.getEntity(id) instanceof Mob mob && level.getEntity(otherSide) instanceof Mob otherMob)
				mob.setTarget(otherMob);
		}
	}

	/** Nearby other relationships (see {@link Config#crimsonDiscordDominoRadius}) gain {@link Config#crimsonDiscordDominoBump} Instability - the doc's own "Domino Effect", a broken relationship destabilizing its neighbors. Deliberately never re-checks those neighbors for collapse itself - see this class's own "Infinite Domino Effect loops" doc section. */
	private static void applyDominoEffect(ServerLevel level, Relationship broken)
	{
		List<Vec3> origins = new ArrayList<>();
		for(UUID id : List.of(broken.entityA, broken.entityB))
			if(level.getEntity(id) instanceof LivingEntity entity)
				origins.add(entity.position());

		if(origins.isEmpty())
			return;

		double radiusSqr = Config.crimsonDiscordDominoRadius * Config.crimsonDiscordDominoRadius;
		long now = level.getGameTime();

		for(Relationship other : new ArrayList<>(byId.values()))
		{
			if(other == broken)
				continue;

			boolean near = false;
			for(UUID memberId : List.of(other.entityA, other.entityB))
			{
				if(!(level.getEntity(memberId) instanceof LivingEntity member))
					continue;

				for(Vec3 origin : origins)
				{
					if(origin.distanceToSqr(member.position()) <= radiusSqr)
					{
						near = true;
						break;
					}
				}
				if(near)
					break;
			}

			if(near)
				adjustInstability(other, (float) Config.crimsonDiscordDominoBump, now);
		}
	}

	private static void removeRelationshipRecord(Relationship rel)
	{
		byId.remove(rel.id);
		byPair.remove(new PairKey(rel.entityA, rel.entityB));

		Set<UUID> aIds = relationshipIdsByEntity.get(rel.entityA);
		if(aIds != null)
			aIds.remove(rel.id);

		Set<UUID> bIds = relationshipIdsByEntity.get(rel.entityB);
		if(bIds != null)
			bIds.remove(rel.id);
	}

	/**
	 * Called every tick from {@code TechBardBloodCrimsonDiscord#onPassiveTick} while the Bard has Crimson
	 * Discord toggled on - the design doc's own "Passive Effect: Social Decay". Throttled to once per
	 * {@link Config#crimsonDiscordPulseIntervalTicks}: every <i>existing</i> relationship touching a nearby
	 * entity gains {@link Config#crimsonDiscordInstabilityGainPerPulse} Instability and loses the same
	 * amount of stability, then is immediately checked for collapse. A relationship whose <i>both</i> sides
	 * are in range is only ever processed once per pulse (not doubled). {@link #sowDiscord} is the other
	 * half - manufacturing brand-new animosity between nearby entities that had no relationship at all.
	 */
	public static void pulseCrimsonDiscordAura(ServerLevel level, ServerPlayer bard)
	{
		long now = level.getGameTime();
		if(now % Config.crimsonDiscordPulseIntervalTicks != 0)
			return;

		double radius = Config.crimsonDiscordAuraRadius;
		List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, bard.getBoundingBox().inflate(radius));

		Set<UUID> processed = new HashSet<>();
		for(LivingEntity entity : nearby)
		{
			for(Relationship rel : getAllFor(entity.getUUID()))
			{
				if(!processed.add(rel.id))
					continue;

				adjustInstability(rel, (float) Config.crimsonDiscordInstabilityGainPerPulse, now);
				adjustStability(rel, (float) -Config.crimsonDiscordInstabilityGainPerPulse * 0.5F);
				checkForCollapse(level, rel);
			}
		}

		sowDiscord(level, now, nearby);
	}

	/**
	 * <b>"Even entities with no existing bond should start to turn on each other"</b> - the half of Crimson
	 * Discord that isn't just decaying pre-existing relationships. Each pulse, seeds up to
	 * {@link Config#crimsonDiscordNewRivalriesPerPulse} brand-new {@link RelationshipType#RIVALRY}
	 * relationships between random nearby {@link Mob} pairs that don't already have <i>any</i> relationship
	 * (never overwrites an existing one, positive or otherwise - that's Schism's job, not this). A freshly
	 * seeded rivalry then climbs at the exact same rate as every other relationship in range - the next
	 * pulse's own loop above picks it up automatically, since it's now "a relationship touching a nearby
	 * entity" like any other - so this doesn't skip the doc's own "slowly" pacing, it just gives previously
	 * unconnected strangers something to escalate. Once a rivalry's Instability reaches
	 * {@link Config#crimsonDiscordFightThreshold}, both mobs are actually set hostile toward each other for
	 * real (not just a hidden number) - if one kills the other, the usual death cleanup
	 * ({@link #onLivingDeath}) removes the relationship same as any other.
	 */
	private static void sowDiscord(ServerLevel level, long now, List<LivingEntity> nearby)
	{
		List<Mob> mobs = new ArrayList<>();
		for(LivingEntity entity : nearby)
			if(entity instanceof Mob mob)
				mobs.add(mob);

		if(mobs.size() >= 2)
		{
			int seeded = 0;
			int attempts = 0;
			int maxAttempts = mobs.size() * 2;

			while(seeded < Config.crimsonDiscordNewRivalriesPerPulse && attempts < maxAttempts)
			{
				attempts++;
				Mob a = mobs.get(level.getRandom().nextInt(mobs.size()));
				Mob b = mobs.get(level.getRandom().nextInt(mobs.size()));
				if(a == b || get(a.getUUID(), b.getUUID()) != null)
					continue;

				getOrCreate(a.getUUID(), b.getUUID(), RelationshipType.RIVALRY, now, 10F, 30F);
				seeded++;
			}
		}

		for(Mob mob : mobs)
		{
			for(Relationship rel : getAllFor(mob.getUUID()))
			{
				if(rel.type != RelationshipType.RIVALRY || rel.instability < Config.crimsonDiscordFightThreshold)
					continue;

				if(level.getEntity(rel.other(mob.getUUID())) instanceof Mob other)
				{
					mob.setTarget(other);
					other.setTarget(mob);
				}
			}
		}
	}

	/** Piggybacks on the same sweep as Crimson Discord's own natural Instability decay - Instability collapse-checking/decay, and (real, project-original addition) "Spending Time Together": already-related pairs currently within {@link Config#relationshipNearbyRadius} of each other passively gain Familiarity. */
	@SubscribeEvent
	private static void onLevelTick(LevelTickEvent.Post event)
	{
		if(!(event.getLevel() instanceof ServerLevel level))
			return;

		long now = level.getGameTime();
		if(now % Config.crimsonDiscordNaturalDecayIntervalTicks != 0)
			return;

		double nearbyRadiusSqr = Config.relationshipNearbyRadius * Config.relationshipNearbyRadius;

		for(Relationship rel : new ArrayList<>(byId.values()))
		{
			checkForCollapse(level, rel);
			if(!byId.containsKey(rel.id))
				continue;

			if(rel.instability > 0F)
				adjustInstability(rel, (float) -Config.crimsonDiscordNaturalDecayAmount, now);

			if(level.getEntity(rel.entityA) instanceof LivingEntity a && level.getEntity(rel.entityB) instanceof LivingEntity b
					&& a.distanceToSqr(b) <= nearbyRadiusSqr)
			{
				adjustFamiliarity(rel, (float) Config.relationshipNearbyFamiliarityGain);
			}
		}
	}

	/**
	 * Auto-detects a real vanilla {@link TamableAnimal} owner or this project's own {@link HopeGolemEntity}
	 * owner and ensures an {@link RelationshipType#OWNERSHIP} relationship exists for the pair, if one
	 * doesn't already - see this class's own "Relationship creation is lazy" doc section. Also (real,
	 * project-original addition, not from either design document by name) ensures a baseline
	 * {@link RelationshipType#HOSTILE} relationship for any real vanilla {@link Enemy}: with its current AI
	 * target if it has one (covers "and their targets" - the target doesn't have to be a player, e.g. an
	 * Iron Golem or Wolf), otherwise with the nearest player within {@link #HOSTILE_NEARBY_PLAYER_RADIUS}
	 * if any (covers "with players" as a standing fact, not only once something has actually provoked the
	 * mob into targeting them). Also (real, project-original addition, not from either design document by
	 * name - a positive counterpart to the Hostile check above, prompted by a real user question: "shouldn't
	 * entities form bonds with entities too? e.g. two {@code minestuck:imp}s should always be friendly with
	 * each other") ensures a baseline {@link RelationshipType#KINSHIP} relationship with every other real
	 * vanilla {@link Mob} of the exact same {@code EntityType} within {@link #KINSHIP_NEARBY_RADIUS} - a
	 * mob's own kind is a standing fact about it, not something it has to earn, the same reasoning
	 * {@link RelationshipType#HOSTILE} already uses for "player" rather than an earned {@link RelationshipType#RIVALRY}.
	 * Deliberately gated on {@code Mob} (not {@code LivingEntity}) so two real players sharing the same
	 * {@code EntityType#PLAYER} never auto-bond this way - only actual mobs of a matching species do. All
	 * three checks are no-ops once a relationship already exists for that specific pair - this never
	 * overwrites an existing relationship of any type, including a prior Hostile one that something else
	 * (Blood Insight, an aura pulse, etc.) has since modified, or one Crimson Discord has since pacified into
	 * a non-Hostile type (see {@link #pacifyHostileRelationship} - the whole point of downgrading in place
	 * rather than deleting is that this method sees a real, non-null record here and doesn't recreate Hostile
	 * from scratch).
	 */
	public static void ensureNaturalRelationship(LivingEntity entity, long tick)
	{
		LivingEntity owner = null;
		if(entity instanceof TamableAnimal tamable && tamable.isTame())
			owner = tamable.getOwner();
		else if(entity instanceof HopeGolemEntity golem)
			owner = golem.getOwner();

		if(owner != null && get(entity.getUUID(), owner.getUUID()) == null)
			getOrCreate(entity.getUUID(), owner.getUUID(), RelationshipType.OWNERSHIP, tick, DEFAULT_STRENGTH, DEFAULT_STABILITY);

		if(entity instanceof Enemy && entity instanceof Mob hostileMob)
		{
			LivingEntity target = hostileMob.getTarget();
			if(target == null)
				target = entity.level().getNearestPlayer(entity, HOSTILE_NEARBY_PLAYER_RADIUS);

			if(target != null && get(entity.getUUID(), target.getUUID()) == null)
				getOrCreate(entity.getUUID(), target.getUUID(), RelationshipType.HOSTILE, tick, DEFAULT_STRENGTH, DEFAULT_STABILITY);
		}

		if(entity instanceof Mob mob)
		{
			AABB area = mob.getBoundingBox().inflate(KINSHIP_NEARBY_RADIUS);
			for(Mob kin : mob.level().getEntitiesOfClass(Mob.class, area, other -> other != mob && other.getType() == mob.getType()))
			{
				if(get(mob.getUUID(), kin.getUUID()) == null)
					getOrCreate(mob.getUUID(), kin.getUUID(), RelationshipType.KINSHIP, tick, DEFAULT_STRENGTH, DEFAULT_STABILITY);
			}
		}
	}

	/** Removes every relationship {@code entityId} was a party to - real cleanup on death, the doc's own "memory leaks from removed entities" concern. */
	public static void remove(UUID entityId)
	{
		recentAttackersByVictim.remove(entityId);
		for(List<AttackRecord> records : recentAttackersByVictim.values())
			records.removeIf(record -> record.attackerId().equals(entityId));

		Set<UUID> ids = relationshipIdsByEntity.remove(entityId);
		if(ids == null)
			return;

		for(UUID id : ids)
		{
			Relationship rel = byId.remove(id);
			if(rel == null)
				continue;

			byPair.remove(new PairKey(rel.entityA, rel.entityB));
			UUID other = rel.other(entityId);
			Set<UUID> otherIds = relationshipIdsByEntity.get(other);
			if(otherIds != null)
				otherIds.remove(id);
		}
	}

	@SubscribeEvent
	private static void onAnimalTame(AnimalTameEvent event)
	{
		if(event.getAnimal().level().isClientSide())
			return;

		long tick = event.getAnimal().level().getGameTime();
		getOrCreate(event.getAnimal().getUUID(), event.getTamer().getUUID(), RelationshipType.OWNERSHIP, tick, TAME_STARTING_STRENGTH, DEFAULT_STABILITY);
	}

	@SubscribeEvent
	private static void onLivingDamage(LivingDamageEvent.Post event)
	{
		if(event.getEntity().level().isClientSide() || !(event.getEntity().level() instanceof ServerLevel level))
			return;
		if(!(event.getSource().getEntity() instanceof LivingEntity attacker))
			return;

		LivingEntity victim = event.getEntity();
		float amount = event.getNewDamage();
		if(amount <= 0)
			return;

		long now = level.getGameTime();

		// Betrayal-style strength collapse for an EXISTING positive relationship only - unchanged from before.
		Relationship existing = get(victim.getUUID(), attacker.getUUID());
		if(existing != null && isPositive(existing.type))
		{
			adjustStrength(existing, -amount, now);
			recordEvent(existing, "Damaged by " + attacker.getName().getString(), now);
		}

		// "Damage -> Conflict increases", the design doc's own unconditional behavior - applies to any
		// pair regardless of prior relationship (creating one if none exists), not just already-positive
		// ones. This is what lets a repeatedly-fought stranger organically become a known Rival over time.
		// A real vanilla Enemy attacker starts this relationship as HOSTILE outright (matching what
		// ensureNaturalRelationship would eventually assign it anyway) rather than the generic FORMING
		// default - so which one runs first (this damage event, or something later querying the mob's
		// relationships) doesn't change the outcome. getOrCreate is a no-op against an existing record
		// (including one Crimson Discord has since pacified into a non-Hostile type), so this never
		// re-labels a pacified pair back to Hostile - see pacifyHostileRelationship's own doc comment.
		if(attacker != victim)
		{
			RelationshipType initialType = attacker instanceof Enemy ? RelationshipType.HOSTILE : RelationshipType.FORMING;
			Relationship hostile = getOrCreate(victim.getUUID(), attacker.getUUID(), initialType, now, DEFAULT_STRENGTH, DEFAULT_STABILITY);
			adjustConflict(hostile, (float) Config.relationshipDamageConflictGain);
			adjustFamiliarity(hostile, (float) Config.relationshipDamageFamiliarityGain);
			deriveType(hostile);
		}

		detectFightingTogether(level, victim.getUUID(), attacker.getUUID(), now);
		detectProtection(victim, attacker, amount, now);
	}

	/**
	 * "Protection": the design doc's own priority-1 Helping event ("most reliable, easy to detect through
	 * damage events") - {@code attacker} damaging {@code victim} counts as Protection if {@code victim} (a
	 * {@link Mob}) is currently targeting someone {@code attacker} already has a positive relationship
	 * with, i.e. {@code attacker} is fighting off a threat to their own ally. Recorded between
	 * {@code attacker} and the protected ally, not between {@code attacker} and {@code victim} (the
	 * generic Damage handling above already covers that pair).
	 */
	private static void detectProtection(LivingEntity victim, LivingEntity attacker, float amount, long now)
	{
		if(!(victim instanceof Mob threatMob) || !(threatMob.getTarget() instanceof LivingEntity protectedAlly) || protectedAlly == attacker)
			return;

		Relationship allyRel = get(attacker.getUUID(), protectedAlly.getUUID());
		if(allyRel != null && isPositive(allyRel.type))
			recordPositiveInteraction(attacker, protectedAlly, RelationshipEventType.PROTECTION, amount, now);
	}

	/**
	 * The design doc's own real, pushed-not-guessed API - "make abilities/items/actions say: I helped this
	 * entity". Finds/creates a relationship between {@code helper} and {@code target} (idempotent, same as
	 * every other creation path in this class - never overwrites an existing relationship's type), applies
	 * {@code type}'s own relative Trust/Affinity/Strength/Familiarity mix scaled by {@code value} (the
	 * significance of this specific interaction - e.g. damage prevented, healing done), records history,
	 * and re-derives type. Ratios are the doc's own per-type effect lists, weighted so Rescue (all three of
	 * Trust/Affinity/Strength, each at a higher rate) genuinely outweighs Sharing (the mildest, no Strength
	 * at all) - "Rescue should be one of the strongest positive relationship events", the doc's own words.
	 */
	public static void recordPositiveInteraction(LivingEntity helper, LivingEntity target, RelationshipEventType type, float value, long tick)
	{
		if(helper == target || value <= 0F)
			return;

		Relationship rel = getOrCreate(helper.getUUID(), target.getUUID(), RelationshipType.FORMING, tick, DEFAULT_STRENGTH, DEFAULT_STABILITY);

		switch(type)
		{
			case HEALING ->
			{
				adjustTrust(rel, value * 0.3F);
				adjustAffinity(rel, value * 0.3F);
				adjustStrength(rel, value * 0.2F, tick);
				adjustFamiliarity(rel, value * 0.2F);
			}
			case PROTECTION ->
			{
				adjustTrust(rel, value * 0.4F);
				adjustStrength(rel, value * 0.3F, tick);
				adjustFamiliarity(rel, value * 0.2F);
			}
			case SHARING ->
			{
				adjustTrust(rel, value * 0.3F);
				adjustAffinity(rel, value * 0.3F);
				adjustFamiliarity(rel, value * 0.2F);
			}
			case RESCUE ->
			{
				adjustTrust(rel, value * 0.5F);
				adjustAffinity(rel, value * 0.5F);
				adjustStrength(rel, value * 0.4F, tick);
			}
		}

		recordEvent(rel, type + " from " + helper.getName().getString(), tick);
		deriveType(rel);
	}

	/** "Sharing": feeding a real vanilla {@link Animal} its own breeding food - the design doc's own priority-2 Helping event ("clear source and target"). */
	@SubscribeEvent
	private static void onEntityInteract(PlayerInteractEvent.EntityInteract event)
	{
		if(event.getLevel().isClientSide() || !(event.getTarget() instanceof Animal animal))
			return;

		ItemStack stack = event.getEntity().getItemInHand(event.getHand());
		if(!animal.isFood(stack))
			return;

		recordPositiveInteraction(event.getEntity(), animal, RelationshipEventType.SHARING, 10F, event.getLevel().getGameTime());
	}

	/**
	 * "Fighting Together": if another attacker already hit the same victim within
	 * {@link Config#relationshipFightingTogetherWindowTicks}, reinforces a relationship <i>between the two
	 * attackers themselves</i> (not either attacker and the victim - that's the generic Damage handling
	 * above). {@link #recentAttackersByVictim} is pruned to the same window on every call, so it never
	 * grows past however many distinct attackers actually hit that one victim recently.
	 */
	private static void detectFightingTogether(ServerLevel level, UUID victimId, UUID attackerId, long now)
	{
		List<AttackRecord> records = recentAttackersByVictim.computeIfAbsent(victimId, k -> new ArrayList<>());
		records.removeIf(record -> now - record.tick() > Config.relationshipFightingTogetherWindowTicks);

		for(AttackRecord prior : records)
		{
			if(prior.attackerId().equals(attackerId))
				continue;

			Relationship allies = getOrCreate(attackerId, prior.attackerId(), RelationshipType.FORMING, now, DEFAULT_STRENGTH, DEFAULT_STABILITY);
			float gain = (float) Config.relationshipFightingTogetherGain;
			adjustTrust(allies, gain);
			adjustFamiliarity(allies, gain);
			adjustStrength(allies, gain, now);
			deriveType(allies);
		}

		records.add(new AttackRecord(attackerId, now));
	}

	@SubscribeEvent
	private static void onLivingDeath(LivingDeathEvent event)
	{
		if(event.getEntity().level().isClientSide() || !(event.getEntity().level() instanceof ServerLevel level))
			return;

		LivingEntity dead = event.getEntity();
		LivingEntity killer = event.getSource().getEntity() instanceof LivingEntity livingKiller ? livingKiller : null;

		// Fires every registered DeathListener (see addDeathListener's own doc comment) against every
		// relationship the dying entity was a party to, exactly as those relationships stood BEFORE the
		// betrayal handling below mutates them (zeroing strength, etc.) and well before remove() wipes
		// them entirely - listeners that care about pre-death strength/trust/stability (e.g.
		// mechanics.doom.RelationshipDoomEvents' betrayal-bonus calculation) need to see the real numbers, not
		// whatever's left after this method's own bookkeeping already ran.
		for(Relationship rel : getAllFor(dead.getUUID()))
			for(DeathListener listener : deathListeners)
				listener.onDeath(level, rel, dead.getUUID(), killer);

		if(killer != null)
		{
			Relationship rel = get(dead.getUUID(), killer.getUUID());
			if(rel != null && isPositive(rel.type))
			{
				rel.strength = 0F;
				adjustAffinity(rel, (float) -Config.relationshipBetrayalAffinityLoss);
				adjustConflict(rel, (float) Config.relationshipBetrayalConflictGain);
				adjustStability(rel, (float) -Config.relationshipBetrayalStabilityLoss);
				recordEvent(rel, "Killed by " + killer.getName().getString(), level.getGameTime());
			}
		}

		remove(dead.getUUID());
	}

	private static boolean isPositive(RelationshipType type)
	{
		return type == RelationshipType.LOYALTY || type == RelationshipType.FRIENDSHIP
				|| type == RelationshipType.FAMILY || type == RelationshipType.OWNERSHIP
				|| type == RelationshipType.KINSHIP;
	}

	private record AttackRecord(UUID attackerId, long tick)
	{
	}

	private record PairKey(UUID a, UUID b)
	{
		PairKey
		{
			if(a.compareTo(b) > 0)
			{
				UUID tmp = a;
				a = b;
				b = tmp;
			}
		}
	}
}
