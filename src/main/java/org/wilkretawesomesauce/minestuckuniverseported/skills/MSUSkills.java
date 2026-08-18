package org.wilkretawesomesauce.minestuckuniverseported.skills;

import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.badges.Badge;
import org.wilkretawesomesauce.minestuckuniverseported.badges.BadgeBuilder;
import org.wilkretawesomesauce.minestuckuniverseported.badges.BadgeEffectBuff;
import org.wilkretawesomesauce.minestuckuniverseported.badges.BadgeKarma;
import org.wilkretawesomesauce.minestuckuniverseported.badges.BadgeOverlord;
import org.wilkretawesomesauce.minestuckuniverseported.badges.BadgePage;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.Abilitech;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechRegistry;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.TechDragonAura;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.TechReturn;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.TechSling;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.blood.TechBloodBleeding;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.blood.TechBloodBubble;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.blood.TechBloodReformer;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.blood.TechBloodTransfusion;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.breath.TechBreathBubble;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.breath.TechBreathConstrain;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.breath.TechBreathGale;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.breath.TechBreathKnockback;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.breath.TechBreathLiberate;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.breath.TechBreathSpaceFallProof;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.breath.TechBreathSpeed;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.breath.TechBreathWindVessel;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.doom.TechDoomBind;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.doom.TechDoomChain;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.doom.TechDoomDecay;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.doom.TechDoomDemise;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.doom.TechDoomDemiseAoE;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.doom.TechDoomVoidBubble;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.heart.TechHeartBond;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.heart.TechHeartProject;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.heart.TechHeartSoulSwitcher;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.heart.TechSoulStun;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.hope.TechHopeCleansing;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.hope.TechHopeGolem;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.hope.TechHopePrayers;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.hope.TechHopeyShit;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.light.TechLightAutoGlorb;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.light.TechLightEnchantersInsight;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.light.TechLightBubble;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.light.TechLightGlorb;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.light.TechLightGlowing;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.light.TechLightInsight;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.light.TechLightStriker;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.life.TechLifeAura;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.life.TechLifeBreed;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.life.TechLifeChloroball;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.life.TechLifeFertility;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.life.TechLifeGrace;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.life.TechLifeLeech;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.mind.TechMindCloak;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.mind.TechMindConfusion;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.mind.TechMindControl;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.mind.TechMindKarmaHeal;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.mind.TechMindStrike;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.rage.TechRageBerserk;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.rage.TechRageFrenzy;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.rage.TechRageManagement;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.rage.TechRageOutburst;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.space.TechSpaceAnchoredTele;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.space.TechSpaceGrab;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.space.TechSpaceManipulator;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.space.TechSpaceResize;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.space.TechSpaceTargetTele;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.space.TechSpaceTele;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time.TechFutureRequest;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time.TechRetrocognition;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time.TechTimeAccelerateSelf;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time.TechTimeAcceleration;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time.TechTimeDilation;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time.TechTimeLoopAlpha;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time.TechTimeLoopBeta;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time.TechTimeLoopOmega;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time.TechTimeYearsInSeconds;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time.TechTimeParallelAction;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time.TechTimeRecall;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time.TechTimeShift;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time.TechTimeSlow;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time.TechTimeStop;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time.TechTimeTables;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time.TechTimeTickUp;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time.TechTimelineBranch;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time.TechTimelineRewind;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.voidAspect.TechVoidGrasp;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.voidAspect.TechVoidSnap;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.voidAspect.TechVoidStep;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.voidAspect.TechVoidVacuum;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.bard.TechBard;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.bard.TechBardMetronome;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.bard.blood.TechBardBloodCrimsonDiscord;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.heir.TechHeir;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.blood.TechBloodBond;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.knight.TechKnightHalt;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.knight.TechKnightWard;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.lord.TechLord;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.mage.TechMage;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.mage.TechMageStudy;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.mage.blood.TechMageBloodGuidance;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.mage.blood.TechMageBloodInsight;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.mage.blood.TechMageBloodMemory;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.mage.breath.TechMageBreathInsight;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.mage.doom.TechMageDoomInsight;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.mage.mind.TechMageMindInsight;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.maid.TechMaid;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.maid.TechMaidServe;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.maid.mind.TechMaidMindConstructGolem;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.maid.doom.TechMaidDoomFinalityEngine;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.maid.doom.TechMaidDoomforge;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.muse.TechMuse;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.page.TechPagePerseverantAwakening;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.page.breath.TechPageBreathFreeWill;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.page.doom.TechPageDoomApocalypseRelease;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.page.doom.TechPageDoomReservoir;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.prince.TechPrinceSlash;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.prince.TechPrinceWrath;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.prince.blood.TechPrinceBloodSchism;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.rogue.TechRogue;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.rogue.TechRogueSteal;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.rogue.doom.TechRogueDoomGraveExchange;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.rogue.doom.TechRogueDoomRedistribution;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.seer.TechSeer;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.seer.TechSeerDodge;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.sylph.TechSylph;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.sylph.TechSylphKarmaRestore;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.sylph.doom.TechSylphDoomDeathUnmade;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.sylph.doom.TechSylphDoomReversal;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.thief.TechThief;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.thief.TechThiefDash;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.thief.TechThiefStickyFingers;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.witch.TechWitch;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.witch.blood.TechBloodWitchCultOfPersonality;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.witch.TechWitchTrap;

