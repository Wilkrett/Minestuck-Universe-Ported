package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.knight;

import com.mraof.minestuck.player.EnumClass;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroClass.TechKnightWard} - a quick tap
 * (grants Resistance III to the caster's current raytraced target and the caster themselves, matching the
 * original's real single-tick {@code PRESS} branch) that, if instead held to 40 ticks, escalates into a
 * 5x1x5 AoE Resistance III pulse for every nearby living entity.
 */
public class TechKnightWard extends TechHeroClass
{
	public TechKnightWard()
	{
		super(Minestuckuniverseported.id("knight_ward"), EnumClass.KNIGHT, 54000, MSUTechType.DEFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE || time >= 45)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 2)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(time == 40)
		{
			if(!player.isCreative() && player.getFoodData().getFoodLevel() < 6)
			{
				player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
				return false;
			}

			for(LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(5, 1, 5), e -> true))
			{
				if(NeoForge.EVENT_BUS.post(new AbilitechTargetedEvent(player, target, this, techSlot, true)).isCanceled())
					continue;
				target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 2400, 2));
			}
			if(!player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 6);
		}

		if(time > 36)
			MSUAbilitechParticles.oneshot(level, player, 20, ClasspectColorHandler.get(EnumClass.KNIGHT));

		LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);
		if(time <= 40)
			MSUAbilitechParticles.aura(level, player, target == null ? 1 : 5, ClasspectColorHandler.get(EnumClass.KNIGHT));

		if(state == AbilitechKeyState.PRESS && target != null
				&& !NeoForge.EVENT_BUS.post(new AbilitechTargetedEvent(player, target, this, techSlot, true)).isCanceled())
		{
			target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 2400, 2));
			player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 2400, 2));
			if(!player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 2);
		}

		return true;
	}

	@Override
	public boolean isUsableExternally(Level level, Player player)
	{
		return player.getFoodData().getFoodLevel() >= 2 && super.isUsableExternally(level, player);
	}
}
