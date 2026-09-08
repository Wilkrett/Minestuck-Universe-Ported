package org.wilkretawesomesauce.minestuckuniverseported.mechanics.soul;

import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.entity.UninhabitedBodyEntity;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * The real operational core of the Soul system - design doc §36 ("SoulManager - the only place that
 * mutates {@link PlayerSoul}"), Phases 1-5 only (leaving/returning/body-theft; generic mob/animal/item
 * hosts and living-player possession are out of scope - see {@link SoulHost}'s own doc comment). Original
 * design for this project, no 1.12.2 counterpart. Static-only, mirrors
 * {@code mechanics.mind.DecisionManager}'s shape.
 * <p>
 * <b>Two different mechanisms for two different relationships</b>, per a real, direct user correction
 * after a live-client test of an earlier version - see {@link PlayerSoul}'s own doc comment for the full
 * story:
 * <ul>
 *     <li><b>Free Spirit</b> ({@link ISoulData#isFreeSpirit()}) is just the real {@link ServerPlayer}
 *     itself, given {@code Abilities#mayfly} (Creative-style toggleable flight - walk normally, fly only
 *     when the player actually double-taps jump, never full noclip) and rendered translucently client-side
 *     ({@code client.SoulSpiritRenderEvents}, gated on {@link SoulSpiritEffect}). Zero custom input/camera
 *     code - vanilla's own always-correct, zero-latency local movement just applies.</li>
 *     <li><b>Occupying a host</b> ({@link ISoulData#getCurrentHostUUID()}) still uses the "hidden anchor"
 *     design, since an {@code UninhabitedBodyEntity} genuinely isn't the local player and there's no
 *     supported way to hand a live connection to a different {@link Entity} without Mixin (confirmed:
 *     {@code ServerPlayer} isn't even in the {@code EntityType} registry, so it can't chunk-serialize or
 *     be {@code /summon}ed like a normal entity either - this project's own precedent for the closest
 *     thing, {@code util.MSUFakePlayer}, never needed to). The real anchor becomes invisible/
 *     {@code setNoGravity(true)}/{@code invulnerable(true)}; {@code ServerPlayer#setCamera(Entity)} (a
 *     real vanilla primitive, not a hack) gives its client the host's point of view; a marker
 *     {@link SoulProjectionEffect} plus a client-side {@code MovementInputUpdateEvent} hook
 *     ({@code client.SoulProjectionClientEvents}) relays WASD/jump/sneak to the host via
 *     {@code network.SoulHostInputPacket}; and {@code SoulProjectionEvents}' own per-tick handler syncs
 *     rotation anchor→host and position host→anchor - this is what keeps the anchor's own unmodified
 *     attack/use-item/interact raycasts landing in the right place with zero custom relay code needed for
 *     those actions. (A body-occupation-specific version of the same reversed-controls/locked-camera/FPS
 *     bug the free-Spirit case had is plausible here too, since the mechanism is the same - not yet
 *     reported, worth testing.)</li>
 * </ul>
 * <p>
 * <b>Physical state is swapped, never duplicated (design doc §9).</b> The real {@link ServerPlayer}'s own
 * inventory/XP/food/health/effects are the only live copy at any moment; whichever body was most recently
 * stepped out of is where that state rests while unoccupied. {@link #flushPlayerInto}/
 * {@link #restorePlayerFrom} are the only two places this ever moves, always as an atomic
 * clear-source-then-fill-destination pair, never a copy that leaves both sides populated.
 * <p>
 * <b>One asymmetry, directly required by design doc §10</b> ("returning to original body... the normal
 * player representation should resume"): {@link #inhabitBody} fully retires the target
 * {@code UninhabitedBodyEntity} (removes it, drops the anchor back to fully-normal camera-less visible
 * control) only when the target's own {@code ownerUUID} equals the reclaiming soul's own UUID. Occupying
 * anyone else's body keeps that body entity alive and visible as the puppet/avatar for as long as it's
 * occupied - this is what makes body theft show the thief walking around wearing the victim's skin/name
 * (design doc §11), and reclaiming your own body is the only case that ever fully collapses back to "just
 * a normal player." A live {@code UninhabitedBodyEntity} can therefore never end up with
 * {@code occupantSoulUUID == ownerUUID} in steady state - the moment that would happen, it retires
 * instead.
 */
public final class SoulManager
{
	private SoulManager()
	{
	}

	private static ISoulData soul(ServerPlayer player)
	{
		return player.getData(MSUAttachments.PLAYER_SOUL);
	}

	// ================================================================================================
	// Public operations
	// ================================================================================================

	/** Design doc §6 ("Leaving the Original Player Body"). Fails (returns {@code false}) if the Soul is already projecting anywhere. */
	public static boolean leaveBody(ServerPlayer player)
	{
		ISoulData soul = soul(player);
		if(soul.isProjecting())
			return false;

		UninhabitedBodyEntity body = BodyManager.getOrCreateBody(player);
		flushPlayerInto(player, body);
		becomeFreeSpirit(player, soul);
		return true;
	}

	/**
	 * Design doc §18 ("Leaving an Inhabited Host") - voluntarily step out of whatever host is currently
	 * occupied (always someone else's body per this class's own doc comment - reclaiming your own is
	 * always a full retire via {@link #inhabitBody} instead, never reached through this path) back into a
	 * free Spirit. Fails if the Soul isn't currently occupying a host.
	 */
	public static boolean leaveHost(ServerPlayer player)
	{
		ISoulData soul = soul(player);
		UUID hostId = soul.getCurrentHostUUID();
		if(hostId == null || !(player.level() instanceof ServerLevel level))
			return false;

		if(!(level.getEntity(hostId) instanceof UninhabitedBodyEntity body))
			return false;

		flushPlayerInto(player, body);
		body.onSoulLeave();
		clearAnchorProjection(player);
		becomeFreeSpirit(player, soul);
		return true;
	}

	/**
	 * Design doc §10/§11 ("Returning to the Original Body" / "Inhabiting Another Player's Body") - one
	 * shared operation for both, per §17 step 2 ("verify the Soul is currently free"): only ever reachable
	 * from a free Spirit. See this class's own doc comment for the own-body-vs-stolen-body asymmetry.
	 */
	public static boolean inhabitBody(ServerPlayer player, UninhabitedBodyEntity target)
	{
		ISoulData soul = soul(player);
		if(!soul.isFreeSpirit() || !target.canBeInhabited())
			return false;

		endFreeSpirit(player, soul);

		restorePlayerFrom(player, target);
		player.moveTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());

		if(player.getUUID().equals(target.getOwnerUUID()))
		{
			soul.setCurrentHostUUID(null);
			BodyManager.retire(target);
		}
		else
		{
			target.onSoulEnter(player.getUUID());
			soul.setCurrentHostUUID(target.getUUID());
			applyAnchorProjection(player, target);
		}

		return true;
	}

	public static boolean isProjecting(ServerPlayer player)
	{
		return soul(player).isProjecting();
	}

	/** Whichever host entity this Soul is currently occupying, or {@code null} if it isn't occupying one (free Spirit or plain normal control - neither needs a target to relay into anymore) or the host isn't currently resolvable (design doc §29). Real callers: {@code network.SoulHostInputPacket}, {@code SoulProjectionEvents}. */
	@Nullable
	public static Entity resolveProjectionTarget(ServerPlayer player)
	{
		UUID hostId = soul(player).getCurrentHostUUID();
		if(hostId == null || !(player.level() instanceof ServerLevel level))
			return null;
		return level.getEntity(hostId);
	}

	/**
	 * Real caller: {@code SoulProjectionEvents}' login/respawn hook - potion effects/invulnerable/camera/
	 * abilities don't reliably survive a respawn or relog for a fresh {@link ServerPlayer} instance, so
	 * this re-establishes them from the persisted {@link PlayerSoul} relationship. No-op if the Soul isn't
	 * currently projecting, or (for host occupation specifically) its target isn't resolvable yet (design
	 * doc §29 - retried automatically the next login/respawn, since nothing else clears the marker effect
	 * in the meantime).
	 */
	public static void reapplyProjectionState(ServerPlayer player)
	{
		ISoulData soul = soul(player);
		if(soul.isFreeSpirit())
		{
			player.addEffect(new MobEffectInstance(MSUMobEffects.SOUL_SPIRIT, -1, 0, true, false));
			player.setInvulnerable(true);
			player.getAbilities().mayfly = true;
			player.onUpdateAbilities();
		}

		Entity target = resolveProjectionTarget(player);
		if(target != null)
			applyAnchorProjection(player, target);
	}

	/**
	 * Design doc §19 ("Host Death/Destruction") - forced ejection, not a voluntary {@link #leaveHost}.
	 * Real caller: {@link BodyManager#onBodyDeath} (before it drops the body's contents - see that
	 * method's own doc comment for why the ordering matters: this flushes the anchor's current items into
	 * the body <i>first</i>, so what actually gets dropped is what the occupant was really carrying).
	 * Package-private - {@code BodyManager} is the only real caller, matching {@code mechanics.mind}'s own
	 * one-way dependency shape between its Manager and Events classes.
	 */
	static void ejectOccupant(UninhabitedBodyEntity body)
	{
		UUID occupantId = body.getOccupyingSoul();
		if(occupantId == null)
			return;

		ServerPlayer player = body.getServer().getPlayerList().getPlayer(occupantId);
		body.onSoulEject();
		if(player == null)
		{
			// Real known gap: the occupant is offline, so there's no live ServerPlayer instance to flush
			// state into/out of or grant flight to. Their PlayerSoul will still say they're occupying a
			// body that's about to be removed - reconciled the next time they log back in (see
			// SoulProjectionEvents' own load-time reconciliation).
			return;
		}

		flushPlayerInto(player, body);
		clearAnchorProjection(player);
		becomeFreeSpirit(player, soul(player));
	}

	// ================================================================================================
	// Free Spirit lifecycle - see this class's own doc comment for why this no longer touches any
	// separate entity, camera, or input relay at all.
	// ================================================================================================

	private static void becomeFreeSpirit(ServerPlayer player, ISoulData soul)
	{
		soul.setCurrentHostUUID(null);
		soul.setFreeSpirit(true);

		player.addEffect(new MobEffectInstance(MSUMobEffects.SOUL_SPIRIT, -1, 0, true, false));
		player.setInvulnerable(true);

		soul.setPreviousMayFly(player.getAbilities().mayfly);
		player.getAbilities().mayfly = true;
		player.onUpdateAbilities();
	}

	private static void endFreeSpirit(ServerPlayer player, ISoulData soul)
	{
		soul.setFreeSpirit(false);

		player.removeEffect(MSUMobEffects.SOUL_SPIRIT);
		player.setInvulnerable(false);

		player.getAbilities().flying = false;
		player.getAbilities().mayfly = soul.getPreviousMayFly();
		player.onUpdateAbilities();
	}

	// ================================================================================================
	// Host occupation anchor hide/show - unchanged camera+relay design, see this class's own doc comment.
	// ================================================================================================

	private static void applyAnchorProjection(ServerPlayer player, Entity cameraTarget)
	{
		player.addEffect(new MobEffectInstance(MSUMobEffects.SOUL_PROJECTION, -1, 0, true, false));
		player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, -1, 0, false, false));
		player.setNoGravity(true);
		player.setInvulnerable(true);
		player.setCamera(cameraTarget);
	}

	private static void clearAnchorProjection(ServerPlayer player)
	{
		player.removeEffect(MSUMobEffects.SOUL_PROJECTION);
		player.removeEffect(MobEffects.INVISIBILITY);
		player.setNoGravity(false);
		player.setInvulnerable(false);
		player.setCamera(player);
	}

	// ================================================================================================
	// Physical state swap - design doc §9, see this class's own doc comment.
	// ================================================================================================

	private static void flushPlayerInto(ServerPlayer player, UninhabitedBodyEntity body)
	{
		Inventory playerInv = player.getInventory();
		SimpleContainer bodyInv = body.getBodyInventory();
		bodyInv.clearContent();
		for(int i = 0; i < bodyInv.getContainerSize(); i++)
		{
			bodyInv.setItem(i, playerInv.getItem(i));
			playerInv.setItem(i, ItemStack.EMPTY);
		}

		for(EquipmentSlot slot : EquipmentSlot.values())
		{
			body.setItemSlot(slot, player.getItemBySlot(slot));
			player.setItemSlot(slot, ItemStack.EMPTY);
		}

		body.setXpLevel(player.experienceLevel);
		body.setXpProgress(player.experienceProgress);
		body.setTotalXp(player.totalExperience);
		player.experienceLevel = 0;
		player.experienceProgress = 0.0F;
		player.totalExperience = 0;
		syncXp(player);

		body.setBodyFoodLevel(player.getFoodData().getFoodLevel());
		body.setBodySaturation(player.getFoodData().getSaturationLevel());
		player.getFoodData().setFoodLevel(20);
		player.getFoodData().setSaturation(5.0F);

		swapAttributes(player, body);
		body.setHealth(Math.min(player.getHealth(), body.getMaxHealth()));
		player.setHealth(player.getMaxHealth());

		for(MobEffectInstance instance : List.copyOf(player.getActiveEffects()))
			body.addEffect(new MobEffectInstance(instance));
		player.removeAllEffects();
	}

	private static void restorePlayerFrom(ServerPlayer player, UninhabitedBodyEntity body)
	{
		Inventory playerInv = player.getInventory();
		SimpleContainer bodyInv = body.getBodyInventory();
		for(int i = 0; i < bodyInv.getContainerSize(); i++)
			playerInv.setItem(i, bodyInv.getItem(i));
		bodyInv.clearContent();

		for(EquipmentSlot slot : EquipmentSlot.values())
		{
			player.setItemSlot(slot, body.getItemBySlot(slot));
			body.setItemSlot(slot, ItemStack.EMPTY);
		}

		player.experienceLevel = body.getXpLevel();
		player.experienceProgress = body.getXpProgress();
		player.totalExperience = body.getTotalXp();
		syncXp(player);

		player.getFoodData().setFoodLevel(body.getBodyFoodLevel());
		player.getFoodData().setSaturation(body.getBodySaturation());

		swapAttributes(body, player);
		player.setHealth(Math.min(body.getHealth(), player.getMaxHealth()));

		player.removeAllEffects();
		for(MobEffectInstance instance : List.copyOf(body.getActiveEffects()))
			player.addEffect(new MobEffectInstance(instance));
		body.removeAllEffects();
	}

	/**
	 * Copies every attribute modifier {@code from} currently carries (Health Boost, equipment/ability-
	 * granted boosts, anything else - design doc §7's own "max-health-related state/modifiers where
	 * applicable") onto the matching attributes of {@code to}, using the exact same {@code AttributeMap}
	 * save/load round-trip vanilla itself uses to persist an entity's attributes across a save/load -
	 * attributes {@code to} doesn't define (e.g. a few player-only reach/interaction attributes
	 * {@link UninhabitedBodyEntity} has no use for) are silently skipped, matching how vanilla's own load
	 * already tolerates a snapshot naming an attribute the loader doesn't recognize. Not reset on
	 * {@code from} afterward - the next transition always overwrites it wholesale again before it could
	 * ever matter (the anchor is invulnerable and never really acts while dormant; a body's own attributes
	 * are always fully replaced by whichever Soul next stashes state into it).
	 */
	private static void swapAttributes(LivingEntity from, LivingEntity to)
	{
		to.getAttributes().load(from.getAttributes().save());
	}

	private static void syncXp(ServerPlayer player)
	{
		player.connection.send(new ClientboundSetExperiencePacket(player.experienceProgress, player.totalExperience, player.experienceLevel));
	}
}
