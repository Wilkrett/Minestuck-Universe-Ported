package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

/**
 * New potion effect, not ported from anything - the target-facing half of "Steal Time"
 * ({@code TechTimeDilation}): slows the affected entity's movement and attack speed. Contrast
 * {@code TechTimeSlow}, which does the same underlying thing but by stacking two separate vanilla
 * effects (Slowness + Mining Fatigue) rather than being its own named effect - this one exists as its
 * own registered {@code MobEffect} specifically so it shows up as "Time Dilation" (its own icon/tooltip),
 * matching what was actually asked for.
 * <p>
 * Movement speed is cut drastically (-45%, moderately-severe end of what was asked for - "clearly
 * crippled but can still reposition/flee somewhat", not reduced to a near-standstill) - a deliberately
 * bigger penalty than {@code TechTimeSlow}'s vanilla-Slowness-based -15%, since this is meant to read as
 * a much more severe, named debuff rather than a small movement tax. Attack speed keeps the original
 * -15% - only movement was ever asked to be drastic.
 * <p>
 * Vanilla Mining Fatigue's slowdown isn't attribute-driven (it's a hardcoded amplifier check inside
 * {@code Player#getDigSpeed()}), so replicating a "slower mining" component here would need a second,
 * unrelated hook for comparatively small thematic gain - skipped. Movement + attack speed alone already
 * covers "your time is being taken from you" directly.
 * <p>
 * <b>Client-side companion</b>: {@code client.TimeDilationVignette} renders a pulsing dark vignette for
 * whoever is actually looking through an affected screen (the local player only, when they themselves
 * are affected) - a "your own perspective is slowing down" visual, not a movement/combat mechanic.
 * <p>
 * <b>Periodic "lag spike" (added after the user asked for stun/damage/a genuinely laggy-looking
 * target, not just a visual)</b>: {@code TimeDilationLagEvents} makes every {@link #PULSE_CYCLE_TICKS}
 * repeat a short, real rubber-band - the affected entity's position is forcibly held at wherever it was
 * right before the spike for {@link #FREEZE_DURATION_TICKS}, attacking/interacting is cancelled for that
 * same window, and a burst of chip damage lands the instant the spike starts. This is a genuine
 * server-authoritative position hold (the one true position everyone, including the affected player's
 * own client, sees getting held and then released), not a fake-packet illusion shown only to observers -
 * see that class's own doc comment for why faking only-what-observers-see was ruled out. The vignette
 * above reuses these same two constants so the screen visibly darkens hardest exactly when the spike
 * hits, instead of pulsing on an unrelated rhythm.
 */
public class TimeDilationEffect extends MobEffect
{
	/** Ticks between "lag spikes" - shared with {@code TimeDilationVignette} and {@code TimeDilationLagEvents} so the visual and the mechanic land on the same beat. */
	public static final int PULSE_CYCLE_TICKS = 60;
	/** How long each spike freezes/blocks the affected entity, starting at the top of every {@link #PULSE_CYCLE_TICKS} cycle. */
	public static final int FREEZE_DURATION_TICKS = 10;
	/** Chip damage dealt once, right as each spike starts. */
	public static final float DAMAGE_PER_PULSE = 2.0F;

	public TimeDilationEffect()
	{
		super(MobEffectCategory.HARMFUL, 0x4B0082);
		addAttributeModifier(Attributes.MOVEMENT_SPEED, Minestuckuniverseported.id("time_dilation_movement_speed"),
				-0.45, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		addAttributeModifier(Attributes.ATTACK_SPEED, Minestuckuniverseported.id("time_dilation_attack_speed"),
				-0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}
}
