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
 * Real conical multi-box archmage hat ({@code client.model.ArchmageHatModel}) - see {@link WizardHatItem}'s
 * own doc comment for the rendering bug this fixes and why {@code initializeClient} is the safe override
 * point here.
 */
public class ArchmageHatItem extends ArmorItem
{
	public ArchmageHatItem(Holder<ArmorMaterial> material, Properties properties)
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
				return MSUHatModels.archmageHat();
			}
		});
	}
}
