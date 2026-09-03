package org.wilkretawesomesauce.minestuckuniverseported.strife;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.strife.StrifeData;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Config;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUFakePlayer;

/**
 * Ported from the "restricted strife" half of MinestuckUniverse (1.12.2)'s {@code StrifeEventHandler}
 * ({@code onPlayerAttack}/{@code onItemInteract}). Only fires when both {@link Config#combatOverhaul} and
 * {@link Config#restrictedStrife} are enabled, matching the original's gating.
 * <p>
 * Simplifications from the original:
 * <ul>
 *     <li>Dropped the {@code WeaponAssignedEvent} indirection that let other mods override the
 *     allow/deny result - nothing in this project posts to it, so it was pure unused ceremony here.</li>
 *     <li>Dropped the {@code OperandiModus}/{@code USABLE_ASSIGNED_ONLY} special cases, both tied to
 *     items/systems that aren't part of this port.</li>
 *     <li>Uses {@link StrifePortfolioHandler#isHeldWeapon} instead of the old NBT-flag
 *     {@code isStackAssigned} check - see the note on {@link StrifeSpecibus} for why.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class StrifeRestrictionEvents
{
	private StrifeRestrictionEvents()
	{
	}

	private static boolean active()
	{
		return Config.combatOverhaul && Config.restrictedStrife;
	}

	/** Prevents attacking without an allocated main-hand weapon (or a fist-kind specibus selected). */
	@SubscribeEvent
	private static void onAttack(AttackEntityEvent event)
	{
		if(!active())
			return;
		// FakePlayer is NeoForge's own fake-player base class - MSUFakePlayer (used by TechTimeZeitgeist
		// and DoomedTimelineClone) extends ServerPlayer directly instead, so it wasn't caught by that check.
		// Without this, a clone's attacks were silently cancelled here: it never goes through
		// StrifePortfolioHandler to "arm" a weapon, so isHeldWeapon() below always failed for it. Same
		// reasoning as the FakePlayer exemption, just a different fake-player type - neither is a human
		// managing a portfolio through the strife UI.
		if(!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer || player instanceof MSUFakePlayer)
			return;

		ItemStack mainHand = player.getMainHandItem();

		if(mainHand.isEmpty())
		{
			StrifeData cap = player.getData(MSUAttachments.STRIFE_PORTFOLIO);
			int sel = cap.getSelectedSpecibusIndex();
			if(sel >= 0 && sel < cap.getPortfolio().length)
			{
				StrifeSpecibus selected = cap.getPortfolio()[sel];
				if(selected != null && selected.isAssigned() && selected.getKindAbstratus().isFist())
					return;
			}
			event.setCanceled(true);
			return;
		}

		if(!StrifePortfolioHandler.isHeldWeapon(player, mainHand))
			event.setCanceled(true);
	}

	/** Prevents right-clicking with items whose kind blocks it (e.g. bows), unless allocated or bypassed. */
	@SubscribeEvent
	private static void onRightClickItem(PlayerInteractEvent.RightClickItem event)
	{
		if(!active())
			return;
		if(!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer)
			return;

		ItemStack stack = event.getItemStack();
		if(stack.isEmpty())
			return;

		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if(itemId != null && Config.restrictedStrifeBypass.contains(itemId.toString()))
			return;

		if(StrifePortfolioHandler.isHeldWeapon(player, stack))
			return;

		for(KindAbstratus kind : MSUKindAbstrataRegistry.getAll())
		{
			if(!kind.isEmpty() && kind.preventsRightClick() && kind.isStackCompatible(stack))
			{
				event.setCanceled(true);
				event.setCancellationResult(InteractionResult.PASS);
				return;
			}
		}
	}
}
