package org.wilkretawesomesauce.minestuckuniverseported.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.beam.Beam;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code client.RenderBeams} - beams aren't real entities (see
 * {@code beam.Beam}'s own doc comment), so there's no per-entity renderer to hook; this draws every active
 * beam directly during level rendering instead, the modern equivalent of the original's
 * {@code RenderWorldLastEvent} hook.
 * <p>
 * <b>Simplified, not the mechanic</b>: the original drew a textured, billboard-style quad sized by
 * {@link Beam#getRadius()} and using {@link Beam#getTexture()}. This draws a plain colored line instead
 * (real synced {@link Beam#color}/{@link Beam#getAlpha()}/length still drive it) - a textured quad needs a
 * camera-facing billboard transform this pass didn't build. Visual-only gap, same category as this
 * project's other stated rendering simplifications (e.g. {@code EntityBubble}'s cube-not-sphere shape) -
 * the beam's real hitbox/damage/collision logic in {@code Beam#onUpdate} doesn't depend on this at all.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class BeamRenderer
{
	private BeamRenderer()
	{
	}

	@SubscribeEvent
	private static void onRenderLevel(RenderLevelStageEvent event)
	{
		if(event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
			return;

		Minecraft mc = Minecraft.getInstance();
		if(mc.level == null)
			return;

		var beams = mc.level.getData(MSUAttachments.BEAM_DATA).getBeams(mc.level);
		if(beams.isEmpty())
			return;

		PoseStack poseStack = event.getPoseStack();
		Vec3 camPos = event.getCamera().getPosition();
		MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
		VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());

		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

		poseStack.pushPose();
		poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
		PoseStack.Pose pose = poseStack.last();

		for(Beam beam : beams)
		{
			Vec3 start = beam.getStartPoint(partialTick);
			Vec3 end = beam.getEndPoint(partialTick);

			float r = ((beam.color >> 16) & 0xFF) / 255F;
			float g = ((beam.color >> 8) & 0xFF) / 255F;
			float b = (beam.color & 0xFF) / 255F;
			float a = Math.max(0F, Math.min(1F, beam.getAlpha()));

			float nx = (float) (end.x - start.x), ny = (float) (end.y - start.y), nz = (float) (end.z - start.z);
			float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
			if(len > 0.0001F)
			{
				nx /= len;
				ny /= len;
				nz /= len;
			}

			consumer.addVertex(pose, (float) start.x, (float) start.y, (float) start.z).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
			consumer.addVertex(pose, (float) end.x, (float) end.y, (float) end.z).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
		}

		poseStack.popPose();
		bufferSource.endBatch(RenderType.lines());
	}
}
