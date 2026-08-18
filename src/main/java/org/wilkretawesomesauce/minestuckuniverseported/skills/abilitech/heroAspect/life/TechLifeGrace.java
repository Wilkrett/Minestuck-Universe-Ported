package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.life;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.badgeEffects.BadgeEffects;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.RelationshipEventType;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.RelationshipManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ported 1:1 from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.life.TechLifeGrace}
 * ("Saving Grace") - hold for 3 seconds, release while aiming at a target to cast a protective ward on
 * them: the next time they'd die, {@link SavingGraceEvents} cancels it, fully heals them, and grants a
 * stacking Absorption buff instead. A target already warded (checked via
 * {@link BadgeEffects#isSavingGraced()} - a plain capability boolean, matching the original's own
 * {@code IBadgeEffects#isSavingGraced}/{@code setSavingGraced} exactly, not a timed potion effect) can't
 * be warded again, and the caster can't re-grant to a target they've already granted to and hasn't yet had
 * their grant consumed (tracked via {@link BadgeEffects#getSavingGraceTargets()}) - both real,
 * matching the original's two-layer check.
 * <p>
 * Also records the caster in {@link SavingGraceEvents#recordCaster} (real, project-original addition,
 * not part of the original tech) purely so a real Rescue relationship (see that class's own doc comment)
 * can be credited at the moment the ward actually triggers, which may be long after this cast.
 */
public class TechLifeGrace extends TechHeroAspect
{
	private static final int CHARGE_TICKS = 60;

	public TechLifeGrace()
	{
		super(Minestuckuniverseported.id("saving_grace"), EnumAspect.LIFE, 890000, MSUTechType.DEFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() <= 0)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(time >= CHARGE_TICKS && state == AbilitechKeyState.RELEASED)
		{
			LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);
			BadgeEffects badgeEffects = player.getData(MSUAttachments.BADGE_EFFECTS);
			BadgeEffects targetBadgeEffects = target == null ? null : target.getData(MSUAttachments.BADGE_EFFECTS);

			if(target == null || badgeEffects.getSavingGraceTargets().contains(target.getUUID()) || targetBadgeEffects.isSavingGraced())
				return false;

			targetBadgeEffects.setSavingGraced(true);
			badgeEffects.getSavingGraceTargets().add(target.getUUID());
			SavingGraceEvents.recordCaster(target.getUUID(), player.getUUID());
			MSUAbilitechParticles.oneshot(level, target, EnumAspect.LIFE, 20);
		}

		MSUAbilitechParticles.aura(level, player, EnumAspect.LIFE, time < CHARGE_TICKS ? 2 : 10);

		return true;
	}

	/**
	 * Spends {@link BadgeEffects#isSavingGraced()} the instant its wearer would otherwise die - cancels
	 * the death, fully heals them, and grants Absorption, matching the original's {@code LivingDeathEvent}
	 * handler exactly (same {@code LOWEST} priority, so anything else that might have prevented the death
	 * gets first say) - including its literal
	 * {@code new PotionEffect(ABSORPTION, existingAmplifier + 2, 1200, false, false)} argument order.
	 * {@code PotionEffect}'s (and its modern {@link MobEffectInstance} equivalent's) constructor is
	 * {@code (effect, duration, amplifier, ...)}, so read literally this grants a huge-amplitude,
	 * few-tick-long shield rather than a modest, long one - kept exactly as the original wrote it rather
	 * than "fixed", since this project doesn't rebalance the original's own numbers.
	 * <p>
	 * Also clears the <i>dying</i> entity's own {@link BadgeEffects#getSavingGraceTargets()} (if
	 * they're a real player - only players carry that state in this project) every time, regardless of
	 * whether they were the one graced - matching the original's own unconditional
	 * {@code badgeEffects.getSavingGraceTargets().clear()} at the end of its handler.
	 * <p>
	 * <b>Rescue</b> (real, project-original addition, wiring "Relationship Helping System"'s own
	 * {@code mechanics.relationship.RelationshipEventType#RESCUE} - the doc's own "one of the strongest
	 * positive relationship events", real caller since {@link BadgeEffects#isSavingGraced()} itself is a
	 * plain flag with no stored caster): {@link #recordCaster} is called by this tech the moment a ward is
	 * granted, recording who cast it in {@link #casterByTarget} purely so this handler can credit the actual
	 * save at the real moment it happens, potentially long after casting - the map entry is consumed
	 * (removed) here regardless of whether a real caster is still resolvable, so a saved player who was
	 * warded by someone who's since logged out or died just doesn't get a Rescue relationship recorded, not a
	 * stale/leaked entry.
	 */
	@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
	public static final class SavingGraceEvents
	{
		/** Flat Rescue significance - "one of the strongest positive relationship events" per the doc, deliberately not scaled by the saved entity's own max health (which would make saving a low-health mob look far less significant than saving a high-health one for what's mechanically the same act - a full save from death). */
		private static final float RESCUE_VALUE = 100F;

		private static final Map<UUID, UUID> casterByTarget = new HashMap<>();

		private SavingGraceEvents()
		{
		}

		/** Called by this tech's own {@code onUseTick} the instant a ward is granted - see this class's own "Rescue" doc section. */
		public static void recordCaster(UUID targetId, UUID casterId)
		{
			casterByTarget.put(targetId, casterId);
		}

		@SubscribeEvent(priority = EventPriority.LOWEST)
		private static void onDeath(LivingDeathEvent event)
		{
			LivingEntity target = event.getEntity();
			BadgeEffects targetBadgeEffects = target.getData(MSUAttachments.BADGE_EFFECTS);

			if(targetBadgeEffects.isSavingGraced())
			{
				event.setCanceled(true);
				targetBadgeEffects.setSavingGraced(false);

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
				player.getData(MSUAttachments.BADGE_EFFECTS).getSavingGraceTargets().clear();
		}
	}
}
