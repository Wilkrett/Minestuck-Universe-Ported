package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.doom;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported (simplified) from MinestuckUniverse (1.12.2)'s
 * {@code skills.abilitech.heroAspect.doom.TechDoomDemise} ("Terminal Demise") - press and aim at a
 * target below 40% health to execute them outright (and if your own health is also at or below 50%,
 * you go down with them). Above 40%, nothing happens beyond a small particle tell - not enough despair
 * to finish the job.
 * <p>
 * The original built a bespoke {@code DamageSource} with {@code setDamageAllowedInCreativeMode()} +
 * {@code setGodproof()}, plus a pair of {@code LivingDeathEvent} priority hacks that force-cancelled and
 * immediately un-cancelled the event, specifically so this could kill straight through creative-mode
 * invulnerability. {@link LivingEntity#kill()} - confirmed via {@code javap} to be a genuinely separate
 * code path from the normal damage pipeline, the same one {@code /kill} uses, which already ignores
 * gamemode invulnerability in vanilla - gets the same "dies no matter what" result directly, with none of
 * that event-hack machinery needed.
 */
public class TechDoomDemise extends TechHeroAspect
{
	private static final float HEALTH_THRESHOLD = 0.4F;
	private static final float SELF_THRESHOLD = 0.5F;
	private static final int ENERGY_USE = 16;

	public TechDoomDemise()
	{
		super(Minestuckuniverseported.id("terminal_demise"), EnumAspect.DOOM, 8250, MSUTechType.OFFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.PRESS)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < ENERGY_USE)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);
		float targetPercent = target == null ? 1.0F : target.getHealth() / target.getMaxHealth();

		if(targetPercent > HEALTH_THRESHOLD)
		{
			MSUAbilitechParticles.aura(level, player, EnumAspect.DOOM, 10);
			return true;
		}

		if(!player.isCreative())
		{
			target.kill();
			if(player.getHealth() / player.getMaxHealth() <= SELF_THRESHOLD)
				player.kill();
		}

		if(!player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - ENERGY_USE);

		MSUAbilitechParticles.aura(level, player, EnumAspect.DOOM, 20);

		return true;
	}
}
