package org.wilkretawesomesauce.minestuckuniverseported;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.wilkretawesomesauce.minestuckuniverseported.strife.MSUKindAbstrata;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUParticles;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Minestuckuniverseported.MODID)
public class Minestuckuniverseported {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "minestuckuniverseported";

    public static ResourceLocation id(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    // Create a Deferred Register to hold Blocks which will all be registered under the "minestuckuniverseported" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "minestuckuniverseported" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "minestuckuniverseported" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Minestuck's own weapons tab (com.mraof.minestuck.item.MSCreativeTabs.WEAPONS, confirmed via javap
    // against the dependency jar - registered under the id "minestuck:weapons") - this addon's own tab
    // is placed directly after it, both by user request and because this addon is a Minestuck addon.
    private static final ResourceKey<CreativeModeTab> MINESTUCK_WEAPONS_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath("minestuck", "weapons"));

    // The one tab every item this addon registers lives in - replaces the earlier per-item placement
    // into vanilla's own Combat tab (see this class's git history) with a single dedicated home.
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MINESTUCK_UNIVERSE_TAB = CREATIVE_MODE_TABS.register("minestuck_universe",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.minestuckuniverseported"))
                    .withTabsAfter(MINESTUCK_WEAPONS_TAB)
                    .icon(() -> MSUItems.STRIFE_CARD.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(MSUItems.STRIFE_CARD.get());
                        // God Tier armor deliberately excluded - broken right now (see items.GodTierArmorItem's
                        // own doc comment: no worn-appearance render at all yet), not to be re-added until fixed.
                        output.accept(MSUItems.WIZARD_HAT.get());
                        output.accept(MSUItems.FROG_HAT.get());
                        output.accept(MSUItems.ARCHMAGE_HAT.get());
                        output.accept(MSUItems.NEEDLEWAND.get());
                        output.accept(MSUItems.MANIPULATED_MATTER.get());
                        output.accept(MSUItems.ABILITECHNOSYNTH.get());
                        output.accept(MSUItems.TEMPORAL_SENDIFICATOR.get());
                        output.accept(MSUItems.MOONSTONE.get());
                        output.accept(org.wilkretawesomesauce.minestuckuniverseported.juju.MSUJujuRegistry.JUJU_MODUS_ITEM.get());
                        output.accept(org.wilkretawesomesauce.minestuckuniverseported.juju.MSUJujuRegistry.CUE_BALL.get());
                        output.accept(MSUItems.GOLEM_SPAWN_EGG.get());
                        output.accept(MSUItems.JUKINATOR.get());
                    })
                    .build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public Minestuckuniverseported(IEventBus modEventBus, ModContainer modContainer) {
        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register our data attachments (e.g. the strife portfolio) to the mod event bus
        MSUAttachments.REGISTER.register(modEventBus);

        // Register this addon's own items and item data components
        MSUItems.REGISTER.register(modEventBus);
        MSUItemComponents.REGISTRY.register(modEventBus);
        MSUArmorMaterials.REGISTER.register(modEventBus);
        MSUMobEffects.REGISTER.register(modEventBus);
        MSUParticles.REGISTER.register(modEventBus);
        MSUBlocks.REGISTER.register(modEventBus);
        MSUBlockEntities.REGISTER.register(modEventBus);
        MSUMenuTypes.REGISTER.register(modEventBus);
        MSUEntityTypes.REGISTER.register(modEventBus);
        modEventBus.addListener(this::registerEntityAttributes);

        // Juju Modus: real item/ModusType registration, loot condition, and loot modifier serializer
        org.wilkretawesomesauce.minestuckuniverseported.juju.MSUJujuRegistry.ITEMS.register(modEventBus);
        org.wilkretawesomesauce.minestuckuniverseported.juju.MSUJujuRegistry.LOOT_CONDITIONS.register(modEventBus);
        org.wilkretawesomesauce.minestuckuniverseported.juju.MSUJujuRegistry.LOOT_MODIFIER_SERIALIZERS.register(modEventBus);

        // Skaian Scroll: loot modifier serializer (item itself is registered via MSUItems above)
        org.wilkretawesomesauce.minestuckuniverseported.item.MSUSkaianScrollRegistry.LOOT_MODIFIER_SERIALIZERS.register(modEventBus);

        // Force-load the strife kind definitions so they're registered before anything needs them
        MSUKindAbstrata.init();
        org.wilkretawesomesauce.minestuckuniverseported.skills.MSUSkills.init();
        org.wilkretawesomesauce.minestuckuniverseported.skills.MSUSkills.init();

        // Force-load the Jukinator-3000's own gamerule registration
        MSUGameRules.init();

        // Register our strife keybindings (client-only event, safe to register unconditionally - it
        // simply never fires on a dedicated server)
        modEventBus.addListener(org.wilkretawesomesauce.minestuckuniverseported.client.MSUKeyMappings::register);
        modEventBus.addListener(org.wilkretawesomesauce.minestuckuniverseported.client.MSUAbilitechKeyMappings::register);
        modEventBus.addListener(org.wilkretawesomesauce.minestuckuniverseported.client.MSUJukinatorKeyMappings::register);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        modEventBus.addListener(this::commonSetup);
    }

    /**
     * Ported from ModularBosses (1.8)'s {@code items.dispenser.BehaviorDispenseCustomMobEgg} - lets a
     * dispenser throw a golem spawn egg the same way it can throw a vanilla egg, via the same generic
     * {@code ProjectileItem} hook {@code GolemSpawnEggItem} itself implements. Deferred to
     * {@code FMLCommonSetupEvent} rather than run directly in the constructor, since
     * {@code MSUItems.GOLEM_SPAWN_EGG.get()} isn't safely resolvable until the item registry has actually
     * finished registering.
     */
    private void commonSetup(final net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        net.minecraft.world.level.block.DispenserBlock.registerBehavior(MSUItems.GOLEM_SPAWN_EGG.get(),
                new net.minecraft.core.dispenser.ProjectileDispenseBehavior(MSUItems.GOLEM_SPAWN_EGG.get()));
    }

    private void registerEntityAttributes(final EntityAttributeCreationEvent event) {
        // HopeGolemEntity extends vanilla IronGolem directly and adds no new attributes of its own,
        // so its own attribute map is just IronGolem's, unmodified.
        event.put(MSUEntityTypes.HOPE_GOLEM.get(), net.minecraft.world.entity.animal.IronGolem.createAttributes().build());
        event.put(MSUEntityTypes.GOLEM.get(), org.wilkretawesomesauce.minestuckuniverseported.entity.GolemEntity.createAttributes().build());
    }
}
