package org.wilkretawesomesauce.minestuckuniverseported.capabilities.mediumData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.wilkretawesomesauce.minestuckuniverseported.Config;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code capabilities.mediumData.MediumData}/{@code IMediumData} -
 * deterministic quest-bed chunk position generation for a Land dimension, seeded once per dimension and
 * cached from then on. Attached directly to a {@code Level}
 * (see {@link org.wilkretawesomesauce.minestuckuniverseported.MSUAttachments#MEDIUM_DATA}), same mechanism
 * {@code mechanics.timeline.TimelineData} already uses - only meaningful for Land dimensions, but (unlike the
 * original's conditional {@code AttachCapabilitiesEvent} gating) NeoForge attachments don't need an
 * explicit "should this level get one" check; it's simply never queried for a non-Land level.
 * <p>
 * The original seeded its {@code Random} with {@code world.getSeed() * world.provider.getDimension()} - a
 * plain int dimension id that no longer exists in 1.21.1 (Minestuck's Land dimensions are per-player
 * dynamic {@code ResourceKey<Level>}s created via Infiniverse, see this project's own Timeline branch
 * system for the same underlying mechanism). {@code dimension().location().hashCode()} stands in for that
 * int multiplier here - same intent (each Land dimension gets its own distinct-but-deterministic seed off
 * the same world seed), adapted to a modern dimension identity.
 */
public class MediumData implements IMediumData, INBTSerializable<CompoundTag>
{
	private ChunkPos questBedChunk;

	public ChunkPos getQuestBedChunk(ServerLevel level)
	{
		if(questBedChunk == null)
		{
			long seed = level.getSeed() * level.dimension().location().hashCode();
			RandomSource random = RandomSource.create(seed);

			int r = (random.nextInt(Config.questBedSpawnArea) + Config.questBedSpawnDistance) / 16;
			double a = random.nextDouble() * Math.PI * 2.0;

			questBedChunk = new ChunkPos((int) (Math.cos(a) * r), (int) (Math.sin(a) * r));
		}

		return questBedChunk;
	}

	public BlockPos getQuestBedPos(ServerLevel level)
	{
		ChunkPos chunkPos = getQuestBedChunk(level);
		return new BlockPos(chunkPos.x * 16, 128, chunkPos.z * 16);
	}

	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider provider)
	{
		CompoundTag nbt = new CompoundTag();
		if(questBedChunk != null)
		{
			nbt.putInt("BedChunkX", questBedChunk.x);
			nbt.putInt("BedChunkZ", questBedChunk.z);
		}
		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt)
	{
		if(nbt.contains("BedChunkX") && nbt.contains("BedChunkZ"))
			questBedChunk = new ChunkPos(nbt.getInt("BedChunkX"), nbt.getInt("BedChunkZ"));
	}
}
