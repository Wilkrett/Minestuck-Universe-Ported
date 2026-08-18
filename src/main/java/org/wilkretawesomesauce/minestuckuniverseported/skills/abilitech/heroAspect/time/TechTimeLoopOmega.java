package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time;

import com.mraof.minestuck.player.ClientPlayerData;
import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import com.mraof.minestuck.player.Title;
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
 * "Timeloop Ω" - the on-demand tier of the three Timeloop techs (see {@link TechTimeLoopAlpha}, hold-to-
 * charge, and {@link TechTimeLoopBeta}, the passive on-death trigger). No charge at all: press to
 * instantly cast a real {@link TimeLoopZone.StackMode#INDEPENDENT} zone covering exactly the last
 * {@link #REWIND_TICKS} (8 seconds) within {@code Config.timeLoopRadius} (15 blocks by default), for 1
 * hunger - the cheapest and fastest of the three, trading that speed/cost for exclusivity.
 * <p>
 * <b>Real classpect exclusivity, not just a flavor tag</b>: unlike every other {@code heroAspect} tech
 * (equipable by any player of the right Aspect regardless of Title class - see {@code MSUHeroClass#HERO}'s
 * own doc comment for why that's this framework's deliberate default), this one additionally requires the
 * player's real Title class to be {@link EnumClass#MUSE} on top of the usual Time-aspect check -
 * {@link #canAppearOnList} layers a class check on top of what {@link TechHeroAspect#canAppearOnList}
 * already does, mirroring {@code heroClass.TechHeroClass#canAppearOnList}'s own real
 * {@link Title#getTitle(ServerPlayer)}/{@link ClientPlayerData#getTitle()} split for the exact same
 * server/client reason that class's own doc comment explains. {@link TechHeroAspect#canUnlock} already
 * calls {@link #canAppearOnList} polymorphically, so overriding just this one method is enough to gate
 * both the shop listing and the actual purchase.
 */
public class TechTimeLoopOmega extends TechHeroAspect
{
	private static final int REWIND_TICKS = 160;

	public TechTimeLoopOmega()
	{
		super(Minestuckuniverseported.id("time_loop_omega"), EnumAspect.TIME, 55000, MSUTechType.DEFENSE, EnumClass.MUSE); // new tech, no original cost to port - picked to fit this project's own cost spread, see class doc comment
		setIcon("default");
	}

	@Override
	public boolean canAppearOnList(Level level, Player player)
	{
		if(!super.canAppearOnList(level, player))
			return false;

		if(player instanceof ServerPlayer serverPlayer)
			return Title.getTitle(serverPlayer).map(t -> t.heroClass() == EnumClass.MUSE).orElse(false);

		Title title = ClientPlayerData.getTitle();
		return title != null && title.heroClass() == EnumClass.MUSE;
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.PRESS)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 1)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer))
			return false;

		TimeLoopZone zone = TimeLoopCaster.cast(serverLevel, serverPlayer, REWIND_TICKS, TimeLoopZone.StackMode.INDEPENDENT, null);
		if(zone == null)
		{
			player.displayClientMessage(Component.translatable("status.minestuckuniverseported.timeline.no_history"), true);
			return false;
		}

		if(!player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);

		player.displayClientMessage(Component.translatable("status.minestuckuniverseported.timeLoop.created",
				zone.getWindowLength() / 20F, REWIND_TICKS / 20F), true);
		MSUAbilitechParticles.oneshot(level, player, EnumAspect.TIME, 30);
		return true;
	}
}
