package org.wilkretawesomesauce.minestuckuniverseported.godtier;

import com.mraof.minestuck.player.Echeladder;
import com.mraof.minestuck.player.Title;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Config;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.MSUItemComponents;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

import java.util.Optional;

/**
 * Ported (in simplified form) from MinestuckUniverse (1.12.2)'s ascension trigger, which was normally
 * reached via {@code GuiGodTierMeditation} after standing on the right terrain (an {@code IGodTierBlock})
 * in the player's own Land, matching their Title's aspect.
 * <p>
 * <b>Simplification:</b> the Land/terrain requirement isn't ported (it needs deeper integration with
 * Minestuck's Sburb connection/dimension system than this pass covers). Instead, ascension triggers
 * simply by equipping a blank (unattuned) God Tier armor piece while eligible: has a Title assigned,
 * meets {@link Config#requiredRungToGT}, and {@link GodTierData#canGodTier()} hasn't been revoked. This
 * is a real, working, testable trigger, but it's a meaningfully easier bar to clear than the original's -
 * treat this as a placeholder for the real ritual rather than a deliberate design decision.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class GodTierEvents
{
	private GodTierEvents()
	{
	}

	@SubscribeEvent
	private static void onEquipmentChange(LivingEquipmentChangeEvent event)
	{
		if(!(event.getEntity() instanceof ServerPlayer player))
			return;

		ItemStack stack = event.getTo();
		if(!isGodTierArmor(stack))
			return;

		GodTierArmorData data = stack.getOrDefault(MSUItemComponents.GOD_TIER_TITLE, GodTierArmorData.BLANK);
		if(data.isAttuned())
			return;

		GodTierData godTier = player.getData(MSUAttachments.GOD_TIER);
		if(!godTier.canGodTier())
			return;

		Optional<Title> title = Title.getTitle(player);
		if(title.isEmpty())
		{
			player.displayClientMessage(Component.translatable("status.minestuckuniverseported.god_tier.no_title"), true);
			return;
		}

		if(Config.requiredRungToGT >= 0 && Echeladder.get(player).getRung() < Config.requiredRungToGT)
		{
			player.displayClientMessage(Component.translatable("status.minestuckuniverseported.god_tier.rung_too_low", Config.requiredRungToGT), true);
			return;
		}

		stack.set(MSUItemComponents.GOD_TIER_TITLE, new GodTierArmorData(title));

		if(!godTier.isAscended())
		{
			godTier.setAscended(true);
			player.displayClientMessage(Component.translatable("status.minestuckuniverseported.god_tier.ascended", title.get().asTextComponent()), false);
		}
	}

	private static boolean isGodTierArmor(ItemStack stack)
	{
		return stack.has(MSUItemComponents.GOD_TIER_TITLE) || stack.getItem() instanceof org.wilkretawesomesauce.minestuckuniverseported.items.GodTierArmorItem;
	}
}
