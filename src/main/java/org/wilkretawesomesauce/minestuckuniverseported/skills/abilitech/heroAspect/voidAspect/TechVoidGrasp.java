package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.voidAspect;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.game.GameData;
import org.wilkretawesomesauce.minestuckuniverseported.gui.itemvoid.ItemVoidMenu;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.voidAspect.TechVoidGrasp}
 * ("Grasp of the Void") - press to open the real Item Void GUI ({@code itemvoid.ItemVoidMenu}, built in
 * an earlier pass and previously only reachable via the {@code /msuitemvoid} debug command - this is
 * that GUI's actual real player-facing trigger, matching the original). Also carries the original's two
 * always-on static handlers that feed items into the void automatically (see {@link FeedEvents}): one for
 * items that despawn on their own, one for items that fall out of the world.
 */
public class TechVoidGrasp extends TechHeroAspect
{
	public TechVoidGrasp()
	{
		super(Minestuckuniverseported.id("grasp_of_the_void"), EnumAspect.VOID, 3700, MSUTechType.UTILITY);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.PRESS)
			return false;
		if(!(player instanceof ServerPlayer serverPlayer))
			return false;

		GameData data = serverPlayer.server.overworld().getData(MSUAttachments.ITEM_VOID);
		serverPlayer.openMenu(new SimpleMenuProvider(
				(containerId, inventory, p) -> new ItemVoidMenu(containerId, inventory, data),
				Component.translatable("gui.minestuckuniverseported.itemVoid.title")));

		MSUAbilitechParticles.aura(level, player, EnumAspect.VOID, 20);

		return true;
	}

	/**
	 * Always-on item-void feed, ported from this tech's own two original static handlers. The original
	 * hooked Forge 1.12.2's {@code ItemExpireEvent} (no longer present in modern NeoForge - confirmed
	 * absent from this project's pinned dependency jar) plus a hand-rolled {@code WorldTickEvent} scan for
	 * below-build-limit items. Both are folded into one {@link LevelTickEvent.Post} scan here: catch an
	 * about-to-despawn {@link ItemEntity} the tick before vanilla would silently remove it
	 * ({@code getAge() >= lifespan - 1}), or one that's fallen below the level's real build limit
	 * ({@code Level#getMinBuildHeight()}, replacing the original's hardcoded {@code -64}) - both funnel the
	 * stack into {@link GameData} and discard the entity instead of losing it.
	 */
	@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
	public static final class FeedEvents
	{
		private FeedEvents()
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
}
