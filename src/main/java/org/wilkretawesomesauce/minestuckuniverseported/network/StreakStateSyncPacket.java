package org.wilkretawesomesauce.minestuckuniverseported.network;

import com.mraof.minestuck.network.MSPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.client.streak.StreakClientState;

/**
 * Broadcasts the real streak-toggle state of an entity to every observer - shaped exactly like
 * {@link CloakSyncPacket}. Sent by {@code command.StreakCommand} on toggle/flavour change, by
 * {@code heroAspect.time.TechTimeAccelerateSelf} (a real gameplay reuse of this same system - see
 * {@code streak.StreakPreference}'s own doc comment for why the extra fields exist), and by
 * {@code streak.StreakTrackingEvents} to a player the instant they start tracking an entity that
 * already has the effect toggled on (covers late joiners).
 */
public record StreakStateSyncPacket(int entityId, boolean enabled, String flavourName,
									 boolean hideTrail, boolean ghostsIgnoreSprint, int ghostTint) implements MSPacket.PlayToClient
{
	public static final Type<StreakStateSyncPacket> ID = new Type<>(Minestuckuniverseported.id("streak/state_sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, StreakStateSyncPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, StreakStateSyncPacket::entityId,
			ByteBufCodecs.BOOL, StreakStateSyncPacket::enabled,
			ByteBufCodecs.STRING_UTF8, StreakStateSyncPacket::flavourName,
			ByteBufCodecs.BOOL, StreakStateSyncPacket::hideTrail,
			ByteBufCodecs.BOOL, StreakStateSyncPacket::ghostsIgnoreSprint,
			ByteBufCodecs.INT, StreakStateSyncPacket::ghostTint,
			StreakStateSyncPacket::new
	);

	@Override
	public void execute(IPayloadContext context)
	{
		if(enabled)
			StreakClientState.setState(entityId, flavourName, hideTrail, ghostsIgnoreSprint, ghostTint);
		else
			StreakClientState.clearState(entityId);
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
