package org.wilkretawesomesauce.minestuckuniverseported.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.client.rewind.RewindGhostPlayback;
import org.wilkretawesomesauce.minestuckuniverseported.client.util.StreakGhostUtils;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.RewindVisuals;

import java.util.List;
import java.util.Map;

/**
 * Draws the rewind-ghost comet - see {@code mechanics.timeline.RewindVisuals}'s own doc comment for the
 * full "why", and {@code client.rewind.RewindGhostPlayback}'s for why this is a dedicated pair rather than
 * a reuse of {@code StreakGhostRenderer}. Sweeps {@link RewindGhostPlayback.Playback#path} in
 * <b>reverse</b> - {@code progress=0} (just started) samples near the newest end of the path, sweeping down
 * to the oldest end at {@code progress=1} - so it genuinely reads as the last few seconds unwinding
 * backward, not a fixed point.
 * <p>
 * <b>Real, user-requested correction</b>: alpha is now capped at {@link RewindGhostPlayback#MAX_ALPHA}
 * (10%) - it used to reach close to full opacity for the leading ghost, relying on
 * {@link RewindGhostPlayback#TINT} alone to read as a "ghost" rather than a solid recolored duplicate of
 * the real entity. Genuine transparency is the primary cue now; the tint is secondary.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class RewindGhostRenderer
{
	private RewindGhostRenderer()
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

		RewindGhostPlayback.pruneFinished();
		Map<Integer, RewindGhostPlayback.Playback> active = RewindGhostPlayback.getActive();
		if(active.isEmpty())
			return;

		Vec3 camPos = event.getCamera().getPosition();
		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		long gameTime = mc.level.getGameTime();

		boolean rendered = false;

		for(Map.Entry<Integer, RewindGhostPlayback.Playback> entry : active.entrySet())
		{
			if(!(mc.level.getEntity(entry.getKey()) instanceof LivingEntity entity))
				continue;

			RewindGhostPlayback.Playback playback = entry.getValue();
			List<RewindVisuals.PathPoint> path = playback.path();

			float elapsedTicks = (gameTime - playback.startTick()) + partialTick;
			float progress = Mth.clamp(elapsedTicks / playback.durationTicks(), 0F, 1F);

			// Reverse sweep: progress 0 lands on the newest sample (index size-1), progress 1 on the
			// oldest (index 0) - the whole point of this rewrite, see this class's own doc comment.
			float exactIndex = (1F - progress) * (path.size() - 1);

			// Ramp in/out over the first/last 15% of the sweep instead of popping the comet in/out abruptly.
			float envelope = Math.min(1F, Math.min(progress / 0.15F, (1F - progress) / 0.15F));

			for(int offset = 0; offset < RewindGhostPlayback.TRAIL_LENGTH; offset++)
			{
				// The trail's own tail extends toward the direction still being swept into (older samples).
				float sampleIndex = exactIndex - offset;
				if(sampleIndex < 0F)
					break;

				RewindVisuals.PathPoint point = path.get(Math.round(sampleIndex));
				float alpha = RewindGhostPlayback.MAX_ALPHA * envelope * (1F - (float) offset / RewindGhostPlayback.TRAIL_LENGTH);
				if(alpha <= 0F)
					continue;

				int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(point.x(), point.y(), point.z()));

				StreakGhostUtils.renderGhostCopy(entity, point.x() - camPos.x, point.y() - camPos.y, point.z() - camPos.z,
						point.yaw(), alpha, RewindGhostPlayback.TINT, poseStack, bufferSource, light, partialTick);
				rendered = true;
			}
		}

		if(rendered)
			bufferSource.endBatch();
	}
}
