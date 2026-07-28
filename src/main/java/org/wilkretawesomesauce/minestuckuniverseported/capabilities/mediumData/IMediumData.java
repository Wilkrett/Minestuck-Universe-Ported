package org.wilkretawesomesauce.minestuckuniverseported.capabilities.mediumData;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code capabilities.mediumData.IMediumData} - the original
 * also extended {@code capabilities.IMSUCapabilityBase<World>}; not repeated here for the same reason
 * every other real capability interface in this project's {@code capabilities} package skips it. Both
 * methods gained an explicit {@link ServerLevel} parameter the original didn't need - see
 * {@link MediumData}'s own doc comment for why (the modern seed-derivation this needs isn't available
 * without a real {@code Level} reference).
 */
public interface IMediumData
{
	ChunkPos getQuestBedChunk(ServerLevel level);

	BlockPos getQuestBedPos(ServerLevel level);
}
