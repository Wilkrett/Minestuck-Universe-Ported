package org.wilkretawesomesauce.minestuckuniverseported.util;

import com.mraof.minestuck.entity.consort.ConsortEntity;
import com.mraof.minestuck.entity.consort.EnumConsort;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.wilkretawesomesauce.minestuckuniverseported.MSUItems;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.consortCosmetics.ConsortHatsData;

/**
 * Real port of MinestuckUniverse (1.12.2)'s {@code util.MSUConsorts#onConsortSpawn} - a ~6.5% chance for a
 * freshly-spawned GENERAL-merchant Consort to become a dedicated abilitech (skill shop) seller, guaranteed
 * to spawn wearing the archmage hat as the visual "I sell abilitechs" tell.
 * <p>
 * <b>Real adaptation, not a guess</b>: the original reassigned the Consort to a brand new
 * {@code EnumConsort.MerchantType} ({@code SHOP_SKILLS}) added at runtime via Forge's 1.12.2-only
 * {@code EnumHelper.addEnum} trick - no such hack exists in modern Java/NeoForge (enums can't be extended
 * at runtime), and this project's own dialogue system is fully data-driven anyway (see
 * {@code data/minestuckuniverseported/minestuck/selectable_dialogue/consort_general_merchant/skill_shop_offer.json}),
 * so a new merchant type isn't needed to reproduce the actual player-facing behavior. Instead, the rolled
 * Consort keeps its real {@link EnumConsort.MerchantType#GENERAL} type and gets tagged with a real
 * {@code minestuckuniverseported:skill_shop_seller} entity-level dialogue flag (Minestuck's own real
 * {@code Condition.Flag}/{@code DialogueComponent#flags()} mechanism, confirmed via {@code javap} against
 * the dependency jar - not sealed, unlike {@code Trigger}) - the skill shop dialogue response's own
 * {@code condition} now requires that flag instead of always offering itself to every General merchant.
 * <p>
 * Runs at {@link EventPriority#HIGH}, ahead of {@link ConsortHatsData}'s own default-priority random
 * hat roll on the same {@link EntityJoinLevelEvent} - that class's own roll now skips any Consort that
 * already has a head stack by the time it runs, so a Consort rolled here always keeps the archmage hat
 * rather than having it immediately overwritten by the unrelated random pool.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class MSUConsorts
{
	/** Matches the original's literal {@code 0.065f}. */
	private static final float SKILL_SELLER_CHANCE = 0.065F;

	public static final String SKILL_SHOP_SELLER_FLAG = "skill_shop_seller";

	private MSUConsorts()
	{
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	private static void onEntityJoinLevel(EntityJoinLevelEvent event)
	{
		// loadedFromDisk() excludes this from re-rolling on every chunk reload, matching the original's own
		// once-per-entity-ever roll (there gated by a "TechShopPass" NBT flag, here by the same real
		// join-vs-load distinction ConsortHatsData already established for its own spawn-time roll).
		if(event.getLevel().isClientSide() || event.loadedFromDisk() || !(event.getEntity() instanceof ConsortEntity consort))
			return;

		if(consort.merchantType != EnumConsort.MerchantType.GENERAL)
			return;

		if(consort.getRandom().nextFloat() >= SKILL_SELLER_CHANCE)
			return;

		consort.getDialogueComponent().flags().add(SKILL_SHOP_SELLER_FLAG);
		ConsortHatsData.equip(consort, new ItemStack(MSUItems.ARCHMAGE_HAT.get()));
	}
}
