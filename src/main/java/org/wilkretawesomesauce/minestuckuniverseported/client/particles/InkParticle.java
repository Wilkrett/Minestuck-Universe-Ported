package org.wilkretawesomesauce.minestuckuniverseported.client.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import org.wilkretawesomesauce.minestuckuniverseported.util.InkParticleOption;

/**
 * Real port of MinestuckUniverse (1.12.2)'s {@code particles.MSUParticles.ParticleInk} - see
 * {@link InkParticleOption}'s own doc comment for why this has no current in-scope caller (ported anyway
 * since the real original source was supplied directly).
 * <p>
 * <b>Preserved oddity, not corrected</b>: the original's {@code onUpdate} called {@code super.onUpdate()}
 * (which - confirmed via {@code javap} against this project's own base {@link Particle#tick()} - already
 * subtracts {@code 0.04 * gravity} from vertical motion every tick on its own) and then <i>separately</i>
 * subtracted the same {@code 0.004 + 0.04 * gravity} formula a second time. With {@code gravity} set to
 * {@code 0.1F} that's {@code 0.004} from the base class plus another {@code 0.008} manually - a real
 * double-counted gravity effect, kept exactly rather than "fixed", matching this project's standing
 * practice of preserving the original's own quirks (e.g. {@code AbilitechnosynthBlock}'s {@code 5/15d}
 * typo, {@code SavingGraceEvents}' literal argument order) rather than silently rebalancing them.
 * <p>
 * Expires the instant it touches water ({@link FluidTags#WATER}, the modern equivalent of the original's
 * {@code Material.WATER} check) - matches the original exactly.
 */
public class InkParticle extends TextureSheetParticle
{
	InkParticle(ClientLevel level, double x, double y, double z, double xVel, double yVel, double zVel, int color, float size)
	{
		super(level, x, y, z, xVel, yVel, zVel);

		float r = (float) Math.floor(color / (256 * 256));
		float g = (float) (Math.floor(color / 256) % 256);
		float b = (float) (color % 256);

		setColor(Math.max(5 / 255F, r / 255F - 5 / 255F), Math.max(5 / 255F, g / 255F - 5 / 255F), Math.max(5 / 255F, b / 255F - 5 / 255F));

		this.quadSize = Math.min(1, Math.max(0, random.nextFloat())) * 5 * size;
		this.gravity = 0.1F;
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

		if(gravity > 0)
			yd -= 0.004D + 0.04D * gravity;

		if(level.getFluidState(BlockPos.containing(x, y, z)).is(FluidTags.WATER))
			remove();
	}

	public static class Provider implements ParticleProvider<InkParticleOption>
	{
		private final SpriteSet spriteSet;

		public Provider(SpriteSet spriteSet)
		{
			this.spriteSet = spriteSet;
		}

		@Override
		public Particle createParticle(InkParticleOption options, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed)
		{
			InkParticle particle = new InkParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, options.color(), options.size());
			particle.setSprite(spriteSet.get(0, 1));
			return particle;
		}
	}
}
