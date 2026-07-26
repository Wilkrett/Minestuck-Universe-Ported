package org.wilkretawesomesauce.minestuckuniverseported.network;

import com.mraof.minestuck.network.MSPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;

/**
 * Same reasoning as {@code StrifePortfolioSyncPacket}: data attachments aren't automatically synced to
 * the client, so without this the loadout/shop screens would just show an empty/stale state until
 * something else happened to trigger a sync. Sent on login/respawn and after any equip/unequip/passive
 * toggle/unlock.
 * <p>
 * Carries {@code capabilities.godTier.GodTierData}'s NBT (the real tech-equip-slot/unlock-tracking data
 * {@code MSUAbilitechScreen}/{@code SkillShopScreen} both read) - <i>not</i> {@code AbilitechLoadout}'s,
 * despite the packet's name. That's a holdover from when this packet carried both (back when
 * {@code AbilitechLoadout} still held the tech-equip-slot fields): once those moved to
 * {@code GodTierData} and the badgeEffects-derived scratch fields moved to a real
 * {@code capabilities.badgeEffects.BadgeEffects} attachment (see each class's own doc comment),
 * {@code AbilitechLoadout} itself was left with nothing that actually needs client-side visibility -
 * its own {@code serializeNBT} is now always empty, so sending it here would've been pure waste. The
 * packet keeps its established name/id rather than churning every call site for a rename; what it
 * carries is what actually needs syncing, not what the name literally says.
 */
public record AbilitechLoadoutSyncPacket(CompoundTag godTierNbt) implements MSPacket.PlayToClient
{
	public static final Type<AbilitechLoadoutSyncPacket> ID = new Type<>(Minestuckuniverseported.id("abilitech/loadout_sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, AbilitechLoadoutSyncPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.COMPOUND_TAG, AbilitechLoadoutSyncPacket::godTierNbt,
			AbilitechLoadoutSyncPacket::new
	);

	public static AbilitechLoadoutSyncPacket create(Player player)
	{
		GodTierData godTier = player.getData(MSUAttachments.GOD_TIER);
		return new AbilitechLoadoutSyncPacket(godTier.serializeNBT(player.registryAccess()));
	}

	@Override
	public void execute(IPayloadContext context)
	{
		Player player = context.player();
		player.getData(MSUAttachments.GOD_TIER).deserializeNBT(player.registryAccess(), godTierNbt);
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
