package org.wilkretawesomesauce.minestuckuniverseported.util;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

import java.util.function.Supplier;

/**
 * Real port of MinestuckUniverse (1.12.2)'s {@code particles.MSUParticles} - registers this addon's
 * particle types (matching Minestuck's own {@code util.MSParticleType} placement of its particle-type
 * registry hub inside {@code util}, confirmed via {@code javap} against the dependency jar) and the two
 * real spawn-helper method families the original exposed: {@code spawnAuraParticles}/
 * {@code spawnBurstParticles} (backing every {@code abilitech.MSUAbilitechParticles} aura/burst/oneshot
 * call, across all 12 {@code heroAspect} packages) and {@code spawnInkParticle} (no in-scope caller yet -
 * see {@link InkParticleOption}'s own doc comment).
 * <p>
 * <b>Real architectural adaptation, not a guess</b>: the original's own versions of these methods only
 * ever did anything on a real client (guarded by {@code world.isRemote}), relying on ability-tick code
 * running identically on both logical sides in 1.12.2's client-predicted singleplayer model, with each
 * client rendering its own particles locally and zero networking involved. This project's abilitech tick
 * logic is server-authoritative only (see {@code abilitech.MSUAbilitechParticles}'s own doc comment,
 * already established before this pass) - these methods take a {@link Level} and, if it's really a
 * {@link ServerLevel}, call {@link ServerLevel#sendParticles}, which already broadcasts to every nearby
 * tracking client on its own, rather than reproducing the original's client-local render model.
 * <p>
 * The original's own {@code enum ParticleType} (AURA/BURST) and {@code PowerParticleState} class backed a
 * continuous per-caster particle-state-tracking capability, re-synced to observers only on change - real
 * infrastructure this project already decided not to reproduce (see
 * {@code abilitech.MSUAbilitechParticles}'s own doc comment): {@link ServerLevel#sendParticles} already
 * broadcasts on its own, so this just calls it directly every tick the original would have called
 * {@code startPowerParticles}, instead of rebuilding that whole change-tracking layer on top.
 */
