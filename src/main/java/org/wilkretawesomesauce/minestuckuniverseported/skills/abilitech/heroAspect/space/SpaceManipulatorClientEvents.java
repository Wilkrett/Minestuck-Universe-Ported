package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.space;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

/**
 * Client-only render outline for {@code abilitech.heroAspect.space.TechSpaceManipulator}'s in-progress
 * corner selection - the modern equivalent of the original {@code TechSpaceManipulator#renderOutline}'s
 * {@code RenderWorldLastEvent} hook, using the real vanilla {@link LevelRenderer#renderLineBox} helper
 * instead of hand-rolled {@code GlStateManager} calls.
 * <p>
 * <b>Simplified, not the mechanic:</b> the original also drew a live placement-preview box while
 * holding a filled {@code ItemManipulatedMatter} and aiming at a block. Only the corner-selection box
 * (the part every player actually needs to see to use the ability) is drawn here - the held-item preview
 * is a visual nicety on top of an already-real capture/place mechanic, not attempted this pass.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class SpaceManipulatorClientEvents
{
	private SpaceManipulatorClientEvents()
	{
	}

	@SubscribeEvent
	private static void onRenderLevel(RenderLevelStageEvent event)
	{
		if(event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
			return;

		Minecraft mc = Minecraft.getInstance();
		if(mc.level == null || mc.player == null)
			return;

		int state = ManipulatorSelectionClientState.getState();
		if(state == ManipulatorSelectionClientState.STATE_CLEARED
				|| !ManipulatorSelectionClientState.getDimension().equals(mc.level.dimension().location()))
			return;

		BlockPos pos1 = ManipulatorSelectionClientState.getPos1();
		BlockPos pos2 = ManipulatorSelectionClientState.getPos2();
		boolean lockedIn = state == ManipulatorSelectionClientState.STATE_BOTH;

		AABB box;
		if(lockedIn)
		{
			box = new AABB(
					Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()), Math.min(pos1.getZ(), pos2.getZ()),
					Math.max(pos1.getX(), pos2.getX()) + 1, Math.max(pos1.getY(), pos2.getY()) + 1, Math.max(pos1.getZ(), pos2.getZ()) + 1);
		}
		else
			box = new AABB(pos1);

		Vec3 camPos = event.getCamera().getPosition();
		box = box.move(-camPos.x, -camPos.y, -camPos.z);

		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
		VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());

		LevelRenderer.renderLineBox(poseStack, consumer, box, 0F, 1F, lockedIn ? 1F : 0.5F, 0.6F);

		bufferSource.endBatch(RenderType.lines());
	}
}
