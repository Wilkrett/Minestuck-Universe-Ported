package org.wilkretawesomesauce.minestuckuniverseported.network;

import com.mraof.minestuck.network.MSPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.client.rewind.RewindGhostPlayback;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.RewindVisuals;

import java.util.List;

/**
 * The network half of {@code mechanics.timeline.RewindVisuals} - carries an entity's own real recorded
 * path across a rewound window, plus exactly how many ticks the real per-tick movers on the server are
 * spending walking it backward ({@code durationTicks}, {@code TimeLoopZone#REVERSE_TICKS_EFFECTIVE} at the moment
 * this fired), to {@code client.rewind.RewindGhostPlayback} - which starts the reverse-sweep comet
 * {@code client.render.RewindGhostRenderer} then draws in lockstep with the real movement, not a
 * separately-guessed duration.
 */
public record RewindGhostPacket(int entityId, List<RewindVisuals.PathPoint> path, int durationTicks) implements MSPacket.PlayToClient
{
	public static final Type<RewindGhostPacket> ID = new Type<>(Minestuckuniverseported.id("timeline/rewind_ghost"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RewindGhostPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, RewindGhostPacket::entityId,
			RewindVisuals.PathPoint.STREAM_CODEC.apply(ByteBufCodecs.list()), RewindGhostPacket::path,
			ByteBufCodecs.VAR_INT, RewindGhostPacket::durationTicks,
			RewindGhostPacket::new
	);

	@Override
	public void execute(IPayloadContext context)
	{
		RewindGhostPlayback.start(entityId, path, durationTicks);
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
