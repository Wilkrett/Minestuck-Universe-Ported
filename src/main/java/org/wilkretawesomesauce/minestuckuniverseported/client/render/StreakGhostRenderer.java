package org.wilkretawesomesauce.minestuckuniverseported.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
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
import org.wilkretawesomesauce.minestuckuniverseported.client.util.StreakGhostUtils;
import org.wilkretawesomesauce.minestuckuniverseported.client.util.StreakSettings;

import java.util.List;
import java.util.Map;

/**
 * Draws sprint-ghost afterimages - translucent copies of a sprinting tracked entity along its recent
 * path - the modern equivalent of iChun's Streak's own model-copy rendering in {@code StreakTag#render}.
 * See {@code client.util.StreakGhostUtils}'s own doc comment for the real-entity/current-pose fidelity
 * tradeoff this makes versus the original's true historical pose replay.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class StreakGhostRenderer
{
	private StreakGhostRenderer()
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

		Vec3 camPos = event.getCamera().getPosition();
		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

		boolean rendered = false;

		for(Map.Entry<Integer, java.util.Deque<StreakTracker.Sample>> entry : history.entrySet())
		{
			Entity entity = mc.level.getEntity(entry.getKey());
			StreakClientState.GhostRenderState renderState = StreakClientState.getGhostRenderState(entry.getKey());
			if(entity == null || renderState == null)
				continue;

			StreakClientState.ActiveState state = renderState.state();
			float fadeMultiplier = renderState.fadeMultiplier();

			boolean isLocalPlayerFirstPerson = entity == mc.player && mc.options.getCameraType() == CameraType.FIRST_PERSON;
			if(isLocalPlayerFirstPerson && !StreakSettings.RENDER_IN_FIRST_PERSON)
				continue;

			List<StreakTracker.Sample> ghosts = StreakGhostUtils.selectGhostSamples(entry.getValue(),
					StreakSettings.SPRINT_GHOST_COUNT, StreakSettings.SPRINT_GHOST_SPACING_TICKS, state.ghostsIgnoreSprint());

			for(int i = 0; i < ghosts.size(); i++)
			{
				StreakTracker.Sample sample = ghosts.get(i);
				if(sample.invisible())
					continue;

				float alpha = StreakSettings.TRAIL_OPACITY * (1F - (float) i / ghosts.size()) * fadeMultiplier;
				int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(sample.x(), sample.y(), sample.z()));

				StreakGhostUtils.renderGhostCopy(entity, sample.x() - camPos.x, sample.y() - camPos.y, sample.z() - camPos.z,
						alpha, state.ghostTint(), poseStack, bufferSource, light, partialTick);
				rendered = true;
			}
		}

		if(rendered)
			bufferSource.endBatch();
	}
}
