package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code TechTimeShift}. Hold to speed up world time, sneak
 * while holding to rewind it instead. Costs 1 food per 30 ticks.
 * <p>
 * Not ported: the boondollar unlock cost (dropped project-wide per the sandbox-mode scope decision).
 */
public class TechTimeShift extends TechHeroAspect
{
	public TechTimeShift()
	{
		super(Minestuckuniverseported.id("celestial_shift"), EnumAspect.TIME, 150, MSUTechType.UTILITY);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 1)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(!player.isCreative() && time % 30 == 0)
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);

		if(level instanceof ServerLevel serverLevel)
			serverLevel.setDayTime(serverLevel.getDayTime() + (player.isShiftKeyDown() ? -20 : 40));

		MSUAbilitechParticles.aura(level, player, EnumAspect.TIME, 6);

		return true;
	}
}
