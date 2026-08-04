package org.wilkretawesomesauce.minestuckuniverseported.network;

import com.mraof.minestuck.network.MSPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.client.WindRibbonClientState;

/**
 * Sync packet for the Breath Wind Engine's real ribbon renderer ({@code client.render.WindRibbonRenderer})
 * - mirrors {@code TetherBondSyncPacket}'s exact shape (caster id keyed to a small piece of ongoing state,
 * not a full-state resync). {@code targetId} of {@code -1} means "released" - begins a client-side
 * fade-out ({@code WindRibbonClientState.clearRibbon} moves the ribbon into its own fading-out map instead
 * of removing it outright, see that class's own doc comment) rather than an instant clear. {@code inward} picks
 * {@code TechBreathLiberate}'s outward flow vs. {@code TechBreathConstrain}'s inward compression (see
 * {@code client.render.WindRibbonRenderer}'s own doc comment for how that changes both color and motion).
 * {@code intensity} (0-1) drives the vortex radius/twist amplitude - re-sent periodically while a ribbon
 * stays active (not every tick) so it can visibly grow as the target's Freedom actually climbs, without
 * spamming a packet 20 times a second - see {@code TechBreathLiberate}'s own call site for the interval.
 */
public record WindRibbonSyncPacket(int casterId, int targetId, boolean inward, float intensity) implements MSPacket.PlayToClient
{
	public static final Type<WindRibbonSyncPacket> ID = new Type<>(Minestuckuniverseported.id("breath/wind_ribbon_sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, WindRibbonSyncPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, WindRibbonSyncPacket::casterId,
			ByteBufCodecs.VAR_INT, WindRibbonSyncPacket::targetId,
			ByteBufCodecs.BOOL, WindRibbonSyncPacket::inward,
			ByteBufCodecs.FLOAT, WindRibbonSyncPacket::intensity,
			WindRibbonSyncPacket::new
	);

	@Override
	public void execute(IPayloadContext context)
	{
		if(targetId < 0)
			WindRibbonClientState.clearRibbon(casterId);
		else
			WindRibbonClientState.setRibbon(casterId, targetId, inward, intensity);
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
