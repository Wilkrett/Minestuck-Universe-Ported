package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.hope;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;
import org.wilkretawesomesauce.minestuckuniverseported.entity.HopeGolemEntity;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.hope.TechHopeGolem}
 * ("Willed Alliance"), now backed by the real {@link HopeGolemEntity} (see that class's own doc
 * comment) instead of the self-buff stand-in used before this pass. Hold and aim at your own existing
 * golem to feed it more hope ticks (extending its remaining lifespan/power); otherwise, hold for 4
 * seconds to summon a fresh one at 40% of {@link HopeGolemEntity#MAX_EFFECTIVE_TICKS}, spawned near you
 * and looking your way.
 */
public class TechHopeGolem extends TechHeroAspect
{
	private static final int SUMMON_CHARGE_TICKS = 80;

	public TechHopeGolem()
	{
		super(Minestuckuniverseported.id("willed_alliance"), EnumAspect.HOPE, 29990, MSUTechType.OFFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.HELD)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 1)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(!(level instanceof ServerLevel serverLevel))
			return false;

		LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);

		if(target instanceof HopeGolemEntity golem && golem.getOwner() == player)
		{
			int fed = Math.max(10 - (int) (player.getHealth() / player.getMaxHealth() * 10), 1) + 10;
			golem.setHopeTicks(golem.getHopeTicks() + fed);

			if(player.tickCount % 10 == 0 && !player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);

			MSUAbilitechParticles.aura(level, player, EnumAspect.HOPE, 4);
			MSUAbilitechParticles.oneshot(level, golem, EnumAspect.HOPE, 10);
		}
		else if(time <= SUMMON_CHARGE_TICKS)
		{
			if(time == SUMMON_CHARGE_TICKS)
			{
				HopeGolemEntity golem = new HopeGolemEntity(org.wilkretawesomesauce.minestuckuniverseported.MSUEntityTypes.HOPE_GOLEM.get(), serverLevel);
				golem.setHopeTicks((int) (HopeGolemEntity.MAX_EFFECTIVE_TICKS * 0.4F));
				golem.setPos(player.getX() + serverLevel.getRandom().nextDouble() * 10 - 5,
						player.getY(), player.getZ() + serverLevel.getRandom().nextDouble() * 10 - 5);
				golem.setCreatedBy(player);
				golem.getLookControl().setLookAt(player.getX(), player.getEyeY(), player.getZ());
				serverLevel.addFreshEntity(golem);
			}

			if(player.tickCount % 10 == 0 && !player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);

			MSUAbilitechParticles.aura(level, player, EnumAspect.HOPE, (int)((float) time / 320F * 20));
		}

		return true;
	}
}
