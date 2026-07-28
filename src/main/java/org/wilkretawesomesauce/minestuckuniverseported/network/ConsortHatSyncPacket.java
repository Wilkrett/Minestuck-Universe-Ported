package org.wilkretawesomesauce.minestuckuniverseported.network;

import com.mraof.minestuck.network.MSPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.client.ConsortHatClientState;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code MSUPacket.Type.UPDATE_HATS} - the real modern equivalent
 * of the original's bespoke hat-sync packet. {@code capabilities.consortCosmetics.ConsortHatsData} tracks
 * the worn hat as plain capability/attachment data (not a real vanilla equipment slot - see that class's
 * own doc comment for why this project deliberately reverted an earlier equipment-slot-based
 * simplification), so nothing else broadcasts it to observers automatically; this packet is the real,
 * necessary substitute. Sent to every player already tracking the wearer whenever the hat changes
 * (pickup, death drop, bug-net drop) and to a player the instant they start tracking an already-hatted
 * Consort/Frog (covers late joiners and anyone just entering render distance) - same two trigger points
 * the original's own {@code onLivingTick}/{@code onStartTracking} pair covered.
 */
public record ConsortHatSyncPacket(int entityId, ItemStack hat, boolean upsideDown) implements MSPacket.PlayToClient
{
	public static final Type<ConsortHatSyncPacket> ID = new Type<>(Minestuckuniverseported.id("consort/hat_sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ConsortHatSyncPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ConsortHatSyncPacket::entityId,
			ItemStack.OPTIONAL_STREAM_CODEC, ConsortHatSyncPacket::hat,
			ByteBufCodecs.BOOL, ConsortHatSyncPacket::upsideDown,
			ConsortHatSyncPacket::new
	);

	@Override
	public void execute(IPayloadContext context)
	{
		ConsortHatClientState.setHat(entityId, hat, upsideDown);
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
