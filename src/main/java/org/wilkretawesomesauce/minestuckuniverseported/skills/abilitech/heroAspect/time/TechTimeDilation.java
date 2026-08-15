package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * New "basic command" tech from the Time Aspect design discussion - "Steal Time": hold and aim at an
 * entity to afflict them with {@link MSUMobEffects#TIME_DILATION} (slower movement + attack speed - see
 * that class) while simultaneously buffing the caster with vanilla Speed + Haste for as long as held -
 * the target's time, taken and given to the caster. Not ported from anything; mechanically it's
 * {@code TechTimeSlow} and {@code TechTimeAccelerateSelf} combined into a single targeted tech, but the
 * target-facing half uses its own new potion effect (as explicitly asked for) rather than reusing
 * vanilla Slowness/Mining Fatigue the way {@code TechTimeSlow} does.
 * <p>
 * Same simple "must currently be looking at the target" requirement as {@code TechTimeSlow}/
 * {@code TechTimeAccelerateSelf} - no slot-tether "lock on and keep tracking even if you look away"
 * behavior like {@code TechTimeTickUp} has. Costs 2 food per 20 ticks (double {@code TechTimeSlow}'s
 * rate) since this is doing the combined work of both of those techs at once - a judgment call, not
 * derived from the design doc.
 */
public class TechTimeDilation extends TechHeroAspect
{
	public TechTimeDilation()
	{
		super(Minestuckuniverseported.id("time_dilation"), EnumAspect.TIME, 15000, MSUTechType.HYBRID); // new tech, no original cost to port - picked to fit this project's own cost spread, see class doc comment
		setIcon("default");
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.HELD)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 2)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);
		if(target == null)
			return true;

		target.addEffect(new MobEffectInstance(MSUMobEffects.TIME_DILATION, 20, 0, false, false));

		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20, 1, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 20, 1, false, false));

		MSUAbilitechParticles.aura(level, player, EnumAspect.TIME, 5);
		MSUAbilitechParticles.aura(level, target, EnumAspect.TIME, 5);

		if(time % 20 == 0 && !player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 2);

		return true;
	}

	/**
	 * New potion effect, not ported from anything - the target-facing half of this tech: slows the
	 * affected entity's movement and attack speed. Contrast {@code TechTimeSlow}, which does the same
	 * underlying thing but by stacking two separate vanilla effects (Slowness + Mining Fatigue) rather
	 * than being its own named effect - this one exists as its own registered {@code MobEffect}
	 * specifically so it shows up as "Time Dilation" (its own icon/tooltip), matching what was actually
	 * asked for.
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
	 * <b>Client-side companion</b>: {@link ClientEvents} renders a pulsing dark vignette for whoever is
	 * actually looking through an affected screen (the local player only, when they themselves are
	 * affected) - a "your own perspective is slowing down" visual, not a movement/combat mechanic.
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
	public static class TimeDilationEffect extends MobEffect
	{
		/** Ticks between "lag spikes" - shared with {@link ClientEvents} and {@code TimeDilationLagEvents} so the visual and the mechanic land on the same beat. */
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

	/**
	 * Client-only "your own perspective is slowing down" overlay for whoever is actually looking through a
	 * {@link MSUMobEffects#TIME_DILATION}-affected screen - only ever true for the local player, since a
	 * screen effect makes no sense for a non-player target (a targeted mob still gets the real movement/
	 * attack-speed slow from the effect itself, just never this overlay - nothing here needs to special-case
	 * that, {@link Minecraft#player} is only ever the local player to begin with).
	 * <p>
	 * Rendered as a darkened band across the top and bottom of the screen (a vignette, not a hard on/off
	 * strobe) via {@link GuiGraphics#fillGradient} - vanilla's own gradient fill only ever interpolates
	 * vertically (top color to bottom color), which is exactly right for top/bottom bands but can't produce a
	 * true left/right-inclusive radial vignette without a custom shader; two vertical bands reads as
	 * "vignette" well enough without needing one.
	 * <p>
	 * Timing reuses {@link TimeDilationEffect#PULSE_CYCLE_TICKS}/{@link TimeDilationEffect#FREEZE_DURATION_TICKS}
	 * directly - the same cycle {@code TimeDilationLagEvents} uses server-side to hold position/deal chip
	 * damage - so the screen visibly darkens hardest at exactly the moment the real "lag spike" hits, then
	 * fades back down over the rest of the cycle, instead of pulsing on an unrelated rhythm.
	 */
	@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
	public static final class ClientEvents
	{
		private static final int MIN_ALPHA = 20;
		private static final int MAX_ALPHA = 200;
		private static final float BAND_FRACTION = 0.3F;

		private ClientEvents()
		{
		}

		@SubscribeEvent
		private static void onRenderGui(RenderGuiEvent.Post event)
		{
			Minecraft mc = Minecraft.getInstance();
			LocalPlayer player = mc.player;
			if(player == null || !player.hasEffect(MSUMobEffects.TIME_DILATION))
				return;

			GuiGraphics guiGraphics = event.getGuiGraphics();
			int screenWidth = guiGraphics.guiWidth();
			int screenHeight = guiGraphics.guiHeight();

			// Same "+ getId()" per-entity offset TimeDilationLagEvents uses server-side, so this always
			// agrees with whether the player is inside a freeze window right now.
			int cycle = TimeDilationEffect.PULSE_CYCLE_TICKS;
			int freeze = TimeDilationEffect.FREEZE_DURATION_TICKS;
			int local = (int) Math.floorMod(mc.level.getGameTime() + player.getId(), (long) cycle);

			float darkness;
			if(local < freeze)
				darkness = 1.0F;
			else
			{
				float decayProgress = (local - freeze) / (float) (cycle - freeze);
				darkness = Math.max(0F, 1.0F - decayProgress);
			}
			int alpha = MIN_ALPHA + (int) ((MAX_ALPHA - MIN_ALPHA) * darkness);

			int dark = alpha << 24;
			int clear = 0;
			int bandHeight = (int) (screenHeight * BAND_FRACTION);

			guiGraphics.fillGradient(0, 0, screenWidth, bandHeight, dark, clear);
			guiGraphics.fillGradient(0, screenHeight - bandHeight, screenWidth, screenHeight, clear, dark);
		}
	}
}