public final class MSUParticles
{
	public static final DeferredRegister<ParticleType<?>> REGISTER = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, Minestuckuniverseported.MODID);

	/** The 16-frame "gears rising" effect, played when a {@code mechanics.timeline.DoomedTimelineClone} spawns or despawns. */
	public static final Supplier<SimpleParticleType> TIME_GEARS_RISE = REGISTER.register("time_gears_rise", () -> new SimpleParticleType(false));

	/** Real port of the original's {@code particles.ParticlePower} - see {@link PowerParticleOption}'s own doc comment. */
	public static final Supplier<ParticleType<PowerParticleOption>> POWER = REGISTER.register("power", () -> new ParticleType<>(false)
	{
		@Override
		public MapCodec<PowerParticleOption> codec()
		{
			return PowerParticleOption.codec(this);
		}

		@Override
		public StreamCodec<? super RegistryFriendlyByteBuf, PowerParticleOption> streamCodec()
		{
			return PowerParticleOption.streamCodec(this);
		}
	});

	/** Real port of the original's {@code particles.MSUParticles.ParticleInk} - see {@link InkParticleOption}'s own doc comment. */
	public static final Supplier<ParticleType<InkParticleOption>> INK = REGISTER.register("ink", () -> new ParticleType<>(false)
	{
		@Override
		public MapCodec<InkParticleOption> codec()
		{
			return InkParticleOption.codec(this);
		}

		@Override
		public StreamCodec<? super RegistryFriendlyByteBuf, InkParticleOption> streamCodec()
		{
			return InkParticleOption.streamCodec(this);
		}
	});

	/** Original design for this project, no 1.12.2 counterpart - see {@link WindWispParticleOption}'s own doc comment. */
	public static final Supplier<ParticleType<WindWispParticleOption>> WIND_WISP = REGISTER.register("wind_wisp", () -> new ParticleType<>(false)
	{
		@Override
		public MapCodec<WindWispParticleOption> codec()
		{
			return WindWispParticleOption.codec(this);
		}

		@Override
		public StreamCodec<? super RegistryFriendlyByteBuf, WindWispParticleOption> streamCodec()
		{
			return WindWispParticleOption.streamCodec(this);
		}
	});

	private MSUParticles()
	{
	}

	/**
	 * Direct port of {@code spawnPowerParticle} - one Power particle at an exact position/velocity, with
	 * the given lifetime and color.
	 */
	public static void spawnPowerParticle(Level level, double x, double y, double z, double xVel, double yVel, double zVel, int maxAge, int hexColor)
	{
		if(!(level instanceof ServerLevel serverLevel))
			return;
		// count=0 is vanilla's own "spawn exactly one particle with an explicit velocity" trick (confirmed
		// via ClientPacketListener#handleParticleEvent, a real longstanding vanilla mechanic, not guessed) -
		// the offset args become the exact velocity instead of a random spread, reproducing the original's
		// own per-particle exact-velocity spawning faithfully.
		serverLevel.sendParticles(new PowerParticleOption(hexColor, maxAge), x, y, z, 0, xVel, yVel, zVel, 1.0);
	}

	/** Direct port of {@code spawnInkParticle(...,size)} - see {@link InkParticleOption}'s own doc comment (no current caller). */
	public static void spawnInkParticle(Level level, double x, double y, double z, double xVel, double yVel, double zVel, int color, float size)
	{
		if(!(level instanceof ServerLevel serverLevel))
			return;
		serverLevel.sendParticles(new InkParticleOption(color, size), x, y, z, 0, xVel, yVel, zVel, 1.0);
	}

	/** Same as {@link #spawnInkParticle(Level, double, double, double, double, double, double, int, float)}, size 1. */
	public static void spawnInkParticle(Level level, double x, double y, double z, double xVel, double yVel, double zVel, int color)
	{
		spawnInkParticle(level, x, y, z, xVel, yVel, zVel, color, 1.0F);
	}

	/** See {@link WindWispParticleOption}'s own doc comment - {@code scale} lets one call site spawn a subtle small wisp and another a much bigger swirling one. */
	public static void spawnWindWisp(Level level, double x, double y, double z, double xVel, double yVel, double zVel, int maxAge, int hexColor, float scale)
	{
		if(!(level instanceof ServerLevel serverLevel))
			return;
		serverLevel.sendParticles(new WindWispParticleOption(hexColor, maxAge, scale), x, y, z, 0, xVel, yVel, zVel, 1.0);
	}

	public static void spawnAuraParticles(Entity entity, int color, int count)
	{
		spawnAuraParticles(entity.level(), entity.getX(), entity.getY(), entity.getZ(), color, count);
	}

	/** Direct port of {@code spawnAuraParticles} - gentle, slow-drifting particles scattered around a point. */
	public static void spawnAuraParticles(Level level, double x, double y, double z, int color, int count)
	{
		for(int i = 0; i < count; i++)
		{
			Vec3 vel = new Vec3(Math.random() - 0.5, Math.random() - 0.25, Math.random() - 0.5)
					.normalize().scale((Math.random() * 8 + 1) * 0.02);
			Vec3 off = new Vec3(Math.random() - 0.5, Math.random(), Math.random() - 0.5)
					.normalize().scale(0.4);

			spawnPowerParticle(level, x + off.x, y + off.y, z + off.z, vel.x, vel.y, vel.z, randomMaxAge(level), color);
		}
	}

	public static void spawnBurstParticles(Entity entity, int color, int count)
	{
		spawnBurstParticles(entity.level(), entity.getX(), entity.getY(), entity.getZ(), color, count);
	}

	/** Direct port of {@code spawnBurstParticles} - faster, upward-biased scattering particles. */
	public static void spawnBurstParticles(Level level, double x, double y, double z, int color, int count)
	{
		for(int i = 0; i < count; i++)
		{
			Vec3 vel = new Vec3(Math.random() - 0.5, 0, Math.random() - 0.5)
					.normalize().scale((Math.random() * 8 + 1) * 0.05);
			Vec3 off = new Vec3(Math.random() - 0.5, 1.5, Math.random() - 0.5)
					.normalize().scale(0.4);

			spawnPowerParticle(level, x + off.x, y + off.y, z + off.z, vel.x, vel.y, vel.z, randomMaxAge(level), color);
		}
	}

	/** Matches the original's own {@code world.rand.nextInt(10)+10} - the only randomization that used the world's own RNG rather than raw {@code Math.random()}. */
	private static int randomMaxAge(Level level)
	{
		return level.getRandom().nextInt(10) + 10;
	}
}
