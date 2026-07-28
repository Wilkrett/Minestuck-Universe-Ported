package org.wilkretawesomesauce.minestuckuniverseported.util;

// Adapted from com.mt1006.mocap.utils.FakePlayer in the mc-mocap-mod project
// (https://github.com/mt1006/mc-mocap-mod), which is licensed under the GNU Lesser General Public
// License v3.0 (LGPL-3.0-only). This file is a modified derivative of that LGPL-licensed source and is
// itself distributed under the same license; see LICENSE-mocap.txt in the project root for the full
// license text. Original work Copyright (c) mt1006.
//
// A network-less ServerPlayer: every packet-handling method is a no-op, and it's wired to a dummy
// Connection/Channel instead of a real client socket. Used by TechTimeParallelAction and
// mechanics.timeline.DoomedTimelineClone to spawn standing/replaying doubles of a player without a full separate
// entity type.
import com.mojang.authlib.GameProfile;
import io.netty.channel.*;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.stats.Stat;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.portal.DimensionTransition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Field;
import java.net.SocketAddress;
import java.util.Set;

// FakePlayer class from Forge
public class MSUFakePlayer extends ServerPlayer
{
	private static final ClientInformation DEFAULT_CLIENT_INFO = ClientInformation.createDefault();

	/**
	 * {@code ServerPlayer#spawnInvulnerableTime} - the few seconds of "just spawned" invulnerability
	 * every real player gets - is a private field only ever decremented inside {@code ServerPlayer#tick()}
	 * (disabled below), so without this it would sit at its constructed default forever and silently
	 * make every fake player permanently immune to any damage that doesn't carry the
	 * {@code BYPASSES_INVULNERABILITY} tag - i.e. most normal attacks - regardless of
	 * {@link #setInvulnerable} or {@link #canDamageClone}. No public getter/setter exists (confirmed via
	 * {@code javap} against this project's pinned NeoForge jar), and there's no way to call "just the
	 * {@code Player}-level" {@code hurt()} and skip {@code ServerPlayer}'s own override from a subclass -
	 * reflection on this one field, zeroed once at construction, is the narrowest fix available. This
	 * project compiles and runs directly against Mojang-mapped names (not SRG/obfuscated), so looking the
	 * field up by its real name is reliable here, unlike on a production Forge/SRG setup.
	 */
	private static final Field SPAWN_INVULNERABLE_TIME_FIELD;
	static
	{
		try
		{
			SPAWN_INVULNERABLE_TIME_FIELD = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
			SPAWN_INVULNERABLE_TIME_FIELD.setAccessible(true);
		}
		catch(NoSuchFieldException e)
		{
			throw new ExceptionInInitializerError(e);
		}
	}

