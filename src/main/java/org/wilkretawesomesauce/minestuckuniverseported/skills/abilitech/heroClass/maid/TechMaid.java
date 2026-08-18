package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.maid;

import com.mraof.minestuck.player.Echeladder;
import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import com.mraof.minestuck.player.Title;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.AspectColorHandler;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.ClasspectColorHandler;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.events.AbilitechTargetedEvent;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.MSUAspectAmbientEffects;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroClass.TechMaid} ("Maid's Favor") -
 * a quick tap blesses the caster's current raytraced target with an ambient {@link MSUAspectAmbientEffects}
 * buff (their own Title aspect, rung-scaled), or a strong flat version of the <i>caster's own</i> aspect
 * buff if the target isn't a player; holding to 39 ticks escalates into the same blessing applied to
 * everyone nearby (5x1x5). A HOPE/MIND/VOID-aspect player target gets {@code GOD_TIER_COMEBACK} instead of
 * their own ambient buff, matching the original's own real special case.
 * <p>
 * <b>Real, added safety check, not an original quirk</b>: the original read the caster's Title aspect with
 * no null guard at all (a real NPE if the caster somehow has no Title) - this port returns {@code false}
 * instead, matching this project's standing practice of fixing genuine crash bugs rather than preserving
 * them (unlike numeric/behavioral quirks, which are kept as-is elsewhere in this project).
 */
public class TechMaid extends TechHeroClass
{
	public TechMaid()
	{
		super(Minestuckuniverseported.id("maid_favor"), EnumClass.MAID, 49550, MSUTechType.DEFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE || time > 40 || !(player instanceof ServerPlayer serverPlayer))
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 2)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		var title = Title.getTitle(serverPlayer);
		if(title.isEmpty())
			return false;
		EnumAspect casterAspect = title.get().heroAspect();

		if(time == 39)
		{
			if(!player.isCreative() && player.getFoodData().getFoodLevel() < 8)
			{
				player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
				return false;
			}

			for(LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(5, 1, 5), e -> e != player))
			{
				if(NeoForge.EVENT_BUS.post(new AbilitechTargetedEvent(player, target, this, techSlot, true)).isCanceled())
					continue;
				applyMaidBlessing(serverPlayer, target, casterAspect);
			}
			if(!player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 8);
		}

		LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);
		if(time <= 36)
			MSUAbilitechParticles.aura(level, player, target == null ? 1 : 5, ClasspectColorHandler.get(EnumClass.MAID));
		else
			MSUAbilitechParticles.burst(level, player, 20, ClasspectColorHandler.get(EnumClass.MAID));

		if(state != AbilitechKeyState.PRESS)
			return true;

		if(target != null && !NeoForge.EVENT_BUS.post(new AbilitechTargetedEvent(player, target, this, techSlot, true)).isCanceled())
		{
			if(!player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 2);
			applyMaidBlessing(serverPlayer, target, casterAspect);
		}

		return true;
	}

	private static void applyMaidBlessing(ServerPlayer caster, LivingEntity target, EnumAspect casterAspect)
	{
		if(!(target instanceof ServerPlayer targetPlayer))
		{
			MSUAbilitechParticles.oneshot(target.level(), target, 10, AspectColorHandler.get(casterAspect));
			target.addEffect(new MobEffectInstance(MSUAspectAmbientEffects.effectFor(casterAspect), 2400, 3));
			return;
		}

		var targetTitle = Title.getTitle(targetPlayer);
		if(targetTitle.isEmpty())
			return;
		EnumAspect targetAspect = targetTitle.get().heroAspect();

		if(targetAspect == EnumAspect.HOPE || targetAspect == EnumAspect.MIND || targetAspect == EnumAspect.VOID)
		{
			MSUAbilitechParticles.oneshot(target.level(), target, 10, AspectColorHandler.get(casterAspect));
			target.addEffect(new MobEffectInstance(MSUMobEffects.GOD_TIER_COMEBACK, 1200, 0));
			return;
		}

		int rung = Echeladder.get(targetPlayer).getRung();
		int amplifier = (int) (rung * MSUAspectAmbientEffects.strengthFor(targetAspect)) + 3;
		target.addEffect(new MobEffectInstance(MSUAspectAmbientEffects.effectFor(targetAspect), 1500, amplifier));
		MSUAbilitechParticles.oneshot(target.level(), target, 10, ClasspectColorHandler.get(EnumClass.MAID));
	}

	@Override
	public boolean isUsableExternally(Level level, Player player)
	{
		return player.getFoodData().getFoodLevel() >= 2 && super.isUsableExternally(level, player);
	}
}
