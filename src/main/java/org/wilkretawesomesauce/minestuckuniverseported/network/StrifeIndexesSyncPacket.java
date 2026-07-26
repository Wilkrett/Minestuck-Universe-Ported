package org.wilkretawesomesauce.minestuckuniverseported.network;

import com.mraof.minestuck.network.MSPacket;
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
 * Sent server -> client for the frequent, cheap updates: which specibus/weapon slot is selected, whether
 * a weapon is currently armed, and whether the abstrata switcher is unlocked. Mirrors the 1.12.2
 * {@code UpdateStrifeDataPacket}'s {@code UpdateType.INDEXES}/{@code CONFIG} (merged into one here), split
 * out from the full portfolio sync so these frequent flips don't have to re-send every specibus' contents.
 */
public record StrifeIndexesSyncPacket(int selectedSpecibus, int selectedWeapon, boolean armed, boolean abstrataSwitcherUnlocked) implements MSPacket.PlayToClient
{
	public static final Type<StrifeIndexesSyncPacket> ID = new Type<>(Minestuckuniverseported.id("strife_indexes_sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, StrifeIndexesSyncPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT,
			StrifeIndexesSyncPacket::selectedSpecibus,
			ByteBufCodecs.INT,
			StrifeIndexesSyncPacket::selectedWeapon,
			ByteBufCodecs.BOOL,
			StrifeIndexesSyncPacket::armed,
			ByteBufCodecs.BOOL,
			StrifeIndexesSyncPacket::abstrataSwitcherUnlocked,
			StrifeIndexesSyncPacket::new
	);

	public static StrifeIndexesSyncPacket create(Player player)
	{
		StrifeData portfolio = player.getData(MSUAttachments.STRIFE_PORTFOLIO);
		return new StrifeIndexesSyncPacket(portfolio.getSelectedSpecibusIndex(), portfolio.getSelectedWeaponIndex(),
				portfolio.isArmed(), portfolio.abstrataSwitcherUnlocked());
	}

	@Override
	public void execute(IPayloadContext context)
	{
		Player player = context.player();
		StrifeData portfolio = player.getData(MSUAttachments.STRIFE_PORTFOLIO);
		portfolio.setSelectedSpecibusIndex(selectedSpecibus);
		portfolio.setSelectedWeaponIndex(selectedWeapon);
		portfolio.setArmed(armed);
		portfolio.unlockAbstrataSwitcher(abstrataSwitcherUnlocked);
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
