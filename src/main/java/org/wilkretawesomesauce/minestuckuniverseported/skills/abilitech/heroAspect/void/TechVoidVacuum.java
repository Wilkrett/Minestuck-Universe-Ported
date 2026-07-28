package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.voidAspect;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.voidAspect.TechVoidVacuum}
 * ("Vacuum Siphon") - hold to create negative space at your feet, pulling every nearby entity towards
 * you with increasing strength, capping out and cutting off automatically after {@link #MAX_TICKS} ticks
 * (8 seconds). Radius, ramp curve, and food cost are all kept exactly as sourced.
 */
public class TechVoidVacuum extends TechHeroAspect
{
	private static final double RADIUS = 10;
	private static final int MAX_TICKS = 160;

	public TechVoidVacuum()
	{
		super(Minestuckuniverseported.id("vacuum_siphon"), EnumAspect.VOID, 430000, MSUTechType.OFFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE || time > MAX_TICKS)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 1)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(time % 10 == 0 && !player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);

		float strength = Math.min(Math.max(0, time - 10) / 80F, 1F);
		AABB area = player.getBoundingBox().inflate(RADIUS);

		for(Entity target : level.getEntities(player, area, e -> e != player))
		{
			Vec3 pull = new Vec3(player.getX() - target.getX(), player.getY() - target.getY(), player.getZ() - target.getZ()).normalize();

			target.hasImpulse = true;
			Vec3 motion = target.getDeltaMovement();
			target.setDeltaMovement(motion.x / 2.0 + pull.x * strength, motion.y + pull.y * strength, motion.z / 2.0 + pull.z * strength);
		}

		if(time > 15)
			MSUAbilitechParticles.burst(level, player, EnumAspect.VOID, 20);
		else
			MSUAbilitechParticles.aura(level, player, EnumAspect.VOID, 10);

		return true;
	}
}
