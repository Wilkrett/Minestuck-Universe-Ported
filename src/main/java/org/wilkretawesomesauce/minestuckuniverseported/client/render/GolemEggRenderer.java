package org.wilkretawesomesauce.minestuckuniverseported.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.wilkretawesomesauce.minestuckuniverseported.MSUItems;
import org.wilkretawesomesauce.minestuckuniverseported.entity.GolemEggEntity;

/**
 * Ported from ModularBosses (1.8)'s real registration for {@code EntityCustomEgg} -
 * {@code RenderSnowball(manager, ModularBossesItems.spawn_egg, itemRender)}, a billboarded icon of a
 * fixed item (not a per-entity {@code ItemStack}, unlike vanilla's own snowball/egg). This is the same
 * "billboard an item icon" technique vanilla's own {@code ThrownItemRenderer} uses internally
 * ({@code ItemRenderer#renderStatic} in {@link ItemDisplayContext#GROUND}), just built directly against
 * a fixed {@link MSUItems#GOLEM_SPAWN_EGG} stack instead of reading one off the entity, since
 * {@link GolemEggEntity} deliberately doesn't carry a real {@code ItemStack} - see that class's own doc
 * comment for why.
 */
public class GolemEggRenderer extends EntityRenderer<GolemEggEntity>
{
	private final ItemRenderer itemRenderer;

	public GolemEggRenderer(EntityRendererProvider.Context context)
	{
		super(context);
		this.itemRenderer = context.getItemRenderer();
	}

	@Override
	public void render(GolemEggEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight)
	{
		poseStack.pushPose();
		poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
		itemRenderer.renderStatic(new ItemStack(MSUItems.GOLEM_SPAWN_EGG.get()), ItemDisplayContext.GROUND,
				packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
		poseStack.popPose();
		super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
	}

	@Override
	public ResourceLocation getTextureLocation(GolemEggEntity entity)
	{
		return TextureAtlas.LOCATION_BLOCKS;
	}
}
