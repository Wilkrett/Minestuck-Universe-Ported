package org.wilkretawesomesauce.minestuckuniverseported;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ================================================================================================
    // Strife - ported from MinestuckUniverse (1.12.2)'s MSUConfig "Strife" category. NeoForge configs
    // are TOML, not the old .cfg format, and there's no setMinValue-without-a-max like 1.12.2 had, so
    // "unlimited" options use Integer.MAX_VALUE as the practical upper bound instead of leaving it open.
    // ================================================================================================

    private static final ModConfigSpec.IntValue ABSTRATA_SWITCHER_RUNG;
    private static final ModConfigSpec.IntValue REQUIRED_RUNG_TO_GT;
    private static final ModConfigSpec.IntValue QUEST_BED_SPAWN_DISTANCE;
    private static final ModConfigSpec.IntValue QUEST_BED_SPAWN_AREA;
    private static final ModConfigSpec.IntValue TIMELINE_HISTORY_TICKS;
    private static final ModConfigSpec.DoubleValue TIMELINE_DOOM_POINTS_PER_TICK;
    private static final ModConfigSpec.IntValue TIMELINE_REWIND_PLAYBACK_SPEED;
    private static final ModConfigSpec.IntValue TIMELINE_CLONE_REPLAY_SPEED;
    private static final ModConfigSpec.IntValue TIMELINE_BRANCH_IDLE_PRUNE_TICKS;
    private static final ModConfigSpec.IntValue TIMELINE_BRANCH_PRUNE_SWEEP_INTERVAL;
    private static final ModConfigSpec.IntValue TIME_LOOP_MAX_DURATION_TICKS;
    private static final ModConfigSpec.IntValue TIME_LOOP_WINDOW_TICKS;
    private static final ModConfigSpec.DoubleValue TIME_LOOP_RADIUS;
    private static final ModConfigSpec.IntValue RETROCOGNITION_OBSERVE_TICKS;
    private static final ModConfigSpec.DoubleValue RETROCOGNITION_OVERLAY_RADIUS;
    private static final ModConfigSpec.DoubleValue TIME_REQUEST_DOOM_PER_TICK_BASE;
    private static final ModConfigSpec.DoubleValue TIME_REQUEST_DOOM_MULTIPLIER_CAP;
    private static final ModConfigSpec.IntValue TIME_REQUEST_DOOM_CHECK_INTERVAL;
    private static final ModConfigSpec.IntValue TIME_REQUEST_EVENT_COOLDOWN_TICKS;
    private static final ModConfigSpec.IntValue TIME_REQUEST_COOLDOWN_TICKS;
    private static final ModConfigSpec.DoubleValue DOOM_DAMAGE_SEVERITY_MAX;
    private static final ModConfigSpec.DoubleValue DOOM_DAMAGE_SEVERITY_CURVE;
    private static final ModConfigSpec.DoubleValue DOOM_DAMAGE_SEVERITY_MIN_THRESHOLD;
    private static final ModConfigSpec.DoubleValue DOOM_DAMAGE_AMPLIFY_MAX;
    private static final ModConfigSpec.DoubleValue DOOM_DAMAGE_AMPLIFY_HALF_POINT;
    private static final ModConfigSpec.DoubleValue DOOM_EFFECT_DURATION_EXTEND_MAX;
    private static final ModConfigSpec.DoubleValue DOOM_EFFECT_DURATION_HALF_POINT;
    private static final ModConfigSpec.DoubleValue DOOM_KILL_BASE;
    private static final ModConfigSpec.DoubleValue DOOM_KILL_PER_MAX_HEALTH;
    private static final ModConfigSpec.DoubleValue DOOM_KILL_CAP;
    private static final ModConfigSpec.IntValue DOOM_PASSIVE_ACCRUAL_CHECK_INTERVAL_TICKS;
    private static final ModConfigSpec.IntValue DOOM_PASSIVE_ACCRUAL_AGE_THRESHOLD_TICKS;
    private static final ModConfigSpec.DoubleValue DOOM_PASSIVE_ACCRUAL_PER_INTERVAL;
    private static final ModConfigSpec.IntValue DOOM_HARVEST_WINDOW_TICKS;
    private static final ModConfigSpec.IntValue DOOM_RELEASE_TICK_INTERVAL_TICKS;
    private static final ModConfigSpec.DoubleValue DOOM_MARK_ACCRUAL_MULTIPLIER;
    private static final ModConfigSpec.DoubleValue DOOM_RELATIONSHIP_DEATH_SCALE;
    private static final ModConfigSpec.DoubleValue DOOM_RELATIONSHIP_DEATH_CAP;
    private static final ModConfigSpec.DoubleValue DOOM_RELATIONSHIP_SEVERANCE_SCALE;
    private static final ModConfigSpec.DoubleValue DOOM_RELATIONSHIP_SEVERANCE_CAP;
    private static final ModConfigSpec.DoubleValue DOOM_BETRAYAL_BASE;
    private static final ModConfigSpec.DoubleValue DOOM_BETRAYAL_CAP;
    private static final ModConfigSpec.IntValue DOOM_ISOLATION_CHECK_INTERVAL_TICKS;
    private static final ModConfigSpec.IntValue DOOM_ISOLATION_RELATIONSHIP_THRESHOLD;
    private static final ModConfigSpec.DoubleValue DOOM_ISOLATION_PER_INTERVAL;
    private static final ModConfigSpec.DoubleValue DOOMFORGE_INJECT_AMOUNT;
    private static final ModConfigSpec.IntValue FINALITY_ENGINE_CHARGE_TICKS;
    private static final ModConfigSpec.DoubleValue FINALITY_ENGINE_BASE_DAMAGE;
    private static final ModConfigSpec.DoubleValue FINALITY_ENGINE_DOOM_SCALE;
    private static final ModConfigSpec.DoubleValue FINALITY_ENGINE_MAX_DAMAGE;
    private static final ModConfigSpec.IntValue DOOM_RESERVOIR_HARVEST_INTERVAL_TICKS;
    private static final ModConfigSpec.DoubleValue DOOM_RESERVOIR_HARVEST_RADIUS;
    private static final ModConfigSpec.DoubleValue DOOM_RESERVOIR_HARVEST_AMOUNT_PER_PULSE;
    private static final ModConfigSpec.DoubleValue APOCALYPSE_RELEASE_RADIUS;
    private static final ModConfigSpec.DoubleValue APOCALYPSE_RELEASE_DAMAGE_SCALE;
    private static final ModConfigSpec.DoubleValue APOCALYPSE_RELEASE_MAX_CONSUME;
    private static final ModConfigSpec.DoubleValue DOOM_REVERSAL_AMOUNT_PER_SECOND;
    private static final ModConfigSpec.DoubleValue DEATH_UNMADE_REMOVE_AMOUNT;
    private static final ModConfigSpec.DoubleValue DOOM_REDISTRIBUTION_AMOUNT;
    private static final ModConfigSpec.DoubleValue SCHISM_DAMAGE_AMPLIFY_FACTOR;
    private static final ModConfigSpec.IntValue SCHISM_HOSTILITY_DURATION_TICKS;
    private static final ModConfigSpec.IntValue SCHISM_CORRUPTION_DURATION_TICKS;
    private static final ModConfigSpec.DoubleValue SCHISM_AURA_RADIUS;
    private static final ModConfigSpec.DoubleValue SCHISM_AURA_WEAKEN_FACTOR;
    private static final ModConfigSpec.DoubleValue SCHISM_AURA_DISRUPTION_CHANCE;
    private static final ModConfigSpec.IntValue SCHISM_AURA_DISRUPTION_INTERVAL_TICKS;
    private static final ModConfigSpec.DoubleValue SCHISM_AURA_OWNERSHIP_DECAY;
    private static final ModConfigSpec.DoubleValue CRIMSON_DISCORD_AURA_RADIUS;
    private static final ModConfigSpec.DoubleValue CRIMSON_DISCORD_INSTABILITY_GAIN_PER_PULSE;
    private static final ModConfigSpec.IntValue CRIMSON_DISCORD_PULSE_INTERVAL_TICKS;
    private static final ModConfigSpec.DoubleValue CRIMSON_DISCORD_BURST_AMOUNT;
    private static final ModConfigSpec.DoubleValue CRIMSON_DISCORD_NATURAL_DECAY_AMOUNT;
    private static final ModConfigSpec.IntValue CRIMSON_DISCORD_NATURAL_DECAY_INTERVAL_TICKS;
    private static final ModConfigSpec.DoubleValue CRIMSON_DISCORD_DOMINO_RADIUS;
    private static final ModConfigSpec.DoubleValue CRIMSON_DISCORD_DOMINO_BUMP;
    private static final ModConfigSpec.DoubleValue CRIMSON_DISCORD_VENGEANCE_FAIL_DIVISOR;
    private static final ModConfigSpec.IntValue CRIMSON_DISCORD_NEW_RIVALRIES_PER_PULSE;
    private static final ModConfigSpec.DoubleValue CRIMSON_DISCORD_FIGHT_THRESHOLD;
    private static final ModConfigSpec.IntValue RELATIONSHIP_FIGHTING_TOGETHER_WINDOW_TICKS;
    private static final ModConfigSpec.DoubleValue RELATIONSHIP_FIGHTING_TOGETHER_GAIN;
    private static final ModConfigSpec.DoubleValue RELATIONSHIP_DAMAGE_CONFLICT_GAIN;
    private static final ModConfigSpec.DoubleValue RELATIONSHIP_DAMAGE_FAMILIARITY_GAIN;
    private static final ModConfigSpec.DoubleValue RELATIONSHIP_BETRAYAL_AFFINITY_LOSS;
    private static final ModConfigSpec.DoubleValue RELATIONSHIP_BETRAYAL_CONFLICT_GAIN;
    private static final ModConfigSpec.DoubleValue RELATIONSHIP_BETRAYAL_STABILITY_LOSS;
    private static final ModConfigSpec.DoubleValue RELATIONSHIP_NEARBY_RADIUS;
    private static final ModConfigSpec.DoubleValue RELATIONSHIP_NEARBY_FAMILIARITY_GAIN;
    private static final ModConfigSpec.IntValue SKAIAN_SCROLL_LIMIT;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> SKAIAN_SCROLL_BLACKLIST;
    private static final ModConfigSpec.BooleanValue COMBAT_OVERHAUL;
    private static final ModConfigSpec.BooleanValue KEEP_PORTFOLIO_ON_DEATH;
    private static final ModConfigSpec.BooleanValue RESTRICTED_STRIFE;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> RESTRICTED_STRIFE_BYPASS;
    private static final ModConfigSpec.IntValue STRIFE_CARD_MOB_DROPS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> STRIFE_CARD_MOB_DROPS_WHITELIST;
    private static final ModConfigSpec.IntValue STRIFE_DECK_MAX_SIZE;
    private static final ModConfigSpec.DoubleValue WEAPON_ATTACK_MULTIPLIER;
    private static final ModConfigSpec.DoubleValue GOLEM_MAX_HEALTH_MULTIPLIER;
    private static final ModConfigSpec.DoubleValue GOLEM_DAMAGE_MULTIPLIER;
    private static final ModConfigSpec.IntValue GOLEM_ATTACK_COOLDOWN_TICKS;
    private static final ModConfigSpec.IntValue GOLEM_EXP_DROP;

    static
    {
        BUILDER.push("strife");

        ABSTRATA_SWITCHER_RUNG = BUILDER
                .comment("Determines the rung needed to unlock the Strife Specibus Quick Switcher. Set it to -1 to let all players use it, or 100 to effectively disable it.")
                .defineInRange("abstrataSwitcherRung", 17, -1, 100);

        COMBAT_OVERHAUL = BUILDER
                .comment("Enables the Strife Portfolio and overrides every Minestuck and Minestuck Universe weapon to better balance them. Other options in the Strife category will only take effect if this is set to true.",
                        "NOTE: in this port, this gates restrictedStrife enforcement and strifeDeckMaxSize/abstrataSwitcherRung are always active regardless. The weapon rebalancing this originally also gated hasn't been ported (depends on weapon items that don't exist here yet).")
                .define("combatOverhaul", true);

        KEEP_PORTFOLIO_ON_DEATH = BUILDER
                .comment("Determines whether the player drops their Strife Portfolio after dying or not.")
                .define("keepPortfolioOnDeath", false);

        RESTRICTED_STRIFE = BUILDER
                .comment("Prevents players from attacking without an allocated weapon in their main hand. It also restricts the use of certain items such as bows.")
                .define("restrictedStrife", false);

        RESTRICTED_STRIFE_BYPASS = BUILDER
                .comment("Determines what items still have right-click functionality, even if Restricted Strife is enabled.")
                .defineListAllowEmpty("restrictedStrifeBypass", List.of(
                        "minecraft:egg",
                        "minecraft:snowball",
                        "minecraft:ender_eye",
                        "minecraft:ender_pearl",
                        "minecraft:potion",
                        "minecraft:experience_bottle"
                ), Config::validateNonBlankString);

        STRIFE_CARD_MOB_DROPS = BUILDER
                .comment("Some mobs have a chance at dropping Strife Specibus Cards allocated to whatever item they're holding when killed by a player. This config determines how many cards each player can get from this method at most.",
                        "NOTE: not wired up yet - mob card drops haven't been ported.")
                .defineInRange("strifeCardMobDrops", 5, 0, Integer.MAX_VALUE);

        STRIFE_CARD_MOB_DROPS_WHITELIST = BUILDER
                .comment("Determines what Kind Abstrata can be dropped by killing underlings.",
                        "NOTE: not wired up yet, see strifeCardMobDrops. Defaults only include kinds that exist in this port so far (see MSUKindAbstrata) - the original's minestuckuniverse:sbahj isn't ported.")
                .defineListAllowEmpty("strifeCardMobDropsWhitelist", List.of(
                        "minestuckuniverseported:sword",
                        "minestuckuniverseported:hammer",
                        "minestuckuniverseported:club",
                        "minestuckuniverseported:cane",
                        "minestuckuniverseported:sickle",
                        "minestuckuniverseported:spoon",
                        "minestuckuniverseported:fork",
                        "minestuckuniverseported:potion",
                        "minestuckuniverseported:projectile",
                        "minestuckuniverseported:claw",
                        "minestuckuniverseported:gauntlet",
                        "minestuckuniverseported:bow",
                        "minestuckuniverseported:shield",
                        "minestuckuniverseported:needles",
                        "minestuckuniverseported:rock",
                        "minestuckuniverseported:bunny",
                        "minestuckuniverseported:joker"
                ), Config::validateNonBlankString);

        STRIFE_DECK_MAX_SIZE = BUILDER
                .comment("Determines the max amount of weapons that can fit inside a single Strife Deck (specibus), set this to -1 to remove the limit.")
                .defineInRange("strifeDeckMaxSize", 20, -1, Integer.MAX_VALUE);

        WEAPON_ATTACK_MULTIPLIER = BUILDER
                .comment("Allows players to tweak how much damage Minestuck and Minestuck Universe weapons do as a percentage against entities that aren't Underlings.",
                        "NOTE: not wired up yet - depends on weapon items that haven't been ported.")
                .defineInRange("weaponAttackMultiplier", 0.15, 0.0, 1.0);

        BUILDER.pop();

        BUILDER.push("godTier");

        REQUIRED_RUNG_TO_GT = BUILDER
                .comment("Determines the echeladder rung required to ascend to God Tier. Set it to -1 to remove the requirement.",
                        "NOTE: the original also required standing in your own Land on terrain matching your aspect; that part isn't ported yet, so ascension currently only checks this rung requirement (and having a Title assigned).")
                .defineInRange("requiredRungToGT", 8, -1, 100);

        QUEST_BED_SPAWN_DISTANCE = BUILDER
                .comment("Determines how far away the Quest Bed can spawn from the center of a player's Land (see godtier.MediumData).")
                .defineInRange("questBedSpawnDistance", 2500, 0, Integer.MAX_VALUE);

        QUEST_BED_SPAWN_AREA = BUILDER
                .comment("Determines the size of the area within which the Quest Bed can spawn on a player's Land.")
                .defineInRange("questBedSpawnArea", 2500, 1, Integer.MAX_VALUE);

        SKAIAN_SCROLL_LIMIT = BUILDER
                .comment("Determines the total number of Skaian Scrolls a player can use in total. Set to negative to ignore the limit.")
                .defineInRange("skaiaScrollLimit", 2, -1, Integer.MAX_VALUE);

        SKAIAN_SCROLL_BLACKLIST = BUILDER
                .comment("Prevents the included Abilitechs (by registry name) from spawning as Skaian Scrolls.")
                .defineListAllowEmpty("skaiaScrollBlacklist", List.of(), Config::validateNonBlankString);

        BUILDER.pop();

        BUILDER.push("timeline");

        TIMELINE_HISTORY_TICKS = BUILDER
                .comment("Max ticks of world history each dimension keeps, for both destructive rewinding and non-destructive Retrocognition. 6000 = 5 minutes.",
                        "Recording is always-on for every loaded level regardless of who's using Time abilities, so this bounds real ongoing memory use, not just a niche cost - defaulted to the full 5-minute max specifically so Retrocognition's own retrocognitionObserveTicks default (also 5 minutes) actually has that much history available, a deliberate ~10x baseline cost increase over the previous 30-second default, accepted for that reason.")
                .defineInRange("timelineHistoryTicks", 6000, 20, 6000);

        TIMELINE_DOOM_POINTS_PER_TICK = BUILDER
                .comment("How many Doom Points (DP) are added per tick rewound/traveled.",
                        "Placeholder mechanic - DP is currently just tracked with no attached consequences (replaces the earlier \"Timeline Debt\" mechanic entirely, not a rename of it). Revisit once DP has a real design.")
                .defineInRange("timelineDoomPointsPerTick", 0.05, 0.0, 100.0);

        TIMELINE_REWIND_PLAYBACK_SPEED = BUILDER
                .comment("How many recorded ticks get restored per real tick while a rewind is playing back. 1 = the world visibly rewinds at the same speed it originally happened; higher values play it back faster.",
                        "Defaults to 1 (real time) rather than fast-forwarded: blocks restoring in visible multi-tick batches read as generic \"world resetting\" rather than something actually happening, especially now that the doomed clone (see mechanics.timeline.DoomedTimelineClone) visibly swings at the moments it caused a block change - that only reads as cause-and-effect if the world isn't several ticks ahead of or behind the clone.")
                .defineInRange("timelineRewindPlaybackSpeed", 1, 1, 200);

        TIMELINE_CLONE_REPLAY_SPEED = BUILDER
                .comment("How many recorded ticks the doomed-timeline clone (see mechanics.timeline.DoomedTimelineClone) replays per real tick.",
                        "Deliberately separate from timelineRewindPlaybackSpeed and defaults to 1 (real time): the world-undo can run fast-forwarded without issue, but the clone is supposed to look like a believable re-enactment of what actually happened, not a sped-up blur. Raising this desyncs the clone from matching pace with when its own recorded actions (like breaking a block) actually happened.")
                .defineInRange("timelineCloneReplaySpeed", 1, 1, 200);

        TIMELINE_BRANCH_IDLE_PRUNE_TICKS = BUILDER
                .comment("How long (in ticks) a parallel timeline branch can sit dormant (unregistered, nobody inside) before it's automatically deleted. 72000 = 1 hour of real time.",
                        "The idle clock starts when the branch goes dormant (last player left), not when it was created - a branch someone keeps visiting never idles out.")
                .defineInRange("timelineBranchIdlePruneTicks", 72000, 20, Integer.MAX_VALUE);

        TIMELINE_BRANCH_PRUNE_SWEEP_INTERVAL = BUILDER
                .comment("How often (in ticks) the server checks for idle branches to prune. 1200 = once a minute. Doesn't need to run every tick.")
                .defineInRange("timelineBranchPruneSweepInterval", 1200, 20, 72000);

        TIME_LOOP_MAX_DURATION_TICKS = BUILDER
                .comment("Max ticks a Time Loop zone (TechTimeLoopAlpha/TechTimeLoopBeta) can be charged to *last* - i.e. how long it keeps repeating before it ends. 600 = 30 seconds, 1200 = 60 seconds.",
                        "Actual duration is however long the ability was charged, clamped to this - deliberately separate from timeLoopWindowTicks below (how much history each individual pass replays). Conflating the two was a real bug: a loop whose duration equalled its window length only ever played through once instead of repeating.")
                .defineInRange("timeLoopMaxDurationTicks", 600, 20, 1200);

        TIME_LOOP_WINDOW_TICKS = BUILDER
                .comment("How many ticks of recorded history each individual Time Loop pass replays before resetting and playing again. 100 = 5 seconds. Not charge-scaled - every loop replays the same window length regardless of how long it was charged to last.",
                        "Clamped to however much history is actually recorded at cast time, same as a rewind's requested length.")
                .defineInRange("timeLoopWindowTicks", 100, 20, 600);

        TIME_LOOP_RADIUS = BUILDER
                .comment("Radius (in blocks) of a Time Loop zone - flat, not charge-scaled. Default 15 matches both Timeloop α's and Timeloop β's real designed radius.")
                .defineInRange("timeLoopRadius", 15.0, 4.0, 50.0);

        RETROCOGNITION_OBSERVE_TICKS = BUILDER
                .comment("How many ticks back Retrocognition's vision reaches, and how long (in real time) the vision lasts before it catches up to \"now\" and ends. 6000 = 5 minutes.",
                        "Clamped to however much history is actually recorded at cast time (see timelineHistoryTicks) regardless of this setting.")
                .defineInRange("retrocognitionObserveTicks", 6000, 20, 6000);

        RETROCOGNITION_OVERLAY_RADIUS = BUILDER
                .comment("Radius (in blocks) around the observing player's current, live position that Retrocognition's past-block/entity overlay covers - recomputed every tick as they move, not fixed at cast time.")
                .defineInRange("retrocognitionOverlayRadius", 24.0, 4.0, 64.0);

        BUILDER.pop();

        BUILDER.push("timeRequest");

        TIME_REQUEST_DOOM_PER_TICK_BASE = BUILDER
                .comment("Base Doom Points/tick each individually open time-borrow request accrues, before the simultaneous-requests multiplier below.")
                .defineInRange("timeRequestDoomPerTickBase", 0.02, 0.0, 100.0);

        TIME_REQUEST_DOOM_MULTIPLIER_CAP = BUILDER
                .comment("Caps how much having multiple requests open at once multiplies each request's own DP accrual rate (the design doc's \"1 request = 1x, 2 = 2x, 3 = 3x\" idea) - stacking requests should be risky, not an unbounded spiral.")
                .defineInRange("timeRequestDoomMultiplierCap", 4.0, 1.0, 100.0);

        TIME_REQUEST_DOOM_CHECK_INTERVAL = BUILDER
                .comment("How often (in ticks) Doom Points accrue and Doom Events get a chance to fire for players with open requests. 200 = 10 seconds.")
                .defineInRange("timeRequestDoomCheckInterval", 200, 20, 12000);

        TIME_REQUEST_EVENT_COOLDOWN_TICKS = BUILDER
                .comment("Minimum ticks before the same Doom Event (by id) can fire again for the same player, so a high, steady DP total doesn't just repeat the same event every check.")
                .defineInRange("timeRequestEventCooldownTicks", 400, 0, Integer.MAX_VALUE);

        TIME_REQUEST_COOLDOWN_TICKS = BUILDER
                .comment("Minimum ticks between uses of the future-item-borrowing Abilitech itself, so it can't be spammed for free progression-appropriate gear.")
                .defineInRange("timeRequestCooldownTicks", 1200, 0, Integer.MAX_VALUE);

        BUILDER.pop();

        // ============================================================================================
        // Doom - the universal per-entity Doom value (mechanics.doom.DoomData). Original design for this project,
        // no 1.12.2 counterpart - unrelated to timelineDoomPointsPerTick/timeRequestDoom* above, which
        // are their own separate, pre-existing "Doom Points" bookkeeping (see mechanics.doom.DoomData's own doc
        // comment for why all three stay distinct).
        // ============================================================================================

        BUILDER.push("doom");

        DOOM_DAMAGE_SEVERITY_MAX = BUILDER
                .comment("Max Doom gained from a single hit whose severity (fraction of current HP it removes) is 1.0 (lethal-equivalent). See doomDamageSeverityCurve for how gain scales below max severity.")
                .defineInRange("doomDamageSeverityMax", 8.0, 0.0, 1000.0);

        DOOM_DAMAGE_SEVERITY_CURVE = BUILDER
                .comment("Exponent applied to hit severity before scaling by doomDamageSeverityMax - higher values make only near-death hits matter (a cubic default: a hit at 50% severity gives ~12.5% of max gain, a 90%-severity hit gives ~73%).")
                .defineInRange("doomDamageSeverityCurve", 3.0, 1.0, 10.0);

        DOOM_DAMAGE_SEVERITY_MIN_THRESHOLD = BUILDER
                .comment("Hits with severity (fraction of current HP removed) below this contribute no Doom at all - trivial pokes shouldn't register.")
                .defineInRange("doomDamageSeverityMinThreshold", 0.05, 0.0, 1.0);

        DOOM_DAMAGE_AMPLIFY_MAX = BUILDER
                .comment("Asymptotic cap on how much high Doom can amplify incoming damage (the 'bad luck'/greater destruction susceptibility natural effect) - 0.5 means damage taken can never be amplified more than +50%, no matter how high Doom climbs.")
                .defineInRange("doomDamageAmplifyMax", 0.5, 0.0, 5.0);

        DOOM_DAMAGE_AMPLIFY_HALF_POINT = BUILDER
                .comment("The Doom value at which half of doomDamageAmplifyMax's bonus is reached (a saturating curve, not linear) - default 500 means an entity at 500 Doom takes +25% damage, approaching but never reaching the +50% cap as Doom keeps climbing.")
                .defineInRange("doomDamageAmplifyHalfPoint", 500.0, 1.0, 100000.0);

        DOOM_EFFECT_DURATION_EXTEND_MAX = BUILDER
                .comment("Same saturating-curve shape as doomDamageAmplifyMax, applied to how much longer harmful (MobEffectCategory.HARMFUL) potion effects last on a high-Doom entity - 0.5 means never more than +50% duration.")
                .defineInRange("doomEffectDurationExtendMax", 0.5, 0.0, 5.0);

        DOOM_EFFECT_DURATION_HALF_POINT = BUILDER
                .comment("The Doom value at which half of doomEffectDurationExtendMax's bonus is reached - see doomDamageAmplifyHalfPoint's comment for the same shape.")
                .defineInRange("doomEffectDurationHalfPoint", 500.0, 1.0, 100000.0);

        DOOM_KILL_BASE = BUILDER
                .comment("Flat Doom a killer gains for killing any other LivingEntity, before the per-max-health scaling below.")
                .defineInRange("doomKillBase", 1.0, 0.0, 1000.0);

        DOOM_KILL_PER_MAX_HEALTH = BUILDER
                .comment("Additional Doom a killer gains per point of the victim's max health - tougher kills matter more, capped by doomKillCap so one-shotting a boss-tier mob doesn't grant an absurd spike.")
                .defineInRange("doomKillPerMaxHealth", 0.05, 0.0, 100.0);

        DOOM_KILL_CAP = BUILDER
                .comment("Max Doom a single kill can ever grant, regardless of the victim's max health.")
                .defineInRange("doomKillCap", 15.0, 0.0, 1000.0);

        DOOM_PASSIVE_ACCRUAL_CHECK_INTERVAL_TICKS = BUILDER
                .comment("How often (in ticks) each living entity's age-based passive Doom accrual is checked. 1200 = once a minute.")
                .defineInRange("doomPassiveAccrualCheckIntervalTicks", 1200, 20, 72000);

        DOOM_PASSIVE_ACCRUAL_AGE_THRESHOLD_TICKS = BUILDER
                .comment("An entity must have been continuously alive at least this long before passive age-based Doom accrual starts at all. 24000 = 20 minutes.")
                .defineInRange("doomPassiveAccrualAgeThresholdTicks", 24000, 0, Integer.MAX_VALUE);

        DOOM_PASSIVE_ACCRUAL_PER_INTERVAL = BUILDER
                .comment("How much Doom accrues per doomPassiveAccrualCheckIntervalTicks once doomPassiveAccrualAgeThresholdTicks is exceeded - deliberately tiny ('very slow' as a real number, not just flavor text).")
                .defineInRange("doomPassiveAccrualPerInterval", 0.1, 0.0, 100.0);

        DOOM_HARVEST_WINDOW_TICKS = BUILDER
                .comment("How long (in ticks) a dying entity's released Doom sits harvestable at their death position before dissipating back into reality unclaimed. 6000 = 5 minutes.")
                .defineInRange("doomHarvestWindowTicks", 6000, 20, 72000);

        DOOM_RELEASE_TICK_INTERVAL_TICKS = BUILDER
                .comment("How often (in ticks) pending released-Doom records are checked for expiry. Doesn't need to run every tick.")
                .defineInRange("doomReleaseTickIntervalTicks", 20, 1, 1200);

        DOOM_MARK_ACCRUAL_MULTIPLIER = BUILDER
                .comment("Default Doom-accumulation-rate multiplier applied by mechanics.doom.DoomMarks#applyDeadShuffleMark (and any future mark applied via the same convenience default) - a marked target's every Doom gain (damage, kills, passive accrual) is multiplied by this.")
                .defineInRange("doomMarkAccrualMultiplier", 2.0, 1.0, 100.0);

        BUILDER.pop();

        // ============================================================================================
        // Doom Relationship Interaction - mechanics.doom.RelationshipDoomEvents generating Doom from the existing
        // mechanics.relationship.RelationshipManager graph ending (death/severance/betrayal/isolation), never a
        // separate relationship mechanic of its own. Original design for this project, no 1.12.2
        // counterpart.
        // ============================================================================================

        BUILDER.push("doomRelationship");

        DOOM_RELATIONSHIP_DEATH_SCALE = BUILDER
                .comment("Scales mechanics.doom.RelationshipDoomEvents#contributionOf(relationship) into the Doom a surviving connected party gains when the other side of that relationship dies.")
                .defineInRange("doomRelationshipDeathScale", 0.5, 0.0, 1000.0);

        DOOM_RELATIONSHIP_DEATH_CAP = BUILDER
                .comment("Max Doom a single relationship's death-of-connected-entity trigger can ever grant the survivor, regardless of contribution.")
                .defineInRange("doomRelationshipDeathCap", 20.0, 0.0, 1000.0);

        DOOM_RELATIONSHIP_SEVERANCE_SCALE = BUILDER
                .comment("Scales mechanics.doom.RelationshipDoomEvents#contributionOf(relationship) into the Doom both surviving parties gain when a relationship severs (Stage 4 Instability collapse) rather than ending via death.")
                .defineInRange("doomRelationshipSeveranceScale", 0.4, 0.0, 1000.0);

        DOOM_RELATIONSHIP_SEVERANCE_CAP = BUILDER
                .comment("Max Doom a single relationship's severance can ever grant each surviving party.")
                .defineInRange("doomRelationshipSeveranceCap", 15.0, 0.0, 1000.0);

        DOOM_BETRAYAL_BASE = BUILDER
                .comment("Base Doom a killer gains on top of the normal death-of-connected-entity Doom when the entity they killed had a positive relationship with them - scaled by that relationship's trust/strength/stability (all 0-1 fractions) before the cap below applies, so betrayal only spikes when trust/strength/stability were genuinely high.")
                .defineInRange("doomBetrayalBase", 10.0, 0.0, 1000.0);

        DOOM_BETRAYAL_CAP = BUILDER
                .comment("Max betrayal-bonus Doom a single kill can ever grant, regardless of how high trust/strength/stability were.")
                .defineInRange("doomBetrayalCap", 25.0, 0.0, 1000.0);

        DOOM_ISOLATION_CHECK_INTERVAL_TICKS = BUILDER
                .comment("How often (in ticks) each online player's current relationship count is checked for isolation Doom. 1200 = once a minute.")
                .defineInRange("doomIsolationCheckIntervalTicks", 1200, 20, 72000);

        DOOM_ISOLATION_RELATIONSHIP_THRESHOLD = BUILDER
                .comment("A player with this many or fewer current relationships (see mechanics.relationship.RelationshipManager#getAllFor) accrues isolation Doom each check. 0 = only a player with literally no relationships at all.")
                .defineInRange("doomIsolationRelationshipThreshold", 0, 0, 100);

        DOOM_ISOLATION_PER_INTERVAL = BUILDER
                .comment("How much Doom accrues per doomIsolationCheckIntervalTicks while at/under the isolation threshold - deliberately tiny, matching the base Doom system's own passive-accrual pacing.")
                .defineInRange("doomIsolationPerInterval", 0.05, 0.0, 100.0);

        BUILDER.pop();

        // ============================================================================================
        // Doom Class Abilities - the 8 new Maid/Page/Sylph/Rogue of Doom techs (heroClass.<class>.doom
        // packages). Original design for this project, no 1.12.2 counterpart.
        // ============================================================================================

        BUILDER.push("doomClass");

        DOOMFORGE_INJECT_AMOUNT = BUILDER
                .comment("Doom directly injected into a target by Maid of Doom's Doomforge, per press.")
                .defineInRange("doomforgeInjectAmount", 15.0, 0.0, 1000.0);

        FINALITY_ENGINE_CHARGE_TICKS = BUILDER
                .comment("How long Maid of Doom's Finality Engine must be held before it fires.")
                .defineInRange("finalityEngineChargeTicks", 25, 1, 600);

        FINALITY_ENGINE_BASE_DAMAGE = BUILDER
                .comment("Flat damage Finality Engine deals before scaling by the target's own current Doom.")
                .defineInRange("finalityEngineBaseDamage", 2.0, 0.0, 1000.0);

        FINALITY_ENGINE_DOOM_SCALE = BUILDER
                .comment("Additional Finality Engine damage per point of the target's own current Doom, before the cap below.")
                .defineInRange("finalityEngineDoomScale", 0.05, 0.0, 100.0);

        FINALITY_ENGINE_MAX_DAMAGE = BUILDER
                .comment("Max damage a single Finality Engine hit can ever deal, regardless of the target's Doom.")
                .defineInRange("finalityEngineMaxDamage", 40.0, 0.0, 10000.0);

        DOOM_RESERVOIR_HARVEST_INTERVAL_TICKS = BUILDER
                .comment("How often (in ticks) Page of Doom's passive Doom Reservoir auto-harvests from the nearby release pool while toggled on. 100 = 5 seconds.")
                .defineInRange("doomReservoirHarvestIntervalTicks", 100, 20, 6000);

        DOOM_RESERVOIR_HARVEST_RADIUS = BUILDER
                .comment("Radius (in blocks) Doom Reservoir auto-harvests within, centered on the Page.")
                .defineInRange("doomReservoirHarvestRadius", 16.0, 1.0, 64.0);

        DOOM_RESERVOIR_HARVEST_AMOUNT_PER_PULSE = BUILDER
                .comment("Max Doom Doom Reservoir harvests per pulse - may harvest less if the release pool doesn't have this much available in range.")
                .defineInRange("doomReservoirHarvestAmountPerPulse", 5.0, 0.0, 1000.0);

        APOCALYPSE_RELEASE_RADIUS = BUILDER
                .comment("Radius (in blocks) of Page of Doom's Apocalypse Release AoE burst.")
                .defineInRange("apocalypseReleaseRadius", 8.0, 1.0, 32.0);

        APOCALYPSE_RELEASE_DAMAGE_SCALE = BUILDER
                .comment("Damage dealt by Apocalypse Release per point of the Page's own Doom actually consumed.")
                .defineInRange("apocalypseReleaseDamageScale", 0.5, 0.0, 100.0);

        APOCALYPSE_RELEASE_MAX_CONSUME = BUILDER
                .comment("Max Doom Apocalypse Release can consume from the Page's own stored Doom in one discharge - may consume less if they don't have this much stored.")
                .defineInRange("apocalypseReleaseMaxConsume", 200.0, 0.0, 100000.0);

        DOOM_REVERSAL_AMOUNT_PER_SECOND = BUILDER
                .comment("Doom removed per second from a tethered target by Sylph of Doom's Doom Reversal.")
                .defineInRange("doomReversalAmountPerSecond", 3.0, 0.0, 1000.0);

        DEATH_UNMADE_REMOVE_AMOUNT = BUILDER
                .comment("Doom instantly removed from a target by Sylph of Doom's Death Unmade, which also clears any Doom Mark they carry.")
                .defineInRange("deathUnmadeRemoveAmount", 100.0, 0.0, 100000.0);

        DOOM_REDISTRIBUTION_AMOUNT = BUILDER
                .comment("Doom moved per press by Rogue of Doom's Doom Redistribution - direction depends on whether the Rogue is sneaking (see that tech's own doc comment).")
                .defineInRange("doomRedistributionAmount", 10.0, 0.0, 1000.0);

        BUILDER.pop();

        BUILDER.push("schism");

        SCHISM_DAMAGE_AMPLIFY_FACTOR = BUILDER
                .comment("How much a corrupted Blood Bond (heroClass.prince.blood.TechPrinceBloodSchism) amplifies shared damage by, applied per bonded member the same way heroClass.witch.blood.TechBloodWitchCultOfPersonality's own (hardcoded, uncorrupted) share fraction reduces it. 1.5 = each other member takes 150% of the original hit, matching the design doc's own 10-damage-in/15-damage-out example.")
                .defineInRange("schismDamageAmplifyFactor", 1.5, 1.0, 100.0);

        SCHISM_HOSTILITY_DURATION_TICKS = BUILDER
                .comment("How long (in ticks) a corrupted bond's Fractured Loyalty hostility (a bonded member forced to target another bonded member, or the killer, after one of them dies) lasts before it's automatically cleared. 200 = 10 seconds.")
                .defineInRange("schismHostilityDurationTicks", 200, 20, Integer.MAX_VALUE);

        SCHISM_CORRUPTION_DURATION_TICKS = BUILDER
                .comment("How long (in ticks) a Corrupted Blood Bond lasts before automatically reverting to normal, uncorrupted Cult of Personality behavior. Set to 0 for corruption to last indefinitely (until the Prince removes it or a Witch of Blood restores it).")
                .defineInRange("schismCorruptionDurationTicks", 0, 0, Integer.MAX_VALUE);

        SCHISM_AURA_RADIUS = BUILDER
                .comment("Radius (in blocks) of the passive Schism Aura (heroClass.prince.blood.TechPrinceBloodSchism, toggled on like any other passive tech) - matches the design doc's own 24-block default.")
                .defineInRange("schismAuraRadius", 24.0, 4.0, 64.0);

        SCHISM_AURA_WEAKEN_FACTOR = BUILDER
                .comment("Multiplies an uncorrupted Blood Bond's own shared-damage fraction for any bonded member currently within the Schism Aura's radius - the design doc's own \"Blood Bond effectiveness reduced by 50%\" (a milder, ambient effect, separate from fully corrupting a bond outright).")
                .defineInRange("schismAuraWeakenFactor", 0.5, 0.0, 1.0);

        SCHISM_AURA_DISRUPTION_CHANCE = BUILDER
                .comment("Chance, checked once per schismAuraDisruptionIntervalTicks for every Mob within the Schism Aura's radius, that it loses its current attack target - the design doc's own \"Target Coordination Loss\" (allied entities have a chance to lose their shared target).")
                .defineInRange("schismAuraDisruptionChance", 0.15, 0.0, 1.0);

        SCHISM_AURA_DISRUPTION_INTERVAL_TICKS = BUILDER
                .comment("How often (in ticks) the Schism Aura's Target Coordination Loss check runs. 100 = 5 seconds.")
                .defineInRange("schismAuraDisruptionIntervalTicks", 100, 20, Integer.MAX_VALUE);

        SCHISM_AURA_OWNERSHIP_DECAY = BUILDER
                .comment("How many strength/stability points a nearby Ownership relationship (mechanics.relationship.RelationshipType.OWNERSHIP - a tamed pet or entity.HopeGolemEntity ally and its owner) loses per Target Coordination Loss check while inside the Schism Aura - the design doc's own \"Bond strength decreases over time\".")
                .defineInRange("schismAuraOwnershipDecay", 2.0, 0.0, 100.0);

        BUILDER.pop();

        BUILDER.push("crimsonDiscord");

        CRIMSON_DISCORD_AURA_RADIUS = BUILDER
                .comment("Radius (in blocks) of the passive Crimson Discord aura (heroClass.bard.blood.TechBardBloodCrimsonDiscord's \"Social Decay\", toggled on like any other passive tech) - matches that design doc's own 24-block recommendation.")
                .defineInRange("crimsonDiscordAuraRadius", 24.0, 4.0, 64.0);

        CRIMSON_DISCORD_INSTABILITY_GAIN_PER_PULSE = BUILDER
                .comment("How much Instability (and, at half this rate, how much Stability) every relationship touching a nearby entity gains per Crimson Discord aura pulse.")
                .defineInRange("crimsonDiscordInstabilityGainPerPulse", 3.0, 0.0, 100.0);

        CRIMSON_DISCORD_PULSE_INTERVAL_TICKS = BUILDER
                .comment("How often (in ticks) the Crimson Discord aura pulses. 100 = 5 seconds.")
                .defineInRange("crimsonDiscordPulseIntervalTicks", 100, 20, Integer.MAX_VALUE);

        CRIMSON_DISCORD_BURST_AMOUNT = BUILDER
                .comment("How much Instability a single press of Crimson Discord (aimed at a specific entity) applies to all of that entity's relationships at once.")
                .defineInRange("crimsonDiscordBurstAmount", 15.0, 0.0, 100.0);

        CRIMSON_DISCORD_NATURAL_DECAY_AMOUNT = BUILDER
                .comment("How much Instability every tracked relationship loses per crimsonDiscordNaturalDecayIntervalTicks when nothing is actively raising it - the design doc's own \"Instability naturally decays over time when no Bard of Blood influence is present\".")
                .defineInRange("crimsonDiscordNaturalDecayAmount", 2.0, 0.0, 100.0);

        CRIMSON_DISCORD_NATURAL_DECAY_INTERVAL_TICKS = BUILDER
                .comment("How often (in ticks) Instability naturally decays, and how often every tracked relationship gets checked for Stage 4 collapse regardless of whether a Bard is nearby. 200 = 10 seconds.")
                .defineInRange("crimsonDiscordNaturalDecayIntervalTicks", 200, 20, Integer.MAX_VALUE);

        CRIMSON_DISCORD_DOMINO_RADIUS = BUILDER
                .comment("Radius (in blocks) around a just-collapsed relationship's own members within which other relationships are hit by the Domino Effect.")
                .defineInRange("crimsonDiscordDominoRadius", 12.0, 4.0, 64.0);

        CRIMSON_DISCORD_DOMINO_BUMP = BUILDER
                .comment("How much Instability the Domino Effect adds to each nearby relationship when one collapses - matches the design doc's own worked example (\"+10 Instability\").")
                .defineInRange("crimsonDiscordDominoBump", 10.0, 0.0, 100.0);

        CRIMSON_DISCORD_VENGEANCE_FAIL_DIVISOR = BUILDER
                .comment("Chance (Instability / this value) that heroClass.witch.blood.CultOfPersonalityManager's own Blood Vengeance retaliation fails to fire for a given bonded member, once its linked mechanics.relationship.RelationshipType.FAMILY relationship has any Instability at all - the higher this number, the less Instability affects retaliation reliability.")
                .defineInRange("crimsonDiscordVengeanceFailDivisor", 150.0, 1.0, 10000.0);

        CRIMSON_DISCORD_NEW_RIVALRIES_PER_PULSE = BUILDER
                .comment("How many brand-new mechanics.relationship.RelationshipType.RIVALRY relationships the Crimson Discord aura seeds per pulse, between random nearby Mob pairs that don't already have any relationship at all - the design doc's own \"even entities with no existing bond should start to turn on each other\". Each seeded rivalry then escalates over time at the same rate as every other relationship in range (see crimsonDiscordInstabilityGainPerPulse).")
                .defineInRange("crimsonDiscordNewRivalriesPerPulse", 2, 0, 100);

        CRIMSON_DISCORD_FIGHT_THRESHOLD = BUILDER
                .comment("Once a RIVALRY relationship's own Instability reaches this value, both mobs are actually set hostile toward each other (real AI targeting, not just a hidden number) - matches the doc's own \"Wolf C -> Attacks Wolf A\"-style spirit, but for total strangers this time, not existing pack/ownership relationships.")
                .defineInRange("crimsonDiscordFightThreshold", 40.0, 0.0, 100.0);

        BUILDER.pop();

        BUILDER.push("relationships");

        RELATIONSHIP_FIGHTING_TOGETHER_WINDOW_TICKS = BUILDER
                .comment("How recently two different attackers must have both hit the same victim for mechanics.relationship.RelationshipManager to treat them as \"Fighting Together\" (the design doc's own event) and reinforce a relationship between the attackers themselves, not either of them and the victim. 100 = 5 seconds.")
                .defineInRange("relationshipFightingTogetherWindowTicks", 100, 20, Integer.MAX_VALUE);

        RELATIONSHIP_FIGHTING_TOGETHER_GAIN = BUILDER
                .comment("How much Trust, Familiarity, and Strength two entities gain toward each other's shared relationship when \"Fighting Together\" triggers.")
                .defineInRange("relationshipFightingTogetherGain", 3.0, 0.0, 100.0);

        RELATIONSHIP_DAMAGE_CONFLICT_GAIN = BUILDER
                .comment("How much Conflict a relationship gains whenever one side damages the other - applies to any pair regardless of existing relationship type (creating one if none exists yet), the design doc's own \"Damage -> Conflict increases\"/\"repeated combat creates a known enemy\".")
                .defineInRange("relationshipDamageConflictGain", 4.0, 0.0, 100.0);

        RELATIONSHIP_DAMAGE_FAMILIARITY_GAIN = BUILDER
                .comment("How much Familiarity a relationship gains alongside relationshipDamageConflictGain whenever one side damages the other.")
                .defineInRange("relationshipDamageFamiliarityGain", 2.0, 0.0, 100.0);

        RELATIONSHIP_BETRAYAL_AFFINITY_LOSS = BUILDER
                .comment("How much Affinity a positive relationship (Loyalty/Friendship/Family/Ownership) loses when one side kills the other - the design doc's own \"Betrayal\" event, on top of Strength dropping to 0 outright.")
                .defineInRange("relationshipBetrayalAffinityLoss", 60.0, 0.0, 200.0);

        RELATIONSHIP_BETRAYAL_CONFLICT_GAIN = BUILDER
                .comment("How much Conflict a positive relationship gains on Betrayal (see relationshipBetrayalAffinityLoss).")
                .defineInRange("relationshipBetrayalConflictGain", 40.0, 0.0, 100.0);

        RELATIONSHIP_BETRAYAL_STABILITY_LOSS = BUILDER
                .comment("How much Stability a positive relationship loses on Betrayal (see relationshipBetrayalAffinityLoss) - the design doc's own \"-Stability\", making an already-betrayed relationship easier to further corrupt or destabilize.")
                .defineInRange("relationshipBetrayalStabilityLoss", 30.0, 0.0, 100.0);

        RELATIONSHIP_NEARBY_RADIUS = BUILDER
                .comment("Radius (in blocks) within which two entities that already have a relationship count as \"nearby\" for the design doc's own \"Spending Time Together\" event (passive Familiarity growth) - checked on the same sweep as Crimson Discord's own natural Instability decay (see crimsonDiscordNaturalDecayIntervalTicks), not a separate timer.")
                .defineInRange("relationshipNearbyRadius", 8.0, 1.0, 64.0);

        RELATIONSHIP_NEARBY_FAMILIARITY_GAIN = BUILDER
                .comment("How much Familiarity two already-related, currently-nearby entities gain per sweep (see relationshipNearbyRadius).")
                .defineInRange("relationshipNearbyFamiliarityGain", 1.0, 0.0, 100.0);

        BUILDER.pop();

        // ============================================================================================
        // Golem - ported from ModularBosses (1.8)'s "206 Golem" config category (entity.EntityGolem).
        // The original's own configurable loot-string-list isn't ported - see GolemEntity's own doc
        // comment for why a flat drop of the mimicked block is used instead.
        // ============================================================================================

        BUILDER.push("golem");

        GOLEM_MAX_HEALTH_MULTIPLIER = BUILDER
                .comment("The golem's max health is its mimicked spawn block's own hardness multiplied by this.")
                .defineInRange("golemMaxHealthMultiplier", 20.0, 1.0, 1000.0);

        GOLEM_DAMAGE_MULTIPLIER = BUILDER
                .comment("The golem's attack damage is its mimicked spawn block's own hardness multiplied by this.")
                .defineInRange("golemDamageMultiplier", 1.0, 0.0, 1000.0);

        GOLEM_ATTACK_COOLDOWN_TICKS = BUILDER
                .comment("Minimum ticks between the golem picking a new attack (Throw/Stomp/Roll) while it has a target.")
                .defineInRange("golemAttackCooldownTicks", 40, 1, Integer.MAX_VALUE);

        GOLEM_EXP_DROP = BUILDER
                .comment("Experience dropped on killing a golem.")
                .defineInRange("golemExpDrop", 10, 0, Integer.MAX_VALUE);

        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    public static int abstrataSwitcherRung;
    public static int requiredRungToGT;
    public static int questBedSpawnDistance;
    public static int questBedSpawnArea;
    public static int skaiaScrollLimit;
    public static Set<String> skaiaScrollBlacklist;
    public static int timelineHistoryTicks;
    public static double timelineDoomPointsPerTick;
    public static int timelineRewindPlaybackSpeed;
    public static int timelineCloneReplaySpeed;
    public static int timelineBranchIdlePruneTicks;
    public static int timelineBranchPruneSweepInterval;
    public static int timeLoopMaxDurationTicks;
    public static int timeLoopWindowTicks;
    public static double timeLoopRadius;
    public static int retrocognitionObserveTicks;
    public static double retrocognitionOverlayRadius;
    public static double timeRequestDoomPerTickBase;
    public static double timeRequestDoomMultiplierCap;
    public static int timeRequestDoomCheckInterval;
    public static int timeRequestEventCooldownTicks;
    public static int timeRequestCooldownTicks;
    public static double doomDamageSeverityMax;
    public static double doomDamageSeverityCurve;
    public static double doomDamageSeverityMinThreshold;
    public static double doomDamageAmplifyMax;
    public static double doomDamageAmplifyHalfPoint;
    public static double doomEffectDurationExtendMax;
    public static double doomEffectDurationHalfPoint;
    public static double doomKillBase;
    public static double doomKillPerMaxHealth;
    public static double doomKillCap;
    public static int doomPassiveAccrualCheckIntervalTicks;
    public static int doomPassiveAccrualAgeThresholdTicks;
    public static double doomPassiveAccrualPerInterval;
    public static int doomHarvestWindowTicks;
    public static int doomReleaseTickIntervalTicks;
    public static double doomMarkAccrualMultiplier;
    public static double doomRelationshipDeathScale;
    public static double doomRelationshipDeathCap;
    public static double doomRelationshipSeveranceScale;
    public static double doomRelationshipSeveranceCap;
    public static double doomBetrayalBase;
    public static double doomBetrayalCap;
    public static int doomIsolationCheckIntervalTicks;
    public static int doomIsolationRelationshipThreshold;
    public static double doomIsolationPerInterval;
    public static double doomforgeInjectAmount;
    public static int finalityEngineChargeTicks;
    public static double finalityEngineBaseDamage;
    public static double finalityEngineDoomScale;
    public static double finalityEngineMaxDamage;
    public static int doomReservoirHarvestIntervalTicks;
    public static double doomReservoirHarvestRadius;
    public static double doomReservoirHarvestAmountPerPulse;
    public static double apocalypseReleaseRadius;
    public static double apocalypseReleaseDamageScale;
    public static double apocalypseReleaseMaxConsume;
    public static double doomReversalAmountPerSecond;
    public static double deathUnmadeRemoveAmount;
    public static double doomRedistributionAmount;
    public static double schismDamageAmplifyFactor;
    public static int schismHostilityDurationTicks;
    public static int schismCorruptionDurationTicks;
    public static double schismAuraRadius;
    public static double schismAuraWeakenFactor;
    public static double schismAuraDisruptionChance;
    public static int schismAuraDisruptionIntervalTicks;
    public static double schismAuraOwnershipDecay;
    public static double crimsonDiscordAuraRadius;
    public static double crimsonDiscordInstabilityGainPerPulse;
    public static int crimsonDiscordPulseIntervalTicks;
    public static double crimsonDiscordBurstAmount;
    public static double crimsonDiscordNaturalDecayAmount;
    public static int crimsonDiscordNaturalDecayIntervalTicks;
    public static double crimsonDiscordDominoRadius;
    public static double crimsonDiscordDominoBump;
    public static double crimsonDiscordVengeanceFailDivisor;
    public static int crimsonDiscordNewRivalriesPerPulse;
    public static double crimsonDiscordFightThreshold;
    public static int relationshipFightingTogetherWindowTicks;
    public static double relationshipFightingTogetherGain;
    public static double relationshipDamageConflictGain;
    public static double relationshipDamageFamiliarityGain;
    public static double relationshipBetrayalAffinityLoss;
    public static double relationshipBetrayalConflictGain;
    public static double relationshipBetrayalStabilityLoss;
    public static double relationshipNearbyRadius;
    public static double relationshipNearbyFamiliarityGain;
    public static boolean combatOverhaul;
    public static boolean keepPortfolioOnDeath;
    public static boolean restrictedStrife;
    public static Set<String> restrictedStrifeBypass;
    public static int strifeCardMobDrops;
    public static Set<String> strifeCardMobDropsWhitelist;
    public static int strifeDeckMaxSize;
    public static double weaponAttackMultiplier;
    public static double golemMaxHealthMultiplier;
    public static double golemDamageMultiplier;
    public static int golemAttackCooldownTicks;
    public static int golemExpDrop;

    private static boolean validateNonBlankString(final Object obj) {
        return obj instanceof String str && !str.isBlank();
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        abstrataSwitcherRung = ABSTRATA_SWITCHER_RUNG.get();
        requiredRungToGT = REQUIRED_RUNG_TO_GT.get();
        questBedSpawnDistance = QUEST_BED_SPAWN_DISTANCE.get();
        questBedSpawnArea = QUEST_BED_SPAWN_AREA.get();
        skaiaScrollLimit = SKAIAN_SCROLL_LIMIT.get();
        skaiaScrollBlacklist = Set.copyOf(SKAIAN_SCROLL_BLACKLIST.get());
        timelineHistoryTicks = TIMELINE_HISTORY_TICKS.get();
        timelineDoomPointsPerTick = TIMELINE_DOOM_POINTS_PER_TICK.get();
        timelineRewindPlaybackSpeed = TIMELINE_REWIND_PLAYBACK_SPEED.get();
        timelineCloneReplaySpeed = TIMELINE_CLONE_REPLAY_SPEED.get();
        timelineBranchIdlePruneTicks = TIMELINE_BRANCH_IDLE_PRUNE_TICKS.get();
        timelineBranchPruneSweepInterval = TIMELINE_BRANCH_PRUNE_SWEEP_INTERVAL.get();
        timeLoopMaxDurationTicks = TIME_LOOP_MAX_DURATION_TICKS.get();
        timeLoopWindowTicks = TIME_LOOP_WINDOW_TICKS.get();
        timeLoopRadius = TIME_LOOP_RADIUS.get();
        retrocognitionObserveTicks = RETROCOGNITION_OBSERVE_TICKS.get();
        retrocognitionOverlayRadius = RETROCOGNITION_OVERLAY_RADIUS.get();
        timeRequestDoomPerTickBase = TIME_REQUEST_DOOM_PER_TICK_BASE.get();
        timeRequestDoomMultiplierCap = TIME_REQUEST_DOOM_MULTIPLIER_CAP.get();
        timeRequestDoomCheckInterval = TIME_REQUEST_DOOM_CHECK_INTERVAL.get();
        timeRequestEventCooldownTicks = TIME_REQUEST_EVENT_COOLDOWN_TICKS.get();
        timeRequestCooldownTicks = TIME_REQUEST_COOLDOWN_TICKS.get();
        doomDamageSeverityMax = DOOM_DAMAGE_SEVERITY_MAX.get();
        doomDamageSeverityCurve = DOOM_DAMAGE_SEVERITY_CURVE.get();
        doomDamageSeverityMinThreshold = DOOM_DAMAGE_SEVERITY_MIN_THRESHOLD.get();
        doomDamageAmplifyMax = DOOM_DAMAGE_AMPLIFY_MAX.get();
        doomDamageAmplifyHalfPoint = DOOM_DAMAGE_AMPLIFY_HALF_POINT.get();
        doomEffectDurationExtendMax = DOOM_EFFECT_DURATION_EXTEND_MAX.get();
        doomEffectDurationHalfPoint = DOOM_EFFECT_DURATION_HALF_POINT.get();
        doomKillBase = DOOM_KILL_BASE.get();
        doomKillPerMaxHealth = DOOM_KILL_PER_MAX_HEALTH.get();
        doomKillCap = DOOM_KILL_CAP.get();
        doomPassiveAccrualCheckIntervalTicks = DOOM_PASSIVE_ACCRUAL_CHECK_INTERVAL_TICKS.get();
        doomPassiveAccrualAgeThresholdTicks = DOOM_PASSIVE_ACCRUAL_AGE_THRESHOLD_TICKS.get();
        doomPassiveAccrualPerInterval = DOOM_PASSIVE_ACCRUAL_PER_INTERVAL.get();
        doomHarvestWindowTicks = DOOM_HARVEST_WINDOW_TICKS.get();
        doomReleaseTickIntervalTicks = DOOM_RELEASE_TICK_INTERVAL_TICKS.get();
        doomMarkAccrualMultiplier = DOOM_MARK_ACCRUAL_MULTIPLIER.get();
        doomRelationshipDeathScale = DOOM_RELATIONSHIP_DEATH_SCALE.get();
        doomRelationshipDeathCap = DOOM_RELATIONSHIP_DEATH_CAP.get();
        doomRelationshipSeveranceScale = DOOM_RELATIONSHIP_SEVERANCE_SCALE.get();
        doomRelationshipSeveranceCap = DOOM_RELATIONSHIP_SEVERANCE_CAP.get();
        doomBetrayalBase = DOOM_BETRAYAL_BASE.get();
        doomBetrayalCap = DOOM_BETRAYAL_CAP.get();
        doomIsolationCheckIntervalTicks = DOOM_ISOLATION_CHECK_INTERVAL_TICKS.get();
        doomIsolationRelationshipThreshold = DOOM_ISOLATION_RELATIONSHIP_THRESHOLD.get();
        doomIsolationPerInterval = DOOM_ISOLATION_PER_INTERVAL.get();
        doomforgeInjectAmount = DOOMFORGE_INJECT_AMOUNT.get();
        finalityEngineChargeTicks = FINALITY_ENGINE_CHARGE_TICKS.get();
        finalityEngineBaseDamage = FINALITY_ENGINE_BASE_DAMAGE.get();
        finalityEngineDoomScale = FINALITY_ENGINE_DOOM_SCALE.get();
        finalityEngineMaxDamage = FINALITY_ENGINE_MAX_DAMAGE.get();
        doomReservoirHarvestIntervalTicks = DOOM_RESERVOIR_HARVEST_INTERVAL_TICKS.get();
        doomReservoirHarvestRadius = DOOM_RESERVOIR_HARVEST_RADIUS.get();
        doomReservoirHarvestAmountPerPulse = DOOM_RESERVOIR_HARVEST_AMOUNT_PER_PULSE.get();
        apocalypseReleaseRadius = APOCALYPSE_RELEASE_RADIUS.get();
        apocalypseReleaseDamageScale = APOCALYPSE_RELEASE_DAMAGE_SCALE.get();
        apocalypseReleaseMaxConsume = APOCALYPSE_RELEASE_MAX_CONSUME.get();
        doomReversalAmountPerSecond = DOOM_REVERSAL_AMOUNT_PER_SECOND.get();
        deathUnmadeRemoveAmount = DEATH_UNMADE_REMOVE_AMOUNT.get();
        doomRedistributionAmount = DOOM_REDISTRIBUTION_AMOUNT.get();
        schismDamageAmplifyFactor = SCHISM_DAMAGE_AMPLIFY_FACTOR.get();
        schismHostilityDurationTicks = SCHISM_HOSTILITY_DURATION_TICKS.get();
        schismCorruptionDurationTicks = SCHISM_CORRUPTION_DURATION_TICKS.get();
        schismAuraRadius = SCHISM_AURA_RADIUS.get();
        schismAuraWeakenFactor = SCHISM_AURA_WEAKEN_FACTOR.get();
        schismAuraDisruptionChance = SCHISM_AURA_DISRUPTION_CHANCE.get();
        schismAuraDisruptionIntervalTicks = SCHISM_AURA_DISRUPTION_INTERVAL_TICKS.get();
        schismAuraOwnershipDecay = SCHISM_AURA_OWNERSHIP_DECAY.get();
        crimsonDiscordAuraRadius = CRIMSON_DISCORD_AURA_RADIUS.get();
        crimsonDiscordInstabilityGainPerPulse = CRIMSON_DISCORD_INSTABILITY_GAIN_PER_PULSE.get();
        crimsonDiscordPulseIntervalTicks = CRIMSON_DISCORD_PULSE_INTERVAL_TICKS.get();
        crimsonDiscordBurstAmount = CRIMSON_DISCORD_BURST_AMOUNT.get();
        crimsonDiscordNaturalDecayAmount = CRIMSON_DISCORD_NATURAL_DECAY_AMOUNT.get();
        crimsonDiscordNaturalDecayIntervalTicks = CRIMSON_DISCORD_NATURAL_DECAY_INTERVAL_TICKS.get();
        crimsonDiscordDominoRadius = CRIMSON_DISCORD_DOMINO_RADIUS.get();
        crimsonDiscordDominoBump = CRIMSON_DISCORD_DOMINO_BUMP.get();
        crimsonDiscordVengeanceFailDivisor = CRIMSON_DISCORD_VENGEANCE_FAIL_DIVISOR.get();
        crimsonDiscordNewRivalriesPerPulse = CRIMSON_DISCORD_NEW_RIVALRIES_PER_PULSE.get();
        crimsonDiscordFightThreshold = CRIMSON_DISCORD_FIGHT_THRESHOLD.get();
        relationshipFightingTogetherWindowTicks = RELATIONSHIP_FIGHTING_TOGETHER_WINDOW_TICKS.get();
        relationshipFightingTogetherGain = RELATIONSHIP_FIGHTING_TOGETHER_GAIN.get();
        relationshipDamageConflictGain = RELATIONSHIP_DAMAGE_CONFLICT_GAIN.get();
        relationshipDamageFamiliarityGain = RELATIONSHIP_DAMAGE_FAMILIARITY_GAIN.get();
        relationshipBetrayalAffinityLoss = RELATIONSHIP_BETRAYAL_AFFINITY_LOSS.get();
        relationshipBetrayalConflictGain = RELATIONSHIP_BETRAYAL_CONFLICT_GAIN.get();
        relationshipBetrayalStabilityLoss = RELATIONSHIP_BETRAYAL_STABILITY_LOSS.get();
        relationshipNearbyRadius = RELATIONSHIP_NEARBY_RADIUS.get();
        relationshipNearbyFamiliarityGain = RELATIONSHIP_NEARBY_FAMILIARITY_GAIN.get();
        combatOverhaul = COMBAT_OVERHAUL.get();
        keepPortfolioOnDeath = KEEP_PORTFOLIO_ON_DEATH.get();
        restrictedStrife = RESTRICTED_STRIFE.get();
        restrictedStrifeBypass = Set.copyOf(RESTRICTED_STRIFE_BYPASS.get());
        strifeCardMobDrops = STRIFE_CARD_MOB_DROPS.get();
        strifeCardMobDropsWhitelist = Set.copyOf(STRIFE_CARD_MOB_DROPS_WHITELIST.get());
        strifeDeckMaxSize = STRIFE_DECK_MAX_SIZE.get();
        weaponAttackMultiplier = WEAPON_ATTACK_MULTIPLIER.get();
        golemMaxHealthMultiplier = GOLEM_MAX_HEALTH_MULTIPLIER.get();
        golemDamageMultiplier = GOLEM_DAMAGE_MULTIPLIER.get();
        golemAttackCooldownTicks = GOLEM_ATTACK_COOLDOWN_TICKS.get();
        golemExpDrop = GOLEM_EXP_DROP.get();
    }
}
