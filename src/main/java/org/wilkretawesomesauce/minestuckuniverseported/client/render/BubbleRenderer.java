package org.wilkretawesomesauce.minestuckuniverseported.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.client.model.MSUModelLayers;
import org.wilkretawesomesauce.minestuckuniverseported.entity.BubbleEntity;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code client.render.RenderBubble} - renders
 * {@link BubbleEntity}'s single cube ({@link BubbleModel}) scaled to the bubble's current size, tinted
 * by its color, with alpha fading in on spawn and out near the end of its life (matches the original's
 * own {@code Math.min(1, ticksExisted/20f) * (lifespan < 0 ? 1 : Math.min(1, lifespan/10f))} formula,
 * see {@link BubbleEntity#getAlpha()}).
 * <p>
 * <b>Real bug fixed, traced against the original's actual scale math rather than guessed</b>: the
 * original computed its final on-screen size as {@code (bubbleSize*2) * 0.03125F} (an outer
 * {@code GlStateManager.scale} times a legacy per-vertex model scale passed into
 * {@code ModelBubble#render}) - those two factors net out to exactly {@code bubbleSize} blocks, since
 * {@code 2 * 0.03125 = 0.0625 = 1/16}, the same implicit "16 model units = 1 block" convention modern
 * {@link ModelPart#render} already bakes in on its own with no extra multiplier needed. An earlier pass
 * of this port divided by 8 instead of scaling by the bubble size directly, and added a vertical
 * translate the original never had - together these rendered every bubble at roughly 1/20th its real
 * size. Fixed to just {@code poseStack.scale(size, size, size)} with {@code size = getBubbleSize()}
 * directly, no extra translate needed here - see {@link BubbleModel}'s own doc comment for a real,
 * separate positioning bug (box spanning the wrong direction from the entity anchor, not this class)
 * found and fixed later.
 */
public class BubbleRenderer extends EntityRenderer<BubbleEntity>
{
	private static final ResourceLocation TEXTURE = Minestuckuniverseported.id("textures/entity/bubble.png");

	private final ModelPart model;

	public BubbleRenderer(EntityRendererProvider.Context context)
	{
		super(context);
		this.model = context.bakeLayer(MSUModelLayers.BUBBLE);
	}

	@Override
	public void render(BubbleEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight)
	{
		poseStack.pushPose();

		float size = entity.getBubbleSize();
		poseStack.scale(size, size, size);

		int color = entity.getColor();
		float red = ((color >> 16) & 0xFF) / 255.0F;
		float green = ((color >> 8) & 0xFF) / 255.0F;
		float blue = (color & 0xFF) / 255.0F;
		float alpha = entity.getAlpha();

		var vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
		int argb = ((int) (alpha * 255.0F) << 24) | ((int) (red * 255.0F) << 16) | ((int) (green * 255.0F) << 8) | (int) (blue * 255.0F);
		model.render(poseStack, vertexConsumer, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, argb);

		poseStack.popPose();
		super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
	}

	@Override
	public ResourceLocation getTextureLocation(BubbleEntity entity)
	{
		return TEXTURE;
	}
}
