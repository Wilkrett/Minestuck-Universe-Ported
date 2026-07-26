package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time;

import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.Config;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;
import org.wilkretawesomesauce.minestuckuniverseported.timeline.TimelineData;
import org.wilkretawesomesauce.minestuckuniverseported.timeline.WorldTickSnapshot;
import org.wilkretawesomesauce.minestuckuniverseported.timeline.vision.PastVisionSession;

import java.util.ArrayList;
import java.util.List;

/**
 * The real "see the past" ability, as distinct from {@code TechTimelineRewind} (destructive) and
 * {@code abilitech.heroAspect.time.TechTimeLoopAlpha} (an area repeating itself). Reworked from its original
 * design: it used to switch the caster to spectator mode and teleport them to their own past position
 * with nothing but the *current* world rendered around them - a stated, deliberate gap ("does not yet
 * render the past"). The user confirmed that didn't work as hoped and asked for real reconstruction
 * without pulling them out of their own body - see {@code timeline.vision.PastVisionSession}'s doc
 * comment for the actual mechanic this now drives.
 * <p>
 * The caster stays exactly where they are, in whatever gamemode they were already in, completely free
 * to keep moving/acting normally - {@link PastVisionSession} and {@code timeline.vision.PastVisionPlayback}
 * do all the work of faking nearby blocks/entities around wherever they end up, tick by tick, for the
 * vision's whole duration. There's nothing to "return" them from afterward.
 */
public class TechRetrocognition extends TechHeroAspect
{
	public TechRetrocognition()
	{
		super(Minestuckuniverseported.id("retrocognition"), EnumAspect.TIME, 25000, MSUTechType.UTILITY, EnumClass.MAGE, EnumClass.SEER); // new tech, no original cost to port - picked to fit this project's own cost spread, see class doc comment
		setIcon("default");
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.PRESS)
			return false;

		if(!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel))
			return false;

		TimelineData data = serverLevel.getData(MSUAttachments.TIMELINE);
		List<WorldTickSnapshot> history = new ArrayList<>(data.getHistory());
		int ticks = Math.min(Config.retrocognitionObserveTicks, history.size());
		if(ticks <= 0)
		{
			player.displayClientMessage(Component.translatable("status.minestuckuniverseported.timeline.no_history"), true);
			return false;
		}

		List<WorldTickSnapshot> window = new ArrayList<>(history.subList(history.size() - ticks, history.size()));

		PastVisionSession session = new PastVisionSession(serverPlayer.getUUID(), window);
		data.getActiveVisions().add(session);

		player.displayClientMessage(Component.translatable("status.minestuckuniverseported.timeline.vision_started", ticks / 20F), true);
		MSUAbilitechParticles.oneshot(level, player, EnumAspect.TIME, 20);
		return true;
	}
}
