package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.heart;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.AbilitechLoadout;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.heart.TechSoulStun}
 * ("Soul Shock") - hold and aim at a target to lock onto it (tether persists even if you look away,
 * same shape as {@code TechTimeTickUp}). A {@link Mob} has its AI disabled outright
 * ({@code setNoAi(true)}); a {@link Player} gets {@link SoulShockedEffect}, which
 * {@code client.gui.SoulShockScreen}/{@code client.SoulShockClientEvents} force into a real,
 * inescapable-except-to-the-pause-menu screen takeover on their own client - the original's actual
 * centerpiece, now built for real instead of the vanilla-debuff stand-in used before this pass.
 */
public class TechSoulStun extends TechHeroAspect
{
	public TechSoulStun()
	{
		super(Minestuckuniverseported.id("soul_shock"), EnumAspect.HEART, 960000, MSUTechType.OFFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		AbilitechLoadout badgeEffects = player.getData(MSUAttachments.ABILITECH_LOADOUT);
		Entity tether = badgeEffects.getTether(techSlot);
		LivingEntity current = tether instanceof LivingEntity livingTether && livingTether.isAlive() ? livingTether : null;

		LivingEntity target = state == AbilitechKeyState.NONE ? null : current;
		if(target == null && state != AbilitechKeyState.NONE)
			target = MSUAbilitechRayTrace.getTargetEntity(player);

		if(current != target)
		{
			if(current instanceof Mob mob)
				mob.setNoAi(false);
			if(target instanceof Mob mob)
				mob.setNoAi(true);
			badgeEffects.setTether(techSlot, target);
		}

		if(state == AbilitechKeyState.NONE)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 1)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(target instanceof Player)
		{
			target.addEffect(new MobEffectInstance(MSUMobEffects.SOUL_SHOCKED, 20, 0, false, false));
			MSUAbilitechParticles.oneshot(level, target, 3, 0xFFB745, 0xFF7929);
		}
		else if(target != null)
			MSUAbilitechParticles.oneshot(level, target, EnumAspect.HEART, 3);

		if(!player.isCreative() && time % 10 == 0)
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);

		if(target instanceof Player)
			MSUAbilitechParticles.aura(level, player, 2, 0xFFB745, 0xFF7929);
		else if(target != null)
			MSUAbilitechParticles.aura(level, player, EnumAspect.HEART, 2);

		return true;
	}

	@Override
	public void onUnequipped(Level level, Player player, int techSlot)
	{
		AbilitechLoadout badgeEffects = player.getData(MSUAttachments.ABILITECH_LOADOUT);
		if(badgeEffects.getTether(techSlot) instanceof Mob mob)
			mob.setNoAi(false);
		badgeEffects.setTether(techSlot, null);
	}
}
