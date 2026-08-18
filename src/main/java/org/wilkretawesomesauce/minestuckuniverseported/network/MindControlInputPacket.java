package org.wilkretawesomesauce.minestuckuniverseported.network;

import com.mraof.minestuck.network.MSPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.badgeEffects.BadgeEffects;
import org.wilkretawesomesauce.minestuckuniverseported.skills.MSUSkills;

/**
 * Client -&gt; server half of {@code abilitech.heroAspect.mind.TechMindControl} ("Mindflayer's Spell")'s
 * real player-movement puppeting - the modern equivalent of the original's
 * {@code MSUPacket.Type.MINDFLAYER_MOVEMENT_INPUT}. Sent every client tick by
 * {@code client.MindControlClientEvents} while the controller carries
 * {@code abilitech.heroAspect.mind.MindControllingEffect}, carrying their own movement input already
 * converted to a world-relative vector (via their own head yaw) exactly like the original did before
 * handing it to the server. The server relays it to whichever player they're actually possessing (found
 * by scanning the sender's own {@link BadgeEffects} tethers for an active
 * {@link MSUSkills#MIND_CONTROL} target) via {@link MindControlSyncPacket}.
 */
public record MindControlInputPacket(float worldX, float worldZ, boolean jump, boolean sneak) implements MSPacket.PlayToServer
{
	public static final Type<MindControlInputPacket> ID = new Type<>(Minestuckuniverseported.id("mind/control_input"));
	public static final StreamCodec<RegistryFriendlyByteBuf, MindControlInputPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, MindControlInputPacket::worldX,
			ByteBufCodecs.FLOAT, MindControlInputPacket::worldZ,
			ByteBufCodecs.BOOL, MindControlInputPacket::jump,
			ByteBufCodecs.BOOL, MindControlInputPacket::sneak,
			MindControlInputPacket::new
	);

	@Override
	public void execute(IPayloadContext context, ServerPlayer player)
	{
		GodTierData godTier = player.getData(MSUAttachments.GOD_TIER);
		BadgeEffects badgeEffects = player.getData(MSUAttachments.BADGE_EFFECTS);

		for(int slot = 0; slot < GodTierData.TECH_SLOTS; slot++)
		{
			if(godTier.getTech(slot) != MSUSkills.MIND_CONTROL)
				continue;

			if(badgeEffects.getTether(slot) instanceof ServerPlayer target)
			{
				PacketDistributor.sendToPlayer(target, new MindControlSyncPacket(true, worldX, worldZ, jump, sneak));
				return;
			}
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
