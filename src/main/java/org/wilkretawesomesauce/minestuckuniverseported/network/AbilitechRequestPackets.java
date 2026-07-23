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
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.Abilitech;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.AbilitechLoadout;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRegistry;

/**
 * Client -> server requests sent by {@code client.gui.MSUAbilitechScreen}, the ported
 * {@code gui.GuiFraymachine}. Ported/consolidated from the original's
 * {@code MSUPacket.Type.EQUIP_ABILITECH}/{@code UNEQUIP_ABILITECH} plus the passive-toggle path
 * (originally handled through {@code IGodTierData#setSkillPassiveEnabled} directly, since that
 * capability was itself synced by a full-data update packet rather than a dedicated one).
 */
public final class AbilitechRequestPackets
{
	private AbilitechRequestPackets()
	{
	}

	public record Equip(ResourceLocation techId, int slot) implements MSPacket.PlayToServer
	{
		public static final Type<Equip> ID = new Type<>(Minestuckuniverseported.id("abilitech/equip"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Equip> STREAM_CODEC = StreamCodec.composite(
				ResourceLocation.STREAM_CODEC, Equip::techId,
				ByteBufCodecs.INT, Equip::slot,
				Equip::new
		);

		@Override
		public void execute(IPayloadContext context, ServerPlayer player)
		{
			Abilitech tech = MSUAbilitechRegistry.get(techId);
			if(tech == null)
				return;

			AbilitechLoadout loadout = player.getData(MSUAttachments.ABILITECH_LOADOUT);
			if(loadout.isTechEquipped(tech))
				return;

			// Real gate, matching the original's own "equip only from what you already own" split - see
			// abilitech.TechBoondollarCost/the real unlock economy this project now has. Unlocking itself
			// happens through the real shop screen (once built) or, in the interim, the creative-only
			// /msu unlock debug command.
			if(!loadout.isUnlocked(tech))
				return;

			if(loadout.getTech(slot) != null)
				loadout.unequipTech(player.level(), player, slot);
			loadout.equipTech(player.level(), player, tech, slot);
			MSUAbilitechPackets.sendLoadoutSync(player);
		}

		@Override
		public Type<? extends CustomPacketPayload> type()
		{
			return ID;
		}
	}

	public record Unequip(int slot) implements MSPacket.PlayToServer
	{
		public static final Type<Unequip> ID = new Type<>(Minestuckuniverseported.id("abilitech/unequip"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Unequip> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.INT, Unequip::slot,
				Unequip::new
		);

		@Override
		public void execute(IPayloadContext context, ServerPlayer player)
		{
			player.getData(MSUAttachments.ABILITECH_LOADOUT).unequipTech(player.level(), player, slot);
			MSUAbilitechPackets.sendLoadoutSync(player);
		}

		@Override
		public Type<? extends CustomPacketPayload> type()
		{
			return ID;
		}
	}

	public record TogglePassive(int slot) implements MSPacket.PlayToServer
	{
		public static final Type<TogglePassive> ID = new Type<>(Minestuckuniverseported.id("abilitech/toggle_passive"));
		public static final StreamCodec<RegistryFriendlyByteBuf, TogglePassive> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.INT, TogglePassive::slot,
				TogglePassive::new
		);

		@Override
		public void execute(IPayloadContext context, ServerPlayer player)
		{
			AbilitechLoadout loadout = player.getData(MSUAttachments.ABILITECH_LOADOUT);
			Abilitech tech = loadout.getTech(slot);
			if(tech == null)
				return;

			boolean enabled = !loadout.isPassiveEnabled(slot);
			loadout.setPassiveEnabled(slot, enabled);
			tech.onPassiveToggle(player.level(), player, enabled);
			MSUAbilitechPackets.sendLoadoutSync(player);
		}

		@Override
		public Type<? extends CustomPacketPayload> type()
		{
			return ID;
		}
	}
}