/**
 * Real port of MinestuckUniverse (1.12.2)'s {@code skills.MSUSkills} - the original's single real
 * registration hub for every {@code Abilitech} and {@code Badge} instance, combined here for the same
 * reason: the original kept {@code Abilitech.ABILITECHS} and {@code Badge.BADGES} as two separate
 * backing lists internally, but still declared every single static field for both in this one class.
 * Replaces the previous split between {@code MSUAbilitechs} (abilitech registration only) and
 * {@code MSUBadges} (badge registration only) - those two classes are gone, not kept as thin re-exports,
 * matching this project's standing practice of not leaving backwards-compatibility shims around.
 * <p>
 * The original's own {@code IForgeRegistry<Skill>}/{@code RegistryBuilder} setup isn't reproduced -
 * {@link MSUAbilitechRegistry} is this project's own real, already-established modern equivalent (a
 * plain list-backed registry, not a Forge {@code IForgeRegistry}), and badges were never registered into
 * any registry at all in the original either (just plain static fields), so there's nothing left to port
 * for that half beyond the fields themselves.
 * <p>
 * {@code TIME_ACCELERATE_SELF}, {@code TIME_SLOW}, and {@code TIME_PARALLEL_ACTION} are new "basic
 * command" techs from the Time Aspect timeline-management design discussion, not ports of anything in
 * the original - see their individual classes for scope notes. Real per-tech costs (where the original
 * passed a second constructor argument here) are instead baked directly into each tech's own constructor
 * - a deliberate shape difference from the original kept from before this pass, not changed now.
 */
public final class MSUSkills
{
	// Generic (non-aspect, non-class) techs - real port of the original's own top-level
	// skills.abilitech.TechDragonAura/TechReturn/TechSling, the only three concrete techs the original ever
	// registered directly under Abilitech -> TechBoondollarCost without a TechHeroAspect/TechHeroClass in
	// between. Real per-tech costs pulled from the original's own MSUSkills.java, same methodology as every
	// other section below.
	public static final Abilitech RETURN_JUMP = MSUAbilitechRegistry.register(new TechReturn());
	public static final Abilitech SYLLADEX_SLING = MSUAbilitechRegistry.register(new TechSling());
	public static final Abilitech DRACONIC_AURA = MSUAbilitechRegistry.register(new TechDragonAura());

	public static final Abilitech TIME_SHIFT = MSUAbilitechRegistry.register(new TechTimeShift());
	public static final Abilitech TIME_STOP = MSUAbilitechRegistry.register(new TechTimeStop());
	public static final Abilitech TIME_ACCELERATION = MSUAbilitechRegistry.register(new TechTimeAcceleration());
	public static final Abilitech TIME_TABLES = MSUAbilitechRegistry.register(new TechTimeTables());
	public static final Abilitech TIME_TICK_UP = MSUAbilitechRegistry.register(new TechTimeTickUp());
	public static final Abilitech TIME_RECALL = MSUAbilitechRegistry.register(new TechTimeRecall());

