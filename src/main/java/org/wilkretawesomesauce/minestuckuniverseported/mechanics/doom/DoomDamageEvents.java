package org.wilkretawesomesauce.minestuckuniverseported.mechanics.doom;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Config;
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
 * {@link Config#doomDamageAmplifyMax} - a deliberate, permanent cap so no amount of accumulated Doom
 * ever makes an entity degenerately fragile.
 * <p>
 * <b>Source - damage-severity accrual</b> ("receiving severe injuries"/"surviving near-death
 * experiences"): runs second ({@link EventPriority#LOW}), reading the already-amplified final damage
 * so severity reflects what the entity actually ends up taking (a deliberate, bounded feedback loop -
 * bounded because both curves saturate). Severity is measured as a fraction of <i>current</i> health
 * (not max), so the same formula reads consistently across wildly different max-health entities
 * without per-entity-type tuning. Trivial hits below {@link Config#doomDamageSeverityMinThreshold}
 * contribute nothing at all; gain scales as {@code severity ^ doomDamageSeverityCurve} up to
 * {@link Config#doomDamageSeverityMax}, so only meaningful near-death hits matter much.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class DoomDamageEvents
{
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

		double multiplier = 1.0 + Config.doomDamageAmplifyMax * (doom / (doom + Config.doomDamageAmplifyHalfPoint));
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
		if(severity < Config.doomDamageSeverityMinThreshold)
			return;

		double gain = Config.doomDamageSeverityMax * Math.pow(severity, Config.doomDamageSeverityCurve);
		entity.getData(MSUAttachments.DOOM_DATA).addDoom(gain);
	}
}
