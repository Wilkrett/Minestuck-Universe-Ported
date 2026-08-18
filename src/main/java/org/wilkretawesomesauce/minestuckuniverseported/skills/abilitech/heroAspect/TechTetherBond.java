package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect;

import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.network.TetherBondImpactPacket;
import org.wilkretawesomesauce.minestuckuniverseported.network.TetherBondSyncPacket;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.AspectColorHandler;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Generic, reusable "tether bond" tech: hit a target to bond to it (a taut tether renders between caster
 * and target - see {@code network.TetherBondSyncPacket}/{@code client.render.TetherBondRenderer}, tinted
 * by whichever {@link #getHeroAspect()} the concrete subclass is), bonus damage on hits against the bonded
 * target while it holds, and a one-time snap of damage the instant the target leaves
 * {@link #getBondRange()} - which also breaks the bond immediately (see {@code network.TetherBondImpactPacket}
 * for the on-target flash that accompanies the snap). Extracted from {@code blood.TechBloodBond} (the
 * first, and so far only, concrete user of this) so a second aspect can reuse the same mechanic just by
 * supplying its own range/multiplier/damage numbers - every visual already keys off {@link #getHeroAspect()}
 * (color via {@code MSUAspectColors}, icon via {@code textures/foci/<aspect>.png}) rather than anything
 * Blood-specific, and this project's own asset pack already ships one {@code foci/*.png} icon per aspect,
 * which is what made this generalization worth doing rather than a one-off duplicate class.
 * <p>
 * Bonds are sticky, not last-hit: {@link #onIncomingDamage} only ever creates a bond when the attacker
 * doesn't already have one, hitting some other target never retargets an existing bond - it only ever
 * ends via {@link #getBondRange()} being exceeded, unequipping, or the target dying. A real, reported bug
 * before this was fixed: hitting anything else used to silently retarget (and NBT-swap) the bond every time.
 * <p>
 * Needs real instance-level event handling (one bonds map per concrete tech instance, not a single
 * class-wide static one two different tether techs would collide over) - but NeoForge's {@code EventBus}
 * refuses to register an object whose <i>declaring</i> class doesn't itself own every {@code @SubscribeEvent}
 * method on it (a subclass instance inheriting them from this abstract base crashes at mod-load with
 * "Only the listener object can have @SubscribeEvent methods" - a real crash hit once during development).
 * {@link Listener} is the fix: a small concrete inner class that owns the annotated methods itself and just
 * delegates back to the outer instance - registered via {@code NeoForge.EVENT_BUS.register(new Listener())}
 * in the constructor instead of {@code register(this)}.
 */
public abstract class TechTetherBond extends TechHeroAspect
{
	private final Map<UUID, UUID> bonds = new WeakHashMap<>();

	protected TechTetherBond(ResourceLocation id, EnumAspect aspect, long cost, MSUTechType techType, EnumClass... flavorClasses)
	{
		super(id, aspect, cost, techType, flavorClasses);
		NeoForge.EVENT_BUS.register(new Listener());
	}

	/** Beyond this distance from the target, the bond snaps: one damage tick, then breaks. */
	protected abstract double getBondRange();

	/** Multiplier applied to any hit landed on the bonded target while the bond holds. */
	protected abstract float getDamageMultiplier();

	/** Damage dealt exactly once, the instant the target leaves {@link #getBondRange()}. */
	protected abstract float getFarDamageAmount();

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(!(player instanceof ServerPlayer) || !(level instanceof ServerLevel serverLevel))
			return false;

		UUID targetId = bonds.get(player.getUUID());
		if(targetId == null)
			return false;

		if(!(serverLevel.getEntity(targetId) instanceof LivingEntity target) || !target.isAlive())
		{
			clearBond(serverLevel, player);
			return false;
		}

		if(player.distanceTo(target) <= getBondRange())
			return false;

		// Snaps once the moment the target leaves range, rather than repeatedly ticking damage for as
		// long as they stay out of range - the bond breaks immediately after, same as any other way it ends.
		target.hurt(serverLevel.damageSources().magic(), getFarDamageAmount());
		MSUAbilitechParticles.oneshot(level, target, 10, AspectColorHandler.get(getHeroAspect()));
		PacketDistributor.sendToPlayersInDimension(serverLevel, new TetherBondImpactPacket(target.getId(), getHeroAspect().ordinal()));

		clearBond(serverLevel, player);

		return false;
	}

	private void onIncomingDamage(LivingIncomingDamageEvent event)
	{
		if(!(event.getSource().getEntity() instanceof Player attacker))
			return;

		GodTierData godTier = attacker.getData(MSUAttachments.GOD_TIER);
		if(!godTier.isTechEquipped(this))
			return;

		LivingEntity target = event.getEntity();
		UUID existing = bonds.get(attacker.getUUID());

		if(existing == null)
		{
			bonds.put(attacker.getUUID(), target.getUUID());
			if(attacker.level() instanceof ServerLevel serverLevel)
				broadcastBond(serverLevel, attacker.getId(), target.getId());
		}
		else if(existing.equals(target.getUUID()))
		{
			event.setAmount(event.getAmount() * getDamageMultiplier());
		}
		// Hitting some other, not-yet-bonded target while already bonded does nothing here - the bond
		// is sticky until it actually breaks (range, unequip, or the bonded target dying).
	}

	@Override
	public void onUnequipped(Level level, Player player, int techSlot)
	{
		if(level instanceof ServerLevel serverLevel)
			clearBond(serverLevel, player);
		else
			bonds.remove(player.getUUID());
	}

	private void clearBond(ServerLevel level, Player player)
	{
		bonds.remove(player.getUUID());
		broadcastBond(level, player.getId(), -1);
	}

	/** Broadcasts the tether's real render-facing state - see {@code network.TetherBondSyncPacket}'s own doc comment. */
	private void broadcastBond(ServerLevel level, int casterId, int targetId)
	{
		PacketDistributor.sendToPlayersInDimension(level, new TetherBondSyncPacket(casterId, targetId, getHeroAspect().ordinal()));
	}

	/** Syncs a just-established bond to an observer who only just started tracking the caster (late joiners, anyone just entering render distance) - same trigger point {@code ConsortHatsData} uses for hats. */
	private void onStartTracking(PlayerEvent.StartTracking event)
	{
		if(event.getEntity().level().isClientSide() || !(event.getTarget() instanceof Player caster))
			return;
		if(!(event.getEntity() instanceof ServerPlayer observer) || !(caster.level() instanceof ServerLevel serverLevel))
			return;

		UUID targetId = bonds.get(caster.getUUID());
		if(targetId == null || !(serverLevel.getEntity(targetId) instanceof LivingEntity target) || !target.isAlive())
			return;

		PacketDistributor.sendToPlayer(observer, new TetherBondSyncPacket(caster.getId(), target.getId(), getHeroAspect().ordinal()));
	}

	/**
	 * Owns the actual {@code @SubscribeEvent} registrations - see this class's own doc comment for why a
	 * delegating inner class exists at all instead of just annotating the outer methods directly.
	 */
	private final class Listener
	{
		@SubscribeEvent
		private void onIncomingDamage(LivingIncomingDamageEvent event)
		{
			TechTetherBond.this.onIncomingDamage(event);
		}

		@SubscribeEvent
		private void onStartTracking(PlayerEvent.StartTracking event)
		{
			TechTetherBond.this.onStartTracking(event);
		}
	}
}
