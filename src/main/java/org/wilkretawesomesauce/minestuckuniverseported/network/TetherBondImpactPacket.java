package org.wilkretawesomesauce.minestuckuniverseported.network;

import com.mraof.minestuck.network.MSPacket;
import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.client.render.TetherBondImpactRenderer;

/**
 * One-shot visual cue for {@code heroAspect.TechTetherBond}'s one-time far-range damage snap - tells every
 * client in the dimension to spawn a short-lived {@code textures/foci/<aspect>.png} icon on the target
 * (tinted with that aspect's own {@code MSUAspectColors} entry), the moment the snap actually lands.
 * {@code aspectOrdinal} is {@link EnumAspect#ordinal()} - which concrete {@code TechTetherBond} subclass
 * this came from picks the icon/color. Not a resynced/cached state like {@code TetherBondSyncPacket}'s
 * tether (nothing needs to persist for late joiners here - if you missed the moment, the flash is already
 * over), just a fire-and-forget spawn call into {@link TetherBondImpactRenderer}.
 */
public record TetherBondImpactPacket(int targetId, int aspectOrdinal) implements MSPacket.PlayToClient
{
	public static final Type<TetherBondImpactPacket> ID = new Type<>(Minestuckuniverseported.id("tether_bond/impact"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TetherBondImpactPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, TetherBondImpactPacket::targetId,
			ByteBufCodecs.VAR_INT, TetherBondImpactPacket::aspectOrdinal,
			TetherBondImpactPacket::new
	);

	@Override
	public void execute(IPayloadContext context)
	{
		TetherBondImpactRenderer.spawn(targetId, EnumAspect.values()[aspectOrdinal]);
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
