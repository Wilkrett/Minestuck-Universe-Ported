package org.wilkretawesomesauce.minestuckuniverseported.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.client.WindRibbonClientState;

import java.util.Map;

/**
 * The real primary Breath visual system - "Breath Visualizer Architecture Decision" design doc's own
 * "Wind Field" renderer (no 1.12.2 counterpart, original design for this project). Renders a genuine
 * procedural ribbon mesh between caster and target (not vanilla particles, which the doc explicitly
 * demotes to secondary/atmospheric-only status - see {@code skills.abilitech.heroAspect.breath.WindEngine}'s
 * own doc comment for that half), plus a spiral vortex around the target. Fed by
 * {@code client.WindRibbonClientState}, populated by {@code network.WindRibbonSyncPacket}.
 * <p>
 * <b>Directly extends {@code TetherBondRenderer}'s own proven technique</b> rather than inventing new
 * rendering machinery: same {@link RenderLevelStageEvent}{@code .Stage.AFTER_PARTICLES} hook, same
 * per-segment camera-facing billboard quad ({@link #renderQuad}, width perpendicular to both the segment's
 * own axis and the direction to the camera - "the standard trick for a textured line/beam with no real
 * geometry"), same near-camera degenerate-quad cutoff, same reused {@code textures/entity/projectiles/clear_beam.png}
 * tintable strip texture (no new art needed - a near-neutral strip that takes a color tint cleanly).
 * <p>
 * <b>What's actually new here, matching the design doc's own requested properties</b> ("smooth curved
 * movement... slight twisting motion... should curve if the target moves... natural turbulence") - and,
 * later, a direct user request for "wavy blue streaks" specifically (matching the reference gif's own
 * look of several independently-undulating parallel bands, not one single line): {@link #renderRibbon}
 * draws {@link #STREAK_COUNT} parallel streaks rather than one, each offset from the caster-target
 * centerline by a fixed (untapered) spacing along {@code basis[0]} - the spacing itself doesn't taper to
 * zero at the endpoints, only each streak's own wave wobble does, so the streaks stay visually spread the
 * whole way and don't all pinch back together into a single point at the caster/target - and each carries
 * its own phase/frequency offset ({@link #ribbonPoint}) so they wave independently rather than moving in
 * perfect lockstep. The underlying wave itself is still two summed sine waves (different spatial frequency
 * and phase) offset perpendicular to the line, tapered to zero at both ends via {@code sin(t*PI)} so each
 * streak still anchors cleanly rather than flailing right at the endpoints. This is the doc's own "Simple"
 * turbulence method (real Perlin noise, the doc's "Better" method, was left for a later pass - summed sines
 * already reads as genuine flowing turbulence and carries far less risk of a subtle noise-implementation
 * bug in a system that can't be tested in a live client this session).
 * <p>
 * {@link #renderVortex} is the doc's own "Spiral Currents" - a gentle wrap around the target (angle
 * advancing with both arc-length and real time, so it visibly rotates), radius/density scaled by
 * {@link WindRibbonClientState.Ribbon#intensity()} (Liberate's own "gentle orbiting rings... grows with
 * Freedom" - see {@code TechBreathLiberate}'s own call site for how that value is derived). For
 * {@code inward} ribbons (Constrain), the spiral's radius shrinks toward the target along its own length
 * instead of holding constant - "air pressure compresses toward the target" made literal, and colored with
 * Breath's own second real palette entry (a deeper blue, never a darker/invented "evil" color - the design
 * doc's own explicit instruction).
 * <p>
 * <b>Two real bugs, caught from a live screenshot</b>: the first version drew every quad twice, in both
 * winding orders, copying {@code TetherBondRenderer}'s own defensive-but-unconfirmed habit of doing that
 * "in case the render type ever turns out to cull backfaces." It doesn't - {@link RenderType#entityTranslucent}
 * is confirmed (a separate {@code entityTranslucentCull} variant exists specifically for the culling case,
 * confirming the plain one doesn't) - so that second copy was rendering identical translucent triangles at
 * the identical position a second time, which is a textbook cause of both symptoms actually reported: z-fighting
 * flicker (floating-point depth precision doesn't reliably agree with itself on truly coincident geometry)
 * and doubled/harder-looking alpha compositing at the edges. Fixed by emitting each quad once. Second, the
 * original vortex used a tight multi-turn spiral (1.5 turns in a ~1-block radius) - camera-facing billboard
 * segments recompute their own orientation every frame from the live camera position, and wherever the
 * curve bends sharply between segments (exactly what a tight spiral does), adjacent segments' orientations
 * diverge a lot frame-to-frame, reading as visible faceting/shimmer even with the z-fight fixed. Softened to
 * {@link #VORTEX_TURNS} well under one full turn at a wider radius - still wraps around the target (the one
 * part of the original screenshot that read correctly), just gently.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class WindRibbonRenderer
{
	private static final ResourceLocation TEXTURE = Minestuckuniverseported.id("textures/entity/projectiles/clear_beam.png");

	private static final int OUTWARD_COLOR = 0x47E2FA;
	private static final int INWARD_COLOR = 0x4379E6;

	// Widened substantially from the first pass's 0.12 (thread-thin) to match the reference look (thick,
	// unmistakably visible bands, not fine lines).
	private static final float RADIUS = 0.35F;
	private static final int SEGMENTS = 20;
	private static final float ALPHA = 0.6F;
	private static final double MIN_CAM_DISTANCE_SQR = 0.6 * 0.6;

	// Frequency roughly halved and amplitude reduced relative to spacing (below) from the first pass -
	// long, lazy curves rather than a tight zigzag, matching the reference's own broad, slow waves and
	// keeping the per-segment bend shallow (less faceting, since {@link #renderQuad}'s billboards only
	// look smooth when consecutive segments don't turn too sharply).
	private static final double TWIST_FREQ_1 = 0.4;
	private static final double TWIST_FREQ_2 = 0.7;
	private static final double TIME_SPEED_1 = 0.08;
	private static final double TIME_SPEED_2 = -0.05;
	private static final double TWIST_AMPLITUDE = 0.3;

	// Fewer, wider-spaced streaks than the first pass (4 at 0.4) - matches the reference's own clearly
	// separated bands with real white gaps between them, rather than a busy cluster of thin threads.
	private static final int STREAK_COUNT = 3;
	private static final double STREAK_SPACING = 0.95;
	/** Per-streak phase stagger, in radians - an irregular multiplier (not a clean fraction of 2*PI) so the streaks don't fall back into sync after a few seconds. */
	private static final float STREAK_PHASE_STAGGER = 1.7F;
	/** Per-streak frequency variation, as a fraction bump per streak index - keeps streaks from looking like exact copies of each other, just phase-shifted. */
	private static final double STREAK_FREQ_VARIATION = 0.09;

	// Well under one full turn (was 1.5) at a wider radius - a gentle wrap around the target, not a tight
	// coil - see this class's own doc comment for why the tight version was the real flicker source.
	private static final int VORTEX_SEGMENTS = 24;
	private static final double VORTEX_TURNS = 0.55;
	private static final double VORTEX_SPIN_SPEED = 0.05;
	private static final double VORTEX_HEIGHT = 1.6;
	private static final double VORTEX_RADIUS_MIN = 0.7;
	private static final double VORTEX_RADIUS_MAX = 1.7;

	private WindRibbonRenderer()
	{
	}

	@SubscribeEvent
	private static void onRenderLevel(RenderLevelStageEvent event)
	{
		if(event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
			return;

		Map<Integer, WindRibbonClientState.Ribbon> ribbons = WindRibbonClientState.getRibbons();
		if(ribbons.isEmpty())
			return;

		Minecraft mc = Minecraft.getInstance();
		if(mc.level == null)
			return;

		PoseStack poseStack = event.getPoseStack();
		Vec3 camPos = event.getCamera().getPosition();
		MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		float time = (mc.level.getGameTime() + partialTick) / 20.0F;

		VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE));

		poseStack.pushPose();
		poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
		PoseStack.Pose pose = poseStack.last();

		for(Map.Entry<Integer, WindRibbonClientState.Ribbon> entry : ribbons.entrySet())
		{
			Entity caster = mc.level.getEntity(entry.getKey());
			Entity target = mc.level.getEntity(entry.getValue().targetId());
			if(!(caster instanceof LivingEntity livingCaster) || !livingCaster.isAlive()
					|| !(target instanceof LivingEntity livingTarget) || !livingTarget.isAlive())
				continue;

			boolean inward = entry.getValue().inward();
			float intensity = entry.getValue().intensity();
			int color = inward ? INWARD_COLOR : OUTWARD_COLOR;
			float r = ((color >> 16) & 0xFF) / 255F;
			float g = ((color >> 8) & 0xFF) / 255F;
			float b = (color & 0xFF) / 255F;

			Vec3 start = livingCaster.getPosition(partialTick).add(0, livingCaster.getEyeHeight() * 0.8, 0);
			Vec3 end = livingTarget.getPosition(partialTick).add(0, livingTarget.getBbHeight() * 0.5, 0);

			renderRibbon(consumer, pose, camPos, start, end, time, r, g, b);
			renderVortex(consumer, pose, camPos, end, time, inward, Math.max(0.2F, intensity), r, g, b);
		}

		poseStack.popPose();
		bufferSource.endBatch(RenderType.entityTranslucent(TEXTURE));
	}

	/** {@link #STREAK_COUNT} parallel wavy streaks between caster and target - see this class's own doc comment. */
	private static void renderRibbon(VertexConsumer consumer, PoseStack.Pose pose, Vec3 camPos, Vec3 start, Vec3 end, float time, float r, float g, float b)
	{
		Vec3 axis = end.subtract(start);
		double length = axis.length();
		if(length < 1.0E-4)
			return;

		Vec3 dir = axis.scale(1.0 / length);
		Vec3[] basis = perpendicularBasis(dir);

		for(int streak = 0; streak < STREAK_COUNT; streak++)
		{
			double streakOffset = (streak - (STREAK_COUNT - 1) / 2.0) * STREAK_SPACING;
			float phase = streak * STREAK_PHASE_STAGGER;
			double freqScale = 1.0 + streak * STREAK_FREQ_VARIATION;

			Vec3 prevPoint = start.add(basis[0].scale(streakOffset));
			for(int i = 1; i <= SEGMENTS; i++)
			{
				float t = (float) i / SEGMENTS;
				Vec3 point = ribbonPoint(start, end, t, (float) length, time, basis, streakOffset, phase, freqScale);

				boolean tooClose = camPos.distanceToSqr(prevPoint) < MIN_CAM_DISTANCE_SQR || camPos.distanceToSqr(point) < MIN_CAM_DISTANCE_SQR;
				if(!tooClose)
					renderQuad(consumer, pose, camPos, prevPoint, point, (float) (i - 1) / SEGMENTS, t, r, g, b, ALPHA);
				prevPoint = point;
			}
		}
	}

	/**
	 * A point on one streak's animated curve - a fixed (untapered) lateral offset from the caster-target
	 * centerline, plus a tapered, time-animated wobble on top - see this class's own doc comment for why
	 * the fixed offset itself doesn't taper (keeps the streaks visibly parallel/spread the whole way) while
	 * the wobble does (keeps each streak anchored, not flailing, right at the caster/target ends).
	 */
	private static Vec3 ribbonPoint(Vec3 start, Vec3 end, float t, float length, float time, Vec3[] basis, double streakOffset, float phase, double freqScale)
	{
		Vec3 base = start.add(end.subtract(start).scale(t));

		double taper = Math.sin(t * Math.PI);
		double phase1 = t * length * TWIST_FREQ_1 * freqScale + time * TIME_SPEED_1 * 20.0 + phase;
		double phase2 = t * length * TWIST_FREQ_2 * freqScale + time * TIME_SPEED_2 * 20.0 + phase * 0.6;

		double offset1 = streakOffset + Math.sin(phase1) * TWIST_AMPLITUDE * taper;
		double offset2 = Math.cos(phase2) * TWIST_AMPLITUDE * 0.6 * taper;

		return base.add(basis[0].scale(offset1)).add(basis[1].scale(offset2));
	}

	/** "Spiral Currents" - see this class's own doc comment. */
	private static void renderVortex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 camPos, Vec3 center, float time, boolean inward, float intensity, float r, float g, float b)
	{
		Vec3 prevPoint = null;
		for(int i = 0; i <= VORTEX_SEGMENTS; i++)
		{
			float t = (float) i / VORTEX_SEGMENTS;
			double angle = t * VORTEX_TURNS * 2.0 * Math.PI + time * VORTEX_SPIN_SPEED * 20.0;
			double radius = (VORTEX_RADIUS_MIN + (VORTEX_RADIUS_MAX - VORTEX_RADIUS_MIN) * intensity) * (inward ? (1.0 - t * 0.7) : 1.0);
			double height = (t - 0.5) * VORTEX_HEIGHT * intensity;

			Vec3 point = center.add(new Vec3(Math.cos(angle) * radius, height, Math.sin(angle) * radius));

			if(prevPoint != null)
			{
				boolean tooClose = camPos.distanceToSqr(prevPoint) < MIN_CAM_DISTANCE_SQR || camPos.distanceToSqr(point) < MIN_CAM_DISTANCE_SQR;
				if(!tooClose)
					renderQuad(consumer, pose, camPos, prevPoint, point, t, t, r, g, b, ALPHA * intensity);
			}
			prevPoint = point;
		}
	}

	/**
	 * Camera-facing billboard strip - see {@code TetherBondRenderer#renderQuad}'s own doc comment, this is
	 * the same technique. Emitted once (a real fix - see this class's own doc comment for why the earlier
	 * double emission was a real z-fighting/flicker bug, not a safety net): {@link RenderType#entityTranslucent}
	 * doesn't cull backfaces, so a single winding is already visible from both sides.
	 */
	private static void renderQuad(VertexConsumer consumer, PoseStack.Pose pose, Vec3 camPos, Vec3 start, Vec3 end, float uStart, float uEnd, float r, float g, float b, float alpha)
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
		widthDir = widthDir.normalize().scale(RADIUS);

		Vec3 s0 = start.subtract(widthDir), s1 = start.add(widthDir);
		Vec3 e0 = end.subtract(widthDir), e1 = end.add(widthDir);

		vertex(consumer, pose, s0, uStart, 1F, r, g, b, alpha);
		vertex(consumer, pose, s1, uStart, 0F, r, g, b, alpha);
		vertex(consumer, pose, e1, uEnd, 0F, r, g, b, alpha);
		vertex(consumer, pose, e0, uEnd, 1F, r, g, b, alpha);
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

	/** Same helper as {@code skills.abilitech.heroAspect.breath.WindEngine#perpendicularBasis} - duplicated rather than shared, since that one operates purely server-side (spawning particles) and this one is client-render-only; sharing would mean either package would need to depend on the other for one small private method. */
	private static Vec3[] perpendicularBasis(Vec3 dir)
	{
		Vec3 reference = Math.abs(dir.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
		Vec3 perp1 = dir.cross(reference).normalize();
		Vec3 perp2 = dir.cross(perp1).normalize();
		return new Vec3[]{perp1, perp2};
	}
}
