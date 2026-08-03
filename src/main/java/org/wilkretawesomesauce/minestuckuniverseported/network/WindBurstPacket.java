package org.wilkretawesomesauce.minestuckuniverseported.network;

import com.mraof.minestuck.network.MSPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.client.render.WindBurstRenderer;

/**
 * One-shot visual cue for {@code heroClass.page.breath.TechPageBreathFreeWill}'s activation burst - mirrors
 * {@code TetherBondImpactPacket}'s exact shape (fire-and-forget, no persisted/resynced state - a late
 * joiner who missed the moment simply never sees it, same as the tether impact flash). Tells every client
 * to spawn a real expanding pressure-wave shell centered on the caster ({@code client.render.WindBurstRenderer}).
 */
public record WindBurstPacket(int casterId) implements MSPacket.PlayToClient
{
	public static final Type<WindBurstPacket> ID = new Type<>(Minestuckuniverseported.id("breath/wind_burst"));
	public static final StreamCodec<RegistryFriendlyByteBuf, WindBurstPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, WindBurstPacket::casterId,
			WindBurstPacket::new
	);

	@Override
	public void execute(IPayloadContext context)
	{
		WindBurstRenderer.spawn(casterId);
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
