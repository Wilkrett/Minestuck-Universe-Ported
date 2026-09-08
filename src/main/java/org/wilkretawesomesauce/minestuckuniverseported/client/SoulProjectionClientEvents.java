package org.wilkretawesomesauce.minestuckuniverseported.client;

import net.minecraft.client.player.Input;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.network.SoulHostInputPacket;

/**
 * Client-side half of the Soul system's "hidden anchor" relay, scoped to occupying a host only - see
 * {@code mechanics.soul.SoulManager}'s own doc comment for the whole design, and {@code PlayerSoul}'s own
 * doc comment for why a free Spirit needs none of this anymore. Structurally identical to
 * {@code TechMindControl.ClientEvents}'s controller half (same {@code rotateYaw} world-relative
 * conversion, same "capture, send, zero local input" shape) - only one direction is needed here (unlike
 * that class's own two-direction controller/target split), since the receiving end
 * ({@code entity.UninhabitedBodyEntity}) isn't itself a real connected player's own client that needs a
 * matching "apply this as local input" half.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class SoulProjectionClientEvents
{
	private SoulProjectionClientEvents()
	{
	}

	@SubscribeEvent
	private static void onMovementInput(MovementInputUpdateEvent event)
	{
		Player player = event.getEntity();
		if(!player.hasEffect(MSUMobEffects.SOUL_PROJECTION))
			return;

		Input input = event.getInput();
		float[] world = rotateYaw(input.leftImpulse, input.forwardImpulse, -player.getYHeadRot() * Mth.DEG_TO_RAD);
		PacketDistributor.sendToServer(new SoulHostInputPacket(world[0], world[1], input.jumping, input.shiftKeyDown));

		input.leftImpulse = 0;
		input.forwardImpulse = 0;
		input.jumping = false;
		input.shiftKeyDown = false;
	}

	/** Reproduces {@code TechMindControl.ClientEvents#rotateYaw} exactly - see that method's own doc comment. */
	private static float[] rotateYaw(float x, float z, float yaw)
	{
		float cos = Mth.cos(yaw);
		float sin = Mth.sin(yaw);
		return new float[]{x * cos + z * sin, z * cos - x * sin};
	}
}
