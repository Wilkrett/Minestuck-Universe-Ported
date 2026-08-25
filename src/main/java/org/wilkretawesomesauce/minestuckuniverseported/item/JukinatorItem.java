package org.wilkretawesomesauce.minestuckuniverseported.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.MSUGameRules;
import org.wilkretawesomesauce.minestuckuniverseported.MSUItemComponents;
import org.wilkretawesomesauce.minestuckuniverseported.network.OpenJukinatorPacket;

import java.util.List;

/**
 * The "Jukinator-3000" - a real, original design for this project, no 1.12.2 counterpart. Shift-right-click
 * loads/unloads a music disc held in the player's other hand (stored via {@link MSUItemComponents#STORED_DISC});
 * a plain right-click with a disc loaded opens a real 4-lane rhythm minigame ({@code client.jukinator.JukinatorScreen})
 * whose chart is generated from the disc's own real vanilla {@code JukeboxSong} data (length/comparator output),
 * randomized fresh each time or seeded per-disc depending on the {@link MSUGameRules#JUKINATOR_RANDOM_CHARTS}
 * gamerule.
 * <p>
 * <b>Known gap, stated plainly, not an oversight</b>: no texture/model exists for this item yet - renders with
 * vanilla's own missing-texture placeholder until real art is made, same category as {@code temporal_sendificator}'s
 * own stated missing-art gap.
 */
public class JukinatorItem extends Item
{
	public JukinatorItem(Properties properties)
	{
		super(properties.stacksTo(1));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack stack = player.getItemInHand(hand);

		if(player.isShiftKeyDown())
			return swapDisc(level, player, hand, stack);

		if(level.isClientSide())
			return InteractionResultHolder.pass(stack);

		ItemStack disc = getLoadedDisc(stack);
		if(disc.isEmpty())
		{
			player.displayClientMessage(Component.translatable("status.jukinator.noDisc"), true);
			return InteractionResultHolder.fail(stack);
		}

		if(player instanceof ServerPlayer serverPlayer)
		{
			boolean seededChart = !level.getGameRules().getBoolean(MSUGameRules.JUKINATOR_RANDOM_CHARTS);
			PacketDistributor.sendToPlayer(serverPlayer, new OpenJukinatorPacket(disc.copy(), seededChart));
		}

		return InteractionResultHolder.success(stack);
	}

	private InteractionResultHolder<ItemStack> swapDisc(Level level, Player player, InteractionHand hand, ItemStack stack)
	{
		if(level.isClientSide())
			return InteractionResultHolder.pass(stack);

		ItemStack loaded = getLoadedDisc(stack);
		if(!loaded.isEmpty())
		{
			stack.remove(MSUItemComponents.STORED_DISC);
			if(!player.getInventory().add(loaded))
				player.drop(loaded, false);
			player.displayClientMessage(Component.translatable("status.jukinator.eject", loaded.getHoverName()), true);
			level.playSound(null, player, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.PLAYERS, 1.0F, 1.0F);
			return InteractionResultHolder.success(stack);
		}

		ItemStack discToInsert = findDiscToInsert(player, hand);
		if(discToInsert.isEmpty())
		{
			player.displayClientMessage(Component.translatable("status.jukinator.noDiscToInsert"), true);
			return InteractionResultHolder.fail(stack);
		}

		Component discName = discToInsert.getHoverName();
		setLoadedDisc(stack, discToInsert.copyWithCount(1));
		discToInsert.shrink(1);
		player.displayClientMessage(Component.translatable("status.jukinator.insert", discName), true);
		level.playSound(null, player, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.PLAYERS, 1.0F, 1.0F);
		return InteractionResultHolder.success(stack);
	}

	/** Unwraps {@code MSUItemComponents#STORED_DISC} (a {@link JukinatorDisc}) into the plain
	 *  {@link ItemStack} the rest of this class works with - see that wrapper's own doc comment for why
	 *  a bare {@code ItemStack} can't be the component's value type directly. */
	private static ItemStack getLoadedDisc(ItemStack jukinatorStack)
	{
		return jukinatorStack.getOrDefault(MSUItemComponents.STORED_DISC, JukinatorDisc.EMPTY).disc();
	}

