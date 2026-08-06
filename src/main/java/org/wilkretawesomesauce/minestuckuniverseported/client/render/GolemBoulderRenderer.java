package org.wilkretawesomesauce.minestuckuniverseported.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.wilkretawesomesauce.minestuckuniverseported.entity.GolemBoulderEntity;

/**
 * Renders {@link GolemBoulderEntity} as a spinning single block of whatever it's mimicking, reusing
 * vanilla's own {@code BlockRenderDispatcher#renderSingleBlock} - the same real block-model path
 * {@code FallingBlockRenderer} builds on, just the simpler public entry point since this entity doesn't
 * need that class's "don't render while sitting exactly on its origin block" check.
 */
public class GolemBoulderRenderer extends EntityRenderer<GolemBoulderEntity>
{
	private final BlockRenderDispatcher dispatcher;

	public GolemBoulderRenderer(EntityRendererProvider.Context context)
	{
		super(context);
		this.dispatcher = context.getBlockRenderDispatcher();
	}

	@Override
	public void render(GolemBoulderEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight)
	{
		poseStack.pushPose();
		poseStack.translate(-0.25, 0.0, -0.25);
		poseStack.scale(0.5F, 0.5F, 0.5F);
		poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees((entity.tickCount + partialTick) * 25.0F));
		poseStack.translate(-0.5, -0.5, -0.5);
		dispatcher.renderSingleBlock(entity.getMimicBlock(), poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
		poseStack.popPose();
		super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
	}

	@Override
	public ResourceLocation getTextureLocation(GolemBoulderEntity entity)
	{
		return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
	}
}
