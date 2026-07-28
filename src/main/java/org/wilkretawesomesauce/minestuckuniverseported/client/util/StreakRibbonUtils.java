package org.wilkretawesomesauce.minestuckuniverseported.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Pure math/geometry helpers for the streak ribbon trail, extracted from iChun's Streak's own
 * decompiled {@code StreakTag#createFor}/{@code StreakTag#render} bytecode (see {@code streak}
 * package's own doc comment for why this had to be decompiled rather than read from source).
 */
public final class StreakRibbonUtils
{
	private StreakRibbonUtils()
	{
	}

	/** Matches the original's {@code createFor} texU accumulation: distance travelled since the last sample, divided by entity height. */
	public static float texUDelta(double dx, double dz, float height)
	{
		if(height <= 0F)
			return 0F;
		return (float) (Math.sqrt(dx * dx + dz * dz) / height);
	}

	/**
	 * A simplified, single-value-per-sample version of the original's alpha ramp - the original computed
	 * two independent, tick-offset-by-one alpha values per segment (one per endpoint); this instead gives
	 * each recorded sample one alpha (shared by the two segments touching it), combining a 5-tick fade-in
	 * from the trail's head and a 20-tick fade-out towards its tail (both divisors matched exactly from
	 * the decompiled bytecode) via a simple minimum rather than reproducing the original's segment-local
	 * dual-value scheme.
	 *
	 * @param age                how many ticks old this sample is (0 = the most recently recorded)
	 * @param totalVisibleSamples how many samples currently make up the visible trail
	 */
	public static float fadeAlpha(int age, int totalVisibleSamples)
	{
		float alpha = 1F;
		if(age < 5)
			alpha = age / 5F;

		int fromTail = totalVisibleSamples - 1 - age;
		if(fromTail < 20)
			alpha = Math.min(alpha, Math.max(0F, fromTail / 20F));

		return Math.max(0F, Math.min(1F, alpha));
	}

	/**
	 * Emits one ribbon quad between two consecutive samples, in the exact vertex order decompiled from
	 * the original (bottom-current, top-current, top-previous, bottom-previous; V=1 at the bottom, V=0
	 * at the top). All positions are expected already camera-relative (the {@link PoseStack} translated
	 * by {@code -camPos}, matching {@code client.render.BeamRenderer}'s own convention).
	 * <p>
	 * U is wrapped to {@code [0,1)} in software rather than relying on GL hardware texture-repeat state
	 * - flavour textures are standalone files, not atlas-stitched, and configuring wrap mode for a custom
	 * {@link net.minecraft.client.renderer.RenderType} is unverified - so this may show an occasional
	 * visual seam at wrap boundaries, an accepted minor simplification.
	 */
	public static void emitRibbonQuad(VertexConsumer consumer, PoseStack.Pose pose, int light,
			float curX, float curY, float curZ, float curHeight, float curU, float curAlpha,
			float prevX, float prevY, float prevZ, float prevHeight, float prevU, float prevAlpha)
	{
		float wrappedCurU = curU - (float) Math.floor(curU);
		float wrappedPrevU = prevU - (float) Math.floor(prevU);

		consumer.addVertex(pose, curX, curY, curZ)
				.setColor(1F, 1F, 1F, curAlpha)
				.setUv(wrappedCurU, 1F)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(pose, 0F, 0F, 1F);

		consumer.addVertex(pose, curX, curY + curHeight, curZ)
				.setColor(1F, 1F, 1F, curAlpha)
				.setUv(wrappedCurU, 0F)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(pose, 0F, 0F, 1F);

		consumer.addVertex(pose, prevX, prevY + prevHeight, prevZ)
				.setColor(1F, 1F, 1F, prevAlpha)
				.setUv(wrappedPrevU, 0F)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(pose, 0F, 0F, 1F);

		consumer.addVertex(pose, prevX, prevY, prevZ)
				.setColor(1F, 1F, 1F, prevAlpha)
				.setUv(wrappedPrevU, 1F)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(pose, 0F, 0F, 1F);
	}
}
