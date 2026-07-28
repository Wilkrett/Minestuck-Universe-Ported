package org.wilkretawesomesauce.minestuckuniverseported.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.wilkretawesomesauce.minestuckuniverseported.MSUItemComponents;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.item.ManipulatedMatterItem;

/**
 * Real placement-preview outline for a held, filled {@link ManipulatedMatterItem} - same
 * {@link RenderLevelStageEvent}/{@link LevelRenderer#renderLineBox} approach this project already uses for
 * {@code SpaceManipulatorClientEvents}'s capture-selection box and {@code AbilitechnosynthPreviewClientEvents}'s
 * multiblock placement box, closing the gap {@code ManipulatedMatterItem}'s own doc comment calls out (the
 * original's {@code ItemManipulatedMatter} had a client-side render outline this port never carried over).
 * <p>
 * The captured region's size is read straight off the item stack's own {@link MSUItemComponents#MANIPULATED_MATTER}
 * component (a real vanilla {@link StructureTemplate} tag, already present on the client - no new sync packet
 * needed here, unlike the corner-selection box, since item components ride along with the stack itself), and
 * the placement origin is computed the exact same way {@link ManipulatedMatterItem#useOn} computes it
 * ({@code clickedPos.relative(clickedFace)}), off the client's own {@code mc.hitResult} rather than a fresh
 * raytrace. Green when the player can currently build there ({@link net.minecraft.world.entity.player.Abilities#mayBuild}
 * - the same gate {@code useOn} itself checks), red otherwise; this doesn't replicate {@code useOn}'s deeper
 * {@code StructureTemplate#placeInWorld} success/failure (e.g. protected regions elsewhere), only the one
 * cheap, already-known-client-side check.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ManipulatedMatterPreviewClientEvents
{
	private ManipulatedMatterPreviewClientEvents()
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

		ItemStack held = player.getMainHandItem().getItem() instanceof ManipulatedMatterItem ? player.getMainHandItem()
				: player.getOffhandItem().getItem() instanceof ManipulatedMatterItem ? player.getOffhandItem() : ItemStack.EMPTY;
		if(held.isEmpty())
			return;

		CompoundTag data = held.get(MSUItemComponents.MANIPULATED_MATTER.get());
		if(data == null || !data.contains(StructureTemplate.SIZE_TAG))
			return;

		ListTag sizeTag = data.getList(StructureTemplate.SIZE_TAG, Tag.TAG_INT);
		if(sizeTag.size() != 3)
			return;

		if(!(mc.hitResult instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK)
			return;

		BlockPos placePos = blockHit.getBlockPos().relative(blockHit.getDirection());
		boolean valid = player.getAbilities().mayBuild;

		AABB box = new AABB(placePos.getX(), placePos.getY(), placePos.getZ(),
				placePos.getX() + sizeTag.getInt(0), placePos.getY() + sizeTag.getInt(1), placePos.getZ() + sizeTag.getInt(2));

		Vec3 camPos = event.getCamera().getPosition();
		box = box.move(-camPos.x, -camPos.y, -camPos.z);

		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
		VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());

		LevelRenderer.renderLineBox(poseStack, consumer, box, valid ? 0F : 1F, valid ? 1F : 0F, 0F, 0.5F);

		bufferSource.endBatch(RenderType.lines());
	}
}
