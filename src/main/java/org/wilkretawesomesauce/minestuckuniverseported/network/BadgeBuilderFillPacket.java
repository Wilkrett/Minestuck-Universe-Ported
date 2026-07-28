package org.wilkretawesomesauce.minestuckuniverseported.network;

import com.mraof.minestuck.computer.editmode.ServerEditHandler;
import com.mraof.minestuck.network.MSPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.skills.MSUSkills;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code network.PacketPlaceBlockArea} - real port of
 * {@code badges.BadgeBuilder}'s own drag-fill mechanic: given the two corners of a cuboid selection (and
 * the original click's hit location/face, reused for every position in the cuboid, matching the original
 * exactly), places whatever real {@link BlockItem} the sender is holding at every replaceable position in
 * that cuboid via the item's own real {@link ItemStack#useOn} - the same call vanilla's own right-click
 * placement uses, so per-block replaceability/placement-context rules apply for free.
 * <p>
 * <b>Real server-side re-validation, not just trusting the client</b> (the original's own equivalent handler
 * trusted the sender entirely once past its {@code editModePlaceCheck} gate): this project's client-side
 * drag tracking ({@code client.BadgeBuilderClientEvents}) can't reliably read whether the badge is actually
 * active - {@code MSUAttachments#GOD_TIER} isn't synced to the client (server-authoritative by design, see
 * that attachment's own doc comment) - so the real qualification check (badge active OR real Minestuck Edit
 * Mode, matching the original's own {@code canEditDrag} OR-condition) happens here instead, plus a distance
 * sanity clamp the original didn't need (its own capability-backed pos1/pos2 couldn't be forged independently
 * of a real clicked position the way an arbitrary network payload can).
 * <p>
 * <b>Scope note</b>: the original's {@code editModePlaceCheck} additionally enforced Minestuck's own
 * per-deploy-list-entry grist cost when the sender happened to also be in real Edit Mode, reflectively
 * reaching into {@code EditData}'s private {@code connection} field to do it. Not reproduced here - this
 * port's real feature is the badge's own always-available fill tool; real Edit Mode's own separate deploy
 * economy is Minestuck's own concern, not re-derived through reflection on a private field this project's
 * pinned dependency doesn't expose any other way.
 */
public record BadgeBuilderFillPacket(BlockPos pos1, BlockPos pos2, Vec3 hitVec, Direction face) implements MSPacket.PlayToServer
{
	private static final int MAX_VOLUME_CREATIVE = 256;
	private static final double MAX_DISTANCE = 48.0;

	public static final Type<BadgeBuilderFillPacket> ID = new Type<>(Minestuckuniverseported.id("badge_builder/fill"));

	private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3_STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.DOUBLE, Vec3::x,
			ByteBufCodecs.DOUBLE, Vec3::y,
			ByteBufCodecs.DOUBLE, Vec3::z,
			Vec3::new
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, BadgeBuilderFillPacket> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, BadgeBuilderFillPacket::pos1,
			BlockPos.STREAM_CODEC, BadgeBuilderFillPacket::pos2,
			VEC3_STREAM_CODEC, BadgeBuilderFillPacket::hitVec,
			NeoForgeStreamCodecs.enumCodec(Direction.class), BadgeBuilderFillPacket::face,
			BadgeBuilderFillPacket::new
	);

	@Override
	public void execute(IPayloadContext context, ServerPlayer player)
	{
		boolean badgeActive = player.getData(MSUAttachments.GOD_TIER).isBadgeActive(MSUSkills.BUILDER_BADGE, player.level(), player);
		if(!badgeActive && !ServerEditHandler.isInEditmode(player))
			return;

		if(pos1.distSqr(player.blockPosition()) > MAX_DISTANCE * MAX_DISTANCE || pos2.distSqr(player.blockPosition()) > MAX_DISTANCE * MAX_DISTANCE)
			return;

		InteractionHand hand = player.getMainHandItem().getItem() instanceof BlockItem ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
		ItemStack stack = player.getItemInHand(hand);
		if(!(stack.getItem() instanceof BlockItem))
			return;

		int minX = Math.min(pos1.getX(), pos2.getX()), maxX = Math.max(pos1.getX(), pos2.getX());
		int minY = Math.min(pos1.getY(), pos2.getY()), maxY = Math.max(pos1.getY(), pos2.getY());
		int minZ = Math.min(pos1.getZ(), pos2.getZ()), maxZ = Math.max(pos1.getZ(), pos2.getZ());

		long volume = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
		if(volume > (player.isCreative() ? MAX_VOLUME_CREATIVE : stack.getCount()))
			return;

		boolean swung = false;
		for(int x = minX; x <= maxX; x++)
			for(int y = minY; y <= maxY; y++)
				for(int z = minZ; z <= maxZ; z++)
				{
					if(stack.isEmpty())
						break;

					BlockPos pos = new BlockPos(x, y, z);
					if(!player.level().getBlockState(pos).canBeReplaced())
						continue;

					BlockHitResult hit = new BlockHitResult(hitVec, face, pos, false);
					InteractionResult result = stack.useOn(new UseOnContext(player, hand, hit));
					if(result == InteractionResult.SUCCESS)
						swung = true;
				}

		if(swung)
			player.swing(hand);
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
