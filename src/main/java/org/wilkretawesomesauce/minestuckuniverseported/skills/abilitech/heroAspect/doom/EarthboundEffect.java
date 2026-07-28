package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.doom;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Real port of MinestuckUniverse (1.12.2)'s {@code potions.PotionFlight(true, ...)} ("EARTHBOUND") -
 * backs {@code TechDoomChain} ("Chains of Despair")'s first debuff stage, replacing this project's
 * earlier vanilla-Slowness stand-in (see that class's own updated doc comment). Every 5 ticks (matching
 * the original's {@code isReady} exactly), forces {@code mayfly}/{@code flying} off for a non-spectator
 * {@link Player} target and syncs abilities to their client - the same real
 * {@code Player#getAbilities().mayBuild}-style ability field this codebase already reads elsewhere
 * (e.g. {@code TechSpaceManipulator}, {@code TechLightGlorb}), just written instead of read. Restored
 * once the effect actually ends by {@link DoomAbilityEvents} - see that class's own doc comment for why
 * this effect can't restore the field itself.
 */
public class EarthboundEffect extends MobEffect
{
	public EarthboundEffect()
	{
		super(MobEffectCategory.HARMFUL, 0xFFCD70);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier)
	{
		return duration % 5 == 0;
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier)
	{
		if(!(entity instanceof Player player) || player.isSpectator())
			return true;

		player.getAbilities().mayfly = false;
		player.getAbilities().flying = false;
		player.onUpdateAbilities();

		if(player.isFallFlying())
			player.stopFallFlying();

		return true;
	}
}
