package org.wilkretawesomesauce.minestuckuniverseported.client.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import org.wilkretawesomesauce.minestuckuniverseported.util.PowerParticleOption;

/**
 * Real port of MinestuckUniverse (1.12.2)'s {@code particles.ParticlePower} - the actual particle behind
 * every {@code util.MSUParticles#spawnAuraParticles}/{@code spawnBurstParticles} call, and therefore every
 * {@code abilitech.MSUAbilitechParticles} aura/burst/oneshot call across all 12 {@code heroAspect}
 * packages - replacing the vanilla {@code ParticleTypes#ENTITY_EFFECT} stand-in that class's own doc
 * comment used to document as a known gap.
 * <p>
 * The original cycled through 7 frames on the old shared terrain particle atlas
 * ({@code setParticleTextureIndex(160 + (particleAge % 14) / 2)}). That specific index range on 1.12.2's
 * shared particle atlas is vanilla's own firework-spark sprite region - confirmed for real, not guessed:
 * the checkerboard-cross pattern this project's actual particle textures now use (below) is a pixel-exact
 * match against a real gameplay reference screenshot of the original mod. So this port now references
 * vanilla's own real {@code minecraft:spark_0}-{@code spark_7} sprites directly (see
 * {@code particles/power.json}) instead of hand-authored placeholder art - the original almost certainly
 * did the same thing (reused an existing shared-atlas region rather than drawing new frames), just
 * expressed through 1.12.2's raw index-arithmetic API instead of a modern named-texture list.
 * <p>
 * The original's cycle was tied to a fixed 14-tick clock independent of the particle's own lifetime - that
 * doesn't reproduce well here: this port's per-particle lifetime ({@code maxAge}, randomized 10-19 ticks -
 * see {@code MSUParticles#randomMaxAge}) is usually *shorter* than a full 14-tick cycle, so a literal port
 * of the modulo (an earlier version of this class did exactly that) meant most particles died before ever
 * reaching the later frames - visually stuck looking like the small "just spawned" frame the whole time.
 * Real fix, matching how {@link TimeGearsRiseParticle} (this class's own sibling) already does it
 * correctly: {@link #setSpriteFromAge(SpriteSet)} ties frame selection to the particle's own real
 * age/lifetime instead, so every particle instance sweeps all 8 frames start-to-finish over however long
 * it actually lives, regardless of the random {@code maxAge} it got.
 * <p>
 * The original's {@code getBrightnessForRender} override forced the particle to always render at least
 * half-lit regardless of ambient light (so it reads as a glowing effect even in the dark), via
 * 1.12.2-specific packed-light bit arithmetic. {@link #getLightColor(float)} reproduces the same *intent*
 * with the modern equivalent ({@link LightTexture#pack(int, int)}, confirmed via {@code javap}) rather
 * than porting bit-for-bit arithmetic tied to a packed-light layout that isn't identical across versions.
 * <p>
 * <b>One real, stated gap</b>: the original set no gravity field of its own on this particle (unlike its
 * sibling {@code ParticleInk}, which explicitly sets one) - relying on whatever vanilla 1.12.2's own base
 * {@code Particle} class defaulted to, which isn't available to verify here. This port defaults to no
 * gravity (matching {@link TimeGearsRiseParticle}'s own same default) - the more likely reading for a
 * floaty magical mote effect.
 */
public class PowerParticle extends TextureSheetParticle
{
	private final SpriteSet spriteSet;

	PowerParticle(ClientLevel level, double x, double y, double z, double xVel, double yVel, double zVel, int maxAge, int hexColor, SpriteSet spriteSet)
	{
		super(level, x, y, z, xVel, yVel, zVel);

		this.spriteSet = spriteSet;
		setColor(((hexColor >> 16) & 0xFF) / 255F, ((hexColor >> 8) & 0xFF) / 255F, (hexColor & 0xFF) / 255F);

		this.lifetime = maxAge;
		this.gravity = 0;
		this.hasPhysics = false;

		setSpriteFromAge(spriteSet);
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

	@Override
	protected int getLightColor(float partialTick)
	{
		// Real modern equivalent of the original's "always at least half-lit" brightness override - forces
		// a bright, glowing look regardless of the particle's actual ambient light level.
		return LightTexture.pack(10, 15);
	}

	public static class Provider implements ParticleProvider<PowerParticleOption>
	{
		private final SpriteSet spriteSet;

		public Provider(SpriteSet spriteSet)
		{
			this.spriteSet = spriteSet;
		}

		@Override
		public Particle createParticle(PowerParticleOption options, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed)
		{
			return new PowerParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, options.maxAge(), options.color(), spriteSet);
		}
	}
}
