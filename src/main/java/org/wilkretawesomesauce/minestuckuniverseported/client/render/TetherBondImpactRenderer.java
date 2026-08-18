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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
 * Real, project-original visual cue (no original 1.12.2 counterpart) for {@code heroAspect.TechTetherBond}'s
 * one-time far-range damage snap: a short-lived, fading, camera-facing {@code textures/foci/<aspect>.png}
 * icon flashed on the target the instant the snap lands, tinted with that aspect's own real color table
 * ({@link AspectColorHandler}, the same source {@code MSUAbilitechParticles}'s aura/burst calls use - each
 * icon is a plain white silhouette, tintable like any of this project's other white-on-transparent icon
 * assets). Icon/color are picked per-impact from whichever {@link EnumAspect} the snap came from - this
 * project's own asset pack already ships one {@code foci/*.png} icon per aspect (confirmed: every
 * {@link EnumAspect} constant's lowercase name matches a real file in that folder), which is what made
 * generalizing this beyond Blood worth doing.
 * Fed entirely by {@code network.TetherBondImpactPacket} - no server-side state to resync, each spawn is
 * a fire-and-forget entry in {@link #active} with its own expiry, purged once it's aged out.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class TetherBondImpactRenderer
{
	private static final int LIFETIME_TICKS = 15;
	private static final float SIZE = 2.0F;

	private static final List<Impact> active = new ArrayList<>();

	private TetherBondImpactRenderer()
	{
	}

	/** Called from {@code network.TetherBondImpactPacket#execute} - records a fresh flash for this entity. */
	public static void spawn(int entityId, EnumAspect aspect)
	{
		Minecraft mc = Minecraft.getInstance();
		if(mc.level == null)
			return;

		active.add(new Impact(entityId, mc.level.getGameTime(), aspect));
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
		active.removeIf(impact -> gameTime - impact.spawnTick > LIFETIME_TICKS);
		if(active.isEmpty())
			return;

		PoseStack poseStack = event.getPoseStack();
		Vec3 camPos = event.getCamera().getPosition();
		MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

		poseStack.pushPose();
		poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
		PoseStack.Pose pose = poseStack.last();

		for(Impact impact : active)
		{
			Entity entity = mc.level.getEntity(impact.entityId);
			if(!(entity instanceof LivingEntity living) || !living.isAlive())
				continue;

			float age = (float) (gameTime - impact.spawnTick) + partialTick;
			float alpha = Math.max(0F, 1.0F - age / LIFETIME_TICKS);
			if(alpha <= 0F)
				continue;

			int[] colors = AspectColorHandler.get(impact.aspect);
			int tint = colors != null && colors.length > 0 ? colors[0] : 0xFFFFFF;
			float r = ((tint >> 16) & 0xFF) / 255F;
			float g = ((tint >> 8) & 0xFF) / 255F;
			float b = (tint & 0xFF) / 255F;

			// Fetched per-impact (not hoisted above the loop) since different bonds can be different
			// aspects at once, each needing its own texture's buffer.
			VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(textureFor(impact.aspect)));

			Vec3 pos = living.getPosition(partialTick).add(0, living.getEyeHeight() * 0.5, 0);
			renderIcon(consumer, pose, camPos, pos, r, g, b, alpha);
		}

		poseStack.popPose();
		bufferSource.endBatch();
	}

	/** A flat, camera-facing square icon (not stretched along any axis, unlike {@code TetherBondRenderer}'s tether). */
	private static void renderIcon(VertexConsumer consumer, PoseStack.Pose pose, Vec3 camPos, Vec3 center, float r, float g, float b, float alpha)
	{
		Vec3 toCam = camPos.subtract(center);
		if(toCam.lengthSqr() < 1.0E-6)
			return;

		Vec3 right = toCam.cross(new Vec3(0, 1, 0));
		if(right.lengthSqr() < 1.0E-6)
			right = toCam.cross(new Vec3(1, 0, 0));
		if(right.lengthSqr() < 1.0E-6)
			return;
		right = right.normalize().scale(SIZE * 0.5);
		Vec3 up = right.cross(toCam).normalize().scale(SIZE * 0.5);

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

	private record Impact(int entityId, long spawnTick, EnumAspect aspect)
	{
	}
}
