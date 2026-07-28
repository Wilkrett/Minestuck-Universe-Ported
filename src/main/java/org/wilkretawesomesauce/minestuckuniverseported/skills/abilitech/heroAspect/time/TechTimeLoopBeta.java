package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.loop.TimeLoopCaster;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.loop.TimeLoopZone;

/**
 * "Timeloop β" - real class name now matches its in-game display name (user-requested rename from
 * {@code TechTimeLoopNested}, sibling to {@link TechTimeLoopAlpha} and {@link TechTimeLoopOmega}). Real
 * redesign, user-requested, no longer the hold-to-charge/release {@link TechTimeLoopAlpha} variant it
 * used to be (that shape moved to {@link TechTimeLoopOmega}, the on-demand tier). Now purely passive:
 * while equipped in any slot (checked the same way {@code mind.MindStrikeEvents} gates its own discrete
 * on-hit payoff - {@link GodTierData#isTechEquipped}, no separate passive-toggle sub-state, since there's
 * nothing to toggle here), the instant this player would die, the death is cancelled and a real
 * {@link TimeLoopZone.StackMode#NESTED} zone (same nesting/stacking identity the original hold-and-release
 * version had) is cast centered on them, covering the last {@link #REWIND_TICKS} (8 seconds) within
 * {@code Config.timeLoopRadius} (15 blocks by default - see that config option's own comment).
 * <p>
 * <b>Two separate things happen on death, not one</b>: {@link TimeLoopZone} itself, like every Time Loop
 * zone, never resets or puppets a real connected player (see that class's own doc comment) - it only loops
 * blocks/other entities within its radius, while a separate {@code mechanics.timeline.DoomedTimelineClone} puppets
 * the caster's own recorded path. That's the right behavior for the surrounding world, but it does nothing
 * for the dying player's own health/position, so this handler also directly heals them back up (same
 * cancel-and-restore shape {@code life.SavingGraceEvents} already established for {@code life.TechLifeGrace}) -
 * the loop is the "everything nearby visibly rewinds" payoff, the direct heal is what actually keeps this
 * player alive to see it.
 */
public class TechTimeLoopBeta extends TechHeroAspect
{
	private static final int REWIND_TICKS = 160;

	public TechTimeLoopBeta()
	{
		super(Minestuckuniverseported.id("time_loop_nested"), EnumAspect.TIME, 35000, MSUTechType.PASSIVE); // new tech, no original cost to port - picked to fit this project's own cost spread, see class doc comment
		setIcon("default");
		NeoForge.EVENT_BUS.register(this);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		return false;
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	private void onDeath(LivingDeathEvent event)
	{
		if(!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel serverLevel))
			return;

		GodTierData godTier = player.getData(MSUAttachments.GOD_TIER);
		if(!godTier.isTechEquipped(this))
			return;

		event.setCanceled(true);
		player.setHealth(player.getMaxHealth());

		TimeLoopZone parent = TimeLoopCaster.findNestParent(serverLevel, player.position());
		TimeLoopZone zone = TimeLoopCaster.cast(serverLevel, player, REWIND_TICKS, TimeLoopZone.StackMode.NESTED,
				parent != null ? parent.getId() : null);

		MSUAbilitechParticles.burst(serverLevel, player, EnumAspect.TIME, 30);
		if(zone != null)
			player.displayClientMessage(Component.translatable("status.minestuckuniverseported.timeLoopNested.deathRewind",
					zone.getWindowLength() / 20F), true);
	}
}
