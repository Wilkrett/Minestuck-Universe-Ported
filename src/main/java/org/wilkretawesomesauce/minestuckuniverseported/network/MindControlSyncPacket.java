package org.wilkretawesomesauce.minestuckuniverseported.network;

import com.mraof.minestuck.network.MSPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.mind.TechMindControl.MindControlClientState;

/**
 * Server -&gt; possessed-target half of {@code abilitech.heroAspect.mind.TechMindControl}'s real
 * movement puppeting - relays the controller's own {@link MindControlInputPacket} down to whichever
 * player is actually being possessed, exactly like the original's server-side handler for
 * {@code MSUPacket.Type.MINDFLAYER_MOVEMENT_INPUT} fed the target's own client its forced movement
 * state. {@code active=false} (sent the instant the tether is released or the ability runs out of food)
 * tells the target's client to stop overriding its own input.
 */
public record MindControlSyncPacket(boolean active, float worldX, float worldZ, boolean jump, boolean sneak) implements MSPacket.PlayToClient
{
	public static final Type<MindControlSyncPacket> ID = new Type<>(Minestuckuniverseported.id("mind/control_sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, MindControlSyncPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, MindControlSyncPacket::active,
			ByteBufCodecs.FLOAT, MindControlSyncPacket::worldX,
			ByteBufCodecs.FLOAT, MindControlSyncPacket::worldZ,
			ByteBufCodecs.BOOL, MindControlSyncPacket::jump,
			ByteBufCodecs.BOOL, MindControlSyncPacket::sneak,
			MindControlSyncPacket::new
	);

	@Override
	public void execute(IPayloadContext context)
	{
		MindControlClientState.update(active, worldX, worldZ, jump, sneak);
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
