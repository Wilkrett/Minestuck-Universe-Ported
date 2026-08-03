package org.wilkretawesomesauce.minestuckuniverseported.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code heroClass.page.breath.TechPageBreathFreeWill}'s activation visual - the "Breath Visualizer
 * Architecture Decision" design doc's own "expanding spherical pressure wave... wind expands outward".
 * Fed by {@code network.WindBurstPacket}, same fire-and-forget shape as {@code TetherBondImpactRenderer}
 * (nothing persisted/resynced - a late joiner who missed the moment just doesn't see it).
 * <p>
 * Rendered as {@link #SPOKES} short camera-facing billboard segments ({@code WindRibbonRenderer}'s own
 * {@code renderQuad} technique - duplicated here rather than shared, see that class's own doc comment for
 * why) scattered on an expanding sphere shell around the caster, each pointing radially outward. Spoke
 * directions are deterministic per burst (seeded from the caster id + spawn tick, regenerated fresh every
 * frame from that same seed rather than stored) so the shell reads as one coherent expanding wave instead
 * of flickering noise. Radius eases outward via a simple {@code 1 - (1-t)^2} curve (fast start, slowing
 * near {@link #MAX_RADIUS}) while alpha fades linearly to 0 - "everyone takes a breath and becomes free."
 * <p>
 * {@link #renderQuad} emits each spoke's quad once now, not twice in both winding orders - the double
 * emission this was originally copied from was a real z-fighting/flicker bug in {@code WindRibbonRenderer},
 * not a needed safety net (its own doc comment has the full explanation - {@link RenderType#entityTranslucent}
 * doesn't cull backfaces).
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class WindBurstRenderer
{
	private static final ResourceLocation TEXTURE = Minestuckuniverseported.id("textures/entity/projectiles/clear_beam.png");
	private static final int COLOR = 0x47E2FA;

	private static final int LIFETIME_TICKS = 25;
	private static final double MAX_RADIUS = 6.0;
	private static final int SPOKES = 28;
	private static final float SPOKE_LENGTH_FRACTION = 0.18F;
	private static final float WIDTH = 0.15F;
	private static final double MIN_CAM_DISTANCE_SQR = 0.6 * 0.6;

	private static final List<Burst> active = new ArrayList<>();

	private WindBurstRenderer()
	{
	}

	/** Called from {@code network.WindBurstPacket#execute} - records a fresh burst centered on this caster. */
	public static void spawn(int casterId)
	{
		Minecraft mc = Minecraft.getInstance();
		if(mc.level == null)
			return;

		active.add(new Burst(casterId, mc.level.getGameTime()));
	}

	@SubscribeEvent
	private static void onRenderLevel(RenderLevelStageEvent event)
	{
		if(event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
			return;

		if(active.isEmpty())
			return;

		Minecraft mc = Minecraft.getInstance();
		if(mc.level == null)
			return;

		long gameTime = mc.level.getGameTime();
		active.removeIf(burst -> gameTime - burst.spawnTick > LIFETIME_TICKS);
		if(active.isEmpty())
			return;

		PoseStack poseStack = event.getPoseStack();
		Vec3 camPos = event.getCamera().getPosition();
		MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

		VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE));

		poseStack.pushPose();
		poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
		PoseStack.Pose pose = poseStack.last();

		float r = ((COLOR >> 16) & 0xFF) / 255F;
		float g = ((COLOR >> 8) & 0xFF) / 255F;
		float b = (COLOR & 0xFF) / 255F;

		for(Burst burst : active)
		{
			Entity entity = mc.level.getEntity(burst.casterId);
			if(!(entity instanceof LivingEntity living) || !living.isAlive())
				continue;

			float age = (float) (gameTime - burst.spawnTick) + partialTick;
			float lifeFraction = Math.min(1F, age / LIFETIME_TICKS);
			float alpha = Math.max(0F, 1F - lifeFraction);
			if(alpha <= 0F)
				continue;

			float eased = 1F - (1F - lifeFraction) * (1F - lifeFraction);
			double radius = MAX_RADIUS * eased;

			Vec3 center = living.getPosition(partialTick).add(0, living.getBbHeight() * 0.5, 0);
			renderShell(consumer, pose, camPos, center, radius, alpha, burst.seed(), r, g, b);
		}

		poseStack.popPose();
		bufferSource.endBatch(RenderType.entityTranslucent(TEXTURE));
	}

	private static void renderShell(VertexConsumer consumer, PoseStack.Pose pose, Vec3 camPos, Vec3 center, double radius, float alpha, long seed, float r, float g, float b)
	{
		RandomSource random = RandomSource.create(seed);

		for(int i = 0; i < SPOKES; i++)
		{
			double theta = random.nextDouble() * 2.0 * Math.PI;
			double phi = Math.acos(2.0 * random.nextDouble() - 1.0);
			Vec3 dir = new Vec3(Math.sin(phi) * Math.cos(theta), Math.cos(phi), Math.sin(phi) * Math.sin(theta));

			Vec3 outer = center.add(dir.scale(radius));
			Vec3 inner = center.add(dir.scale(radius * (1.0 - SPOKE_LENGTH_FRACTION)));

			boolean tooClose = camPos.distanceToSqr(outer) < MIN_CAM_DISTANCE_SQR || camPos.distanceToSqr(inner) < MIN_CAM_DISTANCE_SQR;
			if(!tooClose)
				renderQuad(consumer, pose, camPos, inner, outer, r, g, b, alpha);
		}
	}

	private static void renderQuad(VertexConsumer consumer, PoseStack.Pose pose, Vec3 camPos, Vec3 start, Vec3 end, float r, float g, float b, float alpha)
	{
		Vec3 axis = end.subtract(start);
		if(axis.lengthSqr() < 1.0E-6)
			return;

		Vec3 mid = start.add(end).scale(0.5);
		Vec3 toCam = camPos.subtract(mid);

		Vec3 widthDir = axis.cross(toCam);
		if(widthDir.lengthSqr() < 1.0E-6)
			widthDir = axis.cross(new Vec3(0, 1, 0));
		if(widthDir.lengthSqr() < 1.0E-6)
			return;
		widthDir = widthDir.normalize().scale(WIDTH);

		Vec3 s0 = start.subtract(widthDir), s1 = start.add(widthDir);
		Vec3 e0 = end.subtract(widthDir), e1 = end.add(widthDir);

		// Emitted once, not twice in both winding orders - see WindRibbonRenderer#renderQuad's own doc
		// comment for why the double emission this was originally copied from was a real z-fighting bug,
		// not a needed safety net (entityTranslucent doesn't cull backfaces).
		vertex(consumer, pose, s0, 0F, 1F, r, g, b, alpha);
		vertex(consumer, pose, s1, 0F, 0F, r, g, b, alpha);
		vertex(consumer, pose, e1, 1F, 0F, r, g, b, alpha);
		vertex(consumer, pose, e0, 1F, 1F, r, g, b, alpha);
	}

	private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 pos, float u, float v, float r, float g, float b, float alpha)
	{
		consumer.addVertex(pose, (float) pos.x, (float) pos.y, (float) pos.z)
				.setColor(r, g, b, alpha)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(LightTexture.FULL_BRIGHT)
				.setNormal(pose, 0F, 1F, 0F);
	}

	private record Burst(int casterId, long spawnTick)
	{
		long seed()
		{
			return ((long) casterId << 32) ^ spawnTick;
		}
	}
}
