package org.wilkretawesomesauce.minestuckuniverseported.network;

import com.mraof.minestuck.network.MSPacket;
import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.client.render.FociFlashRenderer;

/**
 * Fire-and-forget visual cue - tells every client in the dimension to flash a fading
 * {@code textures/foci/<aspect>.png} icon at a fixed world position. See
 * {@code skills.abilitech.MSUAbilitechParticles#focusFlash} for the real "easy util" entry point that
 * sends this, and {@link FociFlashRenderer} for the client-side rendering. Same one-shot, nothing-to-resync
 * shape as {@code network.TetherBondImpactPacket} (the effect it generalizes) - if a client wasn't present
 * for the moment this fired, there's nothing left to catch up on.
 */
public record FociFlashPacket(double x, double y, double z, int aspectOrdinal, float size, int lifetimeTicks) implements MSPacket.PlayToClient
{
	public static final Type<FociFlashPacket> ID = new Type<>(Minestuckuniverseported.id("foci_flash"));
	public static final StreamCodec<RegistryFriendlyByteBuf, FociFlashPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.DOUBLE, FociFlashPacket::x,
			ByteBufCodecs.DOUBLE, FociFlashPacket::y,
			ByteBufCodecs.DOUBLE, FociFlashPacket::z,
			ByteBufCodecs.VAR_INT, FociFlashPacket::aspectOrdinal,
			ByteBufCodecs.FLOAT, FociFlashPacket::size,
			ByteBufCodecs.VAR_INT, FociFlashPacket::lifetimeTicks,
			FociFlashPacket::new
	);

	@Override
	public void execute(IPayloadContext context)
	{
		FociFlashRenderer.spawn(new Vec3(x, y, z), EnumAspect.values()[aspectOrdinal], size, lifetimeTicks);
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
