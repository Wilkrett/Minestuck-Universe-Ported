package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.voidAspect;

import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

/**
 * Client-side half of {@code TechVoidStep} ("Voidstep") - see {@link VoidStepEffect}'s own doc comment
 * for the real bug this fixes. Mirrors {@code breath.WindVesselClientEvents}' own shape, but has to set a
 * plain field directly rather than react to a dedicated client-only input/render event, since
 * {@link Player#noPhysics} isn't exposed through any event - hooked at {@link PlayerTickEvent.Post}
 * (after {@code Player#tick()}'s own unconditional {@code this.noPhysics = this.isSpectator()} reset for
 * that same tick, so this actually sticks instead of being immediately overwritten) on the client only.
 * Fires for every client-visible {@code Player} entity that ticks locally (the real local player, and any
 * other nearby real players also voidstepping), not just the local one - correct either way, since the
 * marker effect is only ever actually applied to whoever equipped the tech.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class VoidStepClientEvents
{
	private VoidStepClientEvents()
	{
	}

	@SubscribeEvent
	private static void onPlayerTick(PlayerTickEvent.Post event)
	{
		Player player = event.getEntity();
		if(player.hasEffect(MSUMobEffects.VOID_STEP))
			player.noPhysics = true;
	}
}
