package org.wilkretawesomesauce.minestuckuniverseported.item;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.Config;
import org.wilkretawesomesauce.minestuckuniverseported.MSUItemComponents;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;
import org.wilkretawesomesauce.minestuckuniverseported.network.MSUAbilitechPackets;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.Abilitech;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechRegistry;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code items.ItemSkaianScroll} - dungeon loot that teaches the
 * reader a random Abilitech. See {@code SkaianScrollLootModifier}/{@code MSUSkaianScrollRegistry} for how
 * the random tech is rolled and stamped onto the stack at loot-generation time (as the
 * {@link MSUItemComponents#SKAIAN_SCROLL_TECH} component, replacing the original's raw
 * {@code "Skill"} NBT string).
 * <p>
 * <b>Real, new eligibility gate, not in the original</b>: the original rolled a flat random pick from
 * every registered {@code Skill} (not just Abilitechs) with no Aspect/Class check at all - its own
 * flavor text ("only a select few choose to lend their wisdom to the reader") was never actually backed by
 * code. Chest loot has no player context at roll time (no killer/entity param exists for a container's
 * loot table), so eligibility can't be checked when the scroll spawns - instead, the pool itself is
 * narrowed to Abilitechs only ({@link MSUAbilitechRegistry#getAll()}, filtered {@code !isSuper()} and the
 * config blacklist, matching the original's {@code isObtainable()}/blacklist filter), and the real
 * per-player Aspect/Class eligibility check ({@link Abilitech#canAppearOnList}, already correctly wired
 * for {@code TechHeroAspect}/{@code TechHeroClass} against the reader's real Title) happens at
 * right-click time instead - a Hero of Breath reading a rolled Blood tech's scroll is politely refused
 * rather than granted something they could never actually use.
 */
public class SkaianScrollItem extends Item
{
	public SkaianScrollItem(Properties properties)
	{
		super(properties.stacksTo(1));
	}

	@Nullable
	public static Abilitech getTech(ItemStack stack)
	{
		ResourceLocation id = stack.get(MSUItemComponents.SKAIAN_SCROLL_TECH);
		return id != null ? MSUAbilitechRegistry.get(id) : null;
	}

	public static boolean isSuperScroll(ItemStack stack)
	{
		Boolean isSuper = stack.get(MSUItemComponents.SKAIAN_SCROLL_SUPER);
		return isSuper != null && isSuper;
	}

	/** Rolls a random Abilitech from the real, eligible-for-loot pool and stamps it onto {@code stack}. */
	public static ItemStack storeRandomTech(ItemStack stack, RandomSource random)
	{
		List<Abilitech> pool = new ArrayList<>();
		for(Abilitech tech : MSUAbilitechRegistry.getAll())
		{
			if(tech.isSuper() || !tech.isObtainable())
				continue;
			if(Config.skaiaScrollBlacklist.contains(tech.getId().toString()))
				continue;
			pool.add(tech);
		}

		if(!pool.isEmpty())
			stack.set(MSUItemComponents.SKAIAN_SCROLL_TECH, pool.get(random.nextInt(pool.size())).getId());

		return stack;
	}

	@Override
	public boolean isFoil(ItemStack stack)
	{
		return isSuperScroll(stack);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack stack = player.getItemInHand(hand);

		if(level.isClientSide())
			return InteractionResultHolder.pass(stack);

		Abilitech tech = getTech(stack);
		if(tech == null)
		{
			player.displayClientMessage(Component.translatable("status.skaianScroll.empty"), true);
			return InteractionResultHolder.fail(stack);
		}

		GodTierData godTier = player.getData(MSUAttachments.GOD_TIER);

		if(godTier.isUnlocked(tech))
		{
			player.displayClientMessage(Component.translatable("status.skaianScroll.alreadyUnlocked", tech.getDisplayName()), true);
			return InteractionResultHolder.fail(stack);
		}

		if(!tech.canAppearOnList(level, player))
		{
			player.displayClientMessage(Component.translatable("status.skaianScroll.notEligible", tech.getDisplayName()), true);
			return InteractionResultHolder.fail(stack);
		}

		boolean isSuper = isSuperScroll(stack);
		if(!isSuper && Config.skaiaScrollLimit >= 0 && godTier.getScrollsUsed() >= Config.skaiaScrollLimit)
		{
			player.displayClientMessage(Component.translatable("status.skaianScroll.outOfScrolls"), true);
			return InteractionResultHolder.fail(stack);
		}

		godTier.markUnlocked(tech);
		if(!isSuper)
			godTier.addScrollsUsed();

		for(int slot = 0; slot < godTier.getTechSlots(); slot++)
		{
			if(godTier.getTech(slot) == null)
			{
				godTier.equipTech(level, player, tech, slot);
				break;
			}
		}

		if(player instanceof ServerPlayer serverPlayer)
			MSUAbilitechPackets.sendLoadoutSync(serverPlayer);

		player.displayClientMessage(Component.translatable("status.skaianScroll.unlock", tech.getDisplayName()), true);
		stack.shrink(1);
		return InteractionResultHolder.success(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
	{
		super.appendHoverText(stack, context, tooltip, flag);

		Abilitech tech = getTech(stack);
		if(tech == null)
			return;

		tooltip.add(tech.getDisplayName().copy().withStyle(net.minecraft.ChatFormatting.BLUE, net.minecraft.ChatFormatting.ITALIC));
	}
}
