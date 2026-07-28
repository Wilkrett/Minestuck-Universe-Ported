package org.wilkretawesomesauce.minestuckuniverseported.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.item.AbilitechnosynthItem;

/**
 * Real placement-preview outline for {@link AbilitechnosynthItem} - the "boundary indicator from base
 * minestuck" the user asked to add here. Minestuck's own real
 * {@code com.mraof.minestuck.client.renderer.MachineOutlineRenderer#renderCheckItem} (confirmed via
 * {@code javap}) only ever fires for a held {@code com.mraof.minestuck.item.block.MultiblockItem} - a
 * hardcoded {@code instanceof} check - and never for {@link AbilitechnosynthItem}, which is a plain
 * {@link net.minecraft.world.item.BlockItem} with its own bespoke 16-position placement math (see that
 * class's own doc comment), not a real {@code MultiblockItem}. This is a from-scratch equivalent using the
 * same real {@link RenderLevelStageEvent}/{@link LevelRenderer#renderLineBox} approach already established
 * by this project's own {@code SpaceManipulatorClientEvents}, rather than trying to hook into Minestuck's
 * private, {@code MultiblockItem}-specific plumbing.
 * <p>
 * <b>Real bug fix, caught from a side-by-side screenshot against the real original</b>: an earlier version
 * of this class drew all 16 individual per-cell boxes ({@link AbilitechnosynthItem#getPlacementPositions}) -
 * a jumbled wireframe mess, not what the original ever looked like. The real original always draws exactly
 * one clean bounding-box outline per placed multiblock ({@code MachineOutlineRenderer}'s own real
 * {@code drawPhernaliaPlacementOutline} calls, one per {@code MachineMultiblock#getBoundingBox}), so this now
 * draws a single box over {@link AbilitechnosynthItem#getPlacementEnvelope} instead. Same screenshot also
 * caught a color bug in the same earlier version: it used white for a valid placement, but the real
 * original's own bytecode (re-read carefully this time, confirmed via {@code javap -c}) is
 * <b>green when valid, red when invalid</b> - corrected here to match exactly, including the real
 * {@code 0.5F} alpha literal read directly out of that same bytecode.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class AbilitechnosynthPreviewClientEvents
{
	private AbilitechnosynthPreviewClientEvents()
	{
	}

	@SubscribeEvent
	private static void onRenderLevel(RenderLevelStageEvent event)
	{
		if(event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
			return;

		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if(mc.level == null || player == null)
			return;

		if(!(player.getMainHandItem().getItem() instanceof AbilitechnosynthItem)
				&& !(player.getOffhandItem().getItem() instanceof AbilitechnosynthItem))
			return;

		if(!(mc.hitResult instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK
				|| blockHit.getDirection() != Direction.UP)
			return;

		BlockPos pos = blockHit.getBlockPos();
		if(!mc.level.getBlockState(pos).canBeReplaced())
			pos = pos.above();

		Direction facing = player.getDirection().getOpposite();
		boolean valid = AbilitechnosynthItem.canPlaceAt(mc.level, pos, facing);

		Vec3 camPos = event.getCamera().getPosition();
		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
		VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());

		AABB box = AbilitechnosynthItem.getPlacementEnvelope(pos, facing).move(-camPos.x, -camPos.y, -camPos.z);
		LevelRenderer.renderLineBox(poseStack, consumer, box, valid ? 0F : 1F, valid ? 1F : 0F, 0F, 0.5F);

		bufferSource.endBatch(RenderType.lines());
	}
}
