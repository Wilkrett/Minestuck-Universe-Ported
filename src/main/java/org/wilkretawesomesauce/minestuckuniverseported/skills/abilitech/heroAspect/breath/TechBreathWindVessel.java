package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.breath;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.client.player.Input;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.breath.TechBreathWindVessel}
 * ("Vessel of the Wind") - hold to literally become wind for a while.
 * <p>
 * Real, ported behavior while "wind formed" ({@link MSUMobEffects#WIND_FORMED}, a plain marker potion
 * effect so every observing client automatically knows this player is wind-formed for free, the same
 * way any other potion effect syncs):
 * <ul>
 *     <li>True render cancellation ({@code client.WindVesselClientEvents}, {@code RenderPlayerEvent.Pre})
 *     - a direct modern equivalent of the original's {@code RenderLivingEvent.Pre} trick, not a potion
 *     effect standing in for it.</li>
 *     <li>Client movement input dampened to 10% and nudged upward ({@code MovementInputUpdateEvent}) -
 *     a direct modern equivalent of the original's {@code InputUpdateEvent} hook.</li>
 *     <li>A real forward glide in whatever direction the caster is looking (yaw <i>and</i> pitch),
 *     reasserted every held tick and synced to observers via the same {@code setDeltaMovement}+
 *     {@code hurtMarked} push pattern {@code TechBreathGale}'s own launch already established for a
 *     real connected {@code ServerPlayer}. <b>Not a 1:1 restoration</b> - the original's own forward
 *     push only ever fired from {@code PlayerSPPushOutOfBlocksEvent} (a singleplayer-only client hook
 *     that fires when the client tries to shove the local player back out of a block they're stuck
 *     inside), which doesn't exist in modern NeoForge and was inherently tied to the collision-phasing
 *     below that also isn't reproduced - applying its exact 0.4-strength push every tick unconditionally
 *     (rather than only when stuck) would runaway-accelerate with no cap. This substitutes a capped
 *     glide directly toward {@link #GLIDE_SPEED} instead, the closest real equivalent to "hold to fly
 *     like the wind" once the original's actual trigger condition is gone.
 *     <p>
 *     <b>Real bug fix, confirmed via a live report ("really, really slow")</b>: a first version blended
 *     only 15% of the gap toward the target velocity in per tick
 *     ({@code deltaMovement.scale(0.85).add(target.scale(0.15))}), the same shape as {@code TechBreathGale}'s
 *     push but applied continuously instead of once. That's the difference that broke it: Gale's launch
 *     only has to survive a single tick of friction before the player is airborne (where drag is light);
 *     Wind Vessel's glide reapplies every tick while the player is typically still on or near the ground,
 *     and vanilla's ground friction decays whatever velocity was set <i>before</i> the next tick's 15%
 *     blend runs - solving the resulting fixed point shows steady-state speed converges to roughly 15% of
 *     {@link #GLIDE_SPEED} while grounded, not the intended full glide speed. Fixed by setting velocity
 *     directly to the target every held tick (still recomputed fresh from the current look direction each
 *     tick, so steering stays fully responsive) instead of blending toward it - this can't be crushed by
 *     per-tick friction since it's reasserted in full before each tick's movement resolution.</li>
 *     <li>Fall distance reduced gradually, and {@link Player#walkDist} zeroed (the modern field
 *     backing the original's {@code distanceWalkedModified}, suppressing hunger-from-walking while
 *     wind-formed).</li>
 * </ul>
 * <p>
 * <b>Not reproduced, a confirmed technical gap rather than a guess</b>: the original also stripped any
 * collision box thinner than a full block from the player's own collision list, letting them slip
 * through gaps a solid body couldn't fit through. That's fundamentally different from
 * {@code entity.BubbleEntity}'s own confirmed gap (which was about *other* entities colliding against
 * a bubble) - here it's the player's *own* movement collision resolution that would need to selectively
 * ignore certain block shapes, which happens deep inside vanilla's own collision code with no
 * per-entity override point exposed anywhere in modern NeoForge, and there is no supported way (without
 * Mixin, which this project doesn't use) to give a *real, currently-connected* {@code ServerPlayer}
 * anything like a shrunk hitbox or a collision-shape exemption the way a custom entity type could get
 * one. Left out entirely rather than faked with a risky position-nudging workaround for what the
 * original's own author already flagged (in spirit) as an exploit-shaped mechanic.
 */
public class TechBreathWindVessel extends TechHeroAspect
{
	private static final float FALL_DISTANCE_REDUCTION = 0.02F;
	private static final double GLIDE_SPEED = 1.7;

	public TechBreathWindVessel()
	{
		super(Minestuckuniverseported.id("vessel_of_the_wind"), EnumAspect.BREATH, 85460, MSUTechType.UTILITY);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.HELD)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 1)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		player.fallDistance = Math.max(0, player.fallDistance - FALL_DISTANCE_REDUCTION);
		player.walkDist = 0;
		player.addEffect(new MobEffectInstance(MSUMobEffects.WIND_FORMED, 20, 0, true, false));

		Vec3 target = player.getLookAngle().scale(GLIDE_SPEED);
		player.setDeltaMovement(target);
		player.hurtMarked = true;

		if(time % 20 == 0 && !player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);

		MSUAbilitechParticles.aura(level, player, EnumAspect.BREATH, 10);

		return true;
	}

	/**
	 * Marker effect - carries no attribute modifiers or tick behavior of its own, it exists purely so
	 * "is this player currently wind-formed" is automatically network-synced to every observing client
	 * for free (the same way any potion effect already is), which {@link ClientEvents} needs to decide
	 * whether to hide the player's render and dampen their movement input.
	 */
	public static class WindFormedEffect extends MobEffect
	{
		public WindFormedEffect()
		{
			super(MobEffectCategory.BENEFICIAL, 0x47E2FA);
		}
	}

	/**
	 * Client-side half of this tech - both hooks are real, direct modern equivalents of the original's
	 * own {@code RenderLivingEvent.Pre}/{@code InputUpdateEvent} tricks (see this class's own doc comment
	 * for the one piece of the original - sub-block gap collision-phasing - that does <i>not</i> have
	 * one). Whether a given player is "wind formed" is read directly off {@link MSUMobEffects#WIND_FORMED}
	 * rather than a bespoke synced flag - a plain potion effect is already network-synced to every
	 * observing client for free, which is exactly what both hooks below need (one checks a possibly-remote
	 * player being rendered, the other only ever runs for the local player already).
	 */
	@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
	public static final class ClientEvents
	{
		private ClientEvents()
		{
		}

		@SubscribeEvent
		private static void onRenderPlayer(RenderPlayerEvent.Pre event)
		{
			if(event.getEntity().hasEffect(MSUMobEffects.WIND_FORMED))
				event.setCanceled(true);
		}

		@SubscribeEvent
		private static void onMovementInput(MovementInputUpdateEvent event)
		{
			if(!event.getEntity().hasEffect(MSUMobEffects.WIND_FORMED))
				return;

			Input input = event.getInput();
			input.forwardImpulse *= 0.1F;
			input.leftImpulse *= 0.1F;

			event.getEntity().setDeltaMovement(event.getEntity().getDeltaMovement().add(0, 0.05, 0));
		}
	}
}
