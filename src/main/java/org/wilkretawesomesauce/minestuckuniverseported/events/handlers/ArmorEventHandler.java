package org.wilkretawesomesauce.minestuckuniverseported.events.handlers;

import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.wilkretawesomesauce.minestuckuniverseported.MSUItems;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code events.handlers.ArmorEventHandler} - real package/class
 * match, not a guess (confirmed via the real extracted 1.12.2 source). Only the wizard hat's real magic
 * damage resistance is ported here; the original's sibling checks (spiked helmet thorns retaliation,
 * archmage hat) aren't, since neither {@code spikedHelmet} nor {@code archmageHat} is a registered item in
 * this project yet - real future work, not attempted this pass. This one handler used to live folded into
 * {@code capabilities.consortCosmetics.ConsortHatEvents} (a Consort-cosmetics-specific class it had no real
 * connection to - the original never put it there either) before being moved to its own real, correctly-
 * named home here.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class ArmorEventHandler
{
	private ArmorEventHandler()
	{
	}

	@SubscribeEvent
	private static void onWizardHatMagicResist(LivingIncomingDamageEvent event)
	{
		ItemStack head = event.getEntity().getItemBySlot(EquipmentSlot.HEAD);
		if(head.is(MSUItems.WIZARD_HAT) && event.getSource().is(DamageTypes.MAGIC))
		{
			event.setAmount(event.getAmount() * 0.5F);
			head.hurtAndBreak(1, event.getEntity(), EquipmentSlot.HEAD);
		}
	}
}
