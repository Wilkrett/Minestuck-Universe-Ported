package org.wilkretawesomesauce.minestuckuniverseported.network;

import com.mraof.minestuck.network.MSPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.client.BuilderBadgeClientState;

/**
 * Server -> client mirror of whether the local player currently has {@code badges.BadgeBuilder} active -
 * {@code MSUAttachments#GOD_TIER} itself isn't synced to the client (server-authoritative by design, see
 * that attachment's own doc comment), and every other badge so far only ever needed server-side reads
 * ({@code heroClass.lord.TechLord}, {@code heroClass.mage.TechMage}, etc.), so this is the first badge
 * that needs a real client-side signal at all - see {@code client.BadgeBuilderClientEvents}, the sole
 * consumer, for why (deciding whether to intercept normal right-click block placement with the drag-fill
 * tool instead).
 */
public record BuilderBadgeSyncPacket(boolean active) implements MSPacket.PlayToClient
{
	public static final Type<BuilderBadgeSyncPacket> ID = new Type<>(Minestuckuniverseported.id("badge_builder/sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BuilderBadgeSyncPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, BuilderBadgeSyncPacket::active,
			BuilderBadgeSyncPacket::new
	);

	@Override
	public void execute(IPayloadContext context)
	{
		BuilderBadgeClientState.set(active);
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
