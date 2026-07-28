package org.wilkretawesomesauce.minestuckuniverseported.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mraof.minestuck.client.model.entity.FrogModel;
import com.mraof.minestuck.entity.FrogEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.wilkretawesomesauce.minestuckuniverseported.client.ConsortHatClientState;
import software.bernie.geckolib.animatable.GeoItem;

/**
 * Frogs, unlike Consorts, render through a plain vanilla {@code MobRenderer}/{@code FrogModel}
 * ({@code com.mraof.minestuck.client.renderer.entity.frog.FrogRenderer} - confirmed via {@code javap} it
 * does NOT extend GeckoLib's {@code GeoEntityRenderer} like {@code ConsortRenderer} does), so the real
 * extension point here is vanilla's own {@code RenderLayer}, added via
 * {@code net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers} (see
 * {@code client.MSUClientSetup}), not {@link ConsortHatGeoLayer}'s GeckoLib compile-layers event.
 * <p>
 * Same real problem {@link ConsortHatGeoLayer}'s own doc comment describes - rendering the worn
 * {@code ItemStack} directly (a flat, camera-facing GUI-style icon) reads as a floating item, not a worn
 * hat, confirmed via a real screenshot. {@link ConsortHatGeoLayer} fixes this for Consorts by reusing
 * GeckoLib's real {@code ItemArmorGeoLayer}; no GeckoLib-equivalent exists for a plain vanilla
 * non-{@code HumanoidModel} renderer like this one, and vanilla's own {@code HumanoidArmorLayer} can't be
 * reused directly either (it's hard-bound to a {@code RenderLayerParent<T, ? extends HumanoidModel<T>>}
 * parent, which {@code FrogModel} - a {@code HierarchicalModel}, not a {@code HumanoidModel} - isn't). Real,
 * deliberately-scoped substitute: bakes vanilla's own generic {@code ModelLayers#PLAYER_OUTER_ARMOR} head
 * shape once and renders it fitted at the Frog's real {@code "head"} bone, textured with the item's real
 * {@code ArmorMaterial} outer-layer texture - same real vanilla armor asset, just without trim/dye/glint
 * layering (vanilla's own armor-trim/dye pipeline lives entirely inside {@code HumanoidArmorLayer}'s private
 * methods, not something reusable standalone without duplicating a lot of vanilla internals for a feature
 * this project's real hat pool never actually uses trims/dyes for anyway). This generic flat shape is now
 * only the *fallback*, used for hats with no real custom model of their own ({@code crumply_hat} and the
 * two vanilla helmets in {@code ConsortHatsData#HAT_SPAWN_POOL}).
 * <p>
 * {@code wizard_hat}/{@code archmage_hat}/{@code frog_hat} get their own real conical/flat multi-box
 * models instead - not by this class hardcoding which item is which, but by asking the item itself via the
 * same real vanilla/NeoForge mechanism {@code HumanoidArmorLayer} uses:
 * {@link IClientItemExtensions#of(ItemStack)}{@code .getHumanoidArmorModel(entity, stack, slot, original)}.
 * {@code items.WizardHatItem} and siblings already supply their real baked model through exactly this hook
 * (see that class's own doc comment for the texture-mismatch bug those real models fix) - reusing the same
 * query here means <i>any</i> item, ours or a third-party mod's, that properly implements this real,
 * documented extension point gets correctly represented automatically, with no per-item-class dispatch
 * living in this render layer at all. Anything that doesn't override it (every plain vanilla-shaped hat in
 * {@code ConsortHatsData#HAT_SPAWN_POOL}) gets {@code original} echoed straight back - confirmed via
 * {@code javap} that {@link IClientItemExtensions}'s own default implementation is exactly
 * {@code return original;} - so passing {@link #armorHeadModel} as {@code original} is a real, correct
 * fallback, not a guess.
 * <p>
 * <b>Known, honestly-stated gap</b>: Frogs never wore hats in the original 1.12.2 mod at all (Consort-only
 * there), so there's no original numbers/approach to match for the Frog-specific fit/scale here - this is a
 * reasonable, self-consistent "reuse the real head-slot model, scaled onto a small mob" approach, not yet
 * visually verified in a real client. Unlike {@link ConsortHatGeoLayer}, this path renders through vanilla's
 * own {@code ModelPart#translateAndRotate} (no GeckoLib bone space involved), so it needed no upside-down
 * correction of its own - see this class's {@link #render} for where the real, deliberate 0.1%-chance
 * "upside down" variant (see {@code IConsortHatsData}'s own doc comment) is applied instead of corrected.
 */
public class FrogHatLayer extends RenderLayer<FrogEntity, FrogModel<FrogEntity>>
{
	private static final String HEAD_BONE = "head";

	private final HumanoidModel<LivingEntity> armorHeadModel;

	public FrogHatLayer(RenderLayerParent<FrogEntity, FrogModel<FrogEntity>> parent, EntityModelSet modelSet)
	{
		super(parent);
		this.armorHeadModel = new HumanoidModel<>(modelSet.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, FrogEntity entity,
			float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch)
	{
		ItemStack hat = ConsortHatClientState.getHat(entity.getId());
		if(hat.isEmpty() || !(hat.getItem() instanceof ArmorItem armorItem))
			return;

		// GeckoLib-animated armor (e.g. Minestuck's own iron_lass_glasses) has no real appearance as a
		// vanilla ArmorMaterial texture layer - see ConsortHatGeoLayer's own doc comment for the real black-
		// cube bug this avoids (caught from a live Consort, same underlying pickup path a Frog also uses).
		if(armorItem instanceof GeoItem || armorItem.getMaterial().value().layers().isEmpty())
			return;

		ResourceLocation texture = armorItem.getMaterial().value().layers().get(0).texture(false);

		ModelPart head = getParentModel().root().getChild(HEAD_BONE);

		poseStack.pushPose();
		head.translateAndRotate(poseStack);
		poseStack.scale(1.05F, 1.05F, 1.05F);
		// Real, deliberate 0.1%-chance "upside down" variant (see IConsortHatsData's own doc comment) - unlike
		// ConsortHatGeoLayer, this render path has no GeckoLib-vs-vanilla coordinate mismatch to correct, so
		// the flip has to be added explicitly here instead of skipped.
		if(ConsortHatClientState.isHatUpsideDown(entity.getId()))
			poseStack.scale(-1.0F, -1.0F, 1.0F);
		HumanoidModel<?> model = IClientItemExtensions.of(armorItem).getHumanoidArmorModel(entity, hat, EquipmentSlot.HEAD, armorHeadModel);
		model.getHead().render(poseStack, buffer.getBuffer(RenderType.armorCutoutNoCull(texture)), packedLight, OverlayTexture.NO_OVERLAY);
		poseStack.popPose();
	}
}
