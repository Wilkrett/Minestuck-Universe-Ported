package org.wilkretawesomesauce.minestuckuniverseported.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.MSUItemComponents;
import org.wilkretawesomesauce.minestuckuniverseported.MSUItems;
import org.wilkretawesomesauce.minestuckuniverseported.client.gui.MSUKindSelectScreen;
import org.wilkretawesomesauce.minestuckuniverseported.strife.StrifePortfolioHandler;
import org.wilkretawesomesauce.minestuckuniverseported.strife.StrifeSpecibus;
import org.wilkretawesomesauce.minestuckuniverseported.strife.StrifeSpecibusData;

import java.util.List;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code items.ItemStrifeCard}.
 * <p>
 * A blank card can be given a kind (opens {@link MSUKindSelectScreen}); once assigned, right-clicking
 * assigns it into the player's portfolio like any other weapon (via
 * {@link StrifePortfolioHandler#assignStrife}).
 * <p>
 * Storage difference from the original: the specibus is held in a {@link StrifeSpecibusData} data
 * component ({@link MSUItemComponents#STRIFE_SPECIBUS}) instead of a raw {@code "StrifeSpecibus"} NBT
 * compound - see that class for why.
 * <p>
 * Not yet ported: the item-model "assigned/invalid" texture swap the 1.12.2 version did via an item
 * property override. That mechanism doesn't exist anymore (replaced by the 1.21.2+ item model
 * select/range_dispatch system); the card currently always renders with one static texture regardless
 * of its assignment state. The blank/assigned/invalid textures are all present under
 * textures/item/ already, just not wired into the model yet.
 * <p>
 * Opens {@link MSUKindSelectScreen} via its own static {@code open(Player, InteractionHand)} rather than
 * calling {@code Minecraft.getInstance().setScreen(...)} inline here - see that method's own doc comment
 * for why a bare call like that, inlined into a common class, used to crash a dedicated server outright
 * (this was CLAUDE.md's documented known gap #6, now fixed here and in {@code blocks.AbilitechnosynthBlock}).
 */
public class StrifeCardItem extends Item
{
	public StrifeCardItem(Properties properties)
	{
		super(properties.stacksTo(1));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack stack = player.getItemInHand(hand);

		if(StrifePortfolioHandler.isFull(player))
		{
			if(!level.isClientSide())
				player.displayClientMessage(Component.translatable("status.strife.portfolioFull"), true);
			return InteractionResultHolder.fail(stack);
		}

		StrifeSpecibusData data = stack.get(MSUItemComponents.STRIFE_SPECIBUS);
		if(data != null)
		{
			if(data.isAssigned())
			{
				if(!level.isClientSide())
					StrifePortfolioHandler.assignStrife(player, hand);
			}
			// else: card already has (empty) specibus data but no kind - nothing further to do here,
			// mirrors the original's harmless re-injection of an empty specibus in this case.
		}
		else if(level.isClientSide())
		{
			MSUKindSelectScreen.open(player, hand);
		}

		return InteractionResultHolder.success(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
	{
		super.appendHoverText(stack, context, tooltip, flag);

		StrifeSpecibusData data = stack.get(MSUItemComponents.STRIFE_SPECIBUS);
		if(data == null)
			return;

		if(!data.isAssigned())
		{
			tooltip.add(Component.literal("(invalid data)"));
			return;
		}

		StrifeSpecibus specibus = data.toSpecibus();
		tooltip.add(Component.literal("(").append(specibus.getKindAbstratus().getDisplayName()).append(Component.literal(")")));

		List<ItemStack> contents = specibus.getContents();
		int shown = Math.min(contents.size(), 5);
		for(int i = 0; i < shown; i++)
		{
			ItemStack item = contents.get(i);
			tooltip.add(Component.literal(item.getCount() + "x ").append(item.getHoverName()));
		}
		if(contents.size() > shown)
			tooltip.add(Component.translatable("container.shulkerBox.more", contents.size() - shown));
	}

	public static ItemStack createFromSpecibus(StrifeSpecibus specibus)
	{
		ItemStack stack = new ItemStack(MSUItems.STRIFE_CARD.get());
		stack.set(MSUItemComponents.STRIFE_SPECIBUS, StrifeSpecibusData.fromSpecibus(specibus));
		return stack;
	}
}
