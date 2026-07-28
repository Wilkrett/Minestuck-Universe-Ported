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
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.request.TimeRequestData;

/**
 * Same reasoning as {@code AbilitechLoadoutSyncPacket}: {@link TimeRequestData} isn't automatically
 * synced to the client, so without this the Temporal Sendificator screen would show stale/empty open
 * requests. Sent on login/respawn ({@code timeline.request.TimeRequestEvents}) and after any
 * request create/resolve.
 */
public record TimeRequestSyncPacket(CompoundTag dataNbt) implements MSPacket.PlayToClient
{
	public static final Type<TimeRequestSyncPacket> ID = new Type<>(Minestuckuniverseported.id("time_request/sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TimeRequestSyncPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.COMPOUND_TAG, TimeRequestSyncPacket::dataNbt,
			TimeRequestSyncPacket::new
	);

	public static TimeRequestSyncPacket create(Player player)
	{
		TimeRequestData data = player.getData(MSUAttachments.TIME_REQUEST_DATA);
		return new TimeRequestSyncPacket(data.serializeNBT(player.registryAccess()));
	}

	@Override
	public void execute(IPayloadContext context)
	{
		Player player = context.player();
		TimeRequestData data = player.getData(MSUAttachments.TIME_REQUEST_DATA);
		data.deserializeNBT(player.registryAccess(), dataNbt);
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