	private static void setLoadedDisc(ItemStack jukinatorStack, ItemStack disc)
	{
		jukinatorStack.set(MSUItemComponents.STORED_DISC, new JukinatorDisc(disc));
	}

	/** Checks the exact other hand first (the primary, documented interaction), then falls back to
	 *  scanning the rest of the player's inventory for any disc - so shift-right-click also works for a
	 *  disc sitting in the hotbar/inventory rather than strictly requiring it in the opposite hand. Returns
	 *  the actual live {@link ItemStack} reference (never a copy) so the caller can {@code shrink()} it
	 *  directly, matching {@code BundleItem}'s own real in-place-mutation convention. */
	private static ItemStack findDiscToInsert(Player player, InteractionHand jukinatorHand)
	{
		InteractionHand otherHand = jukinatorHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		ItemStack otherHandStack = player.getItemInHand(otherHand);
		if(otherHandStack.has(DataComponents.JUKEBOX_PLAYABLE))
			return otherHandStack;

		for(int i = 0; i < player.getInventory().getContainerSize(); i++)
		{
			ItemStack candidate = player.getInventory().getItem(i);
			if(candidate.has(DataComponents.JUKEBOX_PLAYABLE))
				return candidate;
		}

		return ItemStack.EMPTY;
	}

	/** Jukinator held on the cursor, right-clicked onto a slot containing a disc (or an empty slot to
	 *  eject into) - the real vanilla mechanism {@code BundleItem} itself uses for the same kind of
	 *  "drop this onto that" inventory interaction, read directly from its own real source rather than
	 *  guessed. */
	@Override
	public boolean overrideStackedOnOther(ItemStack thisStack, Slot slot, ClickAction action, Player player)
	{
		if(action != ClickAction.SECONDARY)
			return false;

		ItemStack loaded = getLoadedDisc(thisStack);
		ItemStack slotStack = slot.getItem();

		if(slotStack.isEmpty())
		{
			if(loaded.isEmpty() || !slot.mayPlace(loaded))
				return false;

			thisStack.remove(MSUItemComponents.STORED_DISC);
			slot.safeInsert(loaded);
			return true;
		}

		if(loaded.isEmpty() && slotStack.has(DataComponents.JUKEBOX_PLAYABLE) && slot.mayPickup(player))
		{
			setLoadedDisc(thisStack, slotStack.copyWithCount(1));
			slot.remove(1);
			return true;
		}

		return false;
	}

	/** Jukinator sitting in a slot, right-clicked while a disc (or nothing, to eject) is on the cursor -
	 *  the symmetric counterpart to {@link #overrideStackedOnOther}, same real {@code BundleItem} shape. */
	@Override
	public boolean overrideOtherStackedOnMe(ItemStack thisStack, ItemStack otherStack, Slot slot, ClickAction action, Player player, SlotAccess cursorAccess)
	{
		if(action != ClickAction.SECONDARY || !slot.allowModification(player))
			return false;

		ItemStack loaded = getLoadedDisc(thisStack);

		if(otherStack.isEmpty())
		{
			if(loaded.isEmpty())
				return false;

			thisStack.remove(MSUItemComponents.STORED_DISC);
			cursorAccess.set(loaded);
			return true;
		}

		if(loaded.isEmpty() && otherStack.has(DataComponents.JUKEBOX_PLAYABLE))
		{
			setLoadedDisc(thisStack, otherStack.copyWithCount(1));
			otherStack.shrink(1);
			return true;
		}

		return false;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
	{
		super.appendHoverText(stack, context, tooltip, flag);

		ItemStack disc = getLoadedDisc(stack);
		tooltip.add(disc.isEmpty()
				? Component.translatable("tooltip.jukinator.noDisc").withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC)
				: Component.translatable("tooltip.jukinator.disc", disc.getHoverName()).withStyle(net.minecraft.ChatFormatting.BLUE, net.minecraft.ChatFormatting.ITALIC));
	}
}
