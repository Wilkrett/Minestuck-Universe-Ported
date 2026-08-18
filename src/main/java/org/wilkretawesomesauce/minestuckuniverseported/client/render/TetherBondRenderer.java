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
import org.wilkretawesomesauce.minestuckuniverseported.client.TetherBondClientState;
import org.wilkretawesomesauce.minestuckuniverseported.util.AspectColorHandler;

import java.util.Map;

/**
 * Real, project-original tether renderer (no original 1.12.2 counterpart - {@code heroAspect.TechTetherBond}
 * is a new, generic mechanic, see that class's own doc comment) for the taut connection between a bond's
 * caster and target, replacing what used to be a continuous particle aura on the caster alone. Reads
 * {@code client.TetherBondClientState}'s synced (caster id -> target id + aspect) cache every frame and
 * draws a real textured, sagging-curve tether (see {@link #renderTether}) per active bond between the two
 * entities' current interpolated eye positions - not a single straight quad, which read as too
 * harsh/distracting - tinted per-bond via {@link AspectColorHandler} using whichever aspect that bond's own
 * {@code TechTetherBond} subclass reported.
 * <p>
 * Deliberately self-contained, not routed through {@code client.render.BeamRenderer}/{@code capabilities.beam.Beam}:
 * a {@code Beam} models a single projectile growing outward from a shooter until it hits something, with
 * its own raytrace/damage tick logic baked into {@code Beam#onUpdate} - none of that fits "a fixed line
 * between two already-known living entities, whose own separate damage-over-distance logic already lives
 * in {@code TechTetherBond#onUseTick}". Forcing this tether through {@code Beam}'s growth/collision model
 * (or reworking that shared class's rendering just for this) would be a bigger, unrelated change to shared
 * code for a purely cosmetic feature - this class owns its own small billboard-quad rendering instead.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class TetherBondRenderer
{
	// A near-neutral white/gray tintable strip, not `textures/streak/fire.png` (that one's baked-in
	// orange fought visibly with a tint instead of taking it cleanly).
	private static final ResourceLocation TEXTURE = Minestuckuniverseported.id("textures/entity/projectiles/clear_beam.png");
	private static final float RADIUS = 0.08F;
	/** How many straight sub-segments approximate the sagging curve - higher looks smoother, costs more vertices. */
	private static final int SEGMENTS = 12;
	/** Sag depth as a fraction of the tether's own length (a taut rope sags more the longer it gets), capped below.
	 * Kept subtle to match the reference look - a taut line with just a faint bow, not an obvious S-curve. */
	private static final float SAG_FRACTION = 0.03F;
	private static final float MAX_SAG = 0.5F;
	/** Segments closer than this to the camera are skipped entirely - a billboard's world-space width gets
	 * magnified without bound as its distance to the camera approaches zero, so the segments right next to
	 * the caster's own eye (this tech's own start point) fanned out into a huge, faceted-looking mess in
	 * first person before this cutoff existed. */
	private static final double MIN_CAM_DISTANCE_SQR = 0.6 * 0.6;
	private static final float ALPHA = 0.55F;
	/** Overrides the aspect color entirely for a corrupted bond (see {@code heroClass.prince.blood.TechPrinceBloodSchism}) - a sickly dark purple, distinct from any real {@code MSUAspectColors} entry. */
	private static final int CORRUPTED_COLOR = 0x4B0082;

	private TetherBondRenderer()
	{
	}

	@SubscribeEvent
	private static void onRenderLevel(RenderLevelStageEvent event)
	{
		if(event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
			return;

		Map<Integer, TetherBondClientState.Bond> bonds = TetherBondClientState.getBonds();
		if(bonds.isEmpty())
			return;

		Minecraft mc = Minecraft.getInstance();
		if(mc.level == null)
			return;

		PoseStack poseStack = event.getPoseStack();
		Vec3 camPos = event.getCamera().getPosition();
		MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

		VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE));

		poseStack.pushPose();
		poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
		PoseStack.Pose pose = poseStack.last();

		for(Map.Entry<Integer, TetherBondClientState.Bond> bond : bonds.entrySet())
		{
			Entity caster = mc.level.getEntity(bond.getKey());
			Entity target = mc.level.getEntity(bond.getValue().targetId());
			if(!(caster instanceof LivingEntity livingCaster) || !livingCaster.isAlive()
					|| !(target instanceof LivingEntity livingTarget) || !livingTarget.isAlive())
				continue;

			int color;
			if(bond.getValue().corrupted())
			{
				color = CORRUPTED_COLOR;
			}
			else
			{
				int[] colors = AspectColorHandler.get(bond.getValue().aspect());
				color = colors != null && colors.length > 0 ? colors[0] : 0xFFFFFF;
			}
			float r = ((color >> 16) & 0xFF) / 255F;
			float g = ((color >> 8) & 0xFF) / 255F;
			float b = (color & 0xFF) / 255F;

			Vec3 start = livingCaster.getPosition(partialTick).add(0, livingCaster.getEyeHeight() * 0.8, 0);
			Vec3 end = livingTarget.getPosition(partialTick).add(0, livingTarget.getEyeHeight() * 0.8, 0);

			renderTether(consumer, pose, camPos, start, end, r, g, b);
		}

		poseStack.popPose();
		bufferSource.endBatch(RenderType.entityTranslucent(TEXTURE));
	}

	/**
	 * Draws the tether as {@link #SEGMENTS} short camera-facing quads along a curve (bowed sideways,
	 * proportional to distance, like a taut line pulled off to one side) rather than one long straight
	 * quad - a dead-straight line between two moving entities read as visually harsh/distracting; the
	 * curve reads as a physical connection instead.
	 */
	private static void renderTether(VertexConsumer consumer, PoseStack.Pose pose, Vec3 camPos, Vec3 start, Vec3 end, float r, float g, float b)
	{
		Vec3 axis = end.subtract(start);
		double length = axis.length();
		if(length < 1.0E-4)
			return;

		// Bows to the caster's left (relative to the start->end direction) rather than sagging downward -
		// a horizontal curve read as less "gravity rope, more distracting droop" and more like a deliberate
		// stylistic arc. up x horizontalAxis points to that left side (confirmed against Minecraft's real
		// +X east/+Z south/+Y up axes: facing south, this yields +X/east, which is genuinely left of south).
		Vec3 horizontalAxis = new Vec3(axis.x, 0, axis.z);
		Vec3 leftDir = new Vec3(0, 1, 0).cross(horizontalAxis);
		if(leftDir.lengthSqr() < 1.0E-6)
			leftDir = new Vec3(1, 0, 0);
		leftDir = leftDir.normalize();

		float sag = Math.min(MAX_SAG, (float) length * SAG_FRACTION);
		Vec3 mid = start.add(end).scale(0.5).add(leftDir.scale(sag));

		Vec3 prevPoint = start;
		for(int i = 1; i <= SEGMENTS; i++)
		{
			float t = (float) i / SEGMENTS;
			Vec3 point = quadraticBezier(start, mid, end, t);

			boolean tooClose = camPos.distanceToSqr(prevPoint) < MIN_CAM_DISTANCE_SQR || camPos.distanceToSqr(point) < MIN_CAM_DISTANCE_SQR;
			if(!tooClose)
				renderQuad(consumer, pose, camPos, prevPoint, point, (float) (i - 1) / SEGMENTS, t, r, g, b);
			prevPoint = point;
		}
	}

	private static Vec3 quadraticBezier(Vec3 a, Vec3 b, Vec3 c, float t)
	{
		float u = 1.0F - t;
		double x = u * u * a.x + 2 * u * t * b.x + t * t * c.x;
		double y = u * u * a.y + 2 * u * t * b.y + t * t * c.y;
		double z = u * u * a.z + 2 * u * t * b.z + t * t * c.z;
		return new Vec3(x, y, z);
	}

	/** Camera-facing "billboard strip": the quad's width runs perpendicular to both this segment's own axis
	 * and the direction to the camera, so it always presents its full texture width to the viewer regardless
	 * of viewing angle - the standard trick for a textured line/beam with no real geometry. */
	private static void renderQuad(VertexConsumer consumer, PoseStack.Pose pose, Vec3 camPos, Vec3 start, Vec3 end, float uStart, float uEnd, float r, float g, float b)
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

		// Emitted both winding orders - a single-winding quad can vanish depending on which side of the
		// tether the camera ends up on if the RenderType this ever gets swapped to turns out to cull
		// backfaces (unconfirmed either way for `entityTranslucent`, but this makes it a non-issue).
		vertex(consumer, pose, s0, uStart, 1F, r, g, b);
		vertex(consumer, pose, s1, uStart, 0F, r, g, b);
		vertex(consumer, pose, e1, uEnd, 0F, r, g, b);
		vertex(consumer, pose, e0, uEnd, 1F, r, g, b);

		vertex(consumer, pose, s0, uStart, 1F, r, g, b);
		vertex(consumer, pose, e0, uEnd, 1F, r, g, b);
		vertex(consumer, pose, e1, uEnd, 0F, r, g, b);
		vertex(consumer, pose, s1, uStart, 0F, r, g, b);
	}

	private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 pos, float u, float v, float r, float g, float b)
	{
		consumer.addVertex(pose, (float) pos.x, (float) pos.y, (float) pos.z)
				.setColor(r, g, b, ALPHA)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(LightTexture.FULL_BRIGHT)
				.setNormal(pose, 0F, 1F, 0F);
	}
}
