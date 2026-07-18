package org.wilkretawesomesauce.minestuckuniverseported;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER.comment("Whether to log the dirt block on common setup").define("logDirtBlock", true);

    private static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER.comment("A magic number").defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER.comment("What you want the introduction message to be for the magic number").define("magicNumberIntroduction", "The magic number is... ");

    // a list of strings that are treated as resource locations for items
    private static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER.comment("A list of items to log on common setup.").defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);

    // ================================================================================================
    // Strife - ported from MinestuckUniverse (1.12.2)'s MSUConfig "Strife" category. NeoForge configs
    // are TOML, not the old .cfg format, and there's no setMinValue-without-a-max like 1.12.2 had, so
    // "unlimited" options use Integer.MAX_VALUE as the practical upper bound instead of leaving it open.
    // ================================================================================================

    private static final ModConfigSpec.IntValue ABSTRATA_SWITCHER_RUNG;
    private static final ModConfigSpec.IntValue REQUIRED_RUNG_TO_GT;
    private static final ModConfigSpec.IntValue TIMELINE_HISTORY_TICKS;
    private static final ModConfigSpec.DoubleValue TIMELINE_DOOM_POINTS_PER_TICK;
    private static final ModConfigSpec.IntValue TIMELINE_REWIND_PLAYBACK_SPEED;
    private static final ModConfigSpec.IntValue TIMELINE_CLONE_REPLAY_SPEED;
    private static final ModConfigSpec.IntValue TIMELINE_BRANCH_IDLE_PRUNE_TICKS;
    private static final ModConfigSpec.IntValue TIMELINE_BRANCH_PRUNE_SWEEP_INTERVAL;
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

        BUILDER.pop();

        BUILDER.push("timeline");

        TIMELINE_HISTORY_TICKS = BUILDER
                .comment("Max ticks of world history each dimension keeps, for both destructive rewinding and non-destructive past-observing. 600 = 30 seconds.",
                        "Recording is now always-on for every loaded level regardless of who's using Time abilities, so this bounds real ongoing memory use, not just a niche cost.")
                .defineInRange("timelineHistoryTicks", 600, 20, 6000);

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

        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean logDirtBlock;
    public static int magicNumber;
    public static String magicNumberIntroduction;
    public static Set<Item> items;

    public static int abstrataSwitcherRung;
    public static int requiredRungToGT;
    public static int timelineHistoryTicks;
    public static double timelineDoomPointsPerTick;
    public static int timelineRewindPlaybackSpeed;
    public static int timelineCloneReplaySpeed;
    public static int timelineBranchIdlePruneTicks;
    public static int timelineBranchPruneSweepInterval;
    public static boolean combatOverhaul;
    public static boolean keepPortfolioOnDeath;
    public static boolean restrictedStrife;
    public static Set<String> restrictedStrifeBypass;
    public static int strifeCardMobDrops;
    public static Set<String> strifeCardMobDropsWhitelist;
    public static int strifeDeckMaxSize;
    public static double weaponAttackMultiplier;

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }

    private static boolean validateNonBlankString(final Object obj) {
        return obj instanceof String str && !str.isBlank();
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();

        // convert the list of strings into a set of items
        items = ITEM_STRINGS.get().stream().map(itemName -> BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemName))).collect(Collectors.toSet());

        abstrataSwitcherRung = ABSTRATA_SWITCHER_RUNG.get();
        requiredRungToGT = REQUIRED_RUNG_TO_GT.get();
        timelineHistoryTicks = TIMELINE_HISTORY_TICKS.get();
        timelineDoomPointsPerTick = TIMELINE_DOOM_POINTS_PER_TICK.get();
        timelineRewindPlaybackSpeed = TIMELINE_REWIND_PLAYBACK_SPEED.get();
        timelineCloneReplaySpeed = TIMELINE_CLONE_REPLAY_SPEED.get();
        timelineBranchIdlePruneTicks = TIMELINE_BRANCH_IDLE_PRUNE_TICKS.get();
        timelineBranchPruneSweepInterval = TIMELINE_BRANCH_PRUNE_SWEEP_INTERVAL.get();
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