	public static final Abilitech TIME_ACCELERATE_SELF = MSUAbilitechRegistry.register(new TechTimeAccelerateSelf());
	public static final Abilitech TIME_SLOW = MSUAbilitechRegistry.register(new TechTimeSlow());
	public static final Abilitech TIME_DILATION = MSUAbilitechRegistry.register(new TechTimeDilation());
	public static final Abilitech TIME_PARALLEL_ACTION = MSUAbilitechRegistry.register(new TechTimeParallelAction());
	public static final Abilitech TIMELINE_REWIND = MSUAbilitechRegistry.register(new TechTimelineRewind());
	public static final Abilitech RETROCOGNITION = MSUAbilitechRegistry.register(new TechRetrocognition());
	public static final Abilitech TIMELINE_BRANCH = MSUAbilitechRegistry.register(new TechTimelineBranch());
	public static final Abilitech FUTURE_REQUEST = MSUAbilitechRegistry.register(new TechFutureRequest());
	public static final Abilitech TIME_LOOP = MSUAbilitechRegistry.register(new TechTimeLoopAlpha());
	public static final Abilitech TIME_LOOP_NESTED = MSUAbilitechRegistry.register(new TechTimeLoopBeta());
	public static final Abilitech TIME_LOOP_OMEGA = MSUAbilitechRegistry.register(new TechTimeLoopOmega());
	public static final Abilitech YEARS_IN_SECONDS = MSUAbilitechRegistry.register(new TechTimeYearsInSeconds());

	// Blood aspect
	public static final Abilitech BLOOD_TRANSFUSION = MSUAbilitechRegistry.register(new TechBloodTransfusion());
	public static final Abilitech BLOOD_BLEEDING = MSUAbilitechRegistry.register(new TechBloodBleeding());
	public static final Abilitech BLOOD_BUBBLE = MSUAbilitechRegistry.register(new TechBloodBubble());
	public static final Abilitech BLOOD_REFORMER = MSUAbilitechRegistry.register(new TechBloodReformer());

	// Breath aspect
	public static final Abilitech BREATH_GALE = MSUAbilitechRegistry.register(new TechBreathGale());
	public static final Abilitech BREATH_KNOCKBACK = MSUAbilitechRegistry.register(new TechBreathKnockback());
	public static final Abilitech BREATH_BUBBLE = MSUAbilitechRegistry.register(new TechBreathBubble());
	public static final Abilitech BREATH_SPACE_FALL_PROOF = MSUAbilitechRegistry.register(new TechBreathSpaceFallProof());
	public static final Abilitech BREATH_SPEED = MSUAbilitechRegistry.register(new TechBreathSpeed());
	public static final Abilitech BREATH_WIND_VESSEL = MSUAbilitechRegistry.register(new TechBreathWindVessel());
	public static final Abilitech BREATH_LIBERATE = MSUAbilitechRegistry.register(new TechBreathLiberate());
	public static final Abilitech BREATH_CONSTRAIN = MSUAbilitechRegistry.register(new TechBreathConstrain());

	// Doom aspect
	public static final Abilitech DOOM_BIND = MSUAbilitechRegistry.register(new TechDoomBind());
	public static final Abilitech DOOM_CHAIN = MSUAbilitechRegistry.register(new TechDoomChain());
	public static final Abilitech DOOM_DECAY = MSUAbilitechRegistry.register(new TechDoomDecay());
	public static final Abilitech DOOM_DEMISE = MSUAbilitechRegistry.register(new TechDoomDemise());
	public static final Abilitech DOOM_DEMISE_AOE = MSUAbilitechRegistry.register(new TechDoomDemiseAoE());
	public static final Abilitech DOOM_VOID_BUBBLE = MSUAbilitechRegistry.register(new TechDoomVoidBubble());

