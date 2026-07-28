package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.mind;

import net.minecraft.client.player.Input;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

/**
 * Client-side half of {@code abilitech.heroAspect.mind.TechMindConfusion} ("Sensory Break") - reverses
 * the local player's own movement input for as long as {@link MSUMobEffects#MIND_CONFUSION} is active,
 * the same marker-effect-plus-{@code MovementInputUpdateEvent} pattern already used by Wind Vessel and
 * Hopeful Outburst.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class MindConfusionClientEvents
{
	private MindConfusionClientEvents()
	{
	}

	@SubscribeEvent
	private static void onMovementInput(MovementInputUpdateEvent event)
	{
		if(!event.getEntity().hasEffect(MSUMobEffects.MIND_CONFUSION))
			return;

		Input input = event.getInput();
		input.forwardImpulse *= -1F;
		input.leftImpulse *= -1F;
	}
}
