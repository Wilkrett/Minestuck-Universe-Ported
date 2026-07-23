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
    private static final ModConfigSpec.BooleanValue COMBAT_OVERHAUL;
    private static final ModConfigSpec.BooleanValue KEEP_PORTFOLIO_ON_DEATH;
    private static final ModConfigSpec.BooleanValue RESTRICTED_STRIFE;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> RESTRICTED_STRIFE_BYPASS;
    private static final ModConfigSpec.IntValue STRIFE_CARD_MOB_DROPS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> STRIFE_CARD_MOB_DROPS_WHITELIST;
    private static final ModConfigSpec.IntValue STRIFE_DECK_MAX_SIZE;
    private static final ModConfigSpec.DoubleValue WEAPON_ATTACK_MULTIPLIER;

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
                        "Defaults to 1 (real time) rather than fast-forwarded: blocks restoring in visible multi-tick batches read as generic \"world resetting\" rather than something actually happening, especially now that the doomed clone (see timeline.DoomedTimelineClone) visibly swings at the moments it caused a block change - that only reads as cause-and-effect if the world isn't several ticks ahead of or behind the clone.")
                .defineInRange("timelineRewindPlaybackSpeed", 1, 1, 200);

        TIMELINE_CLONE_REPLAY_SPEED = BUILDER
                .comment("How many recorded ticks the doomed-timeline clone (see timeline.DoomedTimelineClone) replays per real tick.",
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
                .comment("Max ticks a Time Loop zone (TechTimeLoop/TechTimeLoopNested) can be charged to *last* - i.e. how long it keeps repeating before it ends. 600 = 30 seconds, 1200 = 60 seconds.",
                        "Actual duration is however long the ability was charged, clamped to this - deliberately separate from timeLoopWindowTicks below (how much history each individual pass replays). Conflating the two was a real bug: a loop whose duration equalled its window length only ever played through once instead of repeating.")
                .defineInRange("timeLoopMaxDurationTicks", 600, 20, 1200);

        TIME_LOOP_WINDOW_TICKS = BUILDER
                .comment("How many ticks of recorded history each individual Time Loop pass replays before resetting and playing again. 100 = 5 seconds. Not charge-scaled - every loop replays the same window length regardless of how long it was charged to last.",
                        "Clamped to however much history is actually recorded at cast time, same as a rewind's requested length.")
                .defineInRange("timeLoopWindowTicks", 100, 20, 600);

        TIME_LOOP_RADIUS = BUILDER
                .comment("Radius (in blocks) of a Time Loop zone - flat, not charge-scaled.")
                .defineInRange("timeLoopRadius", 30.0, 4.0, 50.0);

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
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    public static int abstrataSwitcherRung;
    public static int requiredRungToGT;
    public static int questBedSpawnDistance;
    public static int questBedSpawnArea;
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
    public static boolean combatOverhaul;
    public static boolean keepPortfolioOnDeath;
    public static boolean restrictedStrife;
    public static Set<String> restrictedStrifeBypass;
    public static int strifeCardMobDrops;
    public static Set<String> strifeCardMobDropsWhitelist;
    public static int strifeDeckMaxSize;
    public static double weaponAttackMultiplier;

    private static boolean validateNonBlankString(final Object obj) {
        return obj instanceof String str && !str.isBlank();
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        abstrataSwitcherRung = ABSTRATA_SWITCHER_RUNG.get();
        requiredRungToGT = REQUIRED_RUNG_TO_GT.get();
        questBedSpawnDistance = QUEST_BED_SPAWN_DISTANCE.get();
        questBedSpawnArea = QUEST_BED_SPAWN_AREA.get();
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
        combatOverhaul = COMBAT_OVERHAUL.get();
        keepPortfolioOnDeath = KEEP_PORTFOLIO_ON_DEATH.get();
        restrictedStrife = RESTRICTED_STRIFE.get();
        restrictedStrifeBypass = Set.copyOf(RESTRICTED_STRIFE_BYPASS.get());
        strifeCardMobDrops = STRIFE_CARD_MOB_DROPS.get();
        strifeCardMobDropsWhitelist = Set.copyOf(STRIFE_CARD_MOB_DROPS_WHITELIST.get());
        strifeDeckMaxSize = STRIFE_DECK_MAX_SIZE.get();
        weaponAttackMultiplier = WEAPON_ATTACK_MULTIPLIER.get();
    }
}
