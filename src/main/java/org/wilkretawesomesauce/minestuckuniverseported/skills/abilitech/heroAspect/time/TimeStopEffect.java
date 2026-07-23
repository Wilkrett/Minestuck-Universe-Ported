package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code potions.PotionTimeStop}, trimmed down to just the core
 * mechanic: cancels the affected entity's tick entirely, effectively freezing it in place.
 * <p>
 * Not ported: the mouse-sensitivity flattening, the GUI-blocking (only a small allowlist of screens could
 * still be opened while time-stopped), and the hurt/invulnerability-timer countdown-while-frozen handling.
 * All of that was polish/anti-exploit detail on top of the core freeze, not the mechanic itself, and all
 * of it depended on {@code IBadgeEffects} for the client-side state check the original used instead of
 * just checking the potion effect directly (a client-side capability read is a bit faster than asking the
 * server-authoritative effect list, which mattered more in 1.12.2's networking model).
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public class TimeStopEffect extends MobEffect
{
	public TimeStopEffect()
	{
		super(MobEffectCategory.HARMFUL, 0xFF2106);
	}

	@SubscribeEvent
	private static void onEntityTick(EntityTickEvent.Pre event)
	{
		if(event.getEntity() instanceof LivingEntity living && living.hasEffect(MSUMobEffects.TIME_STOP))
			event.setCanceled(true);
	}
}
