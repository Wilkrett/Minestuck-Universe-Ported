package org.wilkretawesomesauce.minestuckuniverseported.client.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import org.wilkretawesomesauce.minestuckuniverseported.util.WindWispParticleOption;

/**
 * The real particle behind {@code util.MSUParticles#spawnWindWisp} - a direct later user request, no
 * 1.12.2 counterpart (this project's own original design, same category as the Freedom/Mind/Timeline
 * systems). After several rounds of tuning {@code client.render.WindRibbonRenderer}'s geometric "lightning
 * tube" mesh trail, a live reference screenshot (a different modpack's Photon-based spell-charging effect -
 * soft, blurred, translucent smoke-ring wisps curling around the caster) made clear the mesh's precise line
 * geometry was never going to read as "natural wind" no matter how it was reshaped - the actual technique
 * needed is a soft particle swarm, not a crisper line.
 * <p>
 * <b>Real vanilla art reuse, not new placeholder art</b> - matching this project's own established
 * convention ({@link PowerParticle}'s own doc comment: its art references vanilla's real firework-spark
 * sprites rather than hand-drawn placeholder frames). This particle's sprite set
 * ({@code particles/wind_wisp.json}) lists vanilla's own real Wind Charge/Breeze "Gust" art -
 * {@code minecraft:gust_0} through {@code minecraft:gust_11} (confirmed by extracting and viewing the real
 * frames from the vanilla 1.21.1 client jar: soft round blur dots and gently curling comma/spiral-ring
 * shapes, genuinely soft-edged and translucent, not sharp) - a thematically perfect, zero-new-art fit for a
 * wind effect, the same reasoning that justified reusing vanilla's spark sprites for {@link PowerParticle}.
 * <p>
 * <b>Two real differences from {@link PowerParticle}</b>, both needed for the "soft wisp" look and both
 * confirmed achievable purely inside a {@code Particle} subclass (no deeper vanilla hook required -
 * {@code SingleQuadParticle#quadSize} and {@code Particle#setAlpha}/{@code alpha} are both freely settable
 * from here): {@link #scale} lets a single texture/particle type serve both a subtle connecting-trail wisp
 * and a much bigger swirling-aura wisp (see {@code WindWispParticleOption}'s own doc comment for why this
 * avoids needing a second registered {@code ParticleType}); and {@link #tick()} now actively animates both
 * {@code alpha} (a short ease-in then ease-out to 0, so it fades in and out rather than popping in/vanishing
 * at {@code maxAge} the way {@link PowerParticle} does) and {@code quadSize} (a mild growth over the
 * particle's life, a gentle "puffing outward" look) - {@link PowerParticle} deliberately does neither of
 * these, so this is a genuinely separate class rather than a shared-base refactor, to avoid changing the
 * look of every other aspect's own particle calls that still go through {@code PowerParticle}.
 */
public class WindWispParticle extends TextureSheetParticle
{
	private static final float BASE_SIZE = 0.4F;
	private static final float FADE_IN_FRACTION = 0.15F;
	private static final float GROWTH_FRACTION = 0.6F;

	private final SpriteSet spriteSet;
	private final float baseQuadSize;

	WindWispParticle(ClientLevel level, double x, double y, double z, double xVel, double yVel, double zVel, int maxAge, int hexColor, float scale, SpriteSet spriteSet)
	{
		super(level, x, y, z, xVel, yVel, zVel);

		this.spriteSet = spriteSet;
		setColor(((hexColor >> 16) & 0xFF) / 255F, ((hexColor >> 8) & 0xFF) / 255F, (hexColor & 0xFF) / 255F);

		this.lifetime = maxAge;
		this.gravity = 0;
		this.hasPhysics = false;
		this.baseQuadSize = BASE_SIZE * scale;
		this.quadSize = baseQuadSize;

		setSpriteFromAge(spriteSet);
		updateFade();
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
		updateFade();
	}

	/** Fades in over {@link #FADE_IN_FRACTION} of the particle's life, then back out to 0 - see this class's own doc comment for why, unlike {@link PowerParticle}, this class animates alpha at all. Also grows {@link #quadSize} slightly over its life for a gentle "puffing outward" feel. */
	private void updateFade()
	{
		float lifeFraction = this.lifetime <= 0 ? 1F : Mth.clamp(this.age / (float) this.lifetime, 0F, 1F);
		float alpha = lifeFraction < FADE_IN_FRACTION
				? lifeFraction / FADE_IN_FRACTION
				: 1F - (lifeFraction - FADE_IN_FRACTION) / (1F - FADE_IN_FRACTION);
		setAlpha(Mth.clamp(alpha, 0F, 1F));

		this.quadSize = baseQuadSize * (1F + lifeFraction * GROWTH_FRACTION);
	}

	@Override
	protected int getLightColor(float partialTick)
	{
		// Same "always at least half-lit" glow trick as PowerParticle - see that class's own doc comment.
		return LightTexture.pack(10, 15);
	}

	public static class Provider implements ParticleProvider<WindWispParticleOption>
	{
		private final SpriteSet spriteSet;

		public Provider(SpriteSet spriteSet)
		{
			this.spriteSet = spriteSet;
		}

		@Override
		public Particle createParticle(WindWispParticleOption options, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed)
		{
			return new WindWispParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, options.maxAge(), options.color(), options.scale(), spriteSet);
		}
	}
}
