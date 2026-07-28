package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUParticles;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code capabilities.badgeEffects.IBadgeEffects#startPowerParticles}/
 * {@code oneshotPowerParticles} - most abilitechs called one of these every tick they were active, tinted
 * by their {@link EnumAspect}'s real color(s) (see {@link MSUAspectColors}). Backed for real by
 * {@link MSUParticles#spawnAuraParticles}/{@link MSUParticles#spawnBurstParticles} - the real ported
 * equivalent of the original's own {@code particles.MSUParticles.spawnAuraParticles}/
 * {@code spawnBurstParticles}, no longer a vanilla {@code ParticleTypes#ENTITY_EFFECT} stand-in (see
 * {@code client.particles.PowerParticle}'s own doc comment for the real particle now backing this).
 * <p>
 * The original tracked each effect's on/off state per caster (keyed by the tech's own {@code Class}) in
 * a capability, synced once on change, and re-rendered client-side every frame for as long as the state
 * persisted - real infrastructure this project has no equivalent of. Modern {@code ServerLevel#sendParticles}
 * (called inside {@link MSUParticles#spawnPowerParticle}) is a much more direct real equivalent for the
 * actual visible result (a burst of particles visible to every nearby client) - it already broadcasts to
 * trackers on its own, so this just calls it directly, every tick the original would have called
 * {@code startPowerParticles}, rather than reproducing the original's own change-tracking/re-sync layer.
 * <p>
 * This class's own job is just the {@link EnumAspect}-color-table lookup (and the per-color splitting of
 * a requested {@code count}) on top of {@link MSUParticles}' lower-level, color-int-taking spawn calls -
 * the original never had this split since its own aspect-color lookup lived in the same
 * {@code IBadgeEffects} capability that called {@code spawnAuraParticles} directly.
 */
public final class MSUAbilitechParticles
{
	private MSUAbilitechParticles()
	{
	}

	/** Gentle, continuous-looking ambient particles around an entity - call every tick an aura effect is active. */
	public static void aura(Level level, Entity entity, EnumAspect aspect, int count)
	{
		spawn(level, entity, aspect, count, MSUParticles::spawnAuraParticles);
	}

	/** A more energetic, scattering burst - call every tick a "charging" effect is active. */
	public static void burst(Level level, Entity entity, EnumAspect aspect, int count)
	{
		spawn(level, entity, aspect, count, MSUParticles::spawnBurstParticles);
	}

	/** A single burst at the moment of a discrete event (a hit landing, a cast completing, etc). */
	public static void oneshot(Level level, Entity entity, EnumAspect aspect, int count)
	{
		burst(level, entity, aspect, count);
	}

	/**
	 * Same as {@link #aura(Level, Entity, EnumAspect, int)}, but with explicit colors instead of an
	 * aspect lookup - for the rare tech (e.g. {@code mechanics.doom.TechDoomVoidBubble}) whose original own
	 * particle call used one-off literal colors rather than its own aspect's table entry.
	 */
	public static void aura(Level level, Entity entity, int count, int... colors)
	{
		spawn(level, entity, colors, count, MSUParticles::spawnAuraParticles);
	}

	/** Explicit-color equivalent of {@link #oneshot(Level, Entity, EnumAspect, int)} - see {@link #aura(Level, Entity, int, int...)}. */
	public static void oneshot(Level level, Entity entity, int count, int... colors)
	{
		spawn(level, entity, colors, count, MSUParticles::spawnBurstParticles);
	}

	/** Explicit-color equivalent of {@link #burst(Level, Entity, EnumAspect, int)} - see {@link #aura(Level, Entity, int, int...)}. */
	public static void burst(Level level, Entity entity, int count, int... colors)
	{
		spawn(level, entity, colors, count, MSUParticles::spawnBurstParticles);
	}

	private static void spawn(Level level, Entity entity, EnumAspect aspect, int count, ParticleCall call)
	{
		int[] colors = MSUAspectColors.get(aspect);
		if(colors == null || colors.length == 0)
			return;
		spawn(level, entity, colors, count, call);
	}

	// level is unused here - MSUParticles' Entity-taking overloads already derive it from entity.level()
	// themselves - kept as a parameter anyway so every public method above keeps its existing call-site
	// signature across the ~24 techs that already call into this class.
	private static void spawn(Level level, Entity entity, int[] colors, int count, ParticleCall call)
	{
		if(count <= 0 || colors == null || colors.length == 0)
			return;

		int perColor = Math.max(1, count / colors.length);
		for(int color : colors)
			call.spawn(entity, color, perColor);
	}

	@FunctionalInterface
	private interface ParticleCall
	{
		void spawn(Entity entity, int color, int count);
	}
}
