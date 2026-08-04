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
 * Real {@link ParticleOptions} backing {@link MSUParticles#WIND_WISP} - modeled directly on
 * {@link PowerParticleOption} (color + per-particle lifetime), plus one addition: {@code scale}, a size
 * multiplier so a single texture/particle type can serve both a subtle connecting-trail wisp and a much
 * bigger swirling-aura wisp without needing a second registered {@code ParticleType} - see
 * {@code client.particles.WindWispParticle}'s own doc comment for the full reasoning behind this whole
 * particle type (a direct later user request to replace the geometric mesh trail's role with a soft,
 * blurred particle-swarm "wind" look, reusing vanilla's real Wind Charge/Breeze {@code gust_0}-{@code gust_11}
 * art instead of new placeholder art).
 */
public record WindWispParticleOption(int color, int maxAge, float scale) implements ParticleOptions
{
	public static MapCodec<WindWispParticleOption> codec(ParticleType<WindWispParticleOption> type)
	{
		return RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.INT.fieldOf("color").forGetter(WindWispParticleOption::color),
				Codec.INT.fieldOf("max_age").forGetter(WindWispParticleOption::maxAge),
				Codec.FLOAT.fieldOf("scale").forGetter(WindWispParticleOption::scale)
		).apply(instance, WindWispParticleOption::new));
	}

	public static StreamCodec<? super RegistryFriendlyByteBuf, WindWispParticleOption> streamCodec(ParticleType<WindWispParticleOption> type)
	{
		return StreamCodec.composite(
				ByteBufCodecs.INT, WindWispParticleOption::color,
				ByteBufCodecs.INT, WindWispParticleOption::maxAge,
				ByteBufCodecs.FLOAT, WindWispParticleOption::scale,
				WindWispParticleOption::new
		);
	}

	@Override
	public ParticleType<?> getType()
	{
		return MSUParticles.WIND_WISP.get();
	}
}
