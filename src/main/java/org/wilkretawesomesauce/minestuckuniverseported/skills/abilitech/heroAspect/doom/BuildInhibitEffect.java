package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.doom;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Real port of MinestuckUniverse (1.12.2)'s {@code potions.PotionBuildInhibit} ("CREATIVE_SHOCK") -
 * backs {@code TechDoomChain} ("Chains of Despair")'s second debuff stage, replacing this project's
 * earlier vanilla-Weakness stand-in. The original set {@code capabilities.allowEdit = false}; the
 * direct modern equivalent is {@code Player#getAbilities().mayBuild} (the same real field this codebase
 * already reads to gate building elsewhere, e.g. {@code TechSpaceManipulator}/{@code TechLightGlorb}) -
 * vanilla's own block break/place logic already enforces it, so no separate block-break-cancelling event
 * handler is needed, matching the original's own "just flip the capability flag" approach exactly.
 * Creative-mode players are left alone, matching the original's {@code !player.isCreative()} check.
 * Restored once the effect actually ends by {@code DoomAbilityEvents} - see that class's own doc comment
 * for why this effect can't restore the field itself.
 */
public class BuildInhibitEffect extends MobEffect
{
	public BuildInhibitEffect()
	{
		super(MobEffectCategory.HARMFUL, 0x993030);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier)
	{
		return duration % 5 == 0;
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier)
	{
		if(!(entity instanceof Player player) || player.isCreative())
			return true;

		player.getAbilities().mayBuild = false;
		player.onUpdateAbilities();

		return true;
	}
}
