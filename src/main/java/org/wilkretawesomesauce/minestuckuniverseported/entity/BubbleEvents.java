package org.wilkretawesomesauce.minestuckuniverseported.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code entity.EntityBubble#onAttack} - a {@link BubbleEntity}
 * with {@code !canEnter()} also blocks attacks from reaching across its boundary: a hit only lands if the
 * attacker and the target are enclosed by the exact same set of "can't enter" bubbles (both outside all
 * of them, or both inside the same one(s)) - never one inside and one outside.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class BubbleEvents
{
	private BubbleEvents()
	{
	}

	@SubscribeEvent
	private static void onIncomingDamage(LivingIncomingDamageEvent event)
	{
		LivingEntity target = event.getEntity();
		if(target.level().isClientSide())
			return;

		Entity source = event.getSource().getDirectEntity() != null ? event.getSource().getDirectEntity() : event.getSource().getEntity();
		if(source == null)
			return;

		if(!enclosingBubbles(source).equals(enclosingBubbles(target)))
			event.setCanceled(true);
	}

	private static Set<UUID> enclosingBubbles(Entity entity)
	{
		Set<UUID> ids = new HashSet<>();
		for(BubbleEntity bubble : entity.level().getEntitiesOfClass(BubbleEntity.class, entity.getBoundingBox().inflate(0.1), b -> !b.canEnter()))
			ids.add(bubble.getUUID());
		return ids;
	}
}
