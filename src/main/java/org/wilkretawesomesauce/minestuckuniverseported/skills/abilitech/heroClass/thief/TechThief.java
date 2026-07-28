package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.thief;

import com.mraof.minestuck.player.EnumClass;
import com.mraof.minestuck.player.Title;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUClassColors;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.AbilitechTargetedEvent;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.MSUAspectAmbientEffects;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroClass.TechThief} ("Thief's
 * Filch") - hold 20+ ticks then release while looking at another (real, Titled) player to lock them with
 * {@code GOD_TIER_LOCK} and copy whichever {@link MSUAspectAmbientEffects} buffs they currently have
 * active onto the caster.
 * <p>
 * <b>Real, stated simplification</b>: the original's stolen {@code GOD_TIER_LOCK} application also
 * cleared the effect's curable-items list so milk couldn't wash it off - 1.21's data-driven curative-item
 * system has no matching per-instance override, so the target can cure it normally here.
 */
public class TechThief extends TechHeroClass
{
	public TechThief()
	{
		super(Minestuckuniverseported.id("thief_filch"), EnumClass.THIEF, 42405, MSUTechType.OFFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 8)
		{
			if(state == AbilitechKeyState.HELD)
				player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(state != AbilitechKeyState.RELEASED)
		{
			MSUAbilitechParticles.aura(level, player, time > 20 ? 5 : 1, MSUClassColors.get(EnumClass.THIEF));
			return true;
		}

		if(time < 20)
			return false;

		LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);

		if(!(target instanceof ServerPlayer targetPlayer) || NeoForge.EVENT_BUS.post(new AbilitechTargetedEvent(player, target, this, techSlot, false)).isCanceled())
			return false;

		MSUAbilitechParticles.aura(level, player, 20, MSUClassColors.get(EnumClass.THIEF));
		MSUAbilitechParticles.oneshot(level, target, 20, MSUClassColors.get(EnumClass.THIEF));
		targetPlayer.addEffect(new MobEffectInstance(MSUMobEffects.GOD_TIER_LOCK, 2400, 0));

		if(Title.getTitle(targetPlayer).isPresent())
			for(Holder<MobEffect> effect : MSUAspectAmbientEffects.getAspectEffects(targetPlayer).keySet())
				if(targetPlayer.hasEffect(effect))
					player.addEffect(new MobEffectInstance(effect, 2400, targetPlayer.getEffect(effect).getAmplifier(), false, true));

		if(!player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 8);

		return true;
	}

	@Override
	public boolean isUsableExternally(Level level, Player player)
	{
		return player.getFoodData().getFoodLevel() >= 8 && super.isUsableExternally(level, player);
	}
}
