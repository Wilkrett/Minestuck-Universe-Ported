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
 * Real, user-requested extension of {@link ConsortHatSyncPacket} - same shape, same two trigger points
 * (worn-item change, a player starting to track an already-dressed Consort), for the new chestplate slot
 * {@code capabilities.consortCosmetics.ConsortHatsData} now also tracks. A separate packet rather than
 * adding a field to {@code ConsortHatSyncPacket} directly: keeps the already-working hat sync path
 * untouched (Consorts/Frogs/Imps all still only ever need the hat half), since only Consorts get a
 * chestplate at all. No upside-down quirk here - that's a hat-only Easter egg.
 */
public record ConsortChestSyncPacket(int entityId, ItemStack chest) implements MSPacket.PlayToClient
{
	public static final Type<ConsortChestSyncPacket> ID = new Type<>(Minestuckuniverseported.id("consort/chest_sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ConsortChestSyncPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ConsortChestSyncPacket::entityId,
			ItemStack.OPTIONAL_STREAM_CODEC, ConsortChestSyncPacket::chest,
			ConsortChestSyncPacket::new
	);

	@Override
	public void execute(IPayloadContext context)
	{
		ConsortHatClientState.setChest(entityId, chest);
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
