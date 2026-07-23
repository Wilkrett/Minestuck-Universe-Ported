package org.wilkretawesomesauce.minestuckuniverseported.network;

import com.mraof.minestuck.network.MSPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.MSUItemComponents;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.strife.KindAbstratus;
import org.wilkretawesomesauce.minestuckuniverseported.strife.MSUKindAbstrataRegistry;
import org.wilkretawesomesauce.minestuckuniverseported.strife.StrifePortfolio;
import org.wilkretawesomesauce.minestuckuniverseported.strife.StrifePortfolioHandler;
import org.wilkretawesomesauce.minestuckuniverseported.strife.StrifeSpecibus;
import org.wilkretawesomesauce.minestuckuniverseported.strife.StrifeSpecibusData;

/**
 * Client -> server packets sent by the strife GUI screens. Ported/consolidated from the 1.12.2
 * originals: {@code SetActiveStrifePacket}, {@code RetrieveStrifeCardPacket}, {@code SwapOffhandStrifePacket},
 * and the specibus-injecting half of {@code AssignStrifePacket}.
 */
public final class MSUStrifeRequestPackets
{
	private MSUStrifeRequestPackets()
	{
	}

	/** Sets which specibus (or, within the active specibus, which weapon slot) is selected. */
	public record SetActiveStrife(int index, boolean isSpecibus) implements MSPacket.PlayToServer
	{
		public static final Type<SetActiveStrife> ID = new Type<>(Minestuckuniverseported.id("strife/set_active"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SetActiveStrife> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.INT, SetActiveStrife::index,
				ByteBufCodecs.BOOL, SetActiveStrife::isSpecibus,
				SetActiveStrife::new
		);

		@Override
		public void execute(IPayloadContext context, ServerPlayer player)
		{
			StrifePortfolio cap = player.getData(MSUAttachments.STRIFE_PORTFOLIO);
			if(isSpecibus)
			{
				cap.setSelectedSpecibusIndex(index);
				cap.setArmed(false);

				for(InteractionHand hand : InteractionHand.values())
					if(StrifePortfolioHandler.isHeldWeapon(player, player.getItemInHand(hand)))
						player.setItemInHand(hand, ItemStack.EMPTY);
			}
			else cap.setSelectedWeaponIndex(index);

			MSUStrifePackets.sendIndexesSync(player);
		}

		@Override
		public Type<? extends CustomPacketPayload> type()
		{
			return ID;
		}
	}

	/** Pulls a specibus out of the portfolio as a card ({@code isCard}), or pulls the selected weapon into hand. */
	public record RetrieveStrife(int index, boolean isCard, InteractionHand hand) implements MSPacket.PlayToServer
	{
		public static final Type<RetrieveStrife> ID = new Type<>(Minestuckuniverseported.id("strife/retrieve"));
		public static final StreamCodec<RegistryFriendlyByteBuf, RetrieveStrife> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.INT, RetrieveStrife::index,
				ByteBufCodecs.BOOL, RetrieveStrife::isCard,
				NeoForgeStreamCodecs.enumCodec(InteractionHand.class), RetrieveStrife::hand,
				RetrieveStrife::new
		);

		@Override
		public void execute(IPayloadContext context, ServerPlayer player)
		{
			if(isCard)
				StrifePortfolioHandler.retrieveCard(player, index);
			else
				StrifePortfolioHandler.retrieveWeapon(player, index, hand);
		}

		@Override
		public Type<? extends CustomPacketPayload> type()
		{
			return ID;
		}
	}

	public record SwapOffhandStrife(int specibusIndex, int weaponIndex) implements MSPacket.PlayToServer
	{
		public static final Type<SwapOffhandStrife> ID = new Type<>(Minestuckuniverseported.id("strife/swap_offhand"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SwapOffhandStrife> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.INT, SwapOffhandStrife::specibusIndex,
				ByteBufCodecs.INT, SwapOffhandStrife::weaponIndex,
				SwapOffhandStrife::new
		);

		@Override
		public void execute(IPayloadContext context, ServerPlayer player)
		{
			StrifePortfolioHandler.swapOffhandWeapon(player, specibusIndex, weaponIndex);
		}

		@Override
		public Type<? extends CustomPacketPayload> type()
		{
			return ID;
		}
	}

