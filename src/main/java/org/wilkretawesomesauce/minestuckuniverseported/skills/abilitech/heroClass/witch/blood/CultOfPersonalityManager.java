package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.witch.blood;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.Config;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.entity.HopeGolemEntity;
import org.wilkretawesomesauce.minestuckuniverseported.network.TetherBondSyncPacket;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.Relationship;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.RelationshipManager;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.RelationshipType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Real, project-original implementation of {@link TechBloodWitchCultOfPersonality} ("Cult of Personality",
 * Witch of Blood exclusive) - ported from a design document, not the 1.12.2 original (this tech doesn't
 * exist there). Deliberately does <b>not</b> reuse {@code heroAspect.TechTetherBond}'s actual bond-formation
 * logic (hit-to-bond, range-based snap-and-break) - that mechanic doesn't fit this ability's real spec at
 * all (a Witch links <i>other</i> entities together and doesn't need to stay near or even be part of the
 * bond, groups can have more than 2 members, and there's no range-based breaking). What <i>is</i> reused,
 * per that spec's own "Blood Influence" visual requirement, is the tether-rendering pipeline
 * ({@code network.TetherBondSyncPacket}/{@code client.render.TetherBondRenderer}) - each bond's members are
 * kept in insertion order and rendered as a chain (member[0]-member[1], member[1]-member[2], ...), one
 * synced pair per adjacent link, all tinted {@link EnumAspect#BLOOD}.
 * <p>
 * <b>Scope, matching the design doc's own "Future Expansion Ideas" split</b>: implements Core Data, bond
 * creation, Effect 1 (Shared Suffering), Effect 2 (Cult Retaliation), and Effect 3 (Blood Influence, the
 * tether visual). Bond Strength scaling, Shared Vitality, Shared Effects, Loyalty Manipulation, and Cult
 * Network merging (multiple bonds joining into one graph) are the doc's own explicitly-labeled future
 * ideas - none are built here. A member already in one bond can't be pulled into a second (no merging), and
 * retaliation only ever applies to {@link Mob} members (a real {@link net.minecraft.world.entity.player.Player}
 * member can't be force-targeted the way a mob's AI can). The design doc's own "Core Data" list also
 * mentions a Bond Strength scalar and an active/severed status flag - both left out rather than kept as
 * inert fields, since nothing here ever changes either (no severing action exists yet, "Blood Severing" is
 * explicitly future work in the same doc, and Bond Strength is explicitly "optional, for future scaling").
 * <p>
 * <b>Linking model</b>: since there's no in-game multi-select GUI, linking is done one pair at a time,
 * chaining forward - the Witch targets an entity (recorded as a pending link source), then targets a
 * second entity to actually form the link; the second entity then becomes the new pending source, so
 * repeatedly casting extends the same chain (A, then B -&gt; links A-B; then C -&gt; links B-C, extending to
 * A-B-C). Sneaking while casting clears the pending source first, letting the Witch start an entirely
 * separate group - see {@link #resetPending}.
 * <p>
 * <b>Commanding bonded Mobs</b> (real, project-original extension beyond the original design doc, added
 * because reactive death-retaliation alone left the Witch with no way to actually direct a neutral or
 * passive member - those never naturally act "for" the Witch on their own, unlike an already-hostile mob):
 * targeting an already-bonded {@link Mob} that belongs to the Witch's own cult "selects" it (see
 * {@link #selectCommandMob}) instead of attempting to link it again (which {@link #link} already refuses).
 * The Witch's next cast then either orders it to attack (targeting a different living entity - see
 * {@link #commandAttack}) or to walk to a location (targeting a block instead - see {@link #commandMoveTo}).
 * <b>Known real limitation, not a bug</b>: {@link #commandAttack} calls the same {@link Mob#setTarget} every
 * other Mob-vs-Mob targeting in this project uses, but a strictly passive {@link net.minecraft.world.entity.animal.Animal}
 * (a cow, sheep, etc.) ships <i>no</i> attack-capable AI goal at all in vanilla - nothing in its
 * {@code GoalSelector} ever reads {@code getTarget()} for combat, so commanding one to attack sets the
 * field but produces no visible behavior. Neutral mobs that already ship a melee/ranged attack goal
 * (Wolf, Bee, Enderman, etc.) - and anything already hostile - respond for real. The "walk to a spot"
 * command has no such gap: {@link net.minecraft.world.entity.ai.navigation.PathNavigation#moveTo} needs no
 * goal support and works uniformly on any {@link Mob}.
 * <p>
 * <b>Corruption</b> ({@code heroClass.prince.blood.TechPrinceBloodSchism}, "Schism", a Prince of Blood
 * exclusive tech ported from a second design document): rather than a separate parallel bond system, a
 * corrupted bond is the exact same {@link CultBond} object with {@link CultBond#corrupted} flipped on -
 * per that doc's own "reference the original Blood Bond whenever possible rather than creating duplicate
 * networks" requirement. While corrupted: {@link #onLivingDamage}'s shared damage is <i>amplified</i>
 * ({@link Config#schismDamageAmplifyFactor}) instead of reduced by {@link #SHARE_FRACTION}; {@link #onLivingDeath}'s
 * "Fractured Loyalty" makes survivors blame the <i>nearest other bonded member</i> instead of the killer
 * (falling back to the killer only if no other member is nearby - see {@link #nearestOtherMember}), with
 * that forced hostility auto-clearing after {@link Config#schismHostilityDurationTicks} (tracked in
 * {@link #hostilityExpiry}, swept every tick in {@link #onLevelTick}); and the tether visual
 * (Effect 3, "Corrupted Awareness") recolors to a fixed dark purple instead of the aspect color - see
 * {@code network.TetherBondSyncPacket}'s own {@code corrupted} field. {@link #corrupt} toggles: pressing
 * Schism on an already-corrupted bond removes the corruption (the design doc's own "Removed by the Prince"
 * cleansing path, and incidentally the same mechanism that prevents "corrupting an already Corrupted Bond").
 * {@link #cleanseByWitch} is the doc's other cleansing path ("Restored by a Witch of Blood") - any Witch of
 * Blood, not just the bond's own creator, since the doc just says "a Witch of Blood". A bond can also expire
 * back to normal on its own if {@link Config#schismCorruptionDurationTicks} is nonzero (also swept in
 * {@link #onLevelTick}). <b>Effect 4 ("Corrupted Effects" - Shared Healing/Regeneration/Strength inverting)
 * has no real consumer yet</b>: those positive effects are themselves still-unbuilt Cult of Personality
 * "Future Expansion Ideas" (Shared Vitality/Shared Effects) - nothing exists to invert until they do.
 * <p>
 * <b>Schism Aura</b> (ported from a third design document, "Schism - Anti-Blood Design Philosophy" -
 * toggled on/off like any other passive tech, via {@code TechPrinceBloodSchism#onPassiveTick}/
 * {@link #pulseSchismAura}): a passive, radius-based (default 24 blocks, {@link Config#schismAuraRadius})
 * presence effect, separate from actually casting Schism on a specific bond. Implements the doc's own
 * "Blood Bond Weakening" (an <i>uncorrupted</i> bond's shared damage is further reduced by
 * {@link Config#schismAuraWeakenFactor} for any member currently in range - see {@link #onLivingDamage})
 * and "Target Coordination Loss" (any {@link Mob} in range has a periodic chance to simply lose its
 * current target - see {@link #pulseSchismAura} for why this is the one generic, species-agnostic stand-in
 * for the doc's more specific per-species "Group AI Disruption" examples, which aren't attempted). The
 * doc's "Layer 2 - Natural Blood" section (villagers, summoned-creature owner-priority, player team-buff
 * disruption) is explicitly broader/speculative ("Possible effects") and not built - no village
 * reputation/summon-ownership/team-buff systems exist in this project to disrupt in the first place.
 * <p>
 * <b>Crimson Discord interop</b> ({@code heroClass.bard.blood.TechBardBloodCrimsonDiscord}, a Bard of
 * Blood exclusive tech ported from a fourth design document): every real {@link #link} also creates a
 * {@link RelationshipType#FAMILY} {@link Relationship} between the two entities ("Family: ... Created
 * Blood Bonds" is literally one of that system's own worked examples) - this is the one bridge between
 * {@code CultBond} and the generic {@link RelationshipManager}, and it's what lets Crimson Discord's own
 * Instability mechanic affect real Cult of Personality bonds without this class needing its own separate
 * Instability field. {@link #onLivingDamage} reads that relationship's instability for an additional
 * sharing-effectiveness reduction on top of Schism Aura's own weakening; {@link #onLivingDeath} reads it
 * for a chance ({@code Instability / }{@link Config#crimsonDiscordVengeanceFailDivisor}) that a given
 * member's retaliation (Blood Vengeance, corrupted or not) simply fails to fire - the doc's own "Blood
 * Vengeance may fail" and "AI coordination becomes unreliable". A static-init block below registers
 * {@link #onRelationshipCollapsed} with {@link RelationshipManager#addCollapseListener} so a Stage 4
 * Instability collapse severs the actual {@code CultBond} for real ("Blood Bonds automatically break") -
 * a one-way callback, not a two-way dependency; see {@link RelationshipManager}'s own doc comment for why.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class CultOfPersonalityManager
{
	static
	{
		RelationshipManager.addCollapseListener(CultOfPersonalityManager::onRelationshipCollapsed);
	}

	/** Fraction of a bonded member's own taken damage that gets transferred to the rest of their bond. */
	private static final float SHARE_FRACTION = 0.35F;
	/** How long a "pending" first-targeted entity stays eligible to be linked by a second cast. */
	private static final long PENDING_LINK_TIMEOUT_TICKS = 200;
	/** How long a selected command target (see this class's own doc comment) stays eligible to receive an order. */
	private static final long COMMAND_SELECTION_TIMEOUT_TICKS = 200;
	private static final double COMMAND_MOVE_SPEED = 1.0;
	/** How close another bonded member must be for a corrupted bond's Fractured Loyalty to blame them instead of the killer. */
	private static final double FRACTURED_LOYALTY_BLAME_RADIUS = 16.0;

	private static final Map<UUID, CultBond> bondsById = new HashMap<>();
	private static final Map<UUID, UUID> memberToBond = new HashMap<>();
	private static final Map<UUID, PendingLink> pendingLinks = new HashMap<>();
	private static final Map<UUID, PendingLink> selectedCommandMobs = new HashMap<>();
	/** Re-entrancy guard for {@link #onLivingDamage} - a transferred hit must not itself trigger another transfer. */
	private static final Set<UUID> currentlyTransferring = new HashSet<>();
	/** Mob id -> tick its Fractured Loyalty-forced target should be cleared, swept in {@link #onLevelTick}. */
	private static final Map<UUID, Long> hostilityExpiry = new HashMap<>();
	/** Prince id -> last tick their passive Schism Aura pulsed (see {@link #pulseSchismAura}) - an entry older than a few ticks is treated as stale (toggled off, unequipped, or logged off) rather than needing an explicit removal hook. */
	private static final Map<UUID, Long> activeSchismAuras = new HashMap<>();
	private static final long AURA_STALE_TICKS = 5;

	private CultOfPersonalityManager()
	{
	}

	/**
	 * Called on every real cast. Returns {@code true} if this cast actually formed a new link (there was a
	 * valid pending source), {@code false} if it only recorded {@code target} as the new pending source
	 * (the first cast of a chain, or the previous pending source expired).
	 */
	public static boolean tryLink(ServerLevel level, ServerPlayer witch, LivingEntity target)
	{
		UUID witchId = witch.getUUID();
		UUID targetId = target.getUUID();
		long now = level.getGameTime();

		PendingLink pending = pendingLinks.get(witchId);
		boolean pendingValid = pending != null && now - pending.tick <= PENDING_LINK_TIMEOUT_TICKS && !pending.entityId.equals(targetId);

		if(pendingValid)
		{
			boolean linked = link(level, witchId, pending.entityId, targetId);
			pendingLinks.put(witchId, new PendingLink(targetId, now));
			return linked;
		}

		pendingLinks.put(witchId, new PendingLink(targetId, now));
		return false;
	}

	/** Lets a Witch deliberately abandon their current chain-in-progress and start an unrelated one - see this class's own doc comment. */
	public static void resetPending(ServerPlayer witch)
	{
		pendingLinks.remove(witch.getUUID());
	}

	/** Whether {@code entity} is a member of a bond actually owned by {@code witch} - see this class's own "Commanding bonded Mobs" doc section. */
	public static boolean isOwnCultMember(ServerPlayer witch, LivingEntity entity)
	{
		CultBond bond = getBond(entity.getUUID());
		return bond != null && bond.witchOwner.equals(witch.getUUID());
	}

	/** Selects {@code mob} as the target of the Witch's next command (attack or move-to) - see this class's own "Commanding bonded Mobs" doc section. */
	public static void selectCommandMob(ServerLevel level, ServerPlayer witch, Mob mob)
	{
		selectedCommandMobs.put(witch.getUUID(), new PendingLink(mob.getUUID(), level.getGameTime()));
	}

	/**
	 * Resolves and consumes the Witch's currently-selected command target, if any (one-shot - the selection
	 * is cleared regardless of whether a real {@link Mob} was actually resolved). {@code null} if nothing is
	 * selected, the selection expired, or the previously-selected entity is no longer a valid, alive member
	 * of the Witch's own cult (left, died, or the bond broke since selecting it).
	 */
	@Nullable
	public static Mob takeSelectedCommandMob(ServerLevel level, ServerPlayer witch)
	{
		UUID witchId = witch.getUUID();
		PendingLink selection = selectedCommandMobs.remove(witchId);
		if(selection == null || level.getGameTime() - selection.tick > COMMAND_SELECTION_TIMEOUT_TICKS)
			return null;

		if(level.getEntity(selection.entityId) instanceof Mob mob && mob.isAlive() && isOwnCultMember(witch, mob))
			return mob;

		return null;
	}

	/** Orders {@code mob} to attack {@code target} - see this class's own doc comment for the real vanilla-AI limitation on strictly passive mobs. */
	public static void commandAttack(Mob mob, LivingEntity target)
	{
		mob.setTarget(target);
	}

	/** Orders {@code mob} to walk to {@code pos} - works uniformly on any {@link Mob}, no goal-support caveat like {@link #commandAttack}. */
	public static void commandMoveTo(Mob mob, BlockPos pos)
	{
		mob.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, COMMAND_MOVE_SPEED);
	}

	/**
	 * Links two entities. If {@code sourceId} is already in a bond, {@code targetId} joins that same bond
	 * (extending the chain); otherwise a brand new bond is created containing just the two of them. If
	 * {@code targetId} is already in a (different) bond, the link is refused entirely - see this class's
	 * own doc comment for why bond-network merging is out of scope.
	 */
	private static boolean link(ServerLevel level, UUID witchId, UUID sourceId, UUID targetId)
	{
		if(memberToBond.containsKey(targetId))
			return false;

		UUID existingBondId = memberToBond.get(sourceId);
		CultBond bond;
		if(existingBondId != null)
		{
			bond = bondsById.get(existingBondId);
		}
		else
		{
			bond = new CultBond(witchId, level.getGameTime());
			bond.members.add(sourceId);
			bondsById.put(bond.id, bond);
			memberToBond.put(sourceId, bond.id);
		}

		bond.members.add(targetId);
		memberToBond.put(targetId, bond.id);

		// Bridges this bond to the generic Relationship system - see this class's own "Crimson Discord
		// interop" doc section. Default strength/stability match RelationshipManager's own OWNERSHIP
		// defaults; nothing about Cult of Personality itself reads this relationship back, only Crimson
		// Discord/Mage of Blood/Schism do.
		RelationshipManager.getOrCreate(sourceId, targetId, RelationshipType.FAMILY, level.getGameTime(), 30F, 50F);

		broadcastChain(level, bond);
		return true;
	}

	/** Registered with {@link RelationshipManager#addCollapseListener} in this class's own static-init block - severs whichever real {@code CultBond} (if any) {@code rel}'s two sides belong to, once their {@link RelationshipType#FAMILY} relationship collapses from Instability. A no-op for any other relationship type. */
	private static void onRelationshipCollapsed(ServerLevel level, Relationship rel)
	{
		if(rel.type != RelationshipType.FAMILY)
			return;

		severBondFor(level, rel.entityA);
		severBondFor(level, rel.entityB);
	}

	private static void severBondFor(ServerLevel level, UUID memberId)
	{
		UUID bondId = memberToBond.get(memberId);
		if(bondId == null)
			return;

		CultBond bond = bondsById.get(bondId);
		if(bond != null)
			severBond(level, bond);
	}

	/** Fully removes {@code bond} and clears its tether visual for every currently-resolvable adjacent pair. */
	private static void severBond(ServerLevel level, CultBond bond)
	{
		List<UUID> members = new ArrayList<>(bond.members);
		bondsById.remove(bond.id);
		for(UUID id : members)
			memberToBond.remove(id);

		for(int i = 0; i + 1 < members.size(); i++)
		{
			if(level.getEntity(members.get(i)) instanceof LivingEntity member)
				PacketDistributor.sendToPlayersInDimension(level, new TetherBondSyncPacket(member.getId(), -1, 0));
		}
	}

	@Nullable
	public static CultBond getBond(UUID memberId)
	{
		UUID bondId = memberToBond.get(memberId);
		return bondId == null ? null : bondsById.get(bondId);
	}

	/** Whether {@code entity} is currently in a bond that's actively corrupted - see this class's own "Corruption" doc section. */
	public static boolean isCorrupted(LivingEntity entity)
	{
		CultBond bond = getBond(entity.getUUID());
		return bond != null && bond.corrupted;
	}

	/**
	 * Corrupts the bond {@code target} belongs to (Schism's own doc-required "target must currently belong
	 * to an active Blood Bond"), or removes an existing corruption if it's already corrupted (the Prince's
	 * own "Removed by the Prince" cleansing path - also what prevents double-corrupting the same bond).
	 */
	public static CorruptResult corrupt(ServerLevel level, ServerPlayer prince, LivingEntity target)
	{
		CultBond bond = getBond(target.getUUID());
		if(bond == null)
			return CorruptResult.NOT_BONDED;

		if(bond.corrupted)
		{
			clearCorruption(bond);
			broadcastChain(level, bond);
			return CorruptResult.CLEANSED;
		}

		bond.corrupted = true;
		bond.corruptedBy = prince.getUUID();
		bond.corruptionTick = level.getGameTime();
		bond.corruptionExpiryTick = Config.schismCorruptionDurationTicks > 0
				? bond.corruptionTick + Config.schismCorruptionDurationTicks
				: -1;

		broadcastChain(level, bond);
		return CorruptResult.CORRUPTED;
	}

	/** The design doc's other cleansing path - "Restored by a Witch of Blood", not specifically the bond's own creator. */
	public static boolean cleanseByWitch(ServerLevel level, LivingEntity target)
	{
		CultBond bond = getBond(target.getUUID());
		if(bond == null || !bond.corrupted)
			return false;

		clearCorruption(bond);
		broadcastChain(level, bond);
		return true;
	}

	private static void clearCorruption(CultBond bond)
	{
		bond.corrupted = false;
		bond.corruptedBy = null;
		bond.corruptionTick = -1;
		bond.corruptionExpiryTick = -1;
	}

	/**
	 * Called every tick from {@code TechPrinceBloodSchism#onPassiveTick} while the Prince has the Schism
	 * Aura toggled on - refreshes their presence (see {@link #activeSchismAuras}) and, throttled to once
	 * per {@link Config#schismAuraDisruptionIntervalTicks}, rolls "Target Coordination Loss" for every
	 * {@link Mob} in range: a {@link Config#schismAuraDisruptionChance} chance to simply drop whatever
	 * target it currently has. Deliberately generic/species-agnostic - the design doc's own worked examples
	 * (wolves abandoning packmates, piglins losing coordination, bees turning on each other) all boil down
	 * to "an allied group's shared target/cohesion breaks down", and clearing {@code getTarget()} is the one
	 * generic vanilla lever that actually disrupts that for any {@link Mob} species without needing
	 * per-species Mixin hooks into whatever private group/formation AI it might have.
	 * <p>
	 * Also implements the "Relationship System" doc's own Layer 2 "Summoned Creatures" bullets for real,
	 * generic {@link RelationshipType#OWNERSHIP} relationships (a tamed vanilla {@link TamableAnimal} or
	 * this project's own {@link HopeGolemEntity} ally - see {@link RelationshipManager#ensureNaturalRelationship}):
	 * every pulse, <i>any</i> nearby {@link LivingEntity} with an Ownership relationship loses
	 * {@link Config#schismAuraOwnershipDecay} strength <i>and</i> stability ("Bond strength decreases over
	 * time", and a weaker relationship resists future weakening even less - see
	 * {@link RelationshipManager#adjustStrength}'s own resistance formula for why decaying stability
	 * compounds this) - not restricted to {@link Mob}s, since the relationship itself (and its decay) is a
	 * plain data fact about a pair, independent of whether either side even has AI to disrupt. The
	 * <i>target-clearing/redirect</i> half below is necessarily {@link Mob}-only, since {@code getTarget()}/
	 * {@code setTarget()} only exist on {@link Mob} - a relationship whose owned side is some other
	 * {@link LivingEntity} still gets weakened, it just has no "target" to lose. When Target Coordination
	 * Loss rolls for an owned mob, it doesn't just clear the target: if another creature owned by the
	 * <i>same</i> owner is also nearby, it's redirected onto that sibling instead (the doc's own "Wolf C
	 * -&gt; Attacks Wolf A" example, and "Target other summons"), falling back to a plain clear if no
	 * sibling is in range.
	 * <p>
	 * <b>Known gap, not attempted</b>: the doc's more specific "Group AI Disruption" bullets (pack
	 * defense, raid formation, coordinated attacks as such) would need reaching into each species' own
	 * private goal/sensor internals - there's no generic vanilla hook for "temporarily disable this mob's
	 * group behavior" the way there is for clearing a target. Same category of gap as this project's other
	 * documented "no clean modern API for this" limitations.
	 */
	public static void pulseSchismAura(ServerLevel level, ServerPlayer prince)
	{
		long now = level.getGameTime();
		activeSchismAuras.put(prince.getUUID(), now);

		if(now % Config.schismAuraDisruptionIntervalTicks != 0)
			return;

		double radius = Config.schismAuraRadius;
		List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, prince.getBoundingBox().inflate(radius));

		for(LivingEntity entity : nearby)
		{
			RelationshipManager.ensureNaturalRelationship(entity, now);
			Relationship ownership = findOwnershipRelationship(entity);
			if(ownership != null)
			{
				RelationshipManager.adjustStrength(ownership, (float) -Config.schismAuraOwnershipDecay, now);
				RelationshipManager.adjustStability(ownership, (float) -Config.schismAuraOwnershipDecay);
			}

			if(!(entity instanceof Mob mob) || mob.getTarget() == null || level.getRandom().nextDouble() >= Config.schismAuraDisruptionChance)
				continue;

			Mob sibling = ownership != null ? findSiblingSummon(nearby, mob, ownership) : null;
			mob.setTarget(sibling);
		}
	}

	@Nullable
	private static Relationship findOwnershipRelationship(LivingEntity entity)
	{
		for(Relationship rel : RelationshipManager.getAllFor(entity.getUUID()))
			if(rel.type == RelationshipType.OWNERSHIP)
				return rel;
		return null;
	}

	/** Another {@link Mob} in {@code nearby} owned by the same party as {@code ownership}'s other side, if any. */
	@Nullable
	private static Mob findSiblingSummon(List<LivingEntity> nearby, Mob mob, Relationship ownership)
	{
		UUID ownerId = ownership.other(mob.getUUID());
		for(LivingEntity other : nearby)
		{
			if(other == mob || !(other instanceof Mob otherMob))
				continue;

			for(Relationship rel : RelationshipManager.getAllFor(other.getUUID()))
			{
				if(rel.type == RelationshipType.OWNERSHIP && rel.other(other.getUUID()).equals(ownerId))
					return otherMob;
			}
		}
		return null;
	}

	/** Whether {@code entity} is currently within any active Schism Aura's radius - see {@link #pulseSchismAura}. */
	private static boolean isNearActiveSchismAura(ServerLevel level, LivingEntity entity)
	{
		if(activeSchismAuras.isEmpty())
			return false;

		long now = level.getGameTime();
		double radiusSqr = Config.schismAuraRadius * Config.schismAuraRadius;

		Iterator<Map.Entry<UUID, Long>> it = activeSchismAuras.entrySet().iterator();
		while(it.hasNext())
		{
			Map.Entry<UUID, Long> entry = it.next();
			if(now - entry.getValue() > AURA_STALE_TICKS)
			{
				it.remove();
				continue;
			}

			if(level.getPlayerByUUID(entry.getKey()) instanceof ServerPlayer prince && prince.distanceToSqr(entity) <= radiusSqr)
				return true;
		}

		return false;
	}

	/** Resolves a bond's members to real, currently-alive entities, quietly dropping any that are no longer valid (removed/unloaded) - real prevention of the design doc's "memory leaks from removed entities"/"orphaned Blood Bonds" concerns, done lazily at read time rather than via a dedicated cleanup pass. Also opportunistically expires a corrupted bond past {@link CultBond#corruptionExpiryTick} - the real per-tick sweep is {@link #onLevelTick}, this just catches it sooner on any path that already touches the bond. */
	private static List<LivingEntity> getAliveMembers(ServerLevel level, CultBond bond)
	{
		if(bond.corrupted && bond.corruptionExpiryTick > 0 && level.getGameTime() >= bond.corruptionExpiryTick)
			clearCorruption(bond);

		List<LivingEntity> alive = new ArrayList<>();
		Iterator<UUID> it = bond.members.iterator();
		while(it.hasNext())
		{
			UUID id = it.next();
			if(level.getEntity(id) instanceof LivingEntity living && living.isAlive())
			{
				alive.add(living);
			}
			else
			{
				it.remove();
				memberToBond.remove(id);
			}
		}

		if(bond.members.size() < 2)
		{
			bondsById.remove(bond.id);
			for(UUID id : bond.members)
				memberToBond.remove(id);
		}

		return alive;
	}

	/** Broadcasts the chain's current member-to-member links for {@code client.render.TetherBondRenderer} - see this class's own doc comment for why a chain of pairs, not a true graph. */
	private static void broadcastChain(ServerLevel level, CultBond bond)
	{
		List<LivingEntity> alive = getAliveMembers(level, bond);
		for(int i = 0; i + 1 < alive.size(); i++)
		{
			PacketDistributor.sendToPlayersInDimension(level,
					new TetherBondSyncPacket(alive.get(i).getId(), alive.get(i + 1).getId(), EnumAspect.BLOOD.ordinal(), bond.corrupted));
		}
	}

	@SubscribeEvent
	private static void onLivingDamage(LivingDamageEvent.Post event)
	{
		if(event.getEntity().level().isClientSide() || !(event.getEntity().level() instanceof ServerLevel level))
			return;

		LivingEntity hurt = event.getEntity();
		if(currentlyTransferring.contains(hurt.getUUID()))
			return;

		CultBond bond = getBond(hurt.getUUID());
		if(bond == null)
			return;

		float amount = event.getNewDamage();
		if(amount <= 0)
			return;

		// Corrupted bonds amplify shared damage instead of reducing it - Schism's own "Shared Pain" (design
		// doc: 10 damage in, 15 out at the default 1.5x) versus Cult of Personality's uncorrupted SHARE_FRACTION.
		float shared;
		if(bond.corrupted)
		{
			shared = amount * (float) Config.schismDamageAmplifyFactor;
		}
		else
		{
			// The passive Schism Aura's own separate, milder "Blood Bond Weakening" - halves (by default) an
			// otherwise-normal bond's own sharing, distinct from (and superseded by, see the corrupted branch
			// above) fully corrupting it outright.
			shared = amount * SHARE_FRACTION;
			if(isNearActiveSchismAura(level, hurt))
				shared *= (float) Config.schismAuraWeakenFactor;
		}

		// Crimson Discord's own "Blood Bonds rapidly lose effectiveness" as Instability rises - a third,
		// independent axis on top of corruption/Schism Aura, applied regardless of which branch above ran.
		float instability = averageBondInstability(bond);
		if(instability > 0F)
			shared *= Math.max(0F, 1F - instability / 130F);

		if(shared <= 0)
			return;

		for(LivingEntity member : getAliveMembers(level, bond))
		{
			if(member == hurt)
				continue;

			currentlyTransferring.add(member.getUUID());
			try
			{
				member.hurt(event.getSource(), shared);
			}
			finally
			{
				currentlyTransferring.remove(member.getUUID());
			}
		}
	}

	@SubscribeEvent
	private static void onLivingDeath(LivingDeathEvent event)
	{
		if(event.getEntity().level().isClientSide() || !(event.getEntity().level() instanceof ServerLevel level))
			return;

		LivingEntity dead = event.getEntity();
		UUID bondId = memberToBond.remove(dead.getUUID());
		if(bondId == null)
			return;

		CultBond bond = bondsById.get(bondId);
		if(bond == null)
			return;

		bond.members.remove(dead.getUUID());

		LivingEntity killer = event.getSource().getEntity() instanceof LivingEntity livingKiller ? livingKiller : null;
		List<LivingEntity> survivors = getAliveMembers(level, bond);

		// Crimson Discord's own "Blood Vengeance may fail"/"AI coordination becomes unreliable" - a
		// per-member chance, scaled by how unstable the bond's own Relationship has become, that this
		// member's retaliation (corrupted Fractured Loyalty or normal killer-targeting alike) just doesn't
		// fire this time. 0 at 0 Instability, so this is a pure no-op for every bond Crimson Discord has
		// never touched.
		float instability = averageBondInstability(bond);

		if(bond.corrupted)
		{
			// Fractured Loyalty: survivors blame the nearest other bonded member instead of avenging the
			// fallen one, falling back to the killer only if no other member is close enough.
			for(LivingEntity member : survivors)
			{
				if(!(member instanceof Mob mob) || vengeanceFails(level, instability))
					continue;

				LivingEntity blame = nearestOtherMember(member, survivors);
				if(blame == null)
					blame = killer;
				if(blame == null)
					continue;

				mob.setTarget(blame);
				hostilityExpiry.put(mob.getUUID(), level.getGameTime() + Config.schismHostilityDurationTicks);
			}
		}
		else if(killer != null)
		{
			for(LivingEntity member : survivors)
			{
				if(member instanceof Mob mob && !vengeanceFails(level, instability))
					mob.setTarget(killer);
			}
		}

		if(bondsById.containsKey(bondId))
			broadcastChain(level, bond);
	}

	private static boolean vengeanceFails(ServerLevel level, float instability)
	{
		return instability > 0F && level.getRandom().nextDouble() < instability / Config.crimsonDiscordVengeanceFailDivisor;
	}

	/** Average Instability across the bond's own chain-adjacent {@link RelationshipType#FAMILY} relationships (see {@link #link}) - {@code 0} for a bond Crimson Discord has never touched. */
	private static float averageBondInstability(CultBond bond)
	{
		List<UUID> members = new ArrayList<>(bond.members);
		float total = 0F;
		int count = 0;

		for(int i = 0; i + 1 < members.size(); i++)
		{
			Relationship rel = RelationshipManager.get(members.get(i), members.get(i + 1));
			if(rel != null)
			{
				total += rel.instability;
				count++;
			}
		}

		return count == 0 ? 0F : total / count;
	}

	/** Nearest other member of {@code survivors} to {@code from} within {@link #FRACTURED_LOYALTY_BLAME_RADIUS}, or {@code null} if none are close enough. */
	@Nullable
	private static LivingEntity nearestOtherMember(LivingEntity from, List<LivingEntity> survivors)
	{
		LivingEntity nearest = null;
		double nearestDistSqr = FRACTURED_LOYALTY_BLAME_RADIUS * FRACTURED_LOYALTY_BLAME_RADIUS;

		for(LivingEntity other : survivors)
		{
			if(other == from)
				continue;

			double distSqr = from.distanceToSqr(other);
			if(distSqr <= nearestDistSqr)
			{
				nearest = other;
				nearestDistSqr = distSqr;
			}
		}

		return nearest;
	}

	/** Sweeps every tick for two time-based reversions: an expired corrupted bond (see {@link CultBond#corruptionExpiryTick}) and expired Fractured Loyalty forced-target hostility (see {@link #hostilityExpiry}). */
	@SubscribeEvent
	private static void onLevelTick(LevelTickEvent.Post event)
	{
		if(!(event.getLevel() instanceof ServerLevel level))
			return;

		long now = level.getGameTime();

		for(CultBond bond : new ArrayList<>(bondsById.values()))
		{
			if(bond.corrupted && bond.corruptionExpiryTick > 0 && now >= bond.corruptionExpiryTick)
			{
				clearCorruption(bond);
				broadcastChain(level, bond);
			}
		}

		if(!hostilityExpiry.isEmpty())
		{
			Iterator<Map.Entry<UUID, Long>> it = hostilityExpiry.entrySet().iterator();
			while(it.hasNext())
			{
				Map.Entry<UUID, Long> entry = it.next();
				if(now < entry.getValue())
					continue;

				if(level.getEntity(entry.getKey()) instanceof Mob mob)
					mob.setTarget(null);
				it.remove();
			}
		}
	}

	/** Result of {@link #corrupt} - what {@code heroClass.prince.blood.TechPrinceBloodSchism} actually did. */
	public enum CorruptResult
	{
		/** {@code target} isn't currently part of any active Blood Bond - nothing to corrupt. */
		NOT_BONDED,
		/** {@code target}'s bond was already corrupted, and this cast just removed that corruption. */
		CLEANSED,
		/** {@code target}'s bond was clean, and this cast corrupted it. */
		CORRUPTED
	}

	private static final class PendingLink
	{
		final UUID entityId;
		final long tick;

		PendingLink(UUID entityId, long tick)
		{
			this.entityId = entityId;
			this.tick = tick;
		}
	}

	public static final class CultBond
	{
		public final UUID id = UUID.randomUUID();
		public final UUID witchOwner;
		public final long createdTick;
		public final LinkedHashSet<UUID> members = new LinkedHashSet<>();

		public boolean corrupted = false;
		@Nullable
		public UUID corruptedBy = null;
		public long corruptionTick = -1;
		/** {@code -1} means "never expires" - see {@link Config#schismCorruptionDurationTicks}. */
		public long corruptionExpiryTick = -1;

		private CultBond(UUID witchOwner, long createdTick)
		{
			this.witchOwner = witchOwner;
			this.createdTick = createdTick;
		}
	}
}
