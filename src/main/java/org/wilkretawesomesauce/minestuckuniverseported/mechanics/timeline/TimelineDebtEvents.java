package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Config;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.AbilitechLoadout;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Turns {@link TimelineData#getTimelineDebt()} into an actual gameplay consequence - this is the real
 * "you can't just save-scum for free" mechanism the design discussion was about. Without this,
 * {@link TimelineManager#rewind} accumulating debt would just be a number nobody has to care about.
 * <p>
 * Above {@link Config#timelineCorruptionThreshold}, Time-tech users get periodic Weakness/Mining Fatigue
 * ("temporal instability sickness"), scaling with how far past the threshold the debt is. Debt only ever
 * goes up from rewinding (see {@link TimelineManager}) and slowly decays over time here, so racking up a
 * lot of small rewinds is cheaper than one huge one, but is still never free.
 * <p>
 * <b>Removed:</b> an earlier version also spawned a hostile "paradox echo" mob near the player at high
 * debt. It didn't do anything meaningful (no real AI tuning, no purpose beyond existing) and was reported
 * as just noise rather than a real consequence, so it's gone rather than kept as decoration. If a mob-based
 * consequence is wanted later, it should probably actually do something (chase/attack with real stakes,
 * or serve as an actual objective) rather than just glow nearby.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class TimelineDebtEvents
{
	private static final int CHECK_INTERVAL_TICKS = 200;

	private TimelineDebtEvents()
	{
	}

	@SubscribeEvent
	private static void onLevelTick(LevelTickEvent.Post event)
	{
//		if(!(event.getLevel() instanceof ServerLevel level))
//			return;
//		if(level.getGameTime() % CHECK_INTERVAL_TICKS != 0)
//			return;
//
//		TimelineData data = level.getData(MSUAttachments.TIMELINE);
//
//		// slow natural decay, so small/occasional rewinds aren't permanently punishing
//		data.addTimelineDebt(-Config.timelineDebtDecayPerCheck);
//
//		double debt = data.getTimelineDebt();
//		if(debt < Config.timelineCorruptionThreshold)
//			return;
//
//		double severity = (debt - Config.timelineCorruptionThreshold) / Math.max(1, Config.timelineCorruptionThreshold);
//		int amplifier = Math.min(3, (int) severity);
//
//		for(ServerPlayer player : level.players())
//		{
//			if(!hasTimeTechEquipped(player))
//				continue;
//
//			player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, CHECK_INTERVAL_TICKS, amplifier));
//			player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, CHECK_INTERVAL_TICKS, amplifier));
//		}
	}

	/**
	 * Moved here from {@code TimelineRecorder}, which used to use this same check to gate whether
	 * recording happened at all - now that recording is unconditional (see that class's doc comment),
	 * this check only matters for deciding who Timeline Debt consequences actually apply to.
	 */
	private static boolean hasTimeTechEquipped(ServerPlayer player)
	{
//		AbilitechLoadout loadout = player.getData(MSUAttachments.ABILITECH_LOADOUT);
//		for(int i = 0; i < loadout.getTechSlots(); i++)
//			if(loadout.getTech(i) instanceof TechHeroAspect tech && tech.getHeroAspect() == EnumAspect.TIME)
//				return true;
		return false;
	}
}
