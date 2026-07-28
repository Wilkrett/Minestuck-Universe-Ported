package org.wilkretawesomesauce.minestuckuniverseported.network;

import com.mraof.minestuck.network.MSPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.Abilitech;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRegistry;

/**
 * Client -> server requests sent by {@code client.gui.SkillShopScreen}. Unlike the original's own real
 * {@code GuiSkillShop} (which mutated its own client-side capability state immediately on a Buy click,
 * then only told the server about it after the fact - real server-side re-validation only happened for
 * its {@code Badge}/{@code MasterBadge} purchase branch, not its plain {@code Abilitech} one), this port
 * makes every purchase server-authoritative instead: the client only ever <i>requests</i> a purchase,
 * the server is the only place {@code canUnlock}/{@code onUnlock} actually run, and the client finds out
 * whether it worked from the resulting {@code AbilitechLoadoutSyncPacket} (already sent on any loadout
 * change) rather than assuming success up front. A deliberate hardening over the original, not a silent
 * behavior change - see this project's own planning notes for why.
 */
public final class SkillShopRequestPackets
{
	private SkillShopRequestPackets()
	{
	}

	public record Purchase(ResourceLocation techId) implements MSPacket.PlayToServer
	{
		public static final Type<Purchase> ID = new Type<>(Minestuckuniverseported.id("abilitech/shop_purchase"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Purchase> STREAM_CODEC = StreamCodec.composite(
				ResourceLocation.STREAM_CODEC, Purchase::techId,
				Purchase::new
		);

		@Override
		public void execute(IPayloadContext context, ServerPlayer player)
		{
			Abilitech tech = MSUAbilitechRegistry.get(techId);
			if(tech == null)
				return;

			GodTierData godTier = player.getData(MSUAttachments.GOD_TIER);
			if(godTier.isUnlocked(tech))
				return;

			if(!tech.canUnlock(player.level(), player))
				return;

			tech.onUnlock(player.level(), player);
			godTier.markUnlocked(tech);
			MSUAbilitechPackets.sendLoadoutSync(player);
		}

		@Override
		public Type<? extends CustomPacketPayload> type()
		{
			return ID;
		}
	}
}
