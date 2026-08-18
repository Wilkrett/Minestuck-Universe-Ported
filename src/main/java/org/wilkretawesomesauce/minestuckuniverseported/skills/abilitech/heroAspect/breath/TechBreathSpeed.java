package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.breath;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.breath.TechBreathSpeed}
 * ("Supersonic Speed") - passive: toggle it on for a constant Speed XI + Jump Boost V. No {@link #onUseTick}
 * override, same as {@code blood.TechBloodReformer} - the original toggled its own passive state directly
 * from a key press, but this project's passive toggle already lives entirely in the loadout GUI
 * ({@code AbilitechLoadout#setPassiveEnabled}), so there's nothing left for {@code onUseTick} to do here.
 * Amplifiers are kept exactly as sourced (10 and 4) rather than softened - a strong, faithful "wind
 * vessel" buff was the actual original design, not a value this port is meant to rebalance.
 */
public class TechBreathSpeed extends TechHeroAspect
{
	public TechBreathSpeed()
	{
		super(Minestuckuniverseported.id("supersonic_speed"), EnumAspect.BREATH, 1776, MSUTechType.PASSIVE);
	}

	@Override
	public boolean onPassiveTick(Level level, Player player, int techSlot)
	{
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20, 10, true, false));
		player.addEffect(new MobEffectInstance(MobEffects.JUMP, 20, 4, true, false));
		MSUAbilitechParticles.aura(level, player, EnumAspect.BREATH, 4);
		return true;
	}

	@Override
	public void onPassiveToggle(Level level, Player player, boolean active)
	{
		super.onPassiveToggle(level, player, active);
		sendToggleMessage(player, active);
	}
}
