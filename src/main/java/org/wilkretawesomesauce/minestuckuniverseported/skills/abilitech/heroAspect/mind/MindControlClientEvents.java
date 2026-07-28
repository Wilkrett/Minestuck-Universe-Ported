package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.mind;

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
import org.wilkretawesomesauce.minestuckuniverseported.network.MindControlInputPacket;

/**
 * Client-side real movement-puppeting for {@code abilitech.heroAspect.mind.TechMindControl}
 * ("Mindflayer's Spell") - both directions of the original's {@code InputUpdateEvent} hook, ported 1:1
 * including its exact world-relative rotation math (the original's {@code Vec3d#rotateYaw}, reproduced
 * directly rather than trusting a differently-conventioned modern equivalent).
 * <p>
 * <b>Controller side:</b> while carrying {@code MindControllingEffect} (a real player target is
 * currently tethered - see {@code TechMindControl}'s own doc comment), captures this client's own
 * movement input, converts it to a world-relative vector using its own head yaw, sends it to the
 * server every tick via {@link MindControlInputPacket}, and zeroes its own local input so the
 * controller doesn't also move themselves while puppeteering - exactly matching the original.
 * <p>
 * <b>Target side:</b> whenever {@link MindControlClientState} says a possession is active, overrides
 * this client's own local input with the received world-relative vector, re-projected onto this
 * client's own current head yaw - so a target's actual movement direction stays correct as the
 * controller (and therefore the target's own forced look direction, see {@code TechMindControl}'s real
 * {@code ServerPlayer#lookAt} call) keeps turning.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class MindControlClientEvents
{
	private MindControlClientEvents()
	{
	}

	@SubscribeEvent
	private static void onMovementInput(MovementInputUpdateEvent event)
	{
		Player player = event.getEntity();
		Input input = event.getInput();

		if(player.hasEffect(MSUMobEffects.MIND_CONTROLLING))
		{
			float[] world = rotateYaw(input.leftImpulse, input.forwardImpulse, -player.getYHeadRot() * Mth.DEG_TO_RAD);
			PacketDistributor.sendToServer(new MindControlInputPacket(world[0], world[1], input.jumping, input.shiftKeyDown));

			input.leftImpulse = 0;
			input.forwardImpulse = 0;
			input.jumping = false;
			input.shiftKeyDown = false;
		}

		if(MindControlClientState.isActive())
		{
			float[] local = rotateYaw(MindControlClientState.getWorldX(), MindControlClientState.getWorldZ(), player.getYHeadRot() * Mth.DEG_TO_RAD);
			input.leftImpulse = local[0];
			input.forwardImpulse = local[1];
			input.jumping = MindControlClientState.isJump();
			input.shiftKeyDown = MindControlClientState.isSneak();
		}
	}

	/** Reproduces {@code net.minecraft.world.phys.Vec3}'s 1.12.2 ancestor {@code Vec3d#rotateYaw} exactly. */
	private static float[] rotateYaw(float x, float z, float yaw)
	{
		float cos = Mth.cos(yaw);
		float sin = Mth.sin(yaw);
		return new float[]{x * cos + z * sin, z * cos - x * sin};
	}
}