	// Heart aspect
	public static final Abilitech HEART_BOND = MSUAbilitechRegistry.register(new TechHeartBond());
	public static final Abilitech HEART_PROJECT = MSUAbilitechRegistry.register(new TechHeartProject());
	public static final Abilitech HEART_SOUL_SWITCHER = MSUAbilitechRegistry.register(new TechHeartSoulSwitcher());
	public static final Abilitech HEART_SOUL_STUN = MSUAbilitechRegistry.register(new TechSoulStun());

	// Hope aspect
	public static final Abilitech HOPE_CLEANSING = MSUAbilitechRegistry.register(new TechHopeCleansing());
	public static final Abilitech HOPE_GOLEM = MSUAbilitechRegistry.register(new TechHopeGolem());
	public static final Abilitech HOPE_PRAYERS = MSUAbilitechRegistry.register(new TechHopePrayers());
	public static final Abilitech HOPE_OUTBURST = MSUAbilitechRegistry.register(new TechHopeyShit());

	// Space aspect
	public static final Abilitech SPACE_TELE = MSUAbilitechRegistry.register(new TechSpaceTele());
	public static final Abilitech SPACE_ANCHORED_TELE = MSUAbilitechRegistry.register(new TechSpaceAnchoredTele());
	public static final Abilitech SPACE_TARGET_TELE = MSUAbilitechRegistry.register(new TechSpaceTargetTele());
	public static final Abilitech SPACE_GRAB = MSUAbilitechRegistry.register(new TechSpaceGrab());
	public static final Abilitech SPACE_RESIZE = MSUAbilitechRegistry.register(new TechSpaceResize());
	public static final Abilitech SPACE_MANIPULATOR = MSUAbilitechRegistry.register(new TechSpaceManipulator());

	// Void aspect
	public static final Abilitech VOID_GRASP = MSUAbilitechRegistry.register(new TechVoidGrasp());
	public static final Abilitech VOID_SNAP = MSUAbilitechRegistry.register(new TechVoidSnap());
	public static final Abilitech VOID_STEP = MSUAbilitechRegistry.register(new TechVoidStep());
	public static final Abilitech VOID_VACUUM = MSUAbilitechRegistry.register(new TechVoidVacuum());

	// Mind aspect
	public static final Abilitech MIND_CLOAK = MSUAbilitechRegistry.register(new TechMindCloak());
	public static final Abilitech MIND_CONFUSION = MSUAbilitechRegistry.register(new TechMindConfusion());
	public static final Abilitech MIND_CONTROL = MSUAbilitechRegistry.register(new TechMindControl());
	public static final Abilitech MIND_KARMA_HEAL = MSUAbilitechRegistry.register(new TechMindKarmaHeal());
	public static final Abilitech MIND_STRIKE = MSUAbilitechRegistry.register(new TechMindStrike());

	// Rage aspect
	public static final Abilitech RAGE_BERSERK = MSUAbilitechRegistry.register(new TechRageBerserk());
	public static final Abilitech RAGE_FRENZY = MSUAbilitechRegistry.register(new TechRageFrenzy());
	public static final Abilitech RAGE_MANAGEMENT = MSUAbilitechRegistry.register(new TechRageManagement());
	public static final Abilitech RAGE_OUTBURST = MSUAbilitechRegistry.register(new TechRageOutburst());

	// Light aspect
	public static final Abilitech LIGHT_GLORB = MSUAbilitechRegistry.register(new TechLightGlorb());
	public static final Abilitech LIGHT_AUTO_GLORB = MSUAbilitechRegistry.register(new TechLightAutoGlorb());
	public static final Abilitech ENCHANTERS_INSIGHT = MSUAbilitechRegistry.register(new TechLightEnchantersInsight());
	public static final Abilitech LIGHT_GLOWING = MSUAbilitechRegistry.register(new TechLightGlowing());
	public static final Abilitech LIGHT_STRIKER = MSUAbilitechRegistry.register(new TechLightStriker());
	public static final Abilitech LIGHT_BUBBLE = MSUAbilitechRegistry.register(new TechLightBubble());
	public static final Abilitech LIGHT_INSIGHT = MSUAbilitechRegistry.register(new TechLightInsight());

