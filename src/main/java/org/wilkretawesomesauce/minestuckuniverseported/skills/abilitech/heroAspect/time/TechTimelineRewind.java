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
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.TimelineManager;

/**
 * The real "Rewind" ability from the Time Aspect timeline-management design discussion: hold to charge,
 * release to actually undo the last few seconds of recorded world history (blocks and nearby entities -
 * see {@code mechanics.timeline.TimelineRecorder} for exactly what's tracked and its limits) via
 * {@link TimelineManager#rewind}, not just your own position like the smaller {@code TechTimeRecall}.
 * <p>
 * Charging longer rewinds further: roughly 1 second of charge per second of rewind, capped at 10 seconds
 * (200 ticks). Costs 3 food per second charged. Every rewind adds Doom Points regardless of size - see
 * {@code mechanics.timeline.TimelineData#getDoomPoints} (currently just a tracked placeholder with no consequences
 * attached yet).
 */
public class TechTimelineRewind extends TechHeroAspect
{
	private static final int MAX_CHARGE_TICKS = 200;
	private static final int ENERGY_USE_PER_SECOND = 3;

	public TechTimelineRewind()
	{
		super(Minestuckuniverseported.id("timeline_rewind"), EnumAspect.TIME, 60000, MSUTechType.UTILITY); // new tech, no original cost to port - picked to fit this project's own cost spread, see class doc comment
		setIcon("default");
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.HELD || state == AbilitechKeyState.PRESS)
		{
			if(time >= MAX_CHARGE_TICKS)
				return false;
			MSUAbilitechParticles.burst(level, player, EnumAspect.TIME, 6);
			return true;
		}

		if(state != AbilitechKeyState.RELEASED)
			return false;

		int chargedTicks = Math.min(time, MAX_CHARGE_TICKS);
		if(chargedTicks < 20)
			return false;

		int cost = (chargedTicks / 20) * ENERGY_USE_PER_SECOND;
		if(!player.isCreative() && player.getFoodData().getFoodLevel() < cost)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(!(level instanceof ServerLevel serverLevel) || !(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer))
			return false;

		int actualTicks = TimelineManager.rewind(serverLevel, serverPlayer, chargedTicks);
		if(actualTicks <= 0)
		{
			player.displayClientMessage(Component.translatable("status.minestuckuniverseported.timeline.no_history"), true);
			return false;
		}

		if(!player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - cost);

		player.displayClientMessage(Component.translatable("status.minestuckuniverseported.timeline.rewinding", actualTicks / 20F), true);
		MSUAbilitechParticles.oneshot(level, player, EnumAspect.TIME, 30);
		return true;
	}
}
