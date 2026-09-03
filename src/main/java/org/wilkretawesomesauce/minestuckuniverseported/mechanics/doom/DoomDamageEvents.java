package org.wilkretawesomesauce.minestuckuniverseported.mechanics.doom;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;

/**
 * Two of the MVP "focused set" hooks the design doc calls for, both on {@link LivingIncomingDamageEvent}
 * (the same pre-mitigation event {@code mechanics.doom.TechDoomBind} already reads/mutates), kept in one class
 * because their ordering is load-bearing: original design for this project, no 1.12.2 counterpart.
 * <p>
 * <b>Natural effect - damage amplification</b> ("greater destruction susceptibility"/"bad luck around
 * survival"): runs first ({@link EventPriority#HIGH}), amplifying incoming damage by a saturating
 * curve of the target's current Doom that asymptotically approaches but never exceeds
 * {@link #DAMAGE_AMPLIFY_MAX} - a deliberate, permanent cap so no amount of accumulated Doom
 * ever makes an entity degenerately fragile.
 * <p>
 * <b>Source - damage-severity accrual</b> ("receiving severe injuries"/"surviving near-death
 * experiences"): runs second ({@link EventPriority#LOW}), reading the already-amplified final damage
 * so severity reflects what the entity actually ends up taking (a deliberate, bounded feedback loop -
 * bounded because both curves saturate). Severity is measured as a fraction of <i>current</i> health
 * (not max), so the same formula reads consistently across wildly different max-health entities
 * without per-entity-type tuning. Trivial hits below {@link #SEVERITY_MIN_THRESHOLD}
 * contribute nothing at all; gain scales as {@code severity ^ SEVERITY_CURVE} up to
 * {@link #SEVERITY_MAX}, so only meaningful near-death hits matter much.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class DoomDamageEvents
{
	/** Asymptotic cap on how much high Doom can amplify incoming damage - 0.5 means never more than +50%. */
	private static final double DAMAGE_AMPLIFY_MAX = 0.5;
	/** The Doom value at which half of {@link #DAMAGE_AMPLIFY_MAX}'s bonus is reached (a saturating curve, not linear). */
	private static final double DAMAGE_AMPLIFY_HALF_POINT = 500.0;
	/** Hits with severity (fraction of current HP removed) below this contribute no Doom at all. */
	private static final double SEVERITY_MIN_THRESHOLD = 0.05;
	/** Max Doom gained from a single hit whose severity is 1.0 (lethal-equivalent). */
	private static final double SEVERITY_MAX = 8.0;
	/** Exponent applied to hit severity before scaling by {@link #SEVERITY_MAX}. */
	private static final double SEVERITY_CURVE = 3.0;

	private DoomDamageEvents()
	{
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	private static void onIncomingDamageAmplify(LivingIncomingDamageEvent event)
	{
		LivingEntity entity = event.getEntity();
		if(entity.level().isClientSide() || event.isCanceled())
			return;

		double doom = entity.getData(MSUAttachments.DOOM_DATA).getDoom();
		if(doom <= 0)
			return;

		double multiplier = 1.0 + DAMAGE_AMPLIFY_MAX * (doom / (doom + DAMAGE_AMPLIFY_HALF_POINT));
		event.setAmount((float)(event.getAmount() * multiplier));
	}

	@SubscribeEvent(priority = EventPriority.LOW)
	private static void onIncomingDamageAccrue(LivingIncomingDamageEvent event)
	{
		LivingEntity entity = event.getEntity();
		if(entity.level().isClientSide() || event.isCanceled())
			return;

		float currentHealth = entity.getHealth();
		if(currentHealth <= 0)
			return;

		double severity = Math.min(1.0, event.getAmount() / currentHealth);
		if(severity < SEVERITY_MIN_THRESHOLD)
			return;

		double gain = SEVERITY_MAX * Math.pow(severity, SEVERITY_CURVE);
		entity.getData(MSUAttachments.DOOM_DATA).addDoom(gain);
	}
}
