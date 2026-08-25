package org.wilkretawesomesauce.minestuckuniverseported.network;

import com.mraof.minestuck.network.MSPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.client.jukinator.JukinatorScreen;

/**
 * Sent by {@code item.JukinatorItem}'s {@code use()} to open the real rhythm-minigame screen client-side -
 * same shape as {@code OpenSkillShopPacket}, the proven pattern in this codebase for triggering a
 * client-only {@link net.minecraft.client.gui.screens.Screen} from a server-resolved decision. Carries the
 * loaded disc stack (the client already has this synced via {@code MSUItemComponents#STORED_DISC}, but the
 * screen needs its own detached copy to generate the chart from, and passing it explicitly avoids having to
 * re-derive "which hand is the Jukinator in" client-side) and {@code seededChart} - resolved server-side
 * from {@code MSUGameRules#JUKINATOR_RANDOM_CHARTS} because custom gamerules are never synced to clients
 * (confirmed via {@code javap} against {@code ClientboundLoginPacket} - see that gamerule class's own doc
 * comment).
 */
public record OpenJukinatorPacket(ItemStack disc, boolean seededChart) implements MSPacket.PlayToClient
{
	public static final Type<OpenJukinatorPacket> ID = new Type<>(Minestuckuniverseported.id("jukinator/open"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenJukinatorPacket> STREAM_CODEC = StreamCodec.composite(
			ItemStack.STREAM_CODEC, OpenJukinatorPacket::disc,
			ByteBufCodecs.BOOL, OpenJukinatorPacket::seededChart,
			OpenJukinatorPacket::new
	);

	@Override
	public void execute(IPayloadContext context)
	{
		Minecraft.getInstance().setScreen(JukinatorScreen.create(disc, seededChart));
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
