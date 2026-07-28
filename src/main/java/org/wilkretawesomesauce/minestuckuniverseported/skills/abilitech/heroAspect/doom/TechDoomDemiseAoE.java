package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.doom;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.doom.TechDoomDemiseAoE}
 * ("Death's Shroud") - a self-sacrifice ultimate. Hold for 5 seconds; at the instant the charge
 * completes, every other player within {@link #RADIUS} below 20% health is executed, your own food is
 * zeroed, and <b>you die too, unconditionally</b> - the original's own design, not something this port
 * softened. Uses {@code LivingEntity#kill()} for the executions, same reasoning as {@code TechDoomDemise}
 * (a real, separate-from-normal-damage code path, already ignoring gamemode invulnerability the way
 * {@code /kill} does, with none of the original's creative-mode death-event hacking needed).
 * <p>
 * The original also warned nearby low-health players with particles during the charge-up (a "someone is
 * about to use Death's Shroud near you" tell). Dropped - purely a telegraph/fairness nicety, not the
 * actual mechanic, and this project has no equivalent per-target particle-only packet helper handy to
 * reuse for it.
 */
public class TechDoomDemiseAoE extends TechHeroAspect
{
	private static final double RADIUS = 12.0;
	private static final int TRIGGER_TICKS = 100;
	private static final float HEALTH_THRESHOLD = 0.2F;

	public TechDoomDemiseAoE()
	{
		super(Minestuckuniverseported.id("deaths_shroud"), EnumAspect.DOOM, 8200, MSUTechType.OFFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE)
			return false;

		if(time < TRIGGER_TICKS)
		{
			MSUAbilitechParticles.aura(level, player, EnumAspect.DOOM, 25);
			return true;
		}
		if(time > TRIGGER_TICKS)
			return false;

		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.0F, 1.0F);

		for(ServerPlayer target : level.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(RADIUS),
				e -> !e.isSpectator() && e != player && e.distanceTo(player) <= RADIUS))
		{
			if(!target.isCreative() && target.getHealth() / target.getMaxHealth() <= HEALTH_THRESHOLD)
				target.kill();
		}

		MSUAbilitechParticles.burst(level, player, EnumAspect.DOOM, 25);
		player.getFoodData().setFoodLevel(0);
		player.kill();

		return true;
	}
}
