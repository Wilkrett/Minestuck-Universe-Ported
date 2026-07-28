package org.wilkretawesomesauce.minestuckuniverseported.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mraof.minestuck.entity.consort.ConsortEntity;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
 * Closes the real, permanent-until-now gap {@code capabilities.consortCosmetics.ConsortHatsData}'s own doc
 * comment calls out. Ported to match the original 1.12.2 mod's own real approach
 * ({@code client.layers.LayerConsortCosmetics}, read directly from the extracted original source, not
 * guessed): for any worn hat that's an {@code ItemArmor} (every entry in
 * {@code ConsortHatsData#HAT_SPAWN_POOL} is one), the original rendered the item's real <i>armor</i> texture
 * on a generic biped head cube fitted onto the Consort's own head, not a flat item icon.
 * <p>
 * <b>Two real attempts before this one, both wrong, both caught the hard way (real screenshots, not
 * reasoning)</b>: a first version used {@link software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer}
 * with {@code ItemDisplayContext.HEAD}, which renders a flat, camera-facing GUI-style icon for a plain
 * {@code ArmorItem} (no special head-context model exists for ordinary armor, only vanilla skulls) - a
 * floating helmet, not a worn one. A second version switched to GeckoLib's own
 * {@link software.bernie.geckolib.renderer.layer.ItemArmorGeoLayer} (its built-in vanilla-armor-on-a-bone
 * layer), which rendered nothing at all - its internal branching (whole-model-vs-{@code GeoArmorRenderer}
 * detection, {@code IClientItemExtensions}-routed model resolution) is built around GeckoLib's own
 * animated-armor-item ecosystem and never produced visible output for a plain vanilla {@code ArmorItem}
 * here, and its exact failure point wasn't worth fully reverse-engineering when a much simpler, fully
 * self-controlled approach - the same one {@link FrogHatLayer} already uses successfully for Frogs - covers
 * the real need directly: bake vanilla's own generic {@code ModelLayers#PLAYER_OUTER_ARMOR} head shape once
 * and render it manually at the bone's position with the item's real armor texture bound, no GeckoLib
 * armor-layer indirection at all. Extends the bare {@link GeoRenderLayer} base (not either of the above)
 * and positions itself via GeckoLib's own real {@link RenderUtil#translateAndRotateMatrixForBone} utility -
 * the same one {@code BlockAndItemGeoLayer}/{@code ItemArmorGeoLayer} use internally, confirmed via
 * {@code javap} against this project's real GeckoLib dependency jar.
 * <p>
 * Targets the {@code "head"} bone, confirmed present under that exact name in all four real Consort species
 * geo models (`assets/minestuck/geo/entity/consort/*.geo.json` - iguana/nakagator/salamander/turtle all read
 * directly, not assumed from one), attached via GeckoLib's real
 * {@code GeoRenderEvent.Entity.CompileRenderLayers} extension point in {@link ConsortHatRenderEvents}.
 * <p>
 * <b>Third real bug, also caught via a real screenshot</b>: the hat rendered right-side-up-shaped but
 * upside-down. Confirmed via {@code javap} against GeckoLib's own (unused here) {@code ItemArmorGeoLayer}
 * that it applies a {@code poseStack.scale(-1, -1, 1)} correction before touching a vanilla
 * {@code HumanoidModel} part, because GeckoLib bone space is mirrored on X/Y relative to vanilla
 * {@code ModelPart} space - {@link #renderForBone} now applies the same correction.
 * <p>
 * <b>Known open issue</b>: with the real conical {@code WizardHatModel}/{@code ArchmageHatModel} now
 * rendered here instead of the old generic flat box, the archmage hat renders as a wide floppy disc (just
 * the brim) with the tall cone missing/hidden - a real, confirmed-via-screenshot regression this class's
 * existing {@code (-1,-1,1)} correction doesn't handle correctly for tall, directional geometry the way it
 * did for the old small box. An attempted fix (dropping the Y flip) made it render upside-down instead, so
 * that attempt was reverted rather than left half-verified - the real fix is still open.
 * <p>
 * <b>Model selection, real fix over an earlier hardcoded-dispatch draft</b>: which {@link HumanoidModel} to
 * render is no longer this class deciding "is this a WizardHatItem/ArchmageHatItem/FrogHatItem" by
 * {@code instanceof} - it now asks the item itself via the same real vanilla/NeoForge mechanism
 * {@code HumanoidArmorLayer} uses: {@link IClientItemExtensions#of(ItemStack)}
 * {@code .getHumanoidArmorModel(entity, stack, slot, original)}. {@code items.WizardHatItem} and siblings
 * already supply their real baked model through exactly this hook, so reusing the query here means
 * <i>any</i> item - ours or a third-party mod's - that properly implements this real, documented extension
 * point gets correctly represented automatically, with zero per-item-class knowledge living in this render
 * layer. Anything that doesn't override it (every plain vanilla-shaped hat in
 * {@code ConsortHatsData#HAT_SPAWN_POOL}) gets {@code original} echoed straight back - confirmed via
 * {@code javap} that {@link IClientItemExtensions}'s own default implementation is exactly
 * {@code return original;} - so passing {@link #armorHeadModel} as {@code original} is a real, correct
 * fallback, not a guess. This is a strictly narrower fix than it might sound: GeckoLib-animated armor still
 * needs the separate {@link GeoItem} skip above regardless, since it doesn't render through a
 * {@link HumanoidModel} at all and so has nothing meaningful to hand back through this hook either way.
 */
public class ConsortHatGeoLayer<T extends ConsortEntity> extends GeoRenderLayer<T>
{
	private static final String FACE_BONE = "face";
	private static final String WAIST_BONE = "waist";

	private final HumanoidModel<LivingEntity> armorHeadModel;

	public ConsortHatGeoLayer(GeoRenderer<T> renderer)
	{
		super(renderer);
		this.armorHeadModel = new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
	}

	@Override
	public void renderForBone(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType,
			MultiBufferSource bufferSource, VertexConsumer buffer,
			float partialTick, int packedLight, int packedOverlay)
	{
		boolean upsideDown = ConsortHatClientState.isHatUpsideDown(animatable.getId());
		String targetBone = upsideDown ? WAIST_BONE : FACE_BONE;

		if (!targetBone.equals(bone.getName()))
			return;

		ItemStack hat = ConsortHatClientState.getHat(animatable.getId());
		if(hat.isEmpty() || !(hat.getItem() instanceof ArmorItem armorItem))
			return;

		if(armorItem instanceof GeoItem || armorItem.getMaterial().value().layers().isEmpty())
			return;

		ResourceLocation texture = armorItem.getMaterial().value().layers().get(0).texture(false);

		poseStack.pushPose();
		RenderUtil.translateAndRotateMatrixForBone(poseStack, bone);
		poseStack.mulPose(Axis.XP.rotationDegrees(15.0F));

		if (upsideDown) {
			poseStack.translate(0.0D, 0.25F, 0.0D);
		} else {
			poseStack.scale(-1.0F, -1.0F, 1.0F);
			poseStack.translate(0.0D, 0.2D, 0.0D);
		}

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
