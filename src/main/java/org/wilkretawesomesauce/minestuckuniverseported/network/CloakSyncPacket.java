package org.wilkretawesomesauce.minestuckuniverseported.network;

import com.mraof.minestuck.network.MSPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.mind.CloakClientState;

/**
 * Broadcasts a real disguise state to every observer - the modern equivalent of the original's
 * {@code IBadgeEffects#getCloakData()} NBT (which every client that could see the cloaked player
 * already had access to server-side; a plain {@link net.minecraft.world.effect.MobEffectInstance}
 * marker can't carry an arbitrary {@code EntityType} payload the way {@code WindFormedEffect} carries a
 * boolean, so this dedicated broadcast packet fills that role instead). Sent by
 * {@code abilitech.heroAspect.mind.TechMindCloak} on cloak/uncloak to every player currently tracking
 * the caster, and by {@code CloakTrackingEvents} to a player the instant they start tracking an already-
 * cloaked entity (covers late joiners and anyone just entering render distance).
 */
public record CloakSyncPacket(int entityId, boolean cloaked, ResourceLocation entityType) implements MSPacket.PlayToClient
{
	public static final Type<CloakSyncPacket> ID = new Type<>(Minestuckuniverseported.id("mind/cloak_sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CloakSyncPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, CloakSyncPacket::entityId,
			ByteBufCodecs.BOOL, CloakSyncPacket::cloaked,
			ResourceLocation.STREAM_CODEC, CloakSyncPacket::entityType,
			CloakSyncPacket::new
	);

	@Override
	public void execute(IPayloadContext context)
	{
		if(cloaked)
			CloakClientState.setCloaked(entityId, entityType);
		else
			CloakClientState.clearCloaked(entityId);
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
