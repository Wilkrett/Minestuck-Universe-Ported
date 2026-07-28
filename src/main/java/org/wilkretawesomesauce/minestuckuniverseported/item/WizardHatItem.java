package org.wilkretawesomesauce.minestuckuniverseported.item;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.wilkretawesomesauce.minestuckuniverseported.client.model.MSUHatModels;

import java.util.function.Consumer;

/**
 * Real conical multi-box wizard hat ({@code client.model.WizardHatModel}), not vanilla's plain flat helmet
 * shape - see that class's own doc comment for the rendering bug this fixes (the imported 1.12.2 texture
 * was authored for the original's own bespoke Blockbench model, not vanilla's UV layout).
 * <p>
 * {@link Item#initializeClient} (confirmed real and present on the common {@code Item} class itself via
 * {@code javap} against this project's own NeoForge dependency jar) is only ever invoked from client-side
 * registration, never on a dedicated server, so the anonymous {@link IClientItemExtensions} below is safe
 * to reference client-only rendering types in - this class itself deliberately holds no client-only field
 * of its own (see {@link MSUHatModels}'s own doc comment for why that distinction matters).
 */
public class WizardHatItem extends ArmorItem
{
	public WizardHatItem(Holder<ArmorMaterial> material, Properties properties)
	{
		super(material, Type.HELMET, properties);
	}

	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer)
	{
		consumer.accept(new IClientItemExtensions()
		{
			@Override
			public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original)
			{
				return MSUHatModels.wizardHat();
			}
		});
	}
}
