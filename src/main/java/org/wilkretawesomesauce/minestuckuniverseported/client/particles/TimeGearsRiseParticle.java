package org.wilkretawesomesauce.minestuckuniverseported.client.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * The 16-frame "gears rising" effect (source: a user-provided animation of stacked cog rings rising and
 * fading), played on {@code mechanics.timeline.DoomedTimelineClone} spawn and despawn. Structured the same way as
 * Minestuck's own {@code client.particles.PlasmaParticle} - a {@link TextureSheetParticle} driven by
 * {@link SpriteSet#get} via age, not a custom render loop.
 */
public class TimeGearsRiseParticle extends TextureSheetParticle
{
	private final SpriteSet spriteSet;

	TimeGearsRiseParticle(ClientLevel level, double x, double y, double z, SpriteSet spriteSet)
	{
		super(level, x, y, z);

		this.spriteSet = spriteSet;
		this.setSpriteFromAge(spriteSet);
		this.lifetime = 16;
		this.gravity = 0;
		this.hasPhysics = false;

		// SingleQuadParticle's own constructor already seeds quadSize with a small random value (~0.1-0.2),
		// and its scale(float) multiplies that random base rather than setting an absolute size - fine for
		// vanilla-style small sparkle particles, not for "cover the whole entity" here. Setting quadSize
		// directly (a square billboard, so this is also the effective height) gives a fixed, predictable
		// size instead: 2 blocks across comfortably covers a player-sized double's ~1.8 block height.
		this.quadSize = 1.0F;
	}

	@Override
	public ParticleRenderType getRenderType()
	{
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	public void tick()
	{
		super.tick();
		setSpriteFromAge(spriteSet);
	}

	public static class Provider implements ParticleProvider<SimpleParticleType>
	{
		private final SpriteSet spriteSet;

		public Provider(SpriteSet spriteSet)
		{
			this.spriteSet = spriteSet;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed)
		{
			return new TimeGearsRiseParticle(level, x, y, z, spriteSet);
		}
	}
}
