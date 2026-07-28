package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.vision;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.EntitySnapshot;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The actual single-client packet fakery {@code PastVisionPlayback} needs, kept separate from that
 * class's reconciliation logic. See {@code timeline.vision} package's design notes in `CLAUDE.md`'s
 * Retrocognition section for why each of these packets was chosen (all confirmed via {@code javap}
 * against this project's pinned NeoForge jar before use, not guessed).
 * <p>
 * <b>Blocks</b>: {@link net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket} overrides
 * what one client renders at a position without touching the real level - the standard "ghost block
 * preview" trick.
 * <p>
 * <b>Entities</b> are harder - vanilla continuously re-syncs every real entity's true position to
 * nearby clients on its own, so a fake position packet for a *real* entity id would just get fought by
 * that automatic tracking. The workaround: hide the real entity from the observer
 * ({@link ClientboundRemoveEntitiesPacket}) and spawn a purely synthetic "ghost" under a brand new,
 * never-before-seen entity id that nothing else is tracking, so nothing fights it. Spawning one needs
 * no real backing {@code Entity} at all ({@link ClientboundAddEntityPacket}'s raw-data constructor),
 * but *moving* one does ({@code ClientboundTeleportEntityPacket}/{@code ClientboundRotateHeadPacket}
 * both read from an {@code Entity} object) - solved with a throwaway {@code Entity} instance
 * ({@code EntityType#create(Level)}, confirmed to never add itself to the level or get ticked) that
 * exists purely to feed those two packet constructors, see {@link GhostEntity}.
 */
final class PastVisionPacketSender
{
	private static final AtomicInteger NEXT_FAKE_ID = new AtomicInteger(Integer.MAX_VALUE);

	private PastVisionPacketSender()
	{
	}

	static void sendFakeBlock(ServerPlayer observer, BlockPos pos, BlockState state)
	{
		observer.connection.send(new ClientboundBlockUpdatePacket(pos, state));
	}

	/** Resyncs one position back to its real, current, true state - used on radius-exit and session end. */
	static void resyncBlock(ServerPlayer observer, ServerLevel level, BlockPos pos)
	{
		observer.connection.send(new ClientboundBlockUpdatePacket(level, pos));
	}

	/**
	 * Hides {@code real} from {@code observer} and spawns a synthetic ghost standing in for it at
	 * {@code snapshot}'s recorded position - null if the entity type couldn't be instantiated (shouldn't
	 * happen for a living entity type in practice, defensive only).
	 */
	@Nullable
	static GhostEntity spawnGhost(ServerPlayer observer, ServerLevel level, LivingEntity real, EntitySnapshot snapshot)
	{
		observer.connection.send(new ClientboundRemoveEntitiesPacket(real.getId()));

		EntityType<?> type = real.getType();
		Entity ghostEntity = type.create(level);
		if(ghostEntity == null)
			return null;

		int fakeId = NEXT_FAKE_ID.getAndDecrement();
		ghostEntity.setId(fakeId);
		ghostEntity.setPos(snapshot.pos());
		ghostEntity.setYRot(snapshot.yaw());
		ghostEntity.setXRot(snapshot.pitch());

		observer.connection.send(new ClientboundAddEntityPacket(fakeId, UUID.randomUUID(),
				snapshot.pos().x, snapshot.pos().y, snapshot.pos().z,
				snapshot.pitch(), snapshot.yaw(), type, 0, Vec3.ZERO, 0.0));

		sendEquipment(observer, fakeId, snapshot.equipment());

		return new GhostEntity(ghostEntity, fakeId, real.getUUID());
	}

	/** Updates an already-spawned ghost's position/rotation for this tick's recorded step. */
	static void moveGhost(ServerPlayer observer, GhostEntity ghost, EntitySnapshot snapshot)
	{
		Entity entity = ghost.entity();
		entity.setPos(snapshot.pos());
		entity.setYRot(snapshot.yaw());
		entity.setXRot(snapshot.pitch());

		observer.connection.send(new ClientboundTeleportEntityPacket(entity));
		observer.connection.send(new ClientboundRotateHeadPacket(entity, (byte) Mth.floor(snapshot.yaw() * 256.0F / 360.0F)));

		sendEquipment(observer, ghost.fakeId(), snapshot.equipment());
	}

	private static void sendEquipment(ServerPlayer observer, int fakeId, Map<EquipmentSlot, ItemStack> equipment)
	{
		List<Pair<EquipmentSlot, ItemStack>> slots = new ArrayList<>();
		for(Map.Entry<EquipmentSlot, ItemStack> entry : equipment.entrySet())
			slots.add(Pair.of(entry.getKey(), entry.getValue()));
		if(!slots.isEmpty())
			observer.connection.send(new ClientboundSetEquipmentPacket(fakeId, slots));
	}

	/** Despawns the ghost and, if the real entity is still around, restores the observer's true view of it. */
	static void despawnGhostAndRestore(ServerPlayer observer, ServerLevel level, GhostEntity ghost)
	{
		observer.connection.send(new ClientboundRemoveEntitiesPacket(ghost.fakeId()));

		if(level.getEntity(ghost.realEntityId()) instanceof Entity real)
			observer.connection.send(new ClientboundAddEntityPacket(real, 0, real.blockPosition()));
	}
}
