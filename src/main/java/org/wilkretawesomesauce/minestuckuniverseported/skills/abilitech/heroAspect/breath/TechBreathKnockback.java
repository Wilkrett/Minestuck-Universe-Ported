package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.breath;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.breath.TechBreathKnockback}
 * ("Windsweeping Typhoon") - hold (up to {@link #MAX_HOLD_TICKS}) to push every nearby entity away from
 * you, ramping up in strength over the first ~4.5 seconds of the hold, while refilling your own and
 * everyone else's air supply within range (a "the wind clears a breathable pocket" side effect the
 * original had bundled into this same tech rather than {@code TechBreathBubble}).
 */
public class TechBreathKnockback extends TechHeroAspect
{
	private static final double RADIUS = 12.0;
	private static final int MAX_HOLD_TICKS = 160;

	public TechBreathKnockback()
	{
		super(Minestuckuniverseported.id("windsweeping_typhoon"), EnumAspect.BREATH, 413000, MSUTechType.OFFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE || time > MAX_HOLD_TICKS)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 1)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(time % 10 == 0 && !player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);

		float strength = -Math.min(Math.max(0, time - 10) / 80.0F, 1.0F);
		player.setAirSupply(player.getMaxAirSupply());

		for(Entity target : level.getEntitiesOfClass(Entity.class, player.getBoundingBox().inflate(RADIUS), e -> e != player))
		{
			if(target instanceof LivingEntity livingTarget)
				livingTarget.setAirSupply(livingTarget.getMaxAirSupply());

			Vec3 direction = new Vec3(player.getX() - target.getX(), player.getY() - target.getY(), player.getZ() - target.getZ()).normalize();
			Vec3 current = target.getDeltaMovement();
			target.setDeltaMovement(current.x / 2.0 + direction.x * strength, current.y + direction.y * strength, current.z / 2.0 + direction.z * strength);
			target.hurtMarked = true;
		}

		if(time == 0)
			MSUAbilitechParticles.burst(level, player, EnumAspect.BREATH, 20);
		MSUAbilitechParticles.aura(level, player, EnumAspect.BREATH, 10);

		return true;
	}
}
