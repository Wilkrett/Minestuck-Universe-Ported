package org.wilkretawesomesauce.minestuckuniverseported.item.armor;

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
 * Real flat multi-box frog-face hat ({@code client.model.FrogHatModel}) - see {@code WizardHatItem}'s own
 * doc comment for the rendering bug this fixes (same category, simpler geometry - a single unrotated part)
 * and why {@code initializeClient} is the safe override point here.
 * <p>
 * Purely about how this item renders <i>worn by a real player</i>. {@code client.render.FrogHatLayer}
 * (the render layer for a Frog <i>mob</i> wearing a dropped hat via {@code ConsortHatsData}) is a separate,
 * already-existing code path this change doesn't touch - it still uses a generic baked vanilla armor head
 * shape rather than this real model, a pre-existing, separately-tracked gap.
 */
public class FrogHatItem extends ArmorItem
{
	public FrogHatItem(Holder<ArmorMaterial> material, Properties properties)
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
				return MSUHatModels.frogHat();
			}
		});
	}
}
