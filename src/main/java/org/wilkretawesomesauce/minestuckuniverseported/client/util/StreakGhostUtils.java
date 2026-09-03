package org.wilkretawesomesauce.minestuckuniverseported.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.wilkretawesomesauce.minestuckuniverseported.client.streak.StreakTracker;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Sprint-ghost afterimage helpers for the streak debug/demo effect. Per the recommendation made (and
 * accepted) while planning this feature: ghosts redraw the <b>real</b> tracked entity (correct
 * skin/armor/held items automatically) at each historical position along its recent sprinting path,
 * showing its current live animation/pose rather than a true historical pose snapshot - reusing the
 * exact "no throwaway entity needed, just call {@code EntityRenderDispatcher#render(entity, x, y, z,
 * ...)} at an explicit world position" idiom already validated by {@code client.CloakRenderEvents}.
 * True historical pose replay would require reaching into {@code EntityModel}/
 * {@code LivingEntityRenderer} internals whose 1.21.1 signatures aren't verified - the single most
 * fragile way to build this - so it isn't attempted here.
 */
public final class StreakGhostUtils
{
	private StreakGhostUtils()
	{
	}

	/**
	 * Picks up to {@code count} recent samples, spaced {@code spacingTicks} apart, most-recent-first.
	 * Only samples where the entity was actually sprinting count, unless {@code ignoreSprint} is true -
	 * real gameplay reuse of this system (e.g. {@code TechTimeAccelerateSelf}'s "Accelerate", which has
	 * nothing to do with sprinting) wants every recent sample to count instead.
	 */
	public static List<StreakTracker.Sample> selectGhostSamples(Deque<StreakTracker.Sample> history, int count, int spacingTicks, boolean ignoreSprint)
	{
		List<StreakTracker.Sample> ordered = new ArrayList<>(history);
		List<StreakTracker.Sample> selected = new ArrayList<>(count);

		for(int i = 1; selected.size() < count; i++)
		{
			int index = ordered.size() - 1 - i * spacingTicks;
			if(index < 0)
				break;

			StreakTracker.Sample sample = ordered.get(index);
			if(ignoreSprint || sample.sprinting())
				selected.add(sample);
		}

		return selected;
	}

	/**
	 * Redraws {@code entity} at an explicit historical world position (using its own <i>current</i> live
	 * yaw - what every existing caller here, the rolling sprint-ghost trail, actually wants, matching this
	 * class's own "current pose, not a true historical snapshot" tradeoff) with reduced alpha and an
	 * optional color tint. Delegates to the explicit-yaw overload below.
	 *
	 * @param tint packed RGB multiplied into the shader color - {@code 0xFFFFFF} leaves it untinted
	 */
	public static void renderGhostCopy(Entity entity, double x, double y, double z, float alpha, int tint,
			PoseStack poseStack, MultiBufferSource bufferSource, int light, float partialTick)
	{
		renderGhostCopy(entity, x, y, z, entity.getYRot(), alpha, tint, poseStack, bufferSource, light, partialTick);
	}

	/**
	 * Same as above, but with an explicit {@code yaw} instead of always reading the entity's own live
	 * rotation - for a caller like {@code mechanics.timeline.RewindVisuals}'s rewind-ghost comet, which
	 * genuinely does have a real historical yaw per sample and wants it actually used, unlike the plain
	 * sprint-ghost trail. Real known limitation, not silently ignored: {@code pitch} still isn't
	 * parameterized, because {@link net.minecraft.client.renderer.entity.EntityRenderDispatcher#render}
	 * itself only ever takes one rotation float (yaw) - head/body pitch is read internally from the
	 * entity's own live {@code getXRot()}, with no parameter to override it short of temporarily mutating
	 * the real entity's own rotation fields (a real side-effect risk not taken here) - so a rewind ghost's
	 * facing direction is historically accurate, its head tilt isn't.
	 *
	 * @param tint packed RGB multiplied into the shader color - {@code 0xFFFFFF} leaves it untinted
	 */
	public static void renderGhostCopy(Entity entity, double x, double y, double z, float yaw, float alpha, int tint,
			PoseStack poseStack, MultiBufferSource bufferSource, int light, float partialTick)
	{
		if(alpha <= 0F)
			return;

		float red = ((tint >> 16) & 0xFF) / 255F;
		float green = ((tint >> 8) & 0xFF) / 255F;
		float blue = (tint & 0xFF) / 255F;

		RenderSystem.setShaderColor(red, green, blue, alpha);
		Minecraft.getInstance().getEntityRenderDispatcher()
				.render(entity, x, y, z, yaw, partialTick, poseStack, bufferSource, light);
		RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
	}
}
