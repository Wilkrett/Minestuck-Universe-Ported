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
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.strife.StrifeData;

/**
 * Sent server -> client to sync a player's own strife portfolio (all specibi and their contents).
 * <p>
 * Simplification vs. the 1.12.2 {@code UpdateStrifeDataPacket}'s {@code UpdateType.PORTFOLIO}: the
 * original could sync a subset of specibus slots (to save bandwidth) and could target any
 * {@code EntityLivingBase} by UUID (for syncing e.g. mobs' portfolios to nearby clients). This port only
 * syncs the receiving player's own full portfolio, since the only current consumer is that player's own
 * GUI (not yet ported) - both simplifications can be revisited if/when non-player portfolio display is
 * needed.
 */
public record StrifePortfolioSyncPacket(CompoundTag portfolioNbt) implements MSPacket.PlayToClient
{
	public static final Type<StrifePortfolioSyncPacket> ID = new Type<>(Minestuckuniverseported.id("strife_portfolio_sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, StrifePortfolioSyncPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.COMPOUND_TAG,
			StrifePortfolioSyncPacket::portfolioNbt,
			StrifePortfolioSyncPacket::new
	);

	public static StrifePortfolioSyncPacket create(Player player)
	{
		StrifeData portfolio = player.getData(MSUAttachments.STRIFE_PORTFOLIO);
		return new StrifePortfolioSyncPacket(portfolio.serializeNBT(player.registryAccess()));
	}

	@Override
	public void execute(IPayloadContext context)
	{
		Player player = context.player();
		StrifeData portfolio = player.getData(MSUAttachments.STRIFE_PORTFOLIO);
		portfolio.deserializeNBT(player.registryAccess(), portfolioNbt);
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
