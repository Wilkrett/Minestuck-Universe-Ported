package org.wilkretawesomesauce.minestuckuniverseported.mechanics.doom;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.freedom.FreedomData;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;

/**
 * The one piece of the "Minestuck Systems Overview" design doc's Freedom/Doom four-quadrant matrix that
 * needed real code - see {@code mechanics.freedom.FreedomRelationshipEvents}' own doc comment for why the
 * other three quadrants are left alone (they already emerge from {@code FreedomEvents}/
 * {@code DoomDamageEvents} running side by side). Original design for this project, no 1.12.2 counterpart.
 * <p>
 * "Low Freedom + High Doom: Trapped by circumstances. Events feel inevitable." - a low-Freedom entity has
 * no behavioral slack to dodge/flee/reroute away from what Doom already makes more dangerous
 * ({@code DoomDamageEvents}' own {@link net.neoforged.bus.api.EventPriority#HIGH} amplification), so this
 * compounds a second, independent multiplier on top scaled by how little Freedom the entity has left -
 * zero effect at neutral (50) or above, up to {@link #LOW_FREEDOM_DOOM_AMPLIFY_MAX} extra at 0. Gated on
 * {@code doom > 0} - Freedom being low with no Doom bound at all isn't "inevitable" anything, there's
 * nothing to feel trapped by.
 * <p>
 * Deliberately a separate class/listener rather than editing {@code DoomDamageEvents} directly (same
 * separation-of-concerns precedent {@code RelationshipDoomEvents} already set for Doom-generation sources) -
 * runs at default ({@code NORMAL}) priority, after {@code DoomDamageEvents}' own {@code HIGH}-priority
 * amplification and before its {@code LOW}-priority severity accrual, so this compounds on the
 * already-Doom-amplified damage and that compounded total is what severity accrual (and the entity's
 * actual health) both see - a deliberate, bounded feedback loop, bounded because every curve involved
 * (Doom's own amplify curve, and this one) saturates.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class FreedomDoomEvents
{
	private static final float LOW_FREEDOM_DOOM_AMPLIFY_MAX = 0.5F;

	private FreedomDoomEvents()
	{
	}

	@SubscribeEvent
	private static void onIncomingDamage(LivingIncomingDamageEvent event)
	{
		LivingEntity entity = event.getEntity();
		if(entity.level().isClientSide() || event.isCanceled())
			return;

		if(entity.getData(MSUAttachments.DOOM_DATA).getDoom() <= 0)
			return;

		float freedom = entity.getData(MSUAttachments.FREEDOM_DATA).getFreedom();
		if(freedom >= FreedomData.DEFAULT)
			return;

		float multiplier = 1.0F + LOW_FREEDOM_DOOM_AMPLIFY_MAX * (1.0F - freedom / FreedomData.DEFAULT);
		event.setAmount(event.getAmount() * multiplier);
	}
}
