package org.wilkretawesomesauce.minestuckuniverseported.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mraof.minestuck.entity.underling.ImpEntity;
import com.mraof.minestuck.entity.underling.UnderlingEntity;
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
 * Real, user-requested extension of {@code capabilities.consortCosmetics.ConsortHatsData}'s pickup-a-hat
 * mechanic to Imps - {@code ConsortHatsData#isHatCapable} already covers the data/pickup side (no original
 * counterpart to port; Imps never wore hats in the original 1.12.2 mod, same "no original numbers to
 * match" category as {@code FrogHatLayer}'s own Frog support). This class is the render-side half,
 * essentially {@link ConsortHatGeoLayer} retargeted: same real bake-a-generic-vanilla-head-shape-or-the-
 * item's-own-real-model approach via {@link IClientItemExtensions#getHumanoidArmorModel}, same real
 * {@link GeoItem} skip for GeckoLib-animated armor (see that class's own doc comment for the black-cube bug
 * this avoids), same real {@link RenderUtil#translateAndRotateMatrixForBone} positioning.
 * <p>
 * <b>Real difference from Consorts</b>: Imps share one generic {@code UnderlingRenderer<T>} across every
 * Underling species (confirmed via {@code javap} - unlike Consorts' one-renderer-per-species
 * {@code ConsortRenderer}), so {@link ImpHatRenderEvents} attaches this layer to every
 * {@code UnderlingRenderer} instance regardless of species, and {@link #renderForBone} itself gates on
 * {@code animatable instanceof ImpEntity} to make sure only real Imps ever actually render a hat. Imp's own
 * real geo model ({@code assets/minestuck/geo/entity/underlings/imp.geo.json}, read directly) only has a
 * plain {@code "head"} bone - no separate {@code "face"}/{@code "waist"} bones the way Turtle Consorts do -
 * so there's no bone-swap for the upside-down variant here; the mirror correction below is just skipped for
 * that case instead, closer to {@link ConsortHatGeoLayer}'s own original (pre-face/waist-split) shape.
 * <p>
 * <b>Position/scale, real fix over an earlier Consort-borrowed guess</b>: the offsets below are derived
 * directly from Imp's own real geo model ({@code imp.geo.json}, read directly), not copied from Consort's
 * tuned values - a real screenshot showed the borrowed offsets left the hat floating well off the head.
 * The {@code head} bone's pivot sits at {@code (0, 18.33333, -2.16733)}; its own cube spans
 * {@code x:[-4,4], y:[18.5,24.5], z:[-8.25,-0.25]} - i.e., relative to the pivot, {@code x:[-4,4],
 * y:[0.167,6.167], z:[-6.083,1.917]}, a 8x6x8 box. Vanilla's generic head cube (what
 * {@code armorHeadModel}/{@code getHumanoidArmorModel} renders) is {@code addBox(-4,-8,-4,8,8,8)} in its
 * own {@code ModelPart}-space - 8 wide/deep, matching Imp's cube exactly on X/Z (no translate needed on
 * either axis beyond re-centering Z), but 8 tall against Imp's 6-tall head, hence the {@code -0.75} Y scale
 * (6/8). Given {@link RenderUtil#translateAndRotateMatrixForBone} lands the pose stack at the bone pivot
 * and {@link #renderForBone} applies {@code translate} before {@code scale} (so the translate vector lands
 * in bone-local space, independent of the mirror/compression scale applied after), solving
 * {@code translate + scale⊙vanillaHeadCube == impHeadCubeRelativeToPivot} for both the Y extremes (top
 * -8px, bottom 0px) and the Z center gives {@code translate = (0, 1/96, -2.08267/16)} blocks - not the
 * round {@code -2/16}/{@code -1/16} guesses the borrowed Consort offsets used.
 * <p>
 * <b>Not yet visually verified in a real client</b>: this is a real derivation from the geo.json's actual
 * numbers, not a guess, but hasn't been screenshot-confirmed after the fix - same "needs a real screenshot"
 * caveat as every other render-layer change in this project's history.
 */
public class ImpHatGeoLayer<T extends UnderlingEntity> extends GeoRenderLayer<T>
{
	private static final String HEAD_BONE = "head";

	private final HumanoidModel<LivingEntity> armorHeadModel;

	public ImpHatGeoLayer(GeoRenderer<T> renderer)
	{
		super(renderer);
		this.armorHeadModel = new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
	}

	@Override
	public void renderForBone(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType,
			MultiBufferSource bufferSource, VertexConsumer buffer,
			float partialTick, int packedLight, int packedOverlay)
	{
		if(!(animatable instanceof ImpEntity))
			return;

		if(!HEAD_BONE.equals(bone.getName()))
			return;

		ItemStack hat = ConsortHatClientState.getHat(animatable.getId());
		if(hat.isEmpty() || !(hat.getItem() instanceof ArmorItem armorItem))
			return;

		if(armorItem instanceof GeoItem || armorItem.getMaterial().value().layers().isEmpty())
			return;

		ResourceLocation texture = armorItem.getMaterial().value().layers().get(0).texture(false);

		poseStack.pushPose();
		RenderUtil.translateAndRotateMatrixForBone(poseStack, bone);
		poseStack.translate(0.0D, 1.0D / 96.0D, -2.08267D / 16.0D);

		poseStack.scale(-1.0F, -0.75F, 1.0F);

		HumanoidModel<?> model = IClientItemExtensions.of(armorItem).getHumanoidArmorModel(animatable, hat, EquipmentSlot.HEAD, armorHeadModel);
		model.getHead().render(
				poseStack,
				bufferSource.getBuffer(RenderType.armorCutoutNoCull(texture)),
				packedLight,
				OverlayTexture.NO_OVERLAY
		);

		poseStack.popPose();
	}
}
