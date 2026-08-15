package org.wilkretawesomesauce.minestuckuniverseported;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.TechSling;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.blood.BleedingEffect;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.breath.TechBreathWindVessel;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.doom.BuildInhibitEffect;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.doom.DecayEffect;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.doom.DecayproofEffect;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.doom.EarthboundEffect;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.heart.GodTierLockEffect;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.heart.TechSoulStun;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.hope.TechHopeyShit;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.life.SavingGracedEffect;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.mind.CalculatingEffect;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.mind.TechMindConfusion;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.mind.TechMindControl;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.mind.MindFortitudeEffect;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.rage.BerserkEffect;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.rage.FrenziedEffect;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.rage.RageShiftedEffect;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time.TechTimeAccelerateSelf;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time.TechTimeDilation;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time.TimeStopEffect;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.voidAspect.ConcealEffect;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.voidAspect.TechVoidStep;
import org.wilkretawesomesauce.minestuckuniverseported.godtier.GodTierComebackEffect;

public final class MSUMobEffects
{
	public static final DeferredRegister<MobEffect> REGISTER = DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, Minestuckuniverseported.MODID);

	public static final DeferredHolder<MobEffect, TimeStopEffect> TIME_STOP = REGISTER.register("time_stop", TimeStopEffect::new);
	public static final DeferredHolder<MobEffect, TechTimeDilation.TimeDilationEffect> TIME_DILATION = REGISTER.register("time_dilation", TechTimeDilation.TimeDilationEffect::new);
	public static final DeferredHolder<MobEffect, BleedingEffect> BLEEDING = REGISTER.register("bleeding", BleedingEffect::new);
	public static final DeferredHolder<MobEffect, TechBreathWindVessel.WindFormedEffect> WIND_FORMED = REGISTER.register("wind_formed", TechBreathWindVessel.WindFormedEffect::new);
	public static final DeferredHolder<MobEffect, TechSoulStun.SoulShockedEffect> SOUL_SHOCKED = REGISTER.register("soul_shocked", TechSoulStun.SoulShockedEffect::new);
	public static final DeferredHolder<MobEffect, TechHopeyShit.HopingEffect> HOPING = REGISTER.register("hoping", TechHopeyShit.HopingEffect::new);
	public static final DeferredHolder<MobEffect, TechMindConfusion.MindConfusionEffect> MIND_CONFUSION = REGISTER.register("mind_confusion", TechMindConfusion.MindConfusionEffect::new);
	public static final DeferredHolder<MobEffect, TechMindControl.MindControllingEffect> MIND_CONTROLLING = REGISTER.register("mind_controlling", TechMindControl.MindControllingEffect::new);
	public static final DeferredHolder<MobEffect, CalculatingEffect> CALCULATING = REGISTER.register("calculating", CalculatingEffect::new);
	public static final DeferredHolder<MobEffect, BerserkEffect> RAGE_BERSERK = REGISTER.register("rage_berserk", BerserkEffect::new);
	public static final DeferredHolder<MobEffect, FrenziedEffect> FRENZIED = REGISTER.register("frenzied", FrenziedEffect::new);
	public static final DeferredHolder<MobEffect, RageShiftedEffect> RAGE_SHIFTED = REGISTER.register("rage_shifted", RageShiftedEffect::new);
	public static final DeferredHolder<MobEffect, SavingGracedEffect> SAVING_GRACED = REGISTER.register("saving_graced", SavingGracedEffect::new);
	public static final DeferredHolder<MobEffect, EarthboundEffect> EARTHBOUND = REGISTER.register("earthbound", EarthboundEffect::new);
	public static final DeferredHolder<MobEffect, BuildInhibitEffect> BUILD_INHIBIT = REGISTER.register("build_inhibit", BuildInhibitEffect::new);
	public static final DeferredHolder<MobEffect, DecayEffect> DECAY = REGISTER.register("decay", DecayEffect::new);
	public static final DeferredHolder<MobEffect, GodTierLockEffect> GOD_TIER_LOCK = REGISTER.register("god_tier_lock", GodTierLockEffect::new);
	public static final DeferredHolder<MobEffect, MindFortitudeEffect> MIND_FORTITUDE = REGISTER.register("mind_fortitude", MindFortitudeEffect::new);
	public static final DeferredHolder<MobEffect, ConcealEffect> CONCEAL = REGISTER.register("conceal", ConcealEffect::new);
	public static final DeferredHolder<MobEffect, GodTierComebackEffect> GOD_TIER_COMEBACK = REGISTER.register("god_tier_comeback", GodTierComebackEffect::new);
	public static final DeferredHolder<MobEffect, TechTimeAccelerateSelf.AcceleratingEffect> ACCELERATING = REGISTER.register("accelerating", TechTimeAccelerateSelf.AcceleratingEffect::new);
	public static final DeferredHolder<MobEffect, DecayproofEffect> DECAYPROOF = REGISTER.register("decayproof", DecayproofEffect::new);
	public static final DeferredHolder<MobEffect, TechSling.SlingChargeEffect> SLING_CHARGE = REGISTER.register("sling_charge", TechSling.SlingChargeEffect::new);
	public static final DeferredHolder<MobEffect, TechVoidStep.VoidStepEffect> VOID_STEP = REGISTER.register("void_step", TechVoidStep.VoidStepEffect::new);

	private MSUMobEffects()
	{
	}
}
