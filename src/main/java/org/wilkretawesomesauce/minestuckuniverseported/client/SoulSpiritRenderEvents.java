package org.wilkretawesomesauce.minestuckuniverseported.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

/**
 * Design doc §5 ("render as the intended translucent/light-blue-or-orange player-head appearance"), as
 * revised by a direct, later user request after a live-client test: a distinct floating-head/box model
 * (this project's original placeholder, {@code entity.PlayerSpiritEntity}) read as "a flying head" - a
 * real, full-body soul render of the player's own model was asked for instead. Since free-Spirit is now
 * just the real player (see {@code mechanics.soul.PlayerSoul}'s own doc comment), that's exactly what
 * this renders: the player's own {@link PlayerModel} (walk cycle, arm swing, everything, reused directly
 * from the real {@link PlayerRenderer} rather than rebuilt), drawn a second time through a translucent,
 * tinted pass instead of the normal opaque one.
 * <p>
 * <b>Three real bugs, all caught from live-client reports, all fixed by reading the actual decompiled
 * source instead of guessing</b> ({@code build/moddev/artifacts/neoforge-21.1.235-sources.jar}, confirmed
 * present in this project's own NeoGradle build output - {@code net.minecraft.client.renderer.entity.
 * player.PlayerRenderer#render}/{@code LivingEntityRenderer#render}/{@code AgeableListModel#renderToBuffer}/
 * {@code EntityModel}, all read directly, not guessed). First attempt: called
 * {@code model.renderToBuffer} straight against {@link RenderPlayerEvent.Pre}'s own {@code PoseStack},
 * assuming (per {@code TechMindCloak.ClientEvents}'s own doc comment, which only ever confirmed
 * <i>position</i> translation) that pose setup had already happened - still upside down. The real source
 * shows {@code PlayerRenderer#render} fires {@code RenderPlayerEvent.Pre} as its very first line and
 * returns immediately if canceled, meaning canceling it skips {@code super.render()} (all of
 * {@code LivingEntityRenderer}'s own scale/rotate/translate/{@code setupAnim} pose work) entirely - at the
 * point {@code Pre} fires, literally none of that has run yet, and the model instance may still be
 * holding stale pose data from whatever it was last used for. Second attempt added just the mirror
 * ({@code scale(-1, -1, 1)} - every vanilla living-entity model is authored Y-down and needs this to read
 * as upright in world space, the same real convention this project's own {@code ConsortHatGeoLayer} had to
 * separately correct for on the GeckoLib side of an unrelated hat bug) and the model-origin shift
 * ({@code translate(0, -1.501, 0)}) - still wrong, because <i>posing</i> the model
 * ({@code model.setupAnim}, which actually sets every limb's rotation for this frame) was never called at
 * all, only the outer transform was. Third attempt reproduced {@code LivingEntityRenderer#render}'s real,
 * relevant sequence directly (skipping only the conditional branches that can never apply to a Spirit -
 * sleeping/spin-attack/death/upside-down-name): body/head yaw and pitch interpolation, the Y-axis body
 * rotation {@code setupRotations} applies, the mirror, the origin shift, and a real
 * {@code prepareMobModel}/{@code setupAnim} call using the player's own live {@code walkAnimation} state -
 * right-side up and correctly posed at last, but rendered as a baby (reported live as "why am I tiny").
 * Root cause: {@code EntityModel#young} is declared {@code public boolean young = true} (confirmed in the
 * real source) - {@code LivingEntityRenderer#render} always resets it per-entity
 * ({@code this.model.young = entity.isBaby()}) before drawing, which this class's own cancel-and-redraw
 * skips entirely, same as {@code riding}/{@code attackTime} - so {@code AgeableListModel#renderToBuffer}
 * (the {@link PlayerModel} ancestor actually implementing {@code renderToBuffer}) took its baby-scaling
 * branch on every single call. Fixed by explicitly resetting all three fields before rendering, the same
 * "reset shared mutable render state before reusing it" idiom {@code LivingEntityRenderer#render} itself
 * already follows for exactly this reason - {@code PlayerRenderer}'s model instance is a shared singleton
 * reused to draw every visible player each frame, not a per-entity object, so anything not explicitly
 * reset can leak stale state from wherever it was last used.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class SoulSpiritRenderEvents
{
	private static final float ALPHA = 0.45F;
	private static final int TINT_RGB = 0x9FD9FF;

	private SoulSpiritRenderEvents()
	{
	}

	@SubscribeEvent
	private static void onRenderPlayerPre(RenderPlayerEvent.Pre event)
	{
		AbstractClientPlayer player = (AbstractClientPlayer) event.getEntity();
		if(!player.hasEffect(MSUMobEffects.SOUL_SPIRIT))
			return;

		event.setCanceled(true);

		PlayerRenderer renderer = event.getRenderer();
		PlayerModel<AbstractClientPlayer> model = renderer.getModel();
		ResourceLocation texture = renderer.getTextureLocation(player);
		float partialTick = event.getPartialTick();

		// EntityModel#young defaults true (confirmed via the real decompiled source, same jar as the
		// pose-transform fix above) - LivingEntityRenderer#render always resets it per-entity
		// (`this.model.young = entity.isBaby()`) before drawing, which our own cancel-and-redraw skips
		// entirely. Without this, AgeableListModel#renderToBuffer takes its baby-scaling branch every
		// time - the real cause of a live "why am I tiny" report.
		model.young = false;
		model.riding = false;
		model.attackTime = 0.0F;

		float bodyRot = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
		float headRot = Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
		float netHeadYaw = Mth.wrapDegrees(headRot - bodyRot);
		float xRot = Mth.lerp(partialTick, player.xRotO, player.getXRot());
		float ageInTicks = player.tickCount + partialTick;

		float limbSwingSpeed = 0.0F;
		float limbSwingPos = 0.0F;
		if(player.isAlive())
		{
			limbSwingSpeed = player.walkAnimation.speed(partialTick);
			limbSwingPos = player.walkAnimation.position(partialTick);
			if(limbSwingSpeed > 1.0F)
				limbSwingSpeed = 1.0F;
		}

		PoseStack poseStack = event.getPoseStack();
		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyRot));
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		poseStack.translate(0.0F, -1.501F, 0.0F);

		model.prepareMobModel(player, limbSwingPos, limbSwingSpeed, partialTick);
		model.setupAnim(player, limbSwingPos, limbSwingSpeed, ageInTicks, netHeadYaw, xRot);

		int argb = ((int) (ALPHA * 255.0F) << 24) | TINT_RGB;
		MultiBufferSource buffer = event.getMultiBufferSource();
		VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(texture));
		model.renderToBuffer(poseStack, vertexConsumer, event.getPackedLight(), OverlayTexture.NO_OVERLAY, argb);

		poseStack.popPose();
	}
}
