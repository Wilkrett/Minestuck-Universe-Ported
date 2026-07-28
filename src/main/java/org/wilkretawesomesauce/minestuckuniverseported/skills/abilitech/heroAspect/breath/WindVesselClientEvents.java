package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.breath;

import net.minecraft.client.player.Input;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

/**
 * Client-side half of {@code abilitech.heroAspect.breath.TechBreathWindVessel} ("Vessel of the Wind") -
 * both hooks confirmed to have real, direct modern equivalents of the original's own
 * {@code RenderLivingEvent.Pre}/{@code InputUpdateEvent} tricks (see that tech's own doc comment for
 * the one piece of the original - sub-block gap collision-phasing - that does <i>not</i> have one).
 * <p>
 * Whether a given player is "wind formed" is read directly off {@link MSUMobEffects#WIND_FORMED}
 * rather than a bespoke synced flag - a plain potion effect is already network-synced to every
 * observing client for free, which is exactly what both hooks below need (one checks a possibly-remote
 * player being rendered, the other only ever runs for the local player already).
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class WindVesselClientEvents
{
	private WindVesselClientEvents()
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