	/**
	 * Sent by {@link org.wilkretawesomesauce.minestuckuniverseported.client.gui.MSUKindSelectScreen} when
	 * picking a kind for a blank strife card held in {@code hand}: stamps that kind onto the card, then
	 * immediately assigns it into the portfolio (mirrors the original's combined
	 * inject-then-{@code assignStrife} behaviour).
	 */
	public record AssignStrifeKind(ResourceLocation kind, InteractionHand hand) implements MSPacket.PlayToServer
	{
		public static final Type<AssignStrifeKind> ID = new Type<>(Minestuckuniverseported.id("strife/assign_kind"));
		public static final StreamCodec<RegistryFriendlyByteBuf, AssignStrifeKind> STREAM_CODEC = StreamCodec.composite(
				ResourceLocation.STREAM_CODEC, AssignStrifeKind::kind,
				NeoForgeStreamCodecs.enumCodec(InteractionHand.class), AssignStrifeKind::hand,
				AssignStrifeKind::new
		);

		@Override
		public void execute(IPayloadContext context, ServerPlayer player)
		{
			KindAbstratus kindAbstratus = MSUKindAbstrataRegistry.get(kind);
			if(kindAbstratus == null)
				return;

			ItemStack stack = player.getItemInHand(hand);
			if(!(stack.getItem() instanceof org.wilkretawesomesauce.minestuckuniverseported.items.StrifeCardItem))
				return;

			stack.set(MSUItemComponents.STRIFE_SPECIBUS, StrifeSpecibusData.fromSpecibus(new StrifeSpecibus(kindAbstratus)));
			StrifePortfolioHandler.assignStrife(player, hand);
		}

		@Override
		public Type<? extends CustomPacketPayload> type()
		{
			return ID;
		}
	}

	/**
	 * Sent when pressing the strife key while holding a plain (non-card) item - the "allocate" half of
	 * "Strife Allocate/Retrieve": assigns whatever's in {@code hand} as a weapon, same as
	 * {@link StrifePortfolioHandler#assignStrife}. Also handles cards, for symmetry, though the GUI path
	 * normally uses {@link AssignStrifeKind} for those instead.
	 */
	public record AssignHeldItem(InteractionHand hand) implements MSPacket.PlayToServer
	{
		public static final Type<AssignHeldItem> ID = new Type<>(Minestuckuniverseported.id("strife/assign_held"));
		public static final StreamCodec<RegistryFriendlyByteBuf, AssignHeldItem> STREAM_CODEC = StreamCodec.composite(
				NeoForgeStreamCodecs.enumCodec(InteractionHand.class), AssignHeldItem::hand,
				AssignHeldItem::new
		);

		@Override
		public void execute(IPayloadContext context, ServerPlayer player)
		{
			StrifePortfolioHandler.assignStrife(player, hand);
		}

		@Override
		public Type<? extends CustomPacketPayload> type()
		{
			return ID;
		}
	}

	/**
	 * Atomically sets both the selected specibus and weapon index, without {@link SetActiveStrife}'s
	 * side effect of clearing held weapons - used when the strife-key HUD switcher is released, since by
	 * that point the correct weapon is about to be equipped anyway (via {@link RetrieveStrife} or
	 * {@link SwapOffhandStrife} sent right after).
	 */
	public record UpdateStrifeIndexes(int specibusIndex, int weaponIndex) implements MSPacket.PlayToServer
	{
		public static final Type<UpdateStrifeIndexes> ID = new Type<>(Minestuckuniverseported.id("strife/update_indexes"));
		public static final StreamCodec<RegistryFriendlyByteBuf, UpdateStrifeIndexes> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.INT, UpdateStrifeIndexes::specibusIndex,
				ByteBufCodecs.INT, UpdateStrifeIndexes::weaponIndex,
				UpdateStrifeIndexes::new
		);

		@Override
		public void execute(IPayloadContext context, ServerPlayer player)
		{
			StrifePortfolio cap = player.getData(MSUAttachments.STRIFE_PORTFOLIO);
			cap.setSelectedSpecibusIndex(specibusIndex);
			cap.setSelectedWeaponIndex(weaponIndex);
		}

		@Override
		public Type<? extends CustomPacketPayload> type()
		{
			return ID;
		}
	}
}
