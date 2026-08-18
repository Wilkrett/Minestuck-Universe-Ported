package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.breath;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.breath.TechBreathGale}
 * ("Tempesting Ascension") - hold to charge, release (after at least 10 ticks) to launch yourself
 * upward and forward and call down a lightning strike at your own feet, dealing real AoE damage to
 * everything nearby except yourself.
 * <p>
 * The lightning bolt itself is spawned {@link LightningBolt#setVisualOnly(boolean) visual-only} - vanilla
 * lightning bolts already deal their own damage and set fires on tick, which would double up with (and
 * fight) the scripted AoE damage loop below that faithfully reproduces the original's own manual damage
 * pass. Damage uses {@code damageSources().lightningBolt()}, matching the original's custom "lightning
 * bolt" crit damage source closely enough without needing a bespoke one.
 */
public class TechBreathGale extends TechHeroAspect
{
	private static final int MIN_CHARGE_TICKS = 10;
	private static final double AOE_RADIUS = 3.0;
	private static final float STRIKE_DAMAGE = 12.0F;

	public TechBreathGale()
	{
		super(Minestuckuniverseported.id("tempesting_ascension"), EnumAspect.BREATH, 1776, MSUTechType.UTILITY);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE)
			return false;

		if(state != AbilitechKeyState.RELEASED)
		{
			MSUAbilitechParticles.aura(level, player, EnumAspect.BREATH, time < 30 ? 2 : 8);
			return true;
		}

		if(time < MIN_CHARGE_TICKS)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 2)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(!(level instanceof ServerLevel serverLevel))
			return false;

		double launchStrength = Math.max(1.0, Math.min(2.0, time / 20.0) + 1.0);
		player.setSprinting(false);
		double yaw = Math.toRadians(-player.getYRot());
		player.setDeltaMovement(Math.sin(yaw) * launchStrength * 0.7, launchStrength, Math.cos(yaw) * launchStrength * 0.7);
		player.hurtMarked = true;

		if(!player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - (int) launchStrength);

		LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
		if(lightning != null)
		{
			lightning.moveTo(player.getX(), player.getY() - 1.0, player.getZ());
			lightning.setVisualOnly(true);
			serverLevel.addFreshEntity(lightning);

			AABB box = new AABB(player.getX() - AOE_RADIUS, player.getY() - AOE_RADIUS, player.getZ() - AOE_RADIUS,
					player.getX() + AOE_RADIUS, player.getY() + 6.0 + AOE_RADIUS, player.getZ() + AOE_RADIUS);
			for(Entity entity : serverLevel.getEntitiesOfClass(Entity.class, box, e -> e != player))
				if(entity instanceof LivingEntity livingTarget)
					livingTarget.hurt(serverLevel.damageSources().lightningBolt(), STRIKE_DAMAGE);
		}

		MSUAbilitechParticles.burst(level, player, EnumAspect.BREATH, 40);

		return true;
	}
}
