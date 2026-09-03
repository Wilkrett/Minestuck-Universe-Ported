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

        // ============================================================================================
        // Everything else that used to live here (timeline/doom/doomRelationship/schism/
        // crimsonDiscord/relationships/golem) was either this project's own original design with no
        // 1.12.2 counterpart, or (golem specifically) a real port of another mod's own config category
        // that still only had a single consuming class - moved to local constants on the one (or few)
        // mechanics/entity classes that actually consume each value; see those classes' own doc
        // comments (entity.GolemEntity for golem specifically). Strife/godTier stay in this file since
        // they're real ports of MinestuckUniverse's own MSUConfig with multiple/cross-cutting consumers.
        // ============================================================================================
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    public static int abstrataSwitcherRung;
    public static int requiredRungToGT;
    public static int questBedSpawnDistance;
    public static int questBedSpawnArea;
    public static int skaiaScrollLimit;
    public static Set<String> skaiaScrollBlacklist;
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
        skaiaScrollLimit = SKAIAN_SCROLL_LIMIT.get();
        skaiaScrollBlacklist = Set.copyOf(SKAIAN_SCROLL_BLACKLIST.get());
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
