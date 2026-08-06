package org.wilkretawesomesauce.minestuckuniverseported.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.wilkretawesomesauce.minestuckuniverseported.entity.GolemFallingBlockEntity;

/**
 * Ported from ModularBosses (1.8)'s {@code client.render.entity.RenderCustomFallingBlock} - a plain
 * single-block model render at the entity's own position (no spin, unlike {@link GolemBoulderRenderer}'s
 * deliberately-added flourish for the thrown boulder), matching the original's own straightforward
 * {@code BlockRendererDispatcher#getBlockModelRenderer().renderModel(...)} call.
 */
public class GolemFallingBlockRenderer extends EntityRenderer<GolemFallingBlockEntity>
{
	private final BlockRenderDispatcher dispatcher;

	public GolemFallingBlockRenderer(EntityRendererProvider.Context context)
	{
		super(context);
		this.dispatcher = context.getBlockRenderDispatcher();
	}

	@Override
	public void render(GolemFallingBlockEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight)
	{
		poseStack.pushPose();
		poseStack.translate(-0.5, 0.0, -0.5);
		dispatcher.renderSingleBlock(entity.getMimicBlock(), poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
		poseStack.popPose();
		super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
	}

	@Override
	public ResourceLocation getTextureLocation(GolemFallingBlockEntity entity)
	{
		return TextureAtlas.LOCATION_BLOCKS;
	}
}
