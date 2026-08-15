package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.space;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.MSUItemComponents;
import org.wilkretawesomesauce.minestuckuniverseported.MSUItems;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.network.ManipulatorSelectionSyncPacket;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.AbilitechLoadout;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.space.TechSpaceManipulator}
 * ("Matter Manipulator"). Tap (aim at a block and quick-release, or whenever a corner isn't set yet) to
 * set corners one at a time - first press sets corner A, the next sets corner B, and a further press
 * after both are set starts a fresh selection at A again. Crouch and press with no corners set to clear
 * the selection instead. Once both corners are set and at least {@link #HOLD_TICKS} have passed, release
 * to collapse the selected region into a {@link ManipulatedMatterItem} - see that class's own doc
 * comment for why this uses a real {@code StructureTemplate} instead of the original's hand-rolled
 * block-list NBT format.
 * <p>
 * The 8-block-per-axis size cap and the {@code (dx*dy*dz)/512f*18} energy cost are both kept exactly as
 * sourced. {@code player.capabilities.allowEdit} (1.12.2) is real player-permission state, not
 * gamemode-only - {@link net.minecraft.world.entity.player.Abilities#mayBuild} is its direct modern
 * equivalent, used here instead of a plain creative-mode check.
 */
public class TechSpaceManipulator extends TechHeroAspect
{
	private static final int MAX_SPAN = 8;
	private static final int HOLD_TICKS = 20;

	public TechSpaceManipulator()
	{
		super(Minestuckuniverseported.id("matter_manipulator"), EnumAspect.SPACE, 560000, MSUTechType.UTILITY);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE)
			return false;

		AbilitechLoadout badgeEffects = player.getData(MSUAttachments.ABILITECH_LOADOUT);
		BlockPos pos1 = badgeEffects.getManipulatedPos1();
		BlockPos pos2 = badgeEffects.getManipulatedPos2();

		if(state != AbilitechKeyState.RELEASED)
		{
			MSUAbilitechParticles.aura(level, player, EnumAspect.SPACE, (time >= HOLD_TICKS && pos1 != null && pos2 != null) ? 6 : 1);
			return true;
		}
		if(!(level instanceof ServerLevel serverLevel))
			return false;

		if(time < HOLD_TICKS || pos1 == null || pos2 == null)
		{
			BlockPos targeted = MSUAbilitechRayTrace.getTargetBlock(player);

			if(targeted == null || player.isCrouching())
			{
				badgeEffects.setManipulatedPos1(null, null);
				badgeEffects.setManipulatedPos2(null, null);
				player.displayClientMessage(Component.translatable("item.manipulatedMatter.posReset"), true);
			}
			else if(pos1 == null)
			{
				badgeEffects.setManipulatedPos1(targeted, level.dimension());
				player.displayClientMessage(Component.translatable("item.manipulatedMatter.posSetA",
						targeted.getX() + ", " + targeted.getY() + ", " + targeted.getZ()), true);
			}
			else if(pos2 == null)
			{
				badgeEffects.setManipulatedPos2(targeted, level.dimension());
				player.displayClientMessage(Component.translatable("item.manipulatedMatter.posSetB",
						targeted.getX() + ", " + targeted.getY() + ", " + targeted.getZ()), true);
			}
			else
			{
				badgeEffects.setManipulatedPos1(targeted, level.dimension());
				badgeEffects.setManipulatedPos2(null, null);
				player.displayClientMessage(Component.translatable("item.manipulatedMatter.posSetA",
						targeted.getX() + ", " + targeted.getY() + ", " + targeted.getZ()), true);
			}
			sendSelectionSync(player, badgeEffects);

			if(time >= HOLD_TICKS)
				MSUAbilitechParticles.burst(level, player, EnumAspect.SPACE, 10);
			else
				MSUAbilitechParticles.aura(level, player, EnumAspect.SPACE, 5);
			return true;
		}

		if(!player.getAbilities().mayBuild)
		{
			player.displayClientMessage(Component.translatable("item.manipulatedMatter.cantEdit"), true);
			MSUAbilitechParticles.burst(level, player, EnumAspect.SPACE, 10);
			return true;
		}

		if(badgeEffects.getManipulatedPos1Dim() != level.dimension() || badgeEffects.getManipulatedPos2Dim() != level.dimension()
				|| Math.abs(pos1.getX() - pos2.getX()) >= MAX_SPAN
				|| Math.abs(pos1.getY() - pos2.getY()) >= MAX_SPAN
				|| Math.abs(pos1.getZ() - pos2.getZ()) >= MAX_SPAN)
		{
			player.displayClientMessage(Component.translatable("item.manipulatedMatter.tooBig"), true);
			MSUAbilitechParticles.burst(level, player, EnumAspect.SPACE, 10);
			return true;
		}

		int energyRequired = (int)((Math.abs(pos1.getX() - pos2.getX()) * Math.abs(pos1.getY() - pos2.getY()) * Math.abs(pos1.getZ() - pos2.getZ())) / 512f * 18);

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < energyRequired)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			MSUAbilitechParticles.burst(level, player, EnumAspect.SPACE, 10);
			return true;
		}

		BlockPos min = new BlockPos(Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()), Math.min(pos1.getZ(), pos2.getZ()));
		Vec3i size = new Vec3i(Math.abs(pos1.getX() - pos2.getX()) + 1, Math.abs(pos1.getY() - pos2.getY()) + 1, Math.abs(pos1.getZ() - pos2.getZ()) + 1);

		StructureTemplate template = new StructureTemplate();
		template.fillFromWorld(serverLevel, min, size, true, null);
		CompoundTag data = template.save(new CompoundTag());

		// The Manipulator collapses matter into an item - it doesn't clone it. Clear the captured region
		// to air now that it's been saved, or a player could re-capture the same blocks endlessly and dupe
		// them.
		BlockPos.betweenClosedStream(min, min.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1))
				.forEach(clearedPos -> serverLevel.setBlock(clearedPos, Blocks.AIR.defaultBlockState(), 3));

		if(!player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - energyRequired);

		ItemStack matter = new ItemStack(MSUItems.MANIPULATED_MATTER.get());
		matter.set(MSUItemComponents.MANIPULATED_MATTER.get(), data);
		if(!player.getInventory().add(matter))
			player.drop(matter, false);

		badgeEffects.setManipulatedPos1(null, null);
		badgeEffects.setManipulatedPos2(null, null);
		sendSelectionSync(player, badgeEffects);

		MSUAbilitechParticles.burst(level, player, EnumAspect.SPACE, 10);

		return true;
	}

	private static void sendSelectionSync(Player player, AbilitechLoadout badgeEffects)
	{
		if(!(player instanceof ServerPlayer serverPlayer))
			return;

		BlockPos pos1 = badgeEffects.getManipulatedPos1();
		BlockPos pos2 = badgeEffects.getManipulatedPos2();
		int state = pos1 == null ? 0 : (pos2 == null ? 1 : 2);

		PacketDistributor.sendToPlayer(serverPlayer, new ManipulatorSelectionSyncPacket(state,
				pos1 == null ? BlockPos.ZERO : pos1, pos2 == null ? BlockPos.ZERO : pos2,
				player.level().dimension().location()));
	}

	/**
	 * Client-side cache of the local player's own in-progress Matter Manipulator corner selection, kept in
	 * sync via {@code network.ManipulatorSelectionSyncPacket}. {@code AbilitechLoadout} (which this tech
	 * mutates server-side) is never synced to the client, so {@link ClientEvents} reads from here instead
	 * of the attachment directly.
	 */
	public static final class ManipulatorSelectionClientState
	{
		public static final int STATE_CLEARED = 0;
		public static final int STATE_POS1_ONLY = 1;
		public static final int STATE_BOTH = 2;

		private static int state = STATE_CLEARED;
		private static BlockPos pos1 = BlockPos.ZERO;
		private static BlockPos pos2 = BlockPos.ZERO;
		private static ResourceLocation dimension = ResourceLocation.withDefaultNamespace("overworld");

		private ManipulatorSelectionClientState()
		{
		}

		public static void set(int newState, BlockPos newPos1, BlockPos newPos2, ResourceLocation newDimension)
		{
			state = newState;
			pos1 = newPos1;
			pos2 = newPos2;
			dimension = newDimension;
		}

		public static int getState()
		{
			return state;
		}

		public static BlockPos getPos1()
		{
			return pos1;
		}

		public static BlockPos getPos2()
		{
			return pos2;
		}

		public static ResourceLocation getDimension()
		{
			return dimension;
		}
	}

	/**
	 * Client-only render outline for this tech's in-progress corner selection - the modern equivalent of
	 * the original {@code TechSpaceManipulator#renderOutline}'s {@code RenderWorldLastEvent} hook, using
	 * the real vanilla {@link LevelRenderer#renderLineBox} helper instead of hand-rolled
	 * {@code GlStateManager} calls.
	 * <p>
	 * <b>Simplified, not the mechanic:</b> the original also drew a live placement-preview box while
	 * holding a filled {@code ItemManipulatedMatter} and aiming at a block. Only the corner-selection box
	 * (the part every player actually needs to see to use the ability) is drawn here - the held-item preview
	 * is a visual nicety on top of an already-real capture/place mechanic, not attempted this pass.
	 */
	@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
	public static final class ClientEvents
	{
		private ClientEvents()
		{
		}

		@SubscribeEvent
		private static void onRenderLevel(RenderLevelStageEvent event)
		{
			if(event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
				return;

			Minecraft mc = Minecraft.getInstance();
			if(mc.level == null || mc.player == null)
				return;

			int state = ManipulatorSelectionClientState.getState();
			if(state == ManipulatorSelectionClientState.STATE_CLEARED
					|| !ManipulatorSelectionClientState.getDimension().equals(mc.level.dimension().location()))
				return;

			BlockPos pos1 = ManipulatorSelectionClientState.getPos1();
			BlockPos pos2 = ManipulatorSelectionClientState.getPos2();
			boolean lockedIn = state == ManipulatorSelectionClientState.STATE_BOTH;

			AABB box;
			if(lockedIn)
			{
				box = new AABB(
						Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()), Math.min(pos1.getZ(), pos2.getZ()),
						Math.max(pos1.getX(), pos2.getX()) + 1, Math.max(pos1.getY(), pos2.getY()) + 1, Math.max(pos1.getZ(), pos2.getZ()) + 1);
			}
			else
				box = new AABB(pos1);

			Vec3 camPos = event.getCamera().getPosition();
			box = box.move(-camPos.x, -camPos.y, -camPos.z);

			PoseStack poseStack = event.getPoseStack();
			MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
			VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());

			LevelRenderer.renderLineBox(poseStack, consumer, box, 0F, 1F, lockedIn ? 1F : 0.5F, 0.6F);

			bufferSource.endBatch(RenderType.lines());
		}
	}
}
