package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.vision;

import net.minecraft.world.entity.Entity;

import java.util.UUID;

/**
 * One "ghost" standing in for a real entity that's currently hidden from an observing player, in
 * {@code PastVisionSession#getActiveGhosts()}. {@link #entity} is a throwaway {@code Entity} instance
 * (from {@code EntityType#create(Level)}) that's never added to the level or ticked - it exists purely
 * so {@code ClientboundTeleportEntityPacket}/{@code ClientboundRotateHeadPacket} (which both read from
 * a real {@code Entity} object rather than taking raw fields) have something to read from. See
 * {@code timeline.vision.PastVisionPacketSender}'s doc comment for the full packet-level explanation.
 *
 * @param entity       the throwaway entity, stamped with {@link #fakeId} via {@code Entity#setId}
 * @param fakeId       the synthetic network id this ghost was spawned under - never collides with a
 *                     real entity id, see {@code PastVisionPacketSender#nextFakeId}
 * @param realEntityId the real entity's UUID this ghost is standing in for, so it can be restored when
 *                     the ghost is dismissed
 */
public record GhostEntity(Entity entity, int fakeId, UUID realEntityId)
{
}
