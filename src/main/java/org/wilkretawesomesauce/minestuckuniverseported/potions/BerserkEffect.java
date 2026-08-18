package org.wilkretawesomesauce.minestuckuniverseported.potions;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

/**
 * Ported 1:1 from MinestuckUniverse (1.12.2)'s {@code potions.PotionBerserk} - backs
 * {@code rage.TechRageBerserk} ("Enraged Berserk"). +10% Movement Speed and +10% Attack Speed (both real
 * {@code MULTIPLY_TOTAL} attribute modifiers), plus a real, dynamic Attack Damage bonus recomputed
 * every {@code max(1, 20 >> amplifier)} ticks off the wearer's current health percentage - angrier the
 * closer to death, exactly matching the original's {@code isReady}/{@code performEffect} formula
 * (@code (amplifier+1) * max(0.2, 1 - health/maxHealth) * 4}). Draining 1 exhaustion per tick from a
 * real {@link Player} wearer and cancelling itself - with a Nausea + Slowness II penalty - the instant
 * their hunger bar actually hits empty is also kept exactly as sourced.
 */
public class BerserkEffect extends MobEffect
{
	private static final ResourceLocation ATTACK_DAMAGE_ID = ResourceLocation.fromNamespaceAndPath(Minestuckuniverseported.MODID, "berserk_attack_damage");

	public BerserkEffect()
	{
		super(MobEffectCategory.BENEFICIAL, 0x442769);
		addAttributeModifier(Attributes.MOVEMENT_SPEED, ResourceLocation.fromNamespaceAndPath(Minestuckuniverseported.MODID, "berserk_speed"), 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		addAttributeModifier(Attributes.ATTACK_SPEED, ResourceLocation.fromNamespaceAndPath(Minestuckuniverseported.MODID, "berserk_attack_speed"), 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier)
	{
		if(duration <= 5)
			return false;
		int interval = 20 >> amplifier;
		return interval <= 0 || duration % interval == 0;
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier)
	{
		AttributeInstance attackDamage = entity.getAttribute(Attributes.ATTACK_DAMAGE);
		if(attackDamage != null)
		{
			double bonus = (amplifier + 1) * Math.max(0.2, 1 - entity.getHealth() / entity.getMaxHealth()) * 4;
			attackDamage.addOrUpdateTransientModifier(new AttributeModifier(ATTACK_DAMAGE_ID, bonus, AttributeModifier.Operation.ADD_VALUE));
		}

		if(entity instanceof Player player && !player.isCreative())
		{
			player.getFoodData().addExhaustion(1.0F);
			if(player.getFoodData().getFoodLevel() <= 0)
			{
				player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 600));
				player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600, 2));
				player.removeEffect(MSUMobEffects.RAGE_BERSERK);
			}
		}
		return true;
	}

	@Override
	public void removeAttributeModifiers(AttributeMap attributeMap)
	{
		super.removeAttributeModifiers(attributeMap);
		AttributeInstance attackDamage = attributeMap.getInstance(Attributes.ATTACK_DAMAGE);
		if(attackDamage != null)
			attackDamage.removeModifier(ATTACK_DAMAGE_ID);
	}
}