	public MSUFakePlayer(ServerLevel level, GameProfile profile)
	{
		super(level.getServer(), level, profile, DEFAULT_CLIENT_INFO);
		this.connection = new FakePlayerNetHandler(level.getServer(), this, profile);
		setInvulnerable(true);

		try
		{
			SPAWN_INVULNERABLE_TIME_FIELD.setInt(this, 0);
		}
		catch(IllegalAccessException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Hook for future callers/subclasses to customize whether a given damage source can actually hurt
	 * this fake player - consulted by the {@link #hurt} override below. Default: damageable exactly when
	 * not flagged invulnerable (i.e. honors {@link #setInvulnerable} the same way vanilla normally would).
	 */
	public boolean canDamageClone(DamageSource source)
	{
		return !isInvulnerable();
	}

	@Override
	public boolean hurt(DamageSource source, float amount)
	{
		if(!canDamageClone(source))
			return false;
		return super.hurt(source, amount);
	}

	@Override public Entity changeDimension(@NotNull DimensionTransition dimensionTransition) { return null; }

	@Override public void displayClientMessage(@NotNull Component chatComponent, boolean actionBar) { }
	@Override public void awardStat(@NotNull Stat stat, int amount) { }
	@Override public void die(@NotNull DamageSource source) { }
	@Override public void tick() { }
	@Override public @Nullable MinecraftServer getServer() { return level().getServer(); }

	/**
	 * {@code Player#attackStrengthTicker} only ever advances inside {@code Player#tick()}, which is
	 * disabled above - without this override, any caller that actually invokes {@link #attack(Entity)}
	 * on a fake player (see {@code TechTimeParallelAction}) would find the cooldown permanently stuck
	 * at 0 (never charges back up between hits), making every attack compute as a near-zero-scale weak
	 * hit forever, not just immediately after spawning. Always reporting a full charge is the correct
	 * fix for an entity that never ticks, not a balance choice - whatever rate-limits how often this
	 * fake player actually calls {@code attack()} is the caller's job (e.g. its own cooldown constant).
	 */
	@Override public float getAttackStrengthScale(float adjustTicks) { return 1.0F; }

	/**
	 * Adapted from the mocap mod's {@code PlayingContext#fluentMovement}. Because this player has a dummy
	 * connection, it doesn't participate in the normal server->client entity tracking sync the way a real
	 * {@code Mob} does - moving it with {@code moveTo}/{@code teleportTo} alone updates its position
	 * server-side but doesn't tell any actual client to visually update it. This broadcasts a
	 * {@code ClientboundTeleportEntityPacket} (the same packet vanilla's own tracking system sends for
	 * normal entity movement) to every real player on the server, which is what actually makes this
	 * entity's movement visible - call this after every position/rotation change, not just once at spawn.
	 * <p>
	 * Simplified from the original: mocap only sends to players within a configurable distance of the
	 * entity, as a bandwidth optimization for potentially many simultaneous recordings; this just
	 * broadcasts to everyone, which is fine at this project's much smaller scale (occasional clones, not
	 * dozens of simultaneous scene playbacks).
	 */
	public void broadcastMovement()
	{
		if(getServer() != null)
			getServer().getPlayerList().broadcastAll(new net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket(this));
	}

	@ParametersAreNonnullByDefault
	private static class FakePlayerNetHandler extends ServerGamePacketListenerImpl
	{
		private static final Connection DUMMY_CONNECTION = new DummyConnection(PacketFlow.CLIENTBOUND);

		public FakePlayerNetHandler(MinecraftServer server, ServerPlayer player, GameProfile profile)
		{
			super(server, DUMMY_CONNECTION, player, new CommonListenerCookie(profile, 0, DEFAULT_CLIENT_INFO, false));
		}

		@Override public void tick() { }
		@Override public void resetPosition() { }
		@Override public void disconnect(Component message) { }
		@Override public void handlePlayerInput(ServerboundPlayerInputPacket packet) { }
		@Override public void handleMoveVehicle(ServerboundMoveVehiclePacket packet) { }
		@Override public void handleAcceptTeleportPacket(ServerboundAcceptTeleportationPacket packet) { }
		@Override public void handleRecipeBookSeenRecipePacket(ServerboundRecipeBookSeenRecipePacket packet) { }
		@Override public void handleRecipeBookChangeSettingsPacket(ServerboundRecipeBookChangeSettingsPacket packet) { }
		@Override public void handleSeenAdvancements(ServerboundSeenAdvancementsPacket packet) { }
		@Override public void handleCustomCommandSuggestions(ServerboundCommandSuggestionPacket packet) { }
		@Override public void handleSetCommandBlock(ServerboundSetCommandBlockPacket packet) { }
		@Override public void handleSetCommandMinecart(ServerboundSetCommandMinecartPacket packet) { }
		@Override public void handlePickItem(ServerboundPickItemPacket packet) { }
		@Override public void handleRenameItem(ServerboundRenameItemPacket packet) { }
		@Override public void handleSetBeaconPacket(ServerboundSetBeaconPacket packet) { }
		@Override public void handleSetStructureBlock(ServerboundSetStructureBlockPacket packet) { }
		@Override public void handleSetJigsawBlock(ServerboundSetJigsawBlockPacket packet) { }
		@Override public void handleJigsawGenerate(ServerboundJigsawGeneratePacket packet) { }
		@Override public void handleSelectTrade(ServerboundSelectTradePacket packet) { }
		@Override public void handleEditBook(ServerboundEditBookPacket packet) { }
		@Override public void handleMovePlayer(ServerboundMovePlayerPacket packet) { }
		@Override public void teleport(double x, double y, double z, float yaw, float pitch) { }
		@Override public void handlePlayerAction(ServerboundPlayerActionPacket packet) { }
		@Override public void handleUseItemOn(ServerboundUseItemOnPacket packet) { }
		@Override public void handleUseItem(ServerboundUseItemPacket packet) { }
		@Override public void handleTeleportToEntityPacket(ServerboundTeleportToEntityPacket packet) { }
		@Override public void handlePaddleBoat(ServerboundPaddleBoatPacket packet) { }
		@Override public void send(Packet<?> packet) { }
		@Override public void send(Packet<?> packet, @Nullable PacketSendListener sendListener) { }
		@Override public void handleSetCarriedItem(ServerboundSetCarriedItemPacket packet) { }
		@Override public void handleChat(ServerboundChatPacket packet) { }
		@Override public void handleAnimate(ServerboundSwingPacket packet) { }
		@Override public void handlePlayerCommand(ServerboundPlayerCommandPacket packet) { }
		@Override public void handleInteract(ServerboundInteractPacket packet) { }
		@Override public void handleClientCommand(ServerboundClientCommandPacket packet) { }
		@Override public void handleContainerClose(ServerboundContainerClosePacket packet) { }
		@Override public void handleContainerClick(ServerboundContainerClickPacket packet) { }
		@Override public void handlePlaceRecipe(ServerboundPlaceRecipePacket packet) { }
		@Override public void handleContainerButtonClick(ServerboundContainerButtonClickPacket packet) { }
		@Override public void handleSetCreativeModeSlot(ServerboundSetCreativeModeSlotPacket packet) { }
		@Override public void handleSignUpdate(ServerboundSignUpdatePacket packet) { }
		@Override public void handlePlayerAbilities(ServerboundPlayerAbilitiesPacket packet) { }
		@Override public void handleChangeDifficulty(ServerboundChangeDifficultyPacket packet) { }
		@Override public void handleLockDifficulty(ServerboundLockDifficultyPacket packet) { }
		@Override public void teleport(double x, double y, double z, float yaw, float pitch, Set<RelativeMovement> relativeSet) { }
		@Override public void ackBlockChangesUpTo(int sequence) { }
		@Override public void handleChatCommand(ServerboundChatCommandPacket packet) { }
		@Override public void handleChatAck(ServerboundChatAckPacket packet) { }
		@Override public void addPendingMessage(PlayerChatMessage message) { }
		@Override public void sendPlayerChatMessage(PlayerChatMessage message, ChatType.Bound boundChatType) { }
		@Override public void sendDisguisedChatMessage(Component content, ChatType.Bound boundChatType) { }
		@Override public void handleChatSessionUpdate(ServerboundChatSessionUpdatePacket packet) { }
	}

	@ParametersAreNonnullByDefault
	private static class DummyConnection extends Connection
	{
		private static final Channel DUMMY_CHANNEL = new DummyChannel();

		public DummyConnection(PacketFlow packetFlow)
		{
			super(packetFlow);
		}
		@Override public @NotNull Channel channel() { return DUMMY_CHANNEL; }
	}

	// based on FailedChannel code
	private static class DummyChannel extends AbstractChannel
	{
		private static final ChannelMetadata METADATA = new ChannelMetadata(false);
		private final ChannelConfig config = new DefaultChannelConfig(this);

		DummyChannel() { super(null); }

		@Override protected AbstractUnsafe newUnsafe() { return new FailedChannelUnsafe(); }
		@Override protected boolean isCompatible(EventLoop loop) { return false; }
		@Override protected SocketAddress localAddress0() { return null; }
		@Override protected SocketAddress remoteAddress0() { return null; }
		@Override protected void doBind(SocketAddress localAddress) {}
		@Override protected void doDisconnect() {}
		@Override protected void doClose() {}
		@Override protected void doBeginRead() {}
		@Override protected void doWrite(ChannelOutboundBuffer in) {}
		@Override public ChannelConfig config() { return config; }
		@Override public boolean isOpen() { return false; }
		@Override public boolean isActive() { return false; }
		@Override public ChannelMetadata metadata() { return METADATA; }

		private final class FailedChannelUnsafe extends AbstractUnsafe
		{
			@Override public void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {}
		}
	}
}
