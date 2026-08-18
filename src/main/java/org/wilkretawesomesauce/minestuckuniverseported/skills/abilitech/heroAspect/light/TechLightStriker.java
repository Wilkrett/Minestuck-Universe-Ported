package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.light;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.light.TechLightStriker}
 * ("Storm of the Striker") - press and aim at a target to mark it Glowing for a full minute; hold for
 * 15 ticks to call down real, visual-only lightning bolts (see {@code breath.TechBreathGale}'s own doc
 * comment for why - the same real armor-bypassing AoE damage pass is reused here rather than a second
 * one-off) on every currently-glowing entity in the world at once, clearing their Glowing afterwards.
 * The original's "Cibernet" (the mod author's own name) easter-egg branch, which swapped these particles
 * to a one-off custom color pair instead of {@link EnumAspect#LIGHT}'s own table, is deliberately not
 * reproduced - a non-functional, non-portable developer in-joke, consistent with the same judgment call
 * made everywhere else it comes up in this project (e.g. {@code hope.TechHopeyShit}).
 */
public class TechLightStriker extends TechHeroAspect
{
	private static final double AOE_RADIUS = 3.0;
	private static final float STRIKE_DAMAGE = 10.0F;
	private static final int CHARGE_TICKS = 15;

	public TechLightStriker()
	{
		super(Minestuckuniverseported.id("storm_of_the_striker"), EnumAspect.LIGHT, 7650, MSUTechType.OFFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.PRESS)
		{
			LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);
			if(target != null)
			{
				target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 1200, 0));
				MSUAbilitechParticles.oneshot(level, target, EnumAspect.LIGHT, 10);
				return true;
			}
		}

		if(state == AbilitechKeyState.NONE || time > CHARGE_TICKS)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 8)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(time > 13)
			MSUAbilitechParticles.burst(level, player, EnumAspect.LIGHT, 20);
		else
			MSUAbilitechParticles.aura(level, player, EnumAspect.LIGHT, 10);

		if(time == CHARGE_TICKS && level instanceof ServerLevel serverLevel)
		{
			for(LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(256),
					e -> e != player && e.hasEffect(MobEffects.GLOWING)))
			{
				if(player.isAlliedTo(target))
					continue;

				LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
				if(lightning == null)
					continue;

				lightning.moveTo(target.getX(), target.getY(), target.getZ());
				lightning.setVisualOnly(true);
				serverLevel.addFreshEntity(lightning);

				AABB box = new AABB(target.getX() - AOE_RADIUS, target.getY() - AOE_RADIUS, target.getZ() - AOE_RADIUS,
						target.getX() + AOE_RADIUS, target.getY() + 6.0 + AOE_RADIUS, target.getZ() + AOE_RADIUS);
				for(Entity struck : serverLevel.getEntitiesOfClass(Entity.class, box, e -> true))
					if(struck instanceof LivingEntity struckLiving)
						struckLiving.hurt(serverLevel.damageSources().lightningBolt(), STRIKE_DAMAGE);

				MSUAbilitechParticles.oneshot(level, target, EnumAspect.LIGHT, 10);
				target.removeEffect(MobEffects.GLOWING);
			}

			if(!player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 5);
		}

		return true;
	}
}
