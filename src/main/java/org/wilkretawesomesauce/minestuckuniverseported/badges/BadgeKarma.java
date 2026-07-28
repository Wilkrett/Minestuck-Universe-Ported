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
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.MSUSkills#KARMA} - real cost: 128
 * {@code minestuckuniverseported:moonstone} + 8000 Gold grist, using the real modern
 * {@link GristCache}/{@link GristSet} API (confirmed via {@code javap} against this project's pinned
 * Minestuck dependency, not guessed - the original's own {@code GristHelper}/{@code GristSet}/
 * {@code MinestuckPlayerData} calls don't exist in this shape anymore). Consumed by
 * {@code heroClass.mage.TechMage}/{@code heroClass.seer.TechSeer}'s own real Karma-reveal threshold.
 */
public class BadgeKarma extends BadgeLevel
{
	private static final long GOLD_COST = 8000;
	private static final int MOONSTONE_COST = 128;

	public BadgeKarma()
	{
		super(Minestuckuniverseported.id("karma"), 5);
	}

	@Override
	public boolean canUnlock(Level level, Player player)
	{
		if(!(player instanceof ServerPlayer serverPlayer))
			return false;

		ItemStack moonstoneCost = new ItemStack(MSUItems.MOONSTONE.get(), MOONSTONE_COST);
		GristSet gristCost = GristSet.of(GristTypes.GOLD.get().amount(GOLD_COST));
		GristCache gristCache = GristCache.get(serverPlayer);

		if(!findItem(player, moonstoneCost, false) || !gristCache.canAfford(gristCost))
			return false;

		findItem(player, moonstoneCost, true);
		gristCache.tryTake(gristCost, GristHelper.EnumSource.SERVER);
		return true;
	}
}
