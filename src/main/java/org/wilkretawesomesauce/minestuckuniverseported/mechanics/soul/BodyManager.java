package org.wilkretawesomesauce.minestuckuniverseported.mechanics.soul;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.wilkretawesomesauce.minestuckuniverseported.MSUEntityTypes;
import org.wilkretawesomesauce.minestuckuniverseported.entity.UninhabitedBodyEntity;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Design doc §36 ("BodyManager - create/reuse bodies, enforce one body per owner, handle body death,
 * schedule replacement bodies, resolve body ownership"), backed by {@link BodyRegistryData} (see that
 * class's own doc comment for why ownership bookkeeping is a separate registry, not part of
 * {@link PlayerSoul}). Original design for this project, no 1.12.2 counterpart. Static-only, mirrors
 * {@code mechanics.mind.DecisionManager}'s shape - the only real consumer is {@link SoulManager}, plus
 * {@code SoulProjectionEvents} for the death hook and replacement tick.
 */
public final class BodyManager
{
	/** "A short delay" (design doc §20) - 5 seconds. */
	private static final int REPLACEMENT_DELAY_TICKS = 100;

	private static final Map<UUID, PendingReplacement> pendingReplacements = new HashMap<>();

	private BodyManager()
	{
	}

	private static BodyRegistryData registry(MinecraftServer server)
	{
		return server.overworld().getData(MSUAttachments.BODY_REGISTRY);
	}

	/**
	 * Pure lookup, no creation - searches every loaded level for the registered body entity (a body may
	 * exist in any dimension the owner last left it in, not just the Overworld the registry itself lives
	 * in). Returns {@code null} if no body is registered, or the registered one isn't currently resolvable
	 * (unloaded chunk - design doc §29, "retain the relationship and resolve it when the entity becomes
	 * available"; callers that need to force it to exist should use {@link #getOrCreateBody} instead).
	 */
	@Nullable
	public static UninhabitedBodyEntity resolveBody(MinecraftServer server, UUID ownerUUID)
	{
		UUID bodyId = registry(server).getBody(ownerUUID);
		if(bodyId == null)
			return null;

		for(ServerLevel level : server.getAllLevels())
		{
			Entity entity = level.getEntity(bodyId);
			if(entity instanceof UninhabitedBodyEntity body)
				return body;
		}
		return null;
	}

	/** Real caller: {@code SoulManager#leaveBody}. Reuses an existing, resolvable body if the registry already has one for this owner; otherwise spawns fresh at the owner's current position (design doc §6, "must appear at the player's position"). */
	static UninhabitedBodyEntity getOrCreateBody(ServerPlayer owner)
	{
		UninhabitedBodyEntity existing = resolveBody(owner.getServer(), owner.getUUID());
		if(existing != null)
			return existing;

		ServerLevel level = (ServerLevel) owner.level();
		UninhabitedBodyEntity body = UninhabitedBodyEntity.create(level, owner);
		level.addFreshEntity(body);
		registry(owner.getServer()).setBody(owner.getUUID(), body.getUUID());
		return body;
	}

	/** Real caller: {@code SoulManager#inhabitBody}'s own-body reclaim path - the body is being fully retired (design doc §10 "the normal player representation should resume"), not just vacated. */
	static void retire(UninhabitedBodyEntity body)
	{
		UUID owner = body.getOwnerUUID();
		if(owner != null)
			registry(body.getServer()).clearBody(owner);
		body.discard();
	}

	/**
	 * Design doc §20 ("Uninhabited Body Death"). Real caller: {@code SoulProjectionEvents}' death
	 * listener. Ejects any current occupant first (their held inventory/XP/health/effects are, by
	 * {@code SoulManager}'s own "swap, never duplicate" rule, already sitting on this body's own fields by
	 * the time this runs - see {@link SoulManager#ejectOccupant}), then drops contents/XP like an ordinary
	 * player death, removes the body, and schedules a fresh replacement.
	 */
	public static void onBodyDeath(UninhabitedBodyEntity body)
	{
		if(body.getOccupyingSoul() != null)
			SoulManager.ejectOccupant(body);

		body.dropContentsAndXp();

		UUID ownerUUID = body.getOwnerUUID();
		String ownerName = body.getOwnerName();
		MinecraftServer server = body.getServer();
		ServerLevel deathLevel = (ServerLevel) body.level();

		if(ownerUUID != null)
			registry(server).clearBody(ownerUUID);

		body.discard();

		if(ownerUUID != null)
			pendingReplacements.put(ownerUUID, new PendingReplacement(ownerUUID, deathLevel, ownerName, REPLACEMENT_DELAY_TICKS));
	}

	/** Real caller: {@code SoulProjectionEvents}' server tick handler - ticks every pending replacement countdown down and spawns once it elapses. */
	public static void tickReplacements(MinecraftServer server)
	{
		if(pendingReplacements.isEmpty())
			return;

		Iterator<Map.Entry<UUID, PendingReplacement>> it = pendingReplacements.entrySet().iterator();
		while(it.hasNext())
		{
			Map.Entry<UUID, PendingReplacement> entry = it.next();
			PendingReplacement pending = entry.getValue();
			if(--pending.ticksRemaining > 0)
				continue;

			it.remove();
			UUID ownerUUID = entry.getKey();

			// Already recreated some other way (e.g. the owner left their body again in the meantime) - don't double-spawn.
			if(registry(server).getBody(ownerUUID) != null)
				continue;

			spawnReplacement(server, pending);
		}
	}

	/**
	 * Spawns at the owner's real respawn point if they're online and have one set, otherwise back where
	 * the old body died - a real, deliberately simple fallback (see this class's own scope note), since
	 * resolving an <i>offline</i> player's persisted respawn point without them being loaded isn't worth
	 * the complexity for this pass.
	 */
	private static void spawnReplacement(MinecraftServer server, PendingReplacement pending)
	{
		ServerPlayer online = server.getPlayerList().getPlayer(pending.ownerUUID);
		ServerLevel spawnLevel = pending.deathLevel;
		BlockPos pos = spawnLevel.getSharedSpawnPos();
		float yaw = 0.0F;

		if(online != null)
		{
			ServerLevel respawnLevel = server.getLevel(online.getRespawnDimension());
			if(respawnLevel != null && online.getRespawnPosition() != null)
			{
				spawnLevel = respawnLevel;
				pos = online.getRespawnPosition();
				yaw = online.getRespawnAngle();
			}
		}

		UninhabitedBodyEntity body = new UninhabitedBodyEntity(MSUEntityTypes.UNINHABITED_BODY.get(), spawnLevel);
		body.setOwner(pending.ownerUUID, pending.ownerName);
		body.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0F);
		spawnLevel.addFreshEntity(body);
		registry(server).setBody(pending.ownerUUID, body.getUUID());
	}

	private static final class PendingReplacement
	{
		private final UUID ownerUUID;
		private final ServerLevel deathLevel;
		private final String ownerName;
		private int ticksRemaining;

		private PendingReplacement(UUID ownerUUID, ServerLevel deathLevel, String ownerName, int ticksRemaining)
		{
			this.ownerUUID = ownerUUID;
			this.deathLevel = deathLevel;
			this.ownerName = ownerName;
			this.ticksRemaining = ticksRemaining;
		}
	}
}
