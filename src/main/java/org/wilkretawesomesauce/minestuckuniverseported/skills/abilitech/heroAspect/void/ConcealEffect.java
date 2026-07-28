package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.voidAspect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

/**
 * Real port of MinestuckUniverse (1.12.2)'s {@code potions.PotionConceal} ("VOID_CONCEAL"/"true
 * concealment") - real invisibility plus real undetectability, not just a render trick.
 * <p>
 * <b>Real API detail confirmed via this project's pinned NeoForge source, not guessed:</b>
 * {@code LivingEntity#updateInvisibilityStatus()} unconditionally resyncs the entity's real invisible
 * flag from {@code hasEffect(MobEffects.INVISIBILITY)} every tick whenever it has any active effect at
 * all - so manually calling {@code setInvisible(true)} here would just get overwritten the same tick.
 * Refreshing a real {@link MobEffects#INVISIBILITY} instance instead (kept in lockstep with Conceal's
 * own remaining duration) lets vanilla's own already-correct apply/remove machinery do the real work,
 * rather than fighting it or reinventing the removal-restore problem {@code EarthboundEffect}/
 * {@code BuildInhibitEffect} needed a companion event handler for.
 * <p>
 * Also zeroes AI detectability via {@link LivingEvent.LivingVisibilityEvent} (the same hook
 * {@code blood.TechBloodReformer}/{@code mind.TechMindCloak} already use for their own visibility
 * tricks) - vanilla Invisibility alone doesn't stop nearby mobs from still noticing an invisible target.
 * Auto-cancels itself if the entity is already Glowing, matching the original's own
 * {@code removePotionEffect(this)} self-cancel exactly (Glowing's "outline visible through walls" render
 * would otherwise defeat the point).
 * <p>
 * <b>No in-scope producer.</b> The original only ever granted this through the never-ported
 * skills/badges/Echeladder Title-Aspect ambient buff system ({@code GTEventHandler#getAspectEffects}).
 * Real, correct mechanics regardless - one real in-scope consumer already exists
 * ({@code TechVoidStep}'s "skip the ambient aura particles while concealed" check). Same "ready
 * infrastructure, no producer yet" category as {@code MindFortitudeEffect}.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ConcealEffect extends MobEffect
{
	public ConcealEffect()
	{
		super(MobEffectCategory.BENEFICIAL, 0x002346);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier)
	{
		return true;
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier)
	{
		if(entity.hasEffect(MobEffects.GLOWING))
		{
			entity.removeEffect(MSUMobEffects.CONCEAL);
			return true;
		}

		entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 25, 0, true, false));
		return true;
	}

	@SubscribeEvent
	private static void onVisibilityCheck(LivingEvent.LivingVisibilityEvent event)
	{
		if(event.getEntity().hasEffect(MSUMobEffects.CONCEAL))
			event.modifyVisibility(0.0);
	}
}
