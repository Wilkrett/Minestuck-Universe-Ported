package org.wilkretawesomesauce.minestuckuniverseported.network;

import com.mraof.minestuck.network.MSPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.space.TechSpaceManipulator.ManipulatorSelectionClientState;

/**
 * Broadcasts the caster's own in-progress Matter Manipulator corner selection to their client. Unlike
 * {@code GodTierData}, {@code BadgeEffects} (which holds {@code manipulatedPos1/2}) is never synced
 * wholesale, and {@code TechSpaceManipulator#onUseTick} only ever runs on the logical server side
 * ({@code AbilitechEvents} skips the client side entirely) - so without this packet the client's own copy
 * of {@code getManipulatedPos1/2} stayed permanently null and {@code SpaceManipulatorClientEvents} never
 * had anything to draw a box around. Sent to the owning player only, every time either corner changes.
 */
public record ManipulatorSelectionSyncPacket(int state, BlockPos pos1, BlockPos pos2, ResourceLocation dimension) implements MSPacket.PlayToClient
{
	public static final Type<ManipulatorSelectionSyncPacket> ID = new Type<>(Minestuckuniverseported.id("space/manipulator_selection_sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ManipulatorSelectionSyncPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ManipulatorSelectionSyncPacket::state,
			BlockPos.STREAM_CODEC, ManipulatorSelectionSyncPacket::pos1,
			BlockPos.STREAM_CODEC, ManipulatorSelectionSyncPacket::pos2,
			ResourceLocation.STREAM_CODEC, ManipulatorSelectionSyncPacket::dimension,
			ManipulatorSelectionSyncPacket::new
	);

	@Override
	public void execute(IPayloadContext context)
	{
		ManipulatorSelectionClientState.set(state, pos1, pos2, dimension);
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
