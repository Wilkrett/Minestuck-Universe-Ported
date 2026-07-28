package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.heart;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.heart.TechHeartSoulSwitcher}
 * ("Soul Switcher") - hold and aim at a target, release after ~3 seconds to trade souls with them: not
 * just position, but rotation, velocity, health, active potion effects, and selected hotbar slot, all
 * swapped both ways at once. See {@link SoulData} (now ported in full) for exactly what's captured. Also applies
 * {@link MSUMobEffects#GOD_TIER_LOCK} to the swap target for 100 ticks, matching the original's own real
 * application exactly (see {@link GodTierLockEffect}'s own doc comment for what that marker gates).
 */
public class TechHeartSoulSwitcher extends TechHeroAspect
{
	private static final int RELEASE_THRESHOLD_TICKS = 60;
	private static final int ENERGY_USE = 8;

	public TechHeartSoulSwitcher()
	{
		super(Minestuckuniverseported.id("soul_switcher"), EnumAspect.HEART, 84600, MSUTechType.HYBRID);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < ENERGY_USE)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(state != AbilitechKeyState.RELEASED || time < RELEASE_THRESHOLD_TICKS)
		{
			MSUAbilitechParticles.aura(level, player, EnumAspect.HEART, time < 60 ? 2 : 10);
			return true;
		}

		LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);
		if(target == null)
			return false;

		SoulData targetSoul = new SoulData(target);
		SoulData playerSoul = new SoulData(player);

		targetSoul.apply(player);
		playerSoul.apply(target);

		target.addEffect(new MobEffectInstance(MSUMobEffects.GOD_TIER_LOCK, 100, 0, false, false));

		if(!player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - ENERGY_USE);

		MSUAbilitechParticles.aura(level, player, EnumAspect.HEART, 4);
		MSUAbilitechParticles.oneshot(level, target, EnumAspect.HEART, 2);

		return true;
	}
}
