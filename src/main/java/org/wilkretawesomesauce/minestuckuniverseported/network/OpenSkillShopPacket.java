package org.wilkretawesomesauce.minestuckuniverseported.network;

import com.mraof.minestuck.network.MSPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.client.gui.SkillShopScreen;

/**
 * Sent by {@code command.SkillShopCommand} (itself reached from a real Consort dialogue response via
 * Minestuck's own real {@code Trigger.Command} - see that command class's own doc comment for why a
 * command is the real trigger point here, not a custom {@code Trigger} implementation) to open the real
 * Skill Shop screen client-side - the modern equivalent of the original's own
 * {@code MSUPacket.Type.OPEN_GUI} packet. Empty payload - there's nothing to carry, the screen reads the
 * already-synced local {@code AbilitechLoadout}/tech registry directly.
 */
public record OpenSkillShopPacket() implements MSPacket.PlayToClient
{
	public static final Type<OpenSkillShopPacket> ID = new Type<>(Minestuckuniverseported.id("abilitech/open_skill_shop"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenSkillShopPacket> STREAM_CODEC =
			StreamCodec.unit(new OpenSkillShopPacket());

	@Override
	public void execute(IPayloadContext context)
	{
		Minecraft.getInstance().setScreen(new SkillShopScreen());
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
