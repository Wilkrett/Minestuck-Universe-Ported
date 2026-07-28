package org.wilkretawesomesauce.minestuckuniverseported.network;

import com.mraof.minestuck.network.MSPacket;
import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.client.TetherBondClientState;

/**
 * Sync packet for {@code heroAspect.TechTetherBond}'s tether visual - mirrors {@code ConsortHatSyncPacket}'s
 * shape (an entity id keyed to a small piece of state, not a full-state resync like {@code BeamSyncPacket}).
 * {@code targetId} of {@code -1} means "bond cleared" ({@code aspectOrdinal}/{@code corrupted} are
 * meaningless in that case). {@code aspectOrdinal} is {@link EnumAspect#ordinal()} - which concrete
 * {@code TechTetherBond} subclass this bond belongs to determines the tether's color (see
 * {@code client.render.TetherBondRenderer}). {@code corrupted} overrides that color entirely - see
 * {@code heroClass.witch.blood.CultOfPersonalityManager}'s own doc comment for
 * {@code heroClass.prince.blood.TechPrinceBloodSchism}'s "Corrupted Awareness" tether recolor.
 * Sent whenever the caster's bond changes (established, cleared, or corrupted/cleansed - bonds are sticky,
 * never retargeted, see {@code TechTetherBond}'s own doc comment) and to an observer the instant they start
 * tracking a caster who already has one, same two trigger points {@code ConsortHatsData} uses for hats.
 */
public record TetherBondSyncPacket(int casterId, int targetId, int aspectOrdinal, boolean corrupted) implements MSPacket.PlayToClient
{
	public static final Type<TetherBondSyncPacket> ID = new Type<>(Minestuckuniverseported.id("tether_bond/sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TetherBondSyncPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, TetherBondSyncPacket::casterId,
			ByteBufCodecs.VAR_INT, TetherBondSyncPacket::targetId,
			ByteBufCodecs.VAR_INT, TetherBondSyncPacket::aspectOrdinal,
			ByteBufCodecs.BOOL, TetherBondSyncPacket::corrupted,
			TetherBondSyncPacket::new
	);

	/** Convenience for every caller that isn't Schism-aware and never corrupts anything - always sends {@code corrupted = false}. */
	public TetherBondSyncPacket(int casterId, int targetId, int aspectOrdinal)
	{
		this(casterId, targetId, aspectOrdinal, false);
	}

	@Override
	public void execute(IPayloadContext context)
	{
		if(targetId < 0)
			TetherBondClientState.clearBond(casterId);
		else
			TetherBondClientState.setBond(casterId, targetId, EnumAspect.values()[aspectOrdinal], corrupted);
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
