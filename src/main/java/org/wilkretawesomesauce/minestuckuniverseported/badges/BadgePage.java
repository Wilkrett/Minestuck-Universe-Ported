package org.wilkretawesomesauce.minestuckuniverseported.badges;

import com.mraof.minestuck.player.EnumClass;
import com.mraof.minestuck.player.Title;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.badges.BadgePage} - only ever appears for a Page
 * player, real cost: 80 XP levels (spent via {@link Player#giveExperienceLevels(int)}, the real modern
 * equivalent of the original's direct {@code experienceLevel} field mutation). Consumed by
 * {@code heroClass.MSUAspectAmbientEffects}'s own real {@code +2} potion-level bonus.
 */
public class BadgePage extends Badge
{
	private static final int REQUIRED_XP_LEVELS = 80;

	public BadgePage()
	{
		super(Minestuckuniverseported.id("page_potential"));
	}

	@Override
	public boolean canUse(Level level, Player player)
	{
		return !(player.hasEffect(MSUMobEffects.GOD_TIER_LOCK) && player.getEffect(MSUMobEffects.GOD_TIER_LOCK).getAmplifier() >= 1);
	}

	@Override
	public boolean canAppearOnList(Level level, Player player)
	{
		if(!super.canAppearOnList(level, player))
			return false;

		return player instanceof ServerPlayer serverPlayer && Title.getTitle(serverPlayer).map(t -> t.heroClass() == EnumClass.PAGE).orElse(false);
	}

	@Override
	public boolean canUnlock(Level level, Player player)
	{
		if(player.experienceLevel < REQUIRED_XP_LEVELS)
			return false;

		player.giveExperienceLevels(-REQUIRED_XP_LEVELS);
		return true;
	}
}
