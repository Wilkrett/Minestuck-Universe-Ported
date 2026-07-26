package org.wilkretawesomesauce.minestuckuniverseported.strife;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.strife.StrifeData;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.MSUItemComponents;
import org.wilkretawesomesauce.minestuckuniverseported.item.StrifeCardItem;
import org.wilkretawesomesauce.minestuckuniverseported.network.MSUStrifePackets;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code strife.StrifePortfolioHandler}.
 * <p>
 * Server-side API for assigning weapons/specibi to an entity's portfolio and retrieving them again.
 * All mutating methods here are meant to be called server-side only, matching the original (which
 * early-returned on {@code world.isRemote}); the NeoForge equivalent check is {@code level().isClientSide()}.
 * <p>
 * The 1.12.2 original also stamped a {@code "StrifeAssigned"} NBT flag onto held item stacks to track
 * "this is the currently-equipped weapon". This port drops that in favour of {@link #isHeldWeapon},
 * which just compares against the portfolio's current selection directly - see the note on
 * {@link StrifeSpecibus}.
 * <p>
 * See {@link StrifePortfolioEvents} for the login/respawn sync that keeps the client's copy of the
 * portfolio attachment up to date.
 */
public final class StrifePortfolioHandler
{
	private StrifePortfolioHandler()
	{
	}

	private static StrifeData portfolioOf(LivingEntity entity)
	{
		return entity.getData(MSUAttachments.STRIFE_PORTFOLIO);
	}

	public static boolean isFull(LivingEntity entity)
	{
		return portfolioOf(entity).isPortfolioFull();
	}

	public static boolean isEmpty(LivingEntity entity)
	{
		return portfolioOf(entity).isPortfolioEmpty();
	}

	public static StrifeSpecibus[] getPortfolio(LivingEntity entity)
	{
		return portfolioOf(entity).getPortfolio();
	}

	/**
	 * True if {@code stack} matches the item currently sitting in the entity's selected specibus/weapon
	 * slot - i.e. it's "the" weapon currently held from the portfolio. Replaces the old NBT-flag check.
	 */
	public static boolean isHeldWeapon(LivingEntity entity, ItemStack stack)
	{
		if(stack.isEmpty())
			return false;
		StrifeData cap = portfolioOf(entity);
		if(!cap.isArmed() || cap.getSelectedSpecibusIndex() < 0 || cap.getSelectedWeaponIndex() < 0)
			return false;

		StrifeSpecibus[] portfolio = cap.getPortfolio();
		if(cap.getSelectedSpecibusIndex() >= portfolio.length)
			return false;
		StrifeSpecibus selected = portfolio[cap.getSelectedSpecibusIndex()];
		if(selected == null || cap.getSelectedWeaponIndex() >= selected.getContents().size())
			return false;

		return StrifeSpecibus.sameWeapon(selected.getContents().get(cap.getSelectedWeaponIndex()), stack);
	}

	/**
	 * Tries to move the currently-armed weapon into whichever compatible specibus has room, preferring
	 * the currently-selected one. Returns the specibus it ended up in, or null if it didn't fit anywhere.
	 */
	public static StrifeSpecibus moveSelectedWeapon(LivingEntity entity, ItemStack stack)
	{
		if(entity.level().isClientSide())
			return null;

		StrifeData cap = portfolioOf(entity);
		StrifeSpecibus selSpecibus = cap.getSelectedSpecibusIndex() >= 0 ? cap.getPortfolio()[cap.getSelectedSpecibusIndex()] : null;
		if(selSpecibus == null)
			return null;

		if(selSpecibus.putItemStack(stack))
		{
			int prevSelectedSpecibus = cap.getSelectedSpecibusIndex();
			selSpecibus.unassign(cap.getSelectedWeaponIndex());
			cap.setSelectedWeaponIndex(selSpecibus.getContents().indexOf(stack));

			syncPortfolioAndIndexes(entity, prevSelectedSpecibus, cap.getSpecibusIndex(selSpecibus));
			return selSpecibus;
		}

		for(StrifeSpecibus specibus : cap.getPortfolio())
		{
			if(specibus != null && specibus != selSpecibus && specibus.putItemStack(stack))
			{
				int prevSelectedSpecibus = cap.getSelectedSpecibusIndex();
				selSpecibus.unassign(cap.getSelectedWeaponIndex());
				cap.setSelectedSpecibusIndex(cap.getSpecibusIndex(specibus));
				cap.setSelectedWeaponIndex(specibus.getContents().indexOf(stack));

				syncPortfolioAndIndexes(entity, prevSelectedSpecibus, cap.getSpecibusIndex(specibus));
				return specibus;
			}
		}
		return null;
	}

	public static boolean addWeapon(LivingEntity entity, ItemStack stack)
	{
		return addWeapon(entity, stack, true);
	}

	public static boolean addWeapon(LivingEntity entity, ItemStack stack, boolean sendStatusMessage)
	{
		if(entity.level().isClientSide())
			return false;

		StrifeData cap = portfolioOf(entity);
		StrifeSpecibus selSpecibus = cap.getSelectedSpecibusIndex() >= 0 ? cap.getPortfolio()[cap.getSelectedSpecibusIndex()] : null;

		if(selSpecibus != null && selSpecibus.putItemStack(stack))
		{
			if(entity instanceof Player player)
			{
				if(sendStatusMessage)
					player.displayClientMessage(Component.translatable("status.strife.assignWeapon", stack.getHoverName(), specibusDisplayName(selSpecibus)), true);
				syncPortfolio(player, cap.getSpecibusIndex(selSpecibus));
			}
			return true;
		}

		for(StrifeSpecibus specibus : cap.getPortfolio())
		{
			if(specibus != null && specibus != selSpecibus && specibus.putItemStack(stack))
			{
				if(entity instanceof Player player)
				{
					if(sendStatusMessage)
						player.displayClientMessage(Component.translatable("status.strife.assignWeapon", stack.getHoverName(), specibusDisplayName(specibus)), true);
					syncPortfolio(player, cap.getSpecibusIndex(specibus));
				}
				return true;
			}
		}

		if(entity instanceof Player player && sendStatusMessage)
			player.displayClientMessage(Component.translatable("status.strife.weaponMissmach", stack.getHoverName()), true);
		return false;
	}

	public static boolean addSpecibus(LivingEntity entity, StrifeSpecibus specibus)
	{
		if(entity.level().isClientSide())
			return false;

		if(specibus == null)
			specibus = StrifeSpecibus.empty();

		StrifeData cap = portfolioOf(entity);

		if(cap.isPortfolioFull())
		{
			if(entity instanceof Player player)
				player.displayClientMessage(Component.translatable("status.strife.portfolioFull"), true);
			return false;
		}
		if(specibus.isAssigned() && cap.portfolioHasAbstratus(specibus.getKindAbstratus()))
		{
			if(entity instanceof Player player)
				player.displayClientMessage(Component.translatable("status.strife.portfolioDuplicate", specibus.getKindAbstratus().getDisplayName()), true);
			return false;
		}

		cap.addSpecibus(specibus);

		if(entity instanceof Player player)
		{
			if(specibus.isAssigned())
				player.displayClientMessage(Component.translatable("status.strife.assign", specibus.getKindAbstratus().getDisplayName()), true);
			syncPortfolio(player);
		}
		return true;
	}

	/**
	 * Assigns whatever's in the given hand: a strife card with an assigned kind gets consumed and its
	 * specibus added to the portfolio; anything else (including a blank/unassigned card, which is a
	 * no-op here same as the original) is added as a weapon to a compatible specibus.
	 */
	public static void assignStrife(Player player, InteractionHand hand)
	{
		if(player.level().isClientSide())
			return;

		ItemStack stack = player.getItemInHand(hand);

		if(stack.getItem() instanceof StrifeCardItem)
		{
			StrifeSpecibusData data = stack.get(MSUItemComponents.STRIFE_SPECIBUS);
			if(data != null && data.isAssigned() && addSpecibus(player, data.toSpecibus()))
			{
				stack.shrink(1);
			}
			return;
		}

		if(addWeapon(player, stack))
		{
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		}
	}

	/**
	 * Removes the specibus at {@code index} and gives the entity a strife card holding it (dropping it
	 * at their feet if their inventory is full, or if it's a non-player entity).
	 */
	public static void retrieveCard(LivingEntity entity, int index)
	{
		StrifeData cap = portfolioOf(entity);

		if(cap.isArmed() && cap.getSelectedSpecibusIndex() == index)
			for(InteractionHand hand : InteractionHand.values())
				if(isHeldWeapon(entity, entity.getItemInHand(hand)))
					entity.setItemInHand(hand, ItemStack.EMPTY);

		StrifeSpecibus removed = cap.removeSpecibus(index);
		ItemStack card = StrifeCardItem.createFromSpecibus(removed == null ? StrifeSpecibus.empty() : removed);

		if(!entity.level().isClientSide())
		{
			boolean added = entity instanceof Player player && player.getInventory().add(card);
			if(!added)
				entity.spawnAtLocation(card);
		}

		if(entity instanceof Player player)
		{
			syncIndexes(player);
			syncPortfolio(player, index);
		}
	}

	public static void retrieveWeapon(LivingEntity entity, int index, InteractionHand hand)
	{
		StrifeData cap = portfolioOf(entity);

		ItemStack stack = ItemStack.EMPTY;
		try
		{
			stack = cap.getPortfolio()[cap.getSelectedSpecibusIndex()].retrieveStack(cap.getSelectedWeaponIndex());
		}
		catch(RuntimeException ignored)
		{
		}

		ItemStack held = entity.getItemInHand(hand);
		if(held.isEmpty() || isHeldWeapon(entity, held))
		{
			if(isHeldWeapon(entity, held) && matchesSelectedWeapon(cap, held))
			{
				entity.setItemInHand(hand, ItemStack.EMPTY);
				cap.setArmed(false);
			}
			else if(!stack.isEmpty())
			{
				entity.setItemInHand(hand, stack);
				cap.setArmed(true);
			}
			if(entity instanceof Player player)
				syncIndexes(player);
		}
	}

	private static boolean matchesSelectedWeapon(StrifeData cap, ItemStack held)
	{
		StrifeSpecibus[] portfolio = cap.getPortfolio();
		int specibusIdx = cap.getSelectedSpecibusIndex();
		int weaponIdx = cap.getSelectedWeaponIndex();
		return portfolio.length > 0 && specibusIdx >= 0 && weaponIdx >= 0
				&& portfolio[specibusIdx] != null && !portfolio[specibusIdx].getContents().isEmpty()
				&& weaponIdx < portfolio[specibusIdx].getContents().size()
				&& StrifeSpecibus.sameWeapon(portfolio[specibusIdx].getContents().get(weaponIdx), held);
	}

	public static void swapOffhandWeapon(LivingEntity entity, int specibusIndex, int weaponIndex)
	{
		StrifeData cap = portfolioOf(entity);
		ItemStack stack = ItemStack.EMPTY;

		try
		{
			stack = cap.getPortfolio()[specibusIndex].retrieveStack(weaponIndex);
		}
		catch(RuntimeException ignored)
		{
		}

		if(!stack.isEmpty())
		{
			if(cap.isArmed() && cap.getSelectedSpecibusIndex() == specibusIndex && cap.getSelectedWeaponIndex() == weaponIndex)
			{
				cap.setArmed(false);
				InteractionHand handToClear = isHeldWeapon(entity, entity.getOffhandItem()) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
				entity.setItemInHand(handToClear, ItemStack.EMPTY);
			}
			StrifeSpecibus selSpecibus = cap.getPortfolio()[specibusIndex];
			selSpecibus.unassign(weaponIndex);
			if(weaponIndex >= selSpecibus.getContents().size())
				cap.setSelectedWeaponIndex(0);
		}

		ItemStack offhand = entity.getOffhandItem();
		if(offhand.isEmpty() || addWeapon(entity, offhand))
		{
			entity.setItemInHand(InteractionHand.OFF_HAND, stack);
		}
		else
		{
			entity.spawnAtLocation(stack);
		}

		if(entity instanceof Player player)
			syncIndexes(player);
	}

	public static void unassignSelected(LivingEntity entity)
	{
		if(entity.level().isClientSide())
			return;

		StrifeData cap = portfolioOf(entity);
		int sel = cap.getSelectedSpecibusIndex();
		if(sel < 0 || sel >= cap.getPortfolio().length || cap.getPortfolio()[sel] == null)
			return;

		StrifeSpecibus selSpecibus = cap.getPortfolio()[sel];
		selSpecibus.unassign(cap.getSelectedWeaponIndex());

		if(cap.getSelectedWeaponIndex() >= selSpecibus.getContents().size())
			cap.setSelectedWeaponIndex(0);
		cap.setArmed(false);

		if(entity instanceof Player player)
		{
			syncIndexes(player);
			syncPortfolio(player, sel);
		}
	}

	private static String specibusDisplayName(StrifeSpecibus specibus)
	{
		return specibus.getDisplayName();
	}

	// --- networking helpers -------------------------------------------------------------------

	private static void syncPortfolio(Player player, int... specibusIndexes)
	{
		MSUStrifePackets.sendPortfolioSync(player, specibusIndexes);
	}

	private static void syncIndexes(Player player)
	{
		MSUStrifePackets.sendIndexesSync(player);
	}

	private static void syncPortfolioAndIndexes(LivingEntity entity, int... specibusIndexes)
	{
		if(entity instanceof Player player)
		{
			syncPortfolio(player, specibusIndexes);
			syncIndexes(player);
		}
	}
}
