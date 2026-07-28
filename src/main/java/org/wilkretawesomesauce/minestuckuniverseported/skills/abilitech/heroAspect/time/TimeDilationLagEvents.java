package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The "real stun + chip damage" half of {@link TimeDilationEffect}'s lag-spike mechanic - see that
 * class's doc comment for the shared timing constants and the overall picture.
 * <p>
 * <b>Why this holds the entity's real, server-authoritative position instead of faking a delayed one
 * only to observers</b>: the original ask was for a target to visually rubber-band like a genuinely
 * high-ping player. That specifically means <i>other players watching them</i> should see stale
 * position updates while the target's own client and the server's real hit/collision data stay normal -
 * exactly the kind of per-observer illusion {@code timeline.vision.PastVisionSession} already builds for
 * Retrocognition, <b>except that trick is explicitly documented there as not working for real player
 * entities</b> (vanilla's own per-tick tracking sync for a real tracked entity immediately overwrites any
 * single fake position packet sent to an observer - there's no NeoForge event to intercept or delay that
 * sync, and this project doesn't use Mixin/access transformers to hook it directly). Faking it for
 * observers only was therefore not buildable within this project's real constraints, not skipped by
 * choice. What's built instead is the more honest version: the position genuinely doesn't advance for
 * everyone, including the affected player's own client - which is arguably more authentic to "being the
 * laggy one" anyway, since a real laggy player's own corrections are exactly what everyone (themselves
 * included) ends up seeing.
 * <p>
 * {@link #lastFreePosition} tracks, per affected entity, wherever it last was on a tick outside the
 * freeze window - continuously refreshed while unfrozen, then held and reapplied every tick for the
 * <i>next</i> {@link TimeDilationEffect#FREEZE_DURATION_TICKS} once a new cycle starts. Cleared the
 * instant the effect is gone so a stale anchor never lingers.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class TimeDilationLagEvents
{
	private static final Map<UUID, Vec3> lastFreePosition = new HashMap<>();

	private TimeDilationLagEvents()
	{
	}

	private static int localCycleTick(LivingEntity entity)
	{
		// "+ entity.getId()" gives each simultaneously-dilated entity its own offset into the cycle, so
		// several targets don't all spike in perfect unison.
		long gameTime = entity.level().getGameTime();
		return (int) Math.floorMod(gameTime + entity.getId(), (long) TimeDilationEffect.PULSE_CYCLE_TICKS);
	}

	/** Whether {@code entity} is inside a freeze window right now - used to gate attack/interact cancellation too. */
	public static boolean isFrozen(LivingEntity entity)
	{
		return entity.hasEffect(MSUMobEffects.TIME_DILATION) && localCycleTick(entity) < TimeDilationEffect.FREEZE_DURATION_TICKS;
	}

	@SubscribeEvent
	private static void onEntityTickPost(EntityTickEvent.Post event)
	{
		if(event.getEntity().level().isClientSide() || !(event.getEntity() instanceof LivingEntity living))
			return;

		UUID id = living.getUUID();
		if(!living.hasEffect(MSUMobEffects.TIME_DILATION))
		{
			lastFreePosition.remove(id);
			return;
		}

		int local = localCycleTick(living);
		if(local >= TimeDilationEffect.FREEZE_DURATION_TICKS)
		{
			lastFreePosition.put(id, living.position());
			return;
		}

		Vec3 anchor = lastFreePosition.get(id);
		if(anchor != null)
		{
			// ServerPlayer#teleportTo(x,y,z) routes through the real connection for a real connected
			// player (correct and expected here, unlike the MSUFakePlayer gotcha elsewhere in this
			// project) - this is exactly the same "snap the player back" correction vanilla's own
			// anti-cheat position checks use, which is why it reads as a rubber-band rather than a bug.
			if(living instanceof ServerPlayer serverPlayer)
				serverPlayer.teleportTo(anchor.x, anchor.y, anchor.z);
			else
				living.moveTo(anchor.x, anchor.y, anchor.z, living.getYRot(), living.getXRot());
			living.setDeltaMovement(Vec3.ZERO);
		}

		if(local == 0 && living.level() instanceof ServerLevel serverLevel)
			living.hurt(serverLevel.damageSources().magic(), TimeDilationEffect.DAMAGE_PER_PULSE);
	}

	@SubscribeEvent
	private static void onAttack(AttackEntityEvent event)
	{
		if(isFrozen(event.getEntity()))
			event.setCanceled(true);
	}

	@SubscribeEvent
	private static void onEntityInteract(PlayerInteractEvent.EntityInteract event)
	{
		if(isFrozen(event.getEntity()))
			event.setCanceled(true);
	}

	@SubscribeEvent
	private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event)
	{
		if(isFrozen(event.getEntity()))
			event.setCanceled(true);
	}

	@SubscribeEvent
	private static void onRightClickItem(PlayerInteractEvent.RightClickItem event)
	{
		if(isFrozen(event.getEntity()))
			event.setCanceled(true);
	}
}
