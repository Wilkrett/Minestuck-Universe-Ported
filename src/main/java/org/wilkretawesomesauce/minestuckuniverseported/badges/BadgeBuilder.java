package org.wilkretawesomesauce.minestuckuniverseported.badges;

import com.mraof.minestuck.alchemy.GristHelper;
import com.mraof.minestuck.api.alchemy.GristSet;
import com.mraof.minestuck.api.alchemy.GristTypes;
import com.mraof.minestuck.player.GristCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.MSUItems;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.badges.BadgeBuilder} - real cost: holding
 * {@code battlepickOfZillydew} (see {@code MSUItems#BATTLEPICK_OF_ZILLYDEW}/{@code MSUToolTiers}' own doc
 * comment for why that item's stats are approximate rather than exact 1.12.2 parity, a peripheral detail
 * next to this badge's own real feature) plus 20000 Build grist, using the same real modern
 * {@link GristCache}/{@link GristSet} API {@link BadgeKarma} already established for this project (not
 * the original's own dead {@code GristHelper}/{@code MinestuckPlayerData} calls, confirmed via
 * {@code javap} against this project's pinned Minestuck dependency).
 * <p>
 * Once active, lets its owner drag-place a whole cuboid of blocks at once outside of Minestuck's own Edit
 * Mode - see {@code client.BadgeBuilderClientEvents}/{@code network.BadgeBuilderFillPacket} for the real
 * drag-select-and-fill mechanic itself (this class is just the unlock gate, matching the original's own
 * split between {@code BadgeBuilder}'s {@code canUnlock} and its own static event handlers - the modern
 * port keeps that same split, just moved into dedicated classes rather than static methods on this one,
 * matching this project's own file-per-concern convention).
 */
public class BadgeBuilder extends BadgeLevel
{
	private static final long BUILD_GRIST_COST = 20000;

	public BadgeBuilder()
	{
		super(Minestuckuniverseported.id("builder_badge"), 7);
	}

	@Override
	public boolean canUnlock(Level level, Player player)
	{
		if(!(player instanceof ServerPlayer serverPlayer))
			return false;

		ItemStack pickCost = new ItemStack(MSUItems.BATTLEPICK_OF_ZILLYDEW.get());
		GristSet gristCost = GristSet.of(GristTypes.BUILD.get().amount(BUILD_GRIST_COST));
		GristCache gristCache = GristCache.get(serverPlayer);

		if(!findItem(player, pickCost, false) || !gristCache.canAfford(gristCost))
			return false;

		findItem(player, pickCost, true);
		gristCache.tryTake(gristCost, GristHelper.EnumSource.SERVER);
		return true;
	}
}