	// Life aspect
	public static final Abilitech LIFE_LEECH = MSUAbilitechRegistry.register(new TechLifeLeech());
	public static final Abilitech LIFE_AURA = MSUAbilitechRegistry.register(new TechLifeAura());
	public static final Abilitech LIFE_BREED = MSUAbilitechRegistry.register(new TechLifeBreed());
	public static final Abilitech LIFE_CHLOROBALL = MSUAbilitechRegistry.register(new TechLifeChloroball());
	public static final Abilitech LIFE_FERTILITY = MSUAbilitechRegistry.register(new TechLifeFertility());
	public static final Abilitech LIFE_GRACE = MSUAbilitechRegistry.register(new TechLifeGrace());

	// heroClass package - real per-tech costs pulled from the original's own MSUSkills.java (fetched from
	// the real upstream GitHub source - this project's local extracted copy of the original didn't include
	// this particular file, same "pull the real cost, don't guess" methodology already applied everywhere
	// else). Page's own class tech (TechPagePerseverantAwakening) was never its own file in the original -
	// it was defined as an anonymous TechHeroClass subclass directly inside this same class, given a real
	// name here instead.
	public static final Abilitech BARD_DISSONANCE = MSUAbilitechRegistry.register(new TechBard());
	public static final Abilitech BARD_METRONOME = MSUAbilitechRegistry.register(new TechBardMetronome());
	public static final Abilitech CRIMSON_DISCORD = MSUAbilitechRegistry.register(new TechBardBloodCrimsonDiscord());
	public static final Abilitech HEIR_WILL = MSUAbilitechRegistry.register(new TechHeir(Minestuckuniverseported.id("heir_will"), 9000, MSUTechType.PASSIVE, MSUTechType.DEFENSE));
	public static final Abilitech UNIVERSAL_REVERSE = MSUAbilitechRegistry.register(new TechHeir(Minestuckuniverseported.id("universal_reverse"), 5500, MSUTechType.PASSIVE, MSUTechType.OFFENSE));
	public static final Abilitech GUARDIAN_HALT = MSUAbilitechRegistry.register(new TechKnightHalt());
	public static final Abilitech KNIGHT_WARD = MSUAbilitechRegistry.register(new TechKnightWard());
	public static final Abilitech BLOOD_BOND = MSUAbilitechRegistry.register(new TechBloodBond());
	public static final Abilitech LORD_DECREE = MSUAbilitechRegistry.register(new TechLord());
	public static final Abilitech MAGE_AWARENESS = MSUAbilitechRegistry.register(new TechMage());
	public static final Abilitech ARCANE_STUDY = MSUAbilitechRegistry.register(new TechMageStudy());
	public static final Abilitech BLOOD_INSIGHT = MSUAbilitechRegistry.register(new TechMageBloodInsight());
	public static final Abilitech BLOOD_MEMORY = MSUAbilitechRegistry.register(new TechMageBloodMemory());
	public static final Abilitech BLOOD_GUIDANCE = MSUAbilitechRegistry.register(new TechMageBloodGuidance());
	public static final Abilitech DOOM_INSIGHT = MSUAbilitechRegistry.register(new TechMageDoomInsight());
	public static final Abilitech BREATH_INSIGHT = MSUAbilitechRegistry.register(new TechMageBreathInsight());
	public static final Abilitech MIND_INSIGHT = MSUAbilitechRegistry.register(new TechMageMindInsight());
	public static final Abilitech MAID_FAVOR = MSUAbilitechRegistry.register(new TechMaid());
	public static final Abilitech IRRADIANT_SERVITUDE = MSUAbilitechRegistry.register(new TechMaidServe());
	public static final Abilitech CONSTRUCT_GOLEM = MSUAbilitechRegistry.register(new TechMaidMindConstructGolem());
	public static final Abilitech DOOMFORGE = MSUAbilitechRegistry.register(new TechMaidDoomforge());
	public static final Abilitech FINALITY_ENGINE = MSUAbilitechRegistry.register(new TechMaidDoomFinalityEngine());
	public static final Abilitech MUSE_REQUIEM = MSUAbilitechRegistry.register(new TechMuse());
	public static final Abilitech PERSEVERANT_AWAKENING = MSUAbilitechRegistry.register(new TechPagePerseverantAwakening());
	public static final Abilitech DOOM_RESERVOIR = MSUAbilitechRegistry.register(new TechPageDoomReservoir());
	public static final Abilitech APOCALYPSE_RELEASE = MSUAbilitechRegistry.register(new TechPageDoomApocalypseRelease());
	public static final Abilitech FREE_WILL = MSUAbilitechRegistry.register(new TechPageBreathFreeWill());
	public static final Abilitech PRINCE_WRATH = MSUAbilitechRegistry.register(new TechPrinceWrath());
	public static final Abilitech RULING_SLASH = MSUAbilitechRegistry.register(new TechPrinceSlash());
	public static final Abilitech SCHISM = MSUAbilitechRegistry.register(new TechPrinceBloodSchism());
	public static final Abilitech ROGUE_CONTRIBUTION = MSUAbilitechRegistry.register(new TechRogue());
	public static final Abilitech ROGUELIKE_ADAPTABILITY = MSUAbilitechRegistry.register(new TechRogueSteal());
	public static final Abilitech DOOM_REDISTRIBUTION = MSUAbilitechRegistry.register(new TechRogueDoomRedistribution());
	public static final Abilitech GRAVE_EXCHANGE = MSUAbilitechRegistry.register(new TechRogueDoomGraveExchange());
	public static final Abilitech SEER_PREDICTION = MSUAbilitechRegistry.register(new TechSeer());
	public static final Abilitech FORESIGHT_DODGE = MSUAbilitechRegistry.register(new TechSeerDodge());
	public static final Abilitech SYLPH_MEND = MSUAbilitechRegistry.register(new TechSylph());
	public static final Abilitech KARMIC_RESTORATION = MSUAbilitechRegistry.register(new TechSylphKarmaRestore());
	public static final Abilitech DOOM_REVERSAL = MSUAbilitechRegistry.register(new TechSylphDoomReversal());
	public static final Abilitech DEATH_UNMADE = MSUAbilitechRegistry.register(new TechSylphDoomDeathUnmade());
	public static final Abilitech THIEF_FILCH = MSUAbilitechRegistry.register(new TechThief());
	public static final Abilitech HERMES_DASH = MSUAbilitechRegistry.register(new TechThiefDash());
	public static final Abilitech STICKY_FINGERS = MSUAbilitechRegistry.register(new TechThiefStickyFingers());
	public static final Abilitech WITCH_INHIBITION = MSUAbilitechRegistry.register(new TechWitch());
	public static final Abilitech WICKED_TRAP = MSUAbilitechRegistry.register(new TechWitchTrap());
	public static final Abilitech CULT_OF_PERSONALITY = MSUAbilitechRegistry.register(new TechBloodWitchCultOfPersonality());

	// Badges - the 4 real badges any heroClass tech actually reads (KARMA/EFFECT_BUFF/BADGE_PAGE/
	// BADGE_OVERLORD) plus BUILDER_BADGE (real drag-fill building tool, not read by any heroClass tech but
	// real infrastructure in its own right - see badges.BadgeBuilder's own doc comment). Not the full
	// original hierarchy still (see badges.Badge's own doc comment for what's still real, ready future
	// work: MasterBadge/BadgeConsort + ~6 remaining bespoke sacrifice/grist-gated badges like
	// SKELETON_KEY/HOARD_OF_THE_ALCHEMIZER/STRIFE_BADGE/REVENANTS_RETALIATION - none of those are built
	// here, this only covers what's actually reachable today).
	public static final Badge KARMA = new BadgeKarma();
	public static final Badge EFFECT_BUFF = new BadgeEffectBuff();
	public static final Badge BADGE_PAGE = new BadgePage();
	public static final Badge BADGE_OVERLORD = new BadgeOverlord();
	public static final Badge BUILDER_BADGE = new BadgeBuilder();

	private MSUSkills()
	{
	}

	/** Call once during mod setup to make sure this class is loaded and every tech/badge above is registered. */
	public static void init()
	{
	}
}
