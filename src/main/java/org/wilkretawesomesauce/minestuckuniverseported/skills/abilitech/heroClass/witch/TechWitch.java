package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.witch;

import com.mraof.minestuck.player.EnumClass;
import net.minecraft.network.chat.Component;
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
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroClass.TechWitch} ("Witch's
 * Inhibition") - hold 20+ ticks then release while looking at any living target to lock them with a
 * shorter {@code GOD_TIER_LOCK} than {@code thief.TechThief}'s own version (800 ticks vs. 2400, no buff
 * theft). Faithfully returns {@code false} at the very end even on a successful cast, matching the
 * original's own real quirk exactly (not "fixed" - see this project's standing practice of preserving
 * numeric/behavioral oddities the original clearly wrote on purpose, as opposed to genuine crash bugs).
 */
public class TechWitch extends TechHeroClass
{
	public TechWitch()
	{
		super(Minestuckuniverseported.id("witch_inhibition"), EnumClass.WITCH, 905000, MSUTechType.OFFENSE);
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
			MSUAbilitechParticles.aura(level, player, time > 20 ? 5 : 1, MSUClassColors.get(EnumClass.WITCH));
			return true;
		}

		if(time < 20)
			return false;

		LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);
		if(target == null || NeoForge.EVENT_BUS.post(new AbilitechTargetedEvent(player, target, this, techSlot, false)).isCanceled())
			return false;

		MSUAbilitechParticles.oneshot(level, target, 20, MSUClassColors.get(EnumClass.WITCH));
		target.addEffect(new MobEffectInstance(MSUMobEffects.GOD_TIER_LOCK, 800, 1));
		if(!player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 8);

		return false;
	}

	@Override
	public boolean isUsableExternally(Level level, Player player)
	{
		return player.getFoodData().getFoodLevel() >= 8 && super.isUsableExternally(level, player);
	}
}
