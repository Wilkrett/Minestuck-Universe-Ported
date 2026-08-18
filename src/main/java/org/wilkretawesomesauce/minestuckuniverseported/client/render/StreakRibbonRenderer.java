package org.wilkretawesomesauce.minestuckuniverseported.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.client.streak.StreakClientState;
import org.wilkretawesomesauce.minestuckuniverseported.client.streak.StreakTracker;
import org.wilkretawesomesauce.minestuckuniverseported.client.util.StreakRibbonUtils;
import org.wilkretawesomesauce.minestuckuniverseported.client.util.StreakSettings;
import org.wilkretawesomesauce.minestuckuniverseported.client.streak.StreakFlavours;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Draws the streak ribbon trail behind every entity currently tracked by {@link StreakTracker} - the
 * modern equivalent of iChun's Streak's own {@code StreakTag#render}, hooked the same way
 * {@code client.render.BeamRenderer} hooks {@link RenderLevelStageEvent} since there's no per-entity
 * renderer to attach this to (the ribbon spans historical positions, not just the entity's current
 * transform).
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class StreakRibbonRenderer
{
	private StreakRibbonRenderer()
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

		Map<Integer, java.util.Deque<StreakTracker.Sample>> history = StreakTracker.getHistory();
		if(history.isEmpty())
			return;

		PoseStack poseStack = event.getPoseStack();
		Vec3 camPos = event.getCamera().getPosition();
		MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

		poseStack.pushPose();
		poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
		PoseStack.Pose pose = poseStack.last();

		for(Map.Entry<Integer, java.util.Deque<StreakTracker.Sample>> entry : history.entrySet())
		{
			Entity entity = mc.level.getEntity(entry.getKey());
			if(entity == null)
				continue;

			boolean isLocalPlayerFirstPerson = entity == mc.player && mc.options.getCameraType() == CameraType.FIRST_PERSON;
			if(isLocalPlayerFirstPerson && !StreakSettings.RENDER_IN_FIRST_PERSON)
				continue;

			StreakClientState.ActiveState state = StreakClientState.getState(entry.getKey());
			if(state == null || state.hideTrail())
				continue;

			String flavour = state.flavour();

			List<StreakTracker.Sample> samples = new ArrayList<>(entry.getValue());
			if(samples.size() < 2)
				continue;

			VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(StreakFlavours.texture(flavour)));
			int total = samples.size();

			for(int i = 1; i < total; i++)
			{
				StreakTracker.Sample prev = samples.get(i - 1);
				StreakTracker.Sample cur = samples.get(i);
				if(prev.invisible() || cur.invisible())
					continue;

				int prevAge = total - i;
				int curAge = total - 1 - i;
				float prevAlpha = StreakRibbonUtils.fadeAlpha(prevAge, total) * StreakSettings.TRAIL_OPACITY;
				float curAlpha = StreakRibbonUtils.fadeAlpha(curAge, total) * StreakSettings.TRAIL_OPACITY;

				int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(cur.x(), cur.y(), cur.z()));

				StreakRibbonUtils.emitRibbonQuad(consumer, pose, light,
						(float) cur.x(), (float) cur.y(), (float) cur.z(), cur.height(), cur.texU(), curAlpha,
						(float) prev.x(), (float) prev.y(), (float) prev.z(), prev.height(), prev.texU(), prevAlpha);
			}

			bufferSource.endBatch(RenderType.entityTranslucent(StreakFlavours.texture(flavour)));
		}

		poseStack.popPose();
	}
}
