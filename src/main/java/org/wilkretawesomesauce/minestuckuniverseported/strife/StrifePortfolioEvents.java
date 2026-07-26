package org.wilkretawesomesauce.minestuckuniverseported.strife;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.strife.StrifeData;

import com.mraof.minestuck.player.Echeladder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Config;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.item.StrifeCardItem;
import org.wilkretawesomesauce.minestuckuniverseported.network.MSUStrifePackets;

/**
 * Syncs a player's strife portfolio to their client on login and respawn, and reconciles the "armed"
 * weapon against what's actually in the player's hands every tick.
 * <p>
 * The login/respawn sync was missing entirely, which was the cause of the "portfolio shows empty after
 * reloading the world, but starts working again as soon as you add another card" bug: data attachments
 * aren't automatically network-synced in NeoForge (see the note on {@link StrifeData}), so without
 * an explicit sync the client's copy of the attachment just stays at its freshly-constructed empty
 * default from the moment the player object is created, until some other mutation (like adding a card)
 * happens to trigger {@code StrifePortfolioHandler}'s own sync calls. The server-side data was never
 * actually lost - the client just never asked for/received a copy of it after (re)joining.
 * <p>
 * The per-tick reconciliation ({@link #checkArmed}) fixes a duplication bug: {@link StrifeSpecibus#retrieveStack}
 * only <i>copies</i> the weapon into the player's hand, it never removes it from the specibus' contents
 * (matching the 1.12.2 original - see the note there). In the original, that copy was only ever finalized
 * as a real removal once the item actually left the player's hands, via a per-tick check
 * ({@code StrifeEventHandler#checkArmed}) that this ports a simplified version of: if the tracked "armed"
 * weapon is no longer being held in either hand (moved to inventory, dropped, etc.), it's removed from
 * the specibus for real. Without this, retrieving a weapon and then moving/dropping it left the original
 * copy sitting untouched in the specibus - i.e. exactly the reported duplication bug.
 * <p>
 * Not ported from the original's much larger {@code checkArmed}: the "combat overhaul"/"restricted
 * strife" config-gated behaviour, and the bit that auto-absorbs whatever item you swap into the same
 * hotbar slot into the current specibus slot. Both depend on systems (config, item restrictions) that
 * aren't part of this pass.
 * <p>
 * Mirrors the exact login/respawn sync pattern Minestuck itself uses (see {@code player.Echeladder}'s
 * {@code onPlayerLoggedIn}/{@code onPlayerRespawn}).
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class StrifePortfolioEvents
{
	private StrifePortfolioEvents()
	{
	}

	@SubscribeEvent
	private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
	{
		if(event.getEntity() instanceof ServerPlayer player)
			sync(player);
	}

	@SubscribeEvent
	private static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event)
	{
		if(event.getEntity() instanceof ServerPlayer player)
			sync(player);
	}

	/**
	 * {@link MSUAttachments#STRIFE_PORTFOLIO} has {@code copyOnDeath()}, so by the time this fires the
	 * new player entity already has a full copy of the portfolio - our job here is only the deliberate
	 * "drop it as cards on death unless configured to keep it" gameplay rule, not preventing data loss
	 * (that's what copyOnDeath already does).
	 */
	@SubscribeEvent
	private static void onPlayerClone(PlayerEvent.Clone event)
	{
		if(!event.isWasDeath() || Config.keepPortfolioOnDeath)
			return;
		if(!(event.getEntity() instanceof ServerPlayer player))
			return;

		StrifeData cap = player.getData(MSUAttachments.STRIFE_PORTFOLIO);
		for(StrifeSpecibus specibus : cap.getPortfolio())
		{
			if(specibus != null && specibus.isAssigned())
			{
				ItemStack card = StrifeCardItem.createFromSpecibus(specibus);
				if(!player.getInventory().add(card))
					player.drop(card, true);
			}
		}

		cap.clearPortfolio();
		cap.setSelectedSpecibusIndex(-1);
		cap.setArmed(false);
		sync(player);
	}

	@SubscribeEvent
	private static void onPlayerTick(PlayerTickEvent.Post event)
	{
		if(event.getEntity().level().isClientSide())
			return;
		if(event.getEntity() instanceof ServerPlayer player)
		{
			checkArmed(player);
			checkAbstrataSwitcherUnlock(player);
		}
	}

	private static void checkAbstrataSwitcherUnlock(ServerPlayer player)
	{
		StrifeData cap = player.getData(MSUAttachments.STRIFE_PORTFOLIO);
		boolean shouldUnlock = Config.abstrataSwitcherRung < 0 || Echeladder.get(player).getRung() >= Config.abstrataSwitcherRung;

		if(cap.abstrataSwitcherUnlocked() != shouldUnlock)
		{
			cap.unlockAbstrataSwitcher(shouldUnlock);
			MSUStrifePackets.sendIndexesSync(player);
		}
	}

	private static void sync(ServerPlayer player)
	{
		MSUStrifePackets.sendPortfolioSync(player);
		MSUStrifePackets.sendIndexesSync(player);
	}

	private static void checkArmed(ServerPlayer player)
	{
		StrifeData cap = player.getData(MSUAttachments.STRIFE_PORTFOLIO);
		if(!cap.isArmed())
			return;

		ItemStack weapon;
		try
		{
			weapon = cap.getPortfolio()[cap.getSelectedSpecibusIndex()].getContents().get(cap.getSelectedWeaponIndex());
		}
		catch(RuntimeException e)
		{
			// selection no longer points at a real weapon slot (specibus/weapon removed from under it, etc.)
			cap.setArmed(false);
			MSUStrifePackets.sendIndexesSync(player);
			return;
		}

		boolean stillHeld = StrifeSpecibus.sameWeapon(player.getItemInHand(InteractionHand.MAIN_HAND), weapon)
				|| StrifeSpecibus.sameWeapon(player.getItemInHand(InteractionHand.OFF_HAND), weapon);

		if(!stillHeld)
		{
			// the tracked weapon left the player's hands (inventory move, drop, hotbar swap, etc.) - the
			// copy still sitting in the specibus is now a duplicate of whatever the item became, so
			// finalize the removal here instead of leaving a phantom copy behind.
			StrifePortfolioHandler.unassignSelected(player);
		}
	}
}

