package org.wilkretawesomesauce.minestuckuniverseported.network;

import com.mraof.minestuck.network.MSPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time.TechTimeLoopBeta;

/**
 * Sent by {@code client.gui.TimeLoopRewindScreen} in answer to Timeloop &beta;'s real death-save prompt
 * (see {@link TechTimeLoopBeta}'s own doc comment for the full design) - {@code rewind=true} for the
 * "Rewind Time" button, {@code false} for "Let It Be". {@link TechTimeLoopBeta#resolvePrompt} does nothing
 * if the prompt has already been resolved or has expired server-side (a stale click racing the prompt's
 * own timeout), so this is safe to receive at most once meaningfully per death.
 */
public record TimeLoopRewindDecisionPacket(boolean rewind) implements MSPacket.PlayToServer
{
	public static final Type<TimeLoopRewindDecisionPacket> ID = new Type<>(Minestuckuniverseported.id("time_loop_beta/rewind_decision"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TimeLoopRewindDecisionPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, TimeLoopRewindDecisionPacket::rewind,
			TimeLoopRewindDecisionPacket::new
	);

	@Override
	public void execute(IPayloadContext context, ServerPlayer player)
	{
		TechTimeLoopBeta.resolvePrompt(player, rewind);
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
