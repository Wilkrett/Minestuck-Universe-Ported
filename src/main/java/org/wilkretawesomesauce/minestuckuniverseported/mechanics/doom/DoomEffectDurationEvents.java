package org.wilkretawesomesauce.minestuckuniverseported.mechanics.doom;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Config;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;

/**
 * MVP natural-effect hook (2/2) - "accelerated decay"/"greater susceptibility to destruction" applied
 * to harmful potion effects specifically: a high-Doom entity's harmful ({@link MobEffectCategory#HARMFUL})
 * effects last longer, on the same saturating curve as {@link DoomDamageEvents}'s damage amplifier
 * (never more than {@link Config#doomEffectDurationExtendMax}, no matter how high Doom climbs).
 * Original design for this project, no 1.12.2 counterpart.
 * <p>
 * <b>Verified against this project's pinned NeoForge source that {@link MobEffectInstance} exposes no
 * public in-place duration mutator</b> - {@code mapDuration(Int2IntFunction)} is a pure function (it
 * computes what the duration <i>would</i> become, it doesn't assign it; only {@code tickDownDuration}
 * itself uses the result to update the private field), and there is no public duration setter at all.
 * So this hooks {@link MobEffectEvent.Applicable} (confirmed real, fired from
 * {@code CommonHooks#canMobEffectBeApplied}, called by {@code LivingEntity#addEffect} before the
 * incoming instance is merged into any existing one) and uses the guaranteed-safe cancel-and-reapply
 * pattern already proven by {@code skills.abilitech.heroAspect.life.SavingGraceEvents}
 * ({@code setResult}/{@code LivingEntity#addEffect(MobEffectInstance)}): deny the original application
 * via {@link MobEffectEvent.Applicable.Result#DO_NOT_APPLY} (confirmed the actual enum name - not
 * {@code DENY}), then re-add a copy with the scaled duration. A {@link ThreadLocal} re-entrancy guard
 * stops that reapplication from re-triggering this same handler.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class DoomEffectDurationEvents
{
	private static final ThreadLocal<Boolean> REWRITING = ThreadLocal.withInitial(() -> false);

	private DoomEffectDurationEvents()
	{
	}

	@SubscribeEvent
	private static void onApplicable(MobEffectEvent.Applicable event)
	{
		if(REWRITING.get())
			return;

		LivingEntity entity = event.getEntity();
		if(entity.level().isClientSide())
			return;

		MobEffectInstance instance = event.getEffectInstance();
		if(instance.getEffect().value().getCategory() != MobEffectCategory.HARMFUL)
			return;
		if(instance.isInfiniteDuration() || instance.getDuration() <= 0)
			return;

		double doom = entity.getData(MSUAttachments.DOOM_DATA).getDoom();
		if(doom <= 0)
			return;

		double multiplier = 1.0 + Config.doomEffectDurationExtendMax * (doom / (doom + Config.doomEffectDurationHalfPoint));
		if(multiplier <= 1.0)
			return;

		int scaledDuration = (int)Math.round(instance.getDuration() * multiplier);

		event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
		REWRITING.set(true);
		try
		{
			entity.addEffect(new MobEffectInstance(instance.getEffect(), scaledDuration, instance.getAmplifier(),
					instance.isAmbient(), instance.isVisible()));
		}
		finally
		{
			REWRITING.set(false);
		}
	}
}
