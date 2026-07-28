package org.wilkretawesomesauce.minestuckuniverseported.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mraof.minestuck.entity.consort.ConsortEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.wilkretawesomesauce.minestuckuniverseported.client.ConsortHatClientState;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtil;

/**
 * Real, user-requested extension of {@code capabilities.consortCosmetics.ConsortHatsData}'s new chestplate
 * tracking to actual rendering - {@link ConsortHatGeoLayer} retargeted at the {@code "chest"} bone instead
 * of {@code "face"}/{@code "waist"}, same real bake-a-generic-vanilla-body-shape-or-the-item's-own-real-
 * model approach via {@link IClientItemExtensions#getHumanoidArmorModel}, same {@link GeoItem} skip for
 * GeckoLib-animated armor, same {@link RenderUtil#translateAndRotateMatrixForBone} positioning. Renders
 * {@link HumanoidModel#body} instead of {@link HumanoidModel#getHead()}.
 * <p>
 * <b>Position/scale, derived from the real geo model, not guessed</b>: all four real Consort species geo
 * models ({@code assets/minestuck/geo/entity/consort/*.geo.json}, read directly, not assumed from one)
 * share an identical {@code "chest"} bone: pivot {@code (0, 8.75, 2)}, cube
 * {@code origin: (-4, 8.75, -2), size: (8, 4, 4)} - i.e., relative to the pivot, {@code x:[-4,4], y:[0,4],
 * z:[-4,0]}, an 8-wide/4-deep box matching vanilla's generic body cube
 * ({@code addBox(-4,0,-2,8,12,4)}) exactly on X/Z, but only a third as tall (4 vs 12 - this bone covers
 * just the upper chest, not the full torso), hence the {@code -0.3333} Y scale (4/12, the same real-ratio
 * approach {@code ImpHatGeoLayer} used for its own head-bone mismatch, not a round guess). Solving
 * {@code scale⊙(vanillaBodyCube + translate) == chestCubeRelativeToPivot} (this class calls
 * {@code scale()} before {@code translate()}, matching {@link ConsortHatGeoLayer}'s own literal call order
 * - so the translate offset is added in raw {@code ModelPart} space, then the whole sum gets scaled/
 * mirrored, unlike {@code ImpHatGeoLayer}'s opposite call order) for both the Y extremes (0 and 12px) and
 * the Z center gives {@code translate = (0, -0.75, -0.125)} blocks.
 * <p>
 * <b>Not yet visually verified in a real client</b>: a real derivation from the geo.json's actual numbers,
 * not a guess, but hasn't been screenshot-confirmed - same caveat as every other render-layer change in
 * this project's history, doubly so here since {@link ConsortHatGeoLayer}'s own doc comment records three
 * real wrong attempts before its face/waist positioning was actually correct.
 */
public class ConsortChestGeoLayer<T extends ConsortEntity> extends GeoRenderLayer<T>
{
	private static final String CHEST_BONE = "chest";

	private final HumanoidModel<LivingEntity> armorBodyModel;

	public ConsortChestGeoLayer(GeoRenderer<T> renderer)
	{
		super(renderer);
		this.armorBodyModel = new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
	}

	@Override
	public void renderForBone(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType,
			MultiBufferSource bufferSource, VertexConsumer buffer,
			float partialTick, int packedLight, int packedOverlay)
	{
		if(!CHEST_BONE.equals(bone.getName()))
			return;

		ItemStack chest = ConsortHatClientState.getChest(animatable.getId());
		if(chest.isEmpty() || !(chest.getItem() instanceof ArmorItem armorItem))
			return;

		if(armorItem instanceof GeoItem || armorItem.getMaterial().value().layers().isEmpty())
			return;

		ResourceLocation texture = armorItem.getMaterial().value().layers().get(0).texture(false);

		poseStack.pushPose();
		RenderUtil.translateAndRotateMatrixForBone(poseStack, bone);
		poseStack.scale(-1.0F, -0.3333F, 1.0F);
		poseStack.translate(0.0D, -0.75D, -0.125D);

		HumanoidModel<?> model = IClientItemExtensions.of(armorItem).getHumanoidArmorModel(animatable, chest, EquipmentSlot.CHEST, armorBodyModel);
		model.body.render(
				poseStack,
				bufferSource.getBuffer(RenderType.armorCutoutNoCull(texture)),
				packedLight,
				OverlayTexture.NO_OVERLAY
		);

		poseStack.popPose();
	}
}
