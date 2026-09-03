package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.loop.TimeLoopCaster;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.loop.TimeLoopZone;

/**
 * "Timeloop α" - real class name now matches its in-game display name (user-requested rename from
 * {@code TechTimeLoop}, sibling to {@link TechTimeLoopBeta} and {@link TechTimeLoopOmega}). Creates an
 * {@link TimeLoopZone.StackMode#INDEPENDENT} Time Loop zone - a radius-scoped ({@link TimeLoopZone#RADIUS}),
 * repeating rewind of the last {@code chargedTicks} of recorded history, looping for that same duration
 * (capped at {@link TimeLoopZone#MAX_DURATION_TICKS}). See {@code timeline.loop.TimeLoopZone}'s own doc
 * comment for exactly what does and doesn't get puppeted.
 * <p>
 * Hold-to-charge/release shape copied from {@link TechTimelineRewind} - charge time doubles as the loop's
 * duration, same "charge longer = bigger effect" idiom, minimum 1 second charge (20 ticks) to avoid an
 * accidental tap creating a near-instant loop.
 * <p>
 * Any number of {@code INDEPENDENT} zones can freely overlap in space with zero coordination between them
 * - if two overlapping zones both claim the same block/entity on the same tick, whichever ticks later in
 * {@code timeline.loop.TimeLoopPlayback}'s iteration order wins that tick. A real, stated limitation
 * (last-write-wins, not blended), not a solved conflict system. {@link TechTimeLoopBeta} is the other
 * stacking behavior - deterministic parent-child layering instead of this free-for-all.
 */
public class TechTimeLoopAlpha extends TechHeroAspect
{
	private static final int MIN_CHARGE_TICKS = 20;

	public TechTimeLoopAlpha()
	{
		super(Minestuckuniverseported.id("time_loop_alpha"), EnumAspect.TIME, 20000, MSUTechType.UTILITY); // new tech, no original cost to port - picked to fit this project's own cost spread, see class doc comment
		setIcon("time_loop_beta");
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.PRESS || state == AbilitechKeyState.HELD)
		{
			if(time >= TimeLoopZone.MAX_DURATION_TICKS)
				return false;
			MSUAbilitechParticles.burst(level, player, EnumAspect.TIME, 6);
			return true;
		}

		if(state != AbilitechKeyState.RELEASED)
			return false;

		int chargedTicks = Math.min(time, TimeLoopZone.MAX_DURATION_TICKS);
		if(chargedTicks < MIN_CHARGE_TICKS)
			return false;

		if(!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer))
			return false;

		TimeLoopZone zone = TimeLoopCaster.cast(serverLevel, serverPlayer, chargedTicks, TimeLoopZone.StackMode.INDEPENDENT, null);
		if(zone == null)
		{
			player.displayClientMessage(Component.translatable("status.minestuckuniverseported.timeline.no_history"), true);
			return false;
		}

		player.displayClientMessage(Component.translatable("status.minestuckuniverseported.timeLoop.created",
				zone.getWindowLength() / 20F, chargedTicks / 20F), true);
		MSUAbilitechParticles.oneshot(level, player, EnumAspect.TIME, 30);
		return true;
	}
}
