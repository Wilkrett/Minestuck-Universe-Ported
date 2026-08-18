package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.rogue;

import com.mraof.minestuck.player.EnumClass;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.ClasspectColorHandler;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.events.AbilitechTargetedEvent;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroClass.TechRogue} ("Rogue's
 * Contribution") - a quick tap copies every one of the caster's own active potion effects (duration capped
 * at 6000 ticks) onto their current raytraced target; holding to 60 ticks escalates into applying that
 * same snapshot to everyone nearby (5x1x5) instead.
 */
public class TechRogue extends TechHeroClass
{
	public TechRogue()
	{
		super(Minestuckuniverseported.id("rogue_contribution"), EnumClass.ROGUE, 75950, MSUTechType.OFFENSE, MSUTechType.DEFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE || time > 60)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 4)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		List<MobEffectInstance> appliedEffects = new ArrayList<>();
		for(MobEffectInstance effect : player.getActiveEffects())
			appliedEffects.add(new MobEffectInstance(effect.getEffect(), Math.min(effect.getDuration(), 6000), effect.getAmplifier(), effect.isAmbient(), true));

		if(time > 59)
		{
			if(!player.isCreative() && player.getFoodData().getFoodLevel() < 8)
			{
				player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
				return false;
			}

			for(LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(5, 1, 5), e -> e != player))
				if(!NeoForge.EVENT_BUS.post(new AbilitechTargetedEvent(player, target, this, techSlot, null)).isCanceled())
					for(MobEffectInstance effect : appliedEffects)
					{
						target.addEffect(effect);
						MSUAbilitechParticles.oneshot(level, target, 3, ClasspectColorHandler.get(EnumClass.ROGUE));
					}
			if(!player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 8);
		}

		LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);
		if(time < 57)
			MSUAbilitechParticles.oneshot(level, player, target != null ? 5 : 1, ClasspectColorHandler.get(EnumClass.ROGUE));
		else
			MSUAbilitechParticles.burst(level, player, 20, ClasspectColorHandler.get(EnumClass.ROGUE));
		if(state != AbilitechKeyState.PRESS)
			return true;

		if(target != null && !NeoForge.EVENT_BUS.post(new AbilitechTargetedEvent(player, target, this, techSlot, null)).isCanceled())
		{
			for(MobEffectInstance effect : appliedEffects)
				target.addEffect(effect);
			MSUAbilitechParticles.oneshot(level, target, 3, ClasspectColorHandler.get(EnumClass.ROGUE));
			if(!player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 4);
		}

		return true;
	}

	@Override
	public boolean isUsableExternally(Level level, Player player)
	{
		return player.getFoodData().getFoodLevel() >= 4 && super.isUsableExternally(level, player);
	}
}
