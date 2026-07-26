package org.wilkretawesomesauce.minestuckuniverseported.network;

import com.mraof.minestuck.network.MSPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKey;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.SkillKeyStates;

/**
 * Ported from the client -> server half of MinestuckUniverse's {@code SkillKeyStates} flow: the raw
 * press/release edge for one of the 3 activation keys. Sent only when the key's down-state actually
 * changes (see {@code client.MSUAbilitechClient}), not every tick - the actual {@link
 * org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState} state machine advancement
 * happens entirely server-side in {@link SkillKeyStates#tickKeyStates()}.
 */
public record AbilitechKeyPacket(AbilitechKey key, boolean pressed) implements MSPacket.PlayToServer
{
	public static final Type<AbilitechKeyPacket> ID = new Type<>(Minestuckuniverseported.id("abilitech/key"));
	public static final StreamCodec<RegistryFriendlyByteBuf, AbilitechKeyPacket> STREAM_CODEC = StreamCodec.composite(
			NeoForgeStreamCodecs.enumCodec(AbilitechKey.class), AbilitechKeyPacket::key,
			ByteBufCodecs.BOOL, AbilitechKeyPacket::pressed,
			AbilitechKeyPacket::new
	);

	@Override
	public void execute(IPayloadContext context, ServerPlayer player)
	{
		SkillKeyStates keyStates = player.getData(MSUAttachments.SKILL_KEY_STATES);
		keyStates.updateKeyState(key, pressed);
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
