package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.hope;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.hope.TechHopeCleansing}
 * ("Divine Cleansing") - press and aim at a target to strip every potion effect off them (buffs and
 * debuffs alike). Falls back to cleansing yourself if there's no target, or the target has nothing to
 * cleanse.
 */
public class TechHopeCleansing extends TechHeroAspect
{
	private static final int ENERGY_USE = 4;

	public TechHopeCleansing()
	{
		super(Minestuckuniverseported.id("divine_cleansing"), EnumAspect.HOPE, 795000, MSUTechType.UTILITY);
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
		if(target == null || target.getActiveEffects().isEmpty())
			target = player;

		if(target.getActiveEffects().isEmpty())
			return true;

		target.removeAllEffects();

		if(!player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - ENERGY_USE);

		MSUAbilitechParticles.aura(level, player, EnumAspect.HOPE, target != player ? 14 : 10);

		return true;
	}
}
