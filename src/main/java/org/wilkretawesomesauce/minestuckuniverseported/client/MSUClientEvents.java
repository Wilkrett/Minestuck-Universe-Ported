package org.wilkretawesomesauce.minestuckuniverseported.client;

import com.mraof.minestuck.client.gui.playerStats.StrifeSpecibusScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.client.gui.MSUStrifePortfolioScreen;

/**
 * Replaces Minestuck's placeholder {@code StrifeSpecibusScreen} (which just renders the string
 * "This feature isn't implemented yet.") with the ported {@link MSUStrifePortfolioScreen} whenever it's
 * about to open.
 * <p>
 * Uses the same {@code ScreenEvent.Opening} + cancel approach Minestuck's own
 * {@code ClientEditHandler.onScreenOpened} uses to swap screens, rather than trying to reassign the
 * {@code final} factory field on {@code PlayerStatsScreen.NormalGuiType.STRIFE_SPECIBUS} (which would
 * need reflection and is far more fragile).
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class MSUClientEvents
{
	private MSUClientEvents()
	{
	}

	@SubscribeEvent
	public static void onScreenOpening(ScreenEvent.Opening event)
	{
		if(event.getScreen() instanceof StrifeSpecibusScreen)
		{
			event.setCanceled(true);
			Minecraft.getInstance().setScreen(new MSUStrifePortfolioScreen());
		}
	}
}
