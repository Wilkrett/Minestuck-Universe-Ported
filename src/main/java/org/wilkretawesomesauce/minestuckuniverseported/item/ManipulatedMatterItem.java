package org.wilkretawesomesauce.minestuckuniverseported.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.wilkretawesomesauce.minestuckuniverseported.MSUItemComponents;

import java.util.List;
import java.util.function.Consumer;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code items.ItemManipulatedMatter} - a captured region,
 * right-clicked against a block to place it back down. {@code abilitech.heroAspect.space
 * .TechSpaceManipulator} ("Matter Manipulator") is what actually fills the
 * {@link MSUItemComponents#MANIPULATED_MATTER} component this reads; see that class's own doc comment
 * for the capture half and the confirmed real gap this doesn't cover (Space Salt machine resizing).
 * <p>
 * The original stored a hand-rolled block/tile-entity list in NBT and replayed it manually
 * (with its own {@code getPlacementPos} corner math and a client-side render outline). This instead
 * stores a real vanilla {@link StructureTemplate} (the same save/load format Structure Blocks use),
 * placed back with {@link StructureTemplate#placeInWorld} - a direct, real, more general modern
 * equivalent rather than a hand-rolled reimplementation.
 */
public class ManipulatedMatterItem extends Item
{
	public ManipulatedMatterItem(Properties properties)
	{
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context)
	{
		if(!(context.getLevel() instanceof ServerLevel serverLevel))
			return InteractionResult.SUCCESS;

		Player player = context.getPlayer();
		if(player == null || !player.getAbilities().mayBuild)
			return InteractionResult.FAIL;

		ItemStack stack = context.getItemInHand();
		CompoundTag data = stack.get(MSUItemComponents.MANIPULATED_MATTER.get());
		if(data == null || data.isEmpty())
			return InteractionResult.FAIL;

		StructureTemplate template = new StructureTemplate();
		template.load(serverLevel.holderLookup(Registries.BLOCK), data);

		BlockPos placePos = context.getClickedPos().relative(context.getClickedFace());

		boolean placed = template.placeInWorld(serverLevel, placePos, placePos,
				new StructurePlaceSettings(), serverLevel.getRandom(), 2);

		if(!placed)
		{
			player.displayClientMessage(Component.translatable("item.manipulatedMatter.cantEdit"), true);
			return InteractionResult.FAIL;
		}

		stack.shrink(1);
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
	{
		CompoundTag data = stack.get(MSUItemComponents.MANIPULATED_MATTER.get());
		if(data != null && data.contains(StructureTemplate.SIZE_TAG))
		{
			net.minecraft.nbt.ListTag size = data.getList(StructureTemplate.SIZE_TAG, net.minecraft.nbt.Tag.TAG_INT);
			if(size.size() == 3)
				tooltip.add(Component.literal(size.getInt(0) + " x " + size.getInt(1) + " x " + size.getInt(2)));
		}
	}
}
