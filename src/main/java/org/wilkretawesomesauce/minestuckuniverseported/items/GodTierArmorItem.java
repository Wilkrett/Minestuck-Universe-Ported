package org.wilkretawesomesauce.minestuckuniverseported.items;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.wilkretawesomesauce.minestuckuniverseported.MSUItemComponents;
import org.wilkretawesomesauce.minestuckuniverseported.godtier.GodTierArmorData;

import java.util.List;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code items.godtier.ItemGTArmor}: a single generic armor item
 * per equipment slot (hood/shirt/pants/shoes), rather than one item per class - which class/aspect a
 * given stack represents lives in a {@link GodTierArmorData} data component instead of the original's
 * raw {@code class}/{@code aspect} NBT ints. See {@link org.wilkretawesomesauce.minestuckuniverseported.godtier.GodTierEvents}
 * for how a blank piece gets attuned.
 * <p>
 * <b>Known gap:</b> the original rendered each class with its own fully custom multi-part 3D armor model
 * (12 separate {@code ModelGT*} classes, each with its own multi-layer texture set - not the vanilla
 * 2-layer armor convention). That's a large amount of unverifiable-without-rendering modeling work and is
 * not attempted in this pass. Right now these items have a correct 2D inventory icon (the original's
 * generic/unattuned art) but no {@code getArmorTexture} override, so the *worn* appearance will likely
 * show as a missing texture until either flat vanilla-style armor textures or the real custom models are
 * done as a follow-up.
 */
public class GodTierArmorItem extends ArmorItem
{
	public GodTierArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties)
	{
		super(material, type, properties);
	}

	@Override
	public Component getName(ItemStack stack)
	{
		GodTierArmorData data = stack.getOrDefault(MSUItemComponents.GOD_TIER_TITLE, GodTierArmorData.BLANK);
		if(data.title().isEmpty())
			return super.getName(stack);

		return Component.translatable(super.getDescriptionId(stack) + ".attuned", data.title().get().asTextComponent());
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
	{
		super.appendHoverText(stack, context, tooltip, flag);

		GodTierArmorData data = stack.getOrDefault(MSUItemComponents.GOD_TIER_TITLE, GodTierArmorData.BLANK);
		if(data.title().isEmpty())
			tooltip.add(Component.translatable("tooltip.minestuckuniverseported.god_tier.unattuned"));
	}
}
