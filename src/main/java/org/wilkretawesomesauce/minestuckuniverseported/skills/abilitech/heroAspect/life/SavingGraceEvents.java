package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.life;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.AbilitechLoadout;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.RelationshipEventType;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.RelationshipManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Spends {@code TechLifeGrace}'s {@link SavingGracedEffect} the instant its wearer would otherwise die -
 * cancels the death, fully heals them, and grants Absorption, matching the original's
 * {@code LivingDeathEvent} handler exactly (same {@code LOWEST} priority, so anything else that might
 * have prevented the death gets first say) - including its literal
 * {@code new PotionEffect(ABSORPTION, existingAmplifier + 2, 1200, false, false)} argument order.
 * {@code PotionEffect}'s (and its modern {@link MobEffectInstance} equivalent's) constructor is
 * {@code (effect, duration, amplifier, ...)}, so read literally this grants a huge-amplitude,
 * few-tick-long shield rather than a modest, long one - kept exactly as the original wrote it rather
 * than "fixed", since this project doesn't rebalance the original's own numbers.
 * <p>
 * Also clears the <i>dying</i> entity's own {@link AbilitechLoadout#getSavingGraceTargets()} (if
 * they're a real player - only players carry that state in this project) every time, regardless of
 * whether they were the one graced - matching the original's own unconditional
 * {@code badgeEffects.getSavingGraceTargets().clear()} at the end of its handler.
 * <p>
 * <b>Rescue</b> (real, project-original addition, wiring "Relationship Helping System"'s own
 * {@code mechanics.relationship.RelationshipEventType#RESCUE} - the doc's own "one of the strongest positive
 * relationship events", real caller since {@code SavingGracedEffect} itself is a plain marker effect with
 * no stored caster): {@link #recordCaster} is called by {@code TechLifeGrace} the moment a ward is
 * granted, recording who cast it in {@link #casterByTarget} purely so this handler can credit the actual
 * save at the real moment it happens, potentially long after casting - the map entry is consumed
 * (removed) here regardless of whether a real caster is still resolvable, so a saved player who was
 * warded by someone who's since logged out or died just doesn't get a Rescue relationship recorded, not a
 * stale/leaked entry.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class SavingGraceEvents
{
	/** Flat Rescue significance - "one of the strongest positive relationship events" per the doc, deliberately not scaled by the saved entity's own max health (which would make saving a low-health mob look far less significant than saving a high-health one for what's mechanically the same act - a full save from death). */
	private static final float RESCUE_VALUE = 100F;

	private static final Map<UUID, UUID> casterByTarget = new HashMap<>();

	private SavingGraceEvents()
	{
	}

	/** Called by {@code TechLifeGrace} the instant a ward is granted - see this class's own "Rescue" doc section. */
	public static void recordCaster(UUID targetId, UUID casterId)
	{
		casterByTarget.put(targetId, casterId);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	private static void onDeath(LivingDeathEvent event)
	{
		LivingEntity target = event.getEntity();

		if(target.hasEffect(MSUMobEffects.SAVING_GRACED))
		{
			event.setCanceled(true);
			target.removeEffect(MSUMobEffects.SAVING_GRACED);

			MSUAbilitechParticles.burst(target.level(), target, EnumAspect.LIFE, 20);
			target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.WITHER_SPAWN, target.getSoundSource(), 1.0F, 3.0F);

			target.setHealth(target.getMaxHealth());

			int amplifier = target.hasEffect(MobEffects.ABSORPTION) ? target.getEffect(MobEffects.ABSORPTION).getAmplifier() : 0;
			target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, amplifier + 2, 1200, false, false));

			UUID casterId = casterByTarget.remove(target.getUUID());
			if(casterId != null && target.level() instanceof ServerLevel serverLevel && serverLevel.getEntity(casterId) instanceof LivingEntity caster)
				RelationshipManager.recordPositiveInteraction(caster, target, RelationshipEventType.RESCUE, RESCUE_VALUE, serverLevel.getGameTime());
		}

		if(target instanceof Player player)
			player.getData(MSUAttachments.ABILITECH_LOADOUT).getSavingGraceTargets().clear();
	}
}
