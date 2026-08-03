package org.wilkretawesomesauce.minestuckuniverseported.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class MSUPayloads
{
	private MSUPayloads()
	{
	}

	@SubscribeEvent
	private static void register(RegisterPayloadHandlersEvent event)
	{
		PayloadRegistrar registrar = event.registrar(Minestuckuniverseported.MODID).versioned("1");

		registrar.playToClient(StrifePortfolioSyncPacket.ID, StrifePortfolioSyncPacket.STREAM_CODEC,
				com.mraof.minestuck.network.MSPacket.PlayToClient::execute);
		registrar.playToClient(StrifeIndexesSyncPacket.ID, StrifeIndexesSyncPacket.STREAM_CODEC,
				com.mraof.minestuck.network.MSPacket.PlayToClient::execute);

		registrar.playToServer(MSUStrifeRequestPackets.SetActiveStrife.ID, MSUStrifeRequestPackets.SetActiveStrife.STREAM_CODEC,
				(payload, context) -> payload.execute(context, (net.minecraft.server.level.ServerPlayer) context.player()));
		registrar.playToServer(MSUStrifeRequestPackets.RetrieveStrife.ID, MSUStrifeRequestPackets.RetrieveStrife.STREAM_CODEC,
				(payload, context) -> payload.execute(context, (net.minecraft.server.level.ServerPlayer) context.player()));
		registrar.playToServer(MSUStrifeRequestPackets.SwapOffhandStrife.ID, MSUStrifeRequestPackets.SwapOffhandStrife.STREAM_CODEC,
				(payload, context) -> payload.execute(context, (net.minecraft.server.level.ServerPlayer) context.player()));
		registrar.playToServer(MSUStrifeRequestPackets.AssignStrifeKind.ID, MSUStrifeRequestPackets.AssignStrifeKind.STREAM_CODEC,
				(payload, context) -> payload.execute(context, (net.minecraft.server.level.ServerPlayer) context.player()));
		registrar.playToServer(MSUStrifeRequestPackets.AssignHeldItem.ID, MSUStrifeRequestPackets.AssignHeldItem.STREAM_CODEC,
				(payload, context) -> payload.execute(context, (net.minecraft.server.level.ServerPlayer) context.player()));
		registrar.playToServer(MSUStrifeRequestPackets.UpdateStrifeIndexes.ID, MSUStrifeRequestPackets.UpdateStrifeIndexes.STREAM_CODEC,
				(payload, context) -> payload.execute(context, (net.minecraft.server.level.ServerPlayer) context.player()));

		registrar.playToServer(AbilitechKeyPacket.ID, AbilitechKeyPacket.STREAM_CODEC,
				(payload, context) -> payload.execute(context, (net.minecraft.server.level.ServerPlayer) context.player()));
		registrar.playToServer(AbilitechRequestPackets.Equip.ID, AbilitechRequestPackets.Equip.STREAM_CODEC,
				(payload, context) -> payload.execute(context, (net.minecraft.server.level.ServerPlayer) context.player()));
		registrar.playToServer(AbilitechRequestPackets.Unequip.ID, AbilitechRequestPackets.Unequip.STREAM_CODEC,
				(payload, context) -> payload.execute(context, (net.minecraft.server.level.ServerPlayer) context.player()));
		registrar.playToServer(AbilitechRequestPackets.TogglePassive.ID, AbilitechRequestPackets.TogglePassive.STREAM_CODEC,
				(payload, context) -> payload.execute(context, (net.minecraft.server.level.ServerPlayer) context.player()));

		registrar.playToClient(AbilitechLoadoutSyncPacket.ID, AbilitechLoadoutSyncPacket.STREAM_CODEC,
				com.mraof.minestuck.network.MSPacket.PlayToClient::execute);

		registrar.playToClient(TimeRequestSyncPacket.ID, TimeRequestSyncPacket.STREAM_CODEC,
				com.mraof.minestuck.network.MSPacket.PlayToClient::execute);

		registrar.playToClient(BeamSyncPacket.ID, BeamSyncPacket.STREAM_CODEC,
				com.mraof.minestuck.network.MSPacket.PlayToClient::execute);

		registrar.playToServer(MindControlInputPacket.ID, MindControlInputPacket.STREAM_CODEC,
				(payload, context) -> payload.execute(context, (net.minecraft.server.level.ServerPlayer) context.player()));
		registrar.playToClient(MindControlSyncPacket.ID, MindControlSyncPacket.STREAM_CODEC,
				com.mraof.minestuck.network.MSPacket.PlayToClient::execute);

		registrar.playToClient(CloakSyncPacket.ID, CloakSyncPacket.STREAM_CODEC,
				com.mraof.minestuck.network.MSPacket.PlayToClient::execute);

		registrar.playToClient(StreakStateSyncPacket.ID, StreakStateSyncPacket.STREAM_CODEC,
				com.mraof.minestuck.network.MSPacket.PlayToClient::execute);

		registrar.playToClient(OpenSkillShopPacket.ID, OpenSkillShopPacket.STREAM_CODEC,
				com.mraof.minestuck.network.MSPacket.PlayToClient::execute);
		registrar.playToServer(SkillShopRequestPackets.Purchase.ID, SkillShopRequestPackets.Purchase.STREAM_CODEC,
				(payload, context) -> payload.execute(context, (net.minecraft.server.level.ServerPlayer) context.player()));

		registrar.playToServer(BadgeBuilderFillPacket.ID, BadgeBuilderFillPacket.STREAM_CODEC,
				(payload, context) -> payload.execute(context, (net.minecraft.server.level.ServerPlayer) context.player()));
		registrar.playToClient(BuilderBadgeSyncPacket.ID, BuilderBadgeSyncPacket.STREAM_CODEC,
				com.mraof.minestuck.network.MSPacket.PlayToClient::execute);

		registrar.playToClient(ConsortHatSyncPacket.ID, ConsortHatSyncPacket.STREAM_CODEC,
				com.mraof.minestuck.network.MSPacket.PlayToClient::execute);

		registrar.playToClient(ConsortChestSyncPacket.ID, ConsortChestSyncPacket.STREAM_CODEC,
				com.mraof.minestuck.network.MSPacket.PlayToClient::execute);

		registrar.playToClient(ManipulatorSelectionSyncPacket.ID, ManipulatorSelectionSyncPacket.STREAM_CODEC,
				com.mraof.minestuck.network.MSPacket.PlayToClient::execute);

		registrar.playToClient(TetherBondSyncPacket.ID, TetherBondSyncPacket.STREAM_CODEC,
				com.mraof.minestuck.network.MSPacket.PlayToClient::execute);
		registrar.playToClient(TetherBondImpactPacket.ID, TetherBondImpactPacket.STREAM_CODEC,
				com.mraof.minestuck.network.MSPacket.PlayToClient::execute);

		registrar.playToClient(WindRibbonSyncPacket.ID, WindRibbonSyncPacket.STREAM_CODEC,
				com.mraof.minestuck.network.MSPacket.PlayToClient::execute);
		registrar.playToClient(WindBurstPacket.ID, WindBurstPacket.STREAM_CODEC,
				com.mraof.minestuck.network.MSPacket.PlayToClient::execute);
	}
}
