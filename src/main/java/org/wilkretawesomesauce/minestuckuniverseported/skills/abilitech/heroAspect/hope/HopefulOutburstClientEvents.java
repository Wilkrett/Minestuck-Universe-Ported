package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.hope;

import net.minecraft.client.player.Input;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code TechHopeyShit#onMovementInput} ("Hopeful Outburst") -
 * while {@link MSUMobEffects#HOPING} is active, movement input is dampened to 10% and the caster is
 * continuously nudged upward, same as the original including its per-tick upward push constant (0.5).
 * Reads the same synced-marker-effect pattern {@code WindVesselClientEvents}/{@code SoulShockClientEvents}
 * already use, applied by {@code hope.TechHopeyShit} every tick it's held.
 * <p>
 * <b>Real bug fix, not a faithfulness call</b>: the original's own {@code motionY += 0.5f} has no cap at
 * all - a real, confirmed-via-source oversight in the original itself, not a deliberate design choice: the
 * exact same method's nearby-enemy knockback a few lines below explicitly clamps vertical velocity to 0.4
 * (`if (target.motionY > 0.4) target.motionY = 0.4`), so the original's own author clearly intended
 * vertical speed to be bounded here too, just never applied that same clamp to the self-effect. Left
 * uncapped, holding the key compounds +0.5 blocks/tick of upward velocity with literally no ceiling -
 * confirmed via a live playtest report to launch the caster into the stratosphere within a couple of
 * seconds. This is the "preserve the original's own quirks" policy meeting its actual limit: a quirk that
 * makes the tech unusable isn't a quirk worth preserving (unlike e.g. {@code AbilitechnosynthBlock}'s
 * harmless {@code 5/15d} typo).
 * <p>
 * <b>Second real bug, same report</b>: a first attempt at this fix set {@link #MAX_UPWARD_VELOCITY} equal
 * to {@link #UPWARD_PUSH_PER_TICK} (both 0.5) - since {@code min(motion.y + push, cap)} reaches that cap
 * on the very first held tick from rest and never exceeds it, this technically capped the
 * <i>acceleration</i> but not the actual sustained ascent rate, which is what the player experiences: 0.5
 * blocks/tick (10 blocks/second) held for the tech's whole duration still reaches build height in well
 * under a minute - still effectively "the stratosphere," just no longer accelerating further. The cap
 * value itself, not merely the presence of a cap, was the bug. Lowered to a sustained ~3 blocks/second
 * instead - still an immediate, noticeable "giddy float" (still reached in one tick, since
 * {@link #UPWARD_PUSH_PER_TICK} is left at 0.5 so the pop-off feel is unchanged), just not a launch.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class HopefulOutburstClientEvents
{
	private static final double UPWARD_PUSH_PER_TICK = 0.5;
	private static final double MAX_UPWARD_VELOCITY = 0.15;

	private HopefulOutburstClientEvents()
	{
	}

	@SubscribeEvent
	private static void onMovementInput(MovementInputUpdateEvent event)
	{
		if(!event.getEntity().hasEffect(MSUMobEffects.HOPING))
			return;

		Input input = event.getInput();
		input.forwardImpulse *= 0.1F;
		input.leftImpulse *= 0.1F;

		var motion = event.getEntity().getDeltaMovement();
		double newY = Math.min(motion.y + UPWARD_PUSH_PER_TICK, MAX_UPWARD_VELOCITY);
		event.getEntity().setDeltaMovement(motion.x, newY, motion.z);
	}
}
