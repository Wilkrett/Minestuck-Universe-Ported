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
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.AbilitechLoadout;

/**
 * Same reasoning as {@code StrifePortfolioSyncPacket}: data attachments aren't automatically synced to
 * the client, so without this the loadout screen would just show an empty/stale state until something
 * else happened to trigger a sync. Sent on login/respawn and after any equip/unequip/passive-toggle.
 */
public record AbilitechLoadoutSyncPacket(CompoundTag loadoutNbt) implements MSPacket.PlayToClient
{
	public static final Type<AbilitechLoadoutSyncPacket> ID = new Type<>(Minestuckuniverseported.id("abilitech/loadout_sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, AbilitechLoadoutSyncPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.COMPOUND_TAG, AbilitechLoadoutSyncPacket::loadoutNbt,
			AbilitechLoadoutSyncPacket::new
	);

	public static AbilitechLoadoutSyncPacket create(Player player)
	{
		AbilitechLoadout loadout = player.getData(MSUAttachments.ABILITECH_LOADOUT);
		return new AbilitechLoadoutSyncPacket(loadout.serializeNBT(player.registryAccess()));
	}

	@Override
	public void execute(IPayloadContext context)
	{
		Player player = context.player();
		AbilitechLoadout loadout = player.getData(MSUAttachments.ABILITECH_LOADOUT);
		loadout.deserializeNBT(player.registryAccess(), loadoutNbt);
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
