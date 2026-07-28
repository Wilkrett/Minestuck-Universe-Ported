package org.wilkretawesomesauce.minestuckuniverseported.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Real {@link ParticleOptions} backing {@link MSUParticles#POWER} - carries both the color and the
 * per-particle max age MinestuckUniverse (1.12.2)'s {@code particles.ParticlePower} took as constructor
 * arguments. Vanilla's own generic {@code ColorParticleOption} (reused elsewhere in this project, e.g.
 * {@code ParticleTypes#ENTITY_EFFECT}) only carries a color, not a lifetime, so a bespoke option type was
 * needed to keep the original's per-particle randomized 10-19 tick lifespan
 * ({@code world.rand.nextInt(10)+10} in {@code MSUParticles#spawnAuraParticles}/{@code spawnBurstParticles})
 * rather than flattening every Power particle to one fixed duration.
 */
public record PowerParticleOption(int color, int maxAge) implements ParticleOptions
{
	public static MapCodec<PowerParticleOption> codec(ParticleType<PowerParticleOption> type)
	{
		return RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.INT.fieldOf("color").forGetter(PowerParticleOption::color),
				Codec.INT.fieldOf("max_age").forGetter(PowerParticleOption::maxAge)
		).apply(instance, PowerParticleOption::new));
	}

	public static StreamCodec<? super RegistryFriendlyByteBuf, PowerParticleOption> streamCodec(ParticleType<PowerParticleOption> type)
	{
		return StreamCodec.composite(
				ByteBufCodecs.INT, PowerParticleOption::color,
				ByteBufCodecs.INT, PowerParticleOption::maxAge,
				PowerParticleOption::new
		);
	}

	@Override
	public ParticleType<?> getType()
	{
		return MSUParticles.POWER.get();
	}
}
