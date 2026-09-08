package org.wilkretawesomesauce.minestuckuniverseported.network;

import com.mraof.minestuck.network.MSPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.soul.RelayControlled;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.soul.SoulManager;

/**
 * Client -&gt; server half of the Soul system's "hidden anchor" relay - see
 * {@code mechanics.soul.SoulManager}'s own doc comment for the whole design. Sent every client tick by
 * {@code client.SoulProjectionClientEvents} while the sender carries
 * {@code mechanics.soul.SoulProjectionEffect}, carrying their own movement input already converted to a
 * world-relative vector (via their own head yaw), the exact same shape and convention as
 * {@code network.MindflayerMovementInputPacket}.
 */
public record SoulHostInputPacket(float worldX, float worldZ, boolean jump, boolean sneak) implements MSPacket.PlayToServer
{
	public static final Type<SoulHostInputPacket> ID = new Type<>(Minestuckuniverseported.id("soul/host_input"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SoulHostInputPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, SoulHostInputPacket::worldX,
			ByteBufCodecs.FLOAT, SoulHostInputPacket::worldZ,
			ByteBufCodecs.BOOL, SoulHostInputPacket::jump,
			ByteBufCodecs.BOOL, SoulHostInputPacket::sneak,
			SoulHostInputPacket::new
	);

	@Override
	public void execute(IPayloadContext context, ServerPlayer player)
	{
		Entity target = SoulManager.resolveProjectionTarget(player);
		if(target instanceof RelayControlled relayControlled)
			relayControlled.setRelayInput(worldX, worldZ, jump, sneak);
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
