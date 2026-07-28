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
 * Real {@link ParticleOptions} backing {@link MSUParticles#INK} - carries the color and size multiplier
 * MinestuckUniverse (1.12.2)'s {@code particles.MSUParticles.ParticleInk} took as constructor arguments.
 * <b>No current caller</b> - same "ready infrastructure, nothing calls it yet" category as
 * {@code itemvoid.GameData#addItem} - {@code spawnInkParticle} had no in-scope producer anywhere in
 * this project's already-ported code when this was written, ported anyway since the real original
 * {@code particles.MSUParticles} source was supplied directly.
 */
public record InkParticleOption(int color, float size) implements ParticleOptions
{
	public static MapCodec<InkParticleOption> codec(ParticleType<InkParticleOption> type)
	{
		return RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.INT.fieldOf("color").forGetter(InkParticleOption::color),
				Codec.FLOAT.fieldOf("size").forGetter(InkParticleOption::size)
		).apply(instance, InkParticleOption::new));
	}

	public static StreamCodec<? super RegistryFriendlyByteBuf, InkParticleOption> streamCodec(ParticleType<InkParticleOption> type)
	{
		return StreamCodec.composite(
				ByteBufCodecs.INT, InkParticleOption::color,
				ByteBufCodecs.FLOAT, InkParticleOption::size,
				InkParticleOption::new
		);
	}

	@Override
	public ParticleType<?> getType()
	{
		return MSUParticles.INK.get();
	}
}
