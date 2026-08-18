package org.wilkretawesomesauce.minestuckuniverseported.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Real port of MinestuckUniverse (1.12.2)'s {@code potions.PotionDecay} ("DECAY") - backs
 * {@code doom.TechDoomDecay} ("Withering Whisper"), replacing this project's earlier vanilla-Wither
 * stand-in. Damages every {@code 40 >> amplifier} ticks (matching the original's {@code isReady} exactly)
 * via a real armor-bypassing hit, reusing {@code LivingEntity#damageSources().magic()} the same way
 * {@code TechBloodTransfusion}/{@code TimeDilationLagEvents} already do rather than registering a new
 * custom {@code DamageType} for one effect.
 * <p>
 * The original's damage escalated the longer the effect stayed continuously active
 * ({@code (decayTime++)/2}, i.e. 0, 0, 1, 1, 2, 2, 3, 3...), backed by a per-player field on its own
 * unported {@code BADGE_EFFECTS} capability. This tracks the same counter in a small
 * {@link WeakHashMap} instead (self-contained, no new attachment needed) - but unlike the original,
 * which explicitly zeroed its capability field when the potion was removed, this effect has no way to do
 * the same: the real removal hook, {@link net.minecraft.world.effect.MobEffect#removeAttributeModifiers},
 * only ever receives an {@code AttributeMap} with no back-reference to the owning entity (confirmed via
 * this project's pinned NeoForge source - same real gap noted on {@code doom.TechDoomChain.AbilityRestoreEvents}). Left
 * unreset here rather than worked around: unlike that class's ability-restoration case (a genuine
 * permanent soft-lock bug), an un-reset counter only means a *second*, separate application of Decay to
 * the same still-alive target ramps up faster than a first one would - a minor balance quirk, not a
 * breaking one, and arguably still thematic ("the decay lingers").
 * <p>
 * Not reproduced: the original's ~150 lines of custom health-bar HUD re-skin - a cosmetic nicety
 * unrelated to the actual damage mechanic, same category as this project's other accepted visual-only
 * gaps (e.g. {@code BeamRenderer}'s plain-line billboard).
 */
public class DecayEffect extends MobEffect
{
	private static final Map<LivingEntity, Integer> hitCounts = new WeakHashMap<>();

	public DecayEffect()
	{
		super(MobEffectCategory.HARMFUL, 0x204121);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier)
	{
		int interval = 40 >> amplifier;
		return interval <= 0 || duration % interval == 0;
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier)
	{
		int hits = hitCounts.getOrDefault(entity, 0);
		entity.hurt(entity.damageSources().magic(), hits / 2F);
		hitCounts.put(entity, hits + 1);
		return true;
	}
}
