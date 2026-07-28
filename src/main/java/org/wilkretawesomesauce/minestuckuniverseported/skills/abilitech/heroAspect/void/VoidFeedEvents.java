package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.voidAspect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.game.GameData;

/**
 * Always-on item-void feed, ported from {@code TechVoidGrasp}'s two static handlers - see that class's
 * own doc comment for why both are folded into a single {@link LevelTickEvent.Post} scan here instead of
 * the original's now-nonexistent {@code ItemExpireEvent}.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class VoidFeedEvents
{
	private VoidFeedEvents()
	{
	}

	@SubscribeEvent
	private static void onLevelTick(LevelTickEvent.Post event)
	{
		if(!(event.getLevel() instanceof ServerLevel level))
			return;

		GameData data = level.getServer().overworld().getData(MSUAttachments.ITEM_VOID);

		for(Entity entity : level.getAllEntities())
		{
			if(!(entity instanceof ItemEntity item) || item.getItem().isEmpty())
				continue;

			boolean expiring = item.lifespan > 0 && item.getAge() >= item.lifespan - 1;
			boolean fellOut = item.getY() <= level.getMinBuildHeight();

			if(expiring || fellOut)
			{
				data.addItem(item.getItem().copy());
				item.discard();
			}
		}
	}
}
