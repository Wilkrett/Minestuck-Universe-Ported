package org.wilkretawesomesauce.minestuckuniverseported.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.util.AspectColorHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * General-purpose "flash an aspect's {@code textures/foci/<aspect>.png} icon at a fixed world position,
 * fading out over a second" effect - a generalization of {@code TetherBondImpactRenderer}'s own real
 * technique (that class's own doc comment covers the rendering math in full; this is the same
 * camera-facing billboard icon, tinted with the aspect's own {@link AspectColorHandler} entry, just anchored
 * to a fixed {@link Vec3} instead of tracking a living entity - "the place something happened", not "a
 * thing that's still there to follow"). Not a replacement for {@code TetherBondImpactRenderer} (which
 * stays scoped to its own one caller) - this is the reusable version any tech can call into via
 * {@code skills.abilitech.MSUAbilitechParticles#focusFlash}, the "easy util" entry point.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class FociFlashRenderer
{
	/** Defaults, used by {@link #spawn(Vec3, EnumAspect)} - see {@link #spawn(Vec3, EnumAspect, float, int)} for the customizable overload. */
	public static final int DEFAULT_LIFETIME_TICKS = 30;
	public static final float DEFAULT_SIZE = 2.0F;

	private static final List<Flash> active = new ArrayList<>();

	private FociFlashRenderer()
	{
	}

	/** Called from {@code network.FociFlashPacket#execute} - records a fresh flash at this world position, at the default size/duration. */
	public static void spawn(Vec3 pos, EnumAspect aspect)
	{
		spawn(pos, aspect, DEFAULT_SIZE, DEFAULT_LIFETIME_TICKS);
	}

	/** Same as {@link #spawn(Vec3, EnumAspect)}, but with an explicit icon size (world-space width/height, {@link #DEFAULT_SIZE} is 2 blocks) and fade-out duration in ticks ({@link #DEFAULT_LIFETIME_TICKS} is 30, i.e. 1.5 seconds). */
	public static void spawn(Vec3 pos, EnumAspect aspect, float size, int lifetimeTicks)
	{
		Minecraft mc = Minecraft.getInstance();
		if(mc.level == null)
			return;

		active.add(new Flash(pos, mc.level.getGameTime(), aspect, size, lifetimeTicks));
	}

	private static ResourceLocation textureFor(EnumAspect aspect)
	{
		return Minestuckuniverseported.id("textures/foci/" + aspect.name().toLowerCase(Locale.ROOT) + ".png");
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
		active.removeIf(flash -> gameTime - flash.spawnTick > flash.lifetimeTicks);
		if(active.isEmpty())
			return;

		PoseStack poseStack = event.getPoseStack();
		Vec3 camPos = event.getCamera().getPosition();
		MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

		poseStack.pushPose();
		poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
		PoseStack.Pose pose = poseStack.last();

		for(Flash flash : active)
		{
			float age = (float) (gameTime - flash.spawnTick) + partialTick;
			float alpha = Math.max(0F, 1.0F - age / flash.lifetimeTicks);
			if(alpha <= 0F)
				continue;

			int[] colors = AspectColorHandler.get(flash.aspect);
			int tint = colors != null && colors.length > 0 ? colors[0] : 0xFFFFFF;
			float r = ((tint >> 16) & 0xFF) / 255F;
			float g = ((tint >> 8) & 0xFF) / 255F;
			float b = (tint & 0xFF) / 255F;

			VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(textureFor(flash.aspect)));
			renderIcon(consumer, pose, camPos, flash.pos, flash.size, r, g, b, alpha);
		}

		poseStack.popPose();
		bufferSource.endBatch();
	}

	private static void renderIcon(VertexConsumer consumer, PoseStack.Pose pose, Vec3 camPos, Vec3 center, float size, float r, float g, float b, float alpha)
	{
		Vec3 toCam = camPos.subtract(center);
		if(toCam.lengthSqr() < 1.0E-6)
			return;

		Vec3 right = toCam.cross(new Vec3(0, 1, 0));
		if(right.lengthSqr() < 1.0E-6)
			right = toCam.cross(new Vec3(1, 0, 0));
		if(right.lengthSqr() < 1.0E-6)
			return;
		right = right.normalize().scale(size * 0.5);
		Vec3 up = right.cross(toCam).normalize().scale(size * 0.5);

		Vec3 p0 = center.subtract(right).subtract(up);
		Vec3 p1 = center.add(right).subtract(up);
		Vec3 p2 = center.add(right).add(up);
		Vec3 p3 = center.subtract(right).add(up);

		vertex(consumer, pose, p0, 0F, 1F, r, g, b, alpha);
		vertex(consumer, pose, p1, 1F, 1F, r, g, b, alpha);
		vertex(consumer, pose, p2, 1F, 0F, r, g, b, alpha);
		vertex(consumer, pose, p3, 0F, 0F, r, g, b, alpha);
	}

	private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 pos, float u, float v, float r, float g, float b, float a)
	{
		consumer.addVertex(pose, (float) pos.x, (float) pos.y, (float) pos.z)
				.setColor(r, g, b, a)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(LightTexture.FULL_BRIGHT)
				.setNormal(pose, 0F, 1F, 0F);
	}

	private record Flash(Vec3 pos, long spawnTick, EnumAspect aspect, float size, int lifetimeTicks)
	{
	}
}
