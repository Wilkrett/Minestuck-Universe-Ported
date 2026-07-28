package org.wilkretawesomesauce.minestuckuniverseported;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.wilkretawesomesauce.minestuckuniverseported.inventory.TemporalSendificatorMenu;
import org.wilkretawesomesauce.minestuckuniverseported.gui.itemvoid.ItemVoidMenu;
import org.wilkretawesomesauce.minestuckuniverseported.juju.JujuMenu;

/**
 * This addon's first real {@link MenuType} registry - see {@code block.TemporalSendificatorBlock}'s doc
 * comment for why a genuine container menu is used here instead of this project's more common plain-{@code Screen}
 * GUI pattern.
 */
public final class MSUMenuTypes
{
	public static final DeferredRegister<MenuType<?>> REGISTER =
			DeferredRegister.create(Registries.MENU, Minestuckuniverseported.MODID);

	public static final DeferredHolder<MenuType<?>, MenuType<TemporalSendificatorMenu>> TEMPORAL_SENDIFICATOR =
			REGISTER.register("temporal_sendificator", () -> new MenuType<>(TemporalSendificatorMenu::new, FeatureFlags.VANILLA_SET));

	public static final DeferredHolder<MenuType<?>, MenuType<ItemVoidMenu>> ITEM_VOID =
			REGISTER.register("item_void", () -> new MenuType<>(ItemVoidMenu::new, FeatureFlags.VANILLA_SET));

	public static final DeferredHolder<MenuType<?>, MenuType<JujuMenu>> JUJU =
			REGISTER.register("juju", () -> new MenuType<>(JujuMenu::new, FeatureFlags.VANILLA_SET));

	private MSUMenuTypes()
	{
	}
}
