package org.wilkretawesomesauce.minestuckuniverseported.godtier;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

/**
 * Real port of MinestuckUniverse (1.12.2)'s {@code potions.PotionComeback} ("GOD_TIER_COMEBACK") - the
 * real standing passive buff of being ascended: +3 Attack Damage per amplifier level (a real, constant
 * {@link AttributeModifier} - the base {@link net.minecraft.world.effect.MobEffect#addAttributeModifier}
 * builder already scales its amount by {@code (amplifier + 1)} automatically, confirmed via this
 * project's pinned NeoForge source, so no dynamic per-tick modifier like {@code BerserkEffect}'s is
 * needed here), periodic regen once past 60 ticks active (every {@code max(1, 20 >> amplifier)} ticks,
 * matching the original's {@code isReady} exactly), and incoming-damage reduction of
 * {@code (amplifier + 1) * 5%} except for out-of-world damage - see {@code GodTierComebackEvents} for
 * that last part, ported onto {@link net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent}
 * the same way {@code TechDoomBind}'s own "can't be one-shot" check already is.
 * <p>
 * <b>New producer, not a reproduction of the original's.</b> The original only ever granted this through
 * Karma rewards, badge unlocks, and the never-ported {@code heroClass} package - all out of scope. Its
 * mechanic, though, is exactly the "ascended passive buff" {@code godtier.GodTierData}'s own doc comment
 * already lists as a known, not-yet-ported gap ("extra hearts, fall damage immunity... not yet ported").
 * {@code GodTierComebackEvents} gives it a new, clean producer instead: continuously applied to any
 * ascended player, refreshed every tick - the same "reapply every passive tick" idiom
 * {@code TechBreathSpeed} already uses for its own always-on buff.
 */
public class GodTierComebackEffect extends MobEffect
{
	private static final ResourceLocation ATTACK_DAMAGE_ID = ResourceLocation.fromNamespaceAndPath(Minestuckuniverseported.MODID, "god_tier_comeback_attack_damage");

	public GodTierComebackEffect()
	{
		super(MobEffectCategory.BENEFICIAL, 0x00FF00);
		addAttributeModifier(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE_ID, 3.0, AttributeModifier.Operation.ADD_VALUE);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier)
	{
		if(duration < 60)
			return false;
		int interval = 20 >> amplifier;
		return interval <= 0 || duration % interval == 0;
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier)
	{
		if(entity.getHealth() < entity.getMaxHealth())
			entity.heal(1.0F);
		return true;
	}
}
