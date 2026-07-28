package org.wilkretawesomesauce.minestuckuniverseported.network;

import com.mraof.minestuck.network.MSPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.beam.BeamData;

/**
 * Full-state resync of one level's {@link BeamData} - ported from the original's
 * {@code MSUPacket.Type.UPDATE_BEAMS}, sent to every player in the affected dimension whenever a beam fires
 * or releases ({@code beam.BeamEvents#broadcast}), and to a player individually when they (re)join a
 * dimension ({@code beam.BeamEvents}'s login/dimension-change hooks) - same triggers the original used, not
 * a per-tick sync (that's what {@code beam.BeamEvents}'s identical-tick-logic-on-both-sides is for, see
 * {@code beam.Beam}'s own doc comment).
 */
public record BeamSyncPacket(CompoundTag dataNbt) implements MSPacket.PlayToClient
{
	public static final Type<BeamSyncPacket> ID = new Type<>(Minestuckuniverseported.id("beam/sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BeamSyncPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.COMPOUND_TAG, BeamSyncPacket::dataNbt,
			BeamSyncPacket::new
	);

	@Override
	public void execute(IPayloadContext context)
	{
		Player player = context.player();
		BeamData data = player.level().getData(MSUAttachments.BEAM_DATA);
		data.deserializeNBT(player.registryAccess(), dataNbt);
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
