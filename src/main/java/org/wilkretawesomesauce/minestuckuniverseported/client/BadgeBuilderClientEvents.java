package org.wilkretawesomesauce.minestuckuniverseported.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mraof.minestuck.computer.editmode.ClientEditmodeData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.network.BadgeBuilderFillPacket;

import javax.annotation.Nullable;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.badges.BadgeBuilder}'s own
 * {@code onClientTick}/{@code onRightClickBlock}/{@code renderOutline} - client-only drag-select tracking
 * for the real fill mechanic ({@code network.BadgeBuilderFillPacket}, the sole thing actually sent to the
 * server; this class never places a block itself). Qualification mirrors the original's own
 * {@code canEditDrag}: holding a real {@link BlockItem} in either hand, and either
 * {@link BuilderBadgeClientState#isActive()} (see that class's own doc comment for why a dedicated sync
 * packet exists at all) or already in real Minestuck Edit Mode
 * ({@link ClientEditmodeData#isInEditmode()}, the modern equivalent of the original's own
 * {@code ClientEditHandler.isActive()}, confirmed via {@code javap} against this project's pinned
 * Minestuck dependency - the old boolean-returning method doesn't exist anymore, this project's own
 * dependency jar's real replacement does).
 * <p>
 * Cancels normal right-click block placement while qualified (same reasoning as the original's own
 * {@code HIGHEST}-priority cancel) so a single click doesn't both place one block the vanilla way <i>and</i>
 * queue up this tool's own 1x1x1 fill - the drag tool is a full replacement for normal placement while its
 * gate is satisfied, not an addition on top of it, matching the original's own actual behavior.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class BadgeBuilderClientEvents
{
	private static BlockPos pos1;
	private static BlockPos pos2;
	private static boolean dragging;

	private BadgeBuilderClientEvents()
	{
	}

	@SubscribeEvent
	private static void onClientTick(ClientTickEvent.Post event)
	{
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;

		if(player == null || mc.screen != null || !qualifies(player))
		{
			cancelDrag();
			return;
		}

		boolean isDown = mc.options.keyUse.isDown();

		if(isDown)
		{
			BlockPos target = targetBlockPos(mc);

			if(!dragging)
			{
				if(target == null)
					return;
				pos1 = target;
				pos2 = target;
				dragging = true;
			}
			else if(target != null)
				pos2 = target;
		}
		else if(dragging)
		{
			if(pos1 != null && pos2 != null && mc.hitResult instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK)
				PacketDistributor.sendToServer(new BadgeBuilderFillPacket(pos1, pos2, blockHit.getLocation(), blockHit.getDirection()));
			cancelDrag();
		}
	}

	@SubscribeEvent
	private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event)
	{
		if(event.getEntity() instanceof LocalPlayer localPlayer && qualifies(localPlayer))
			event.setCanceled(true);
	}

	@SubscribeEvent
	private static void onRenderLevel(RenderLevelStageEvent event)
	{
		if(event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !dragging || pos1 == null || pos2 == null)
			return;

		Minecraft mc = Minecraft.getInstance();

		AABB box = new AABB(
				Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()), Math.min(pos1.getZ(), pos2.getZ()),
				Math.max(pos1.getX(), pos2.getX()) + 1, Math.max(pos1.getY(), pos2.getY()) + 1, Math.max(pos1.getZ(), pos2.getZ()) + 1);

		Vec3 camPos = event.getCamera().getPosition();
		box = box.move(-camPos.x, -camPos.y, -camPos.z);

		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
		VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());

		LevelRenderer.renderLineBox(poseStack, consumer, box, 0F, 1F, 0F, 0.6F);

		bufferSource.endBatch(RenderType.lines());
	}

	private static void cancelDrag()
	{
		pos1 = null;
		pos2 = null;
		dragging = false;
	}

	private static boolean qualifies(LocalPlayer player)
	{
		boolean holdingBlock = player.getMainHandItem().getItem() instanceof BlockItem || player.getOffhandItem().getItem() instanceof BlockItem;
		return holdingBlock && (BuilderBadgeClientState.isActive() || ClientEditmodeData.isInEditmode());
	}

	@Nullable
	private static BlockPos targetBlockPos(Minecraft mc)
	{
		if(!(mc.hitResult instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK || mc.level == null)
			return null;

		BlockState state = mc.level.getBlockState(blockHit.getBlockPos());
		return state.canBeReplaced() ? blockHit.getBlockPos() : blockHit.getBlockPos().relative(blockHit.getDirection());
	}
}
