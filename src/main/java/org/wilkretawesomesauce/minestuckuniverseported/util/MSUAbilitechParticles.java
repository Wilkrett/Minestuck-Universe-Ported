package org.wilkretawesomesauce.minestuckuniverseported.util;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.network.FociFlashPacket;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code capabilities.badgeEffects.IBadgeEffects#startPowerParticles}/
 * {@code oneshotPowerParticles} - most abilitechs called one of these every tick they were active, tinted
 * by their {@link EnumAspect}'s real color(s) (see {@link AspectColorHandler}). Backed for real by
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
	 * The "easy util" entry point for {@code client.render.FociFlashRenderer}'s real generalized effect -
	 * flashes a fading {@code textures/foci/<aspect>.png} icon at {@code pos}, tinted with that aspect's
	 * own color, for every player in the same dimension. One line, fire-and-forget - matching every other
	 * method in this class, this is meant to be safe to call from any tech's {@code onUseTick} the instant
	 * something worth marking happens (a golem waking up, a bond forming, whatever), without needing to
	 * think about networking. No-op on the client (server-authoritative, like every other real effect call
	 * in this class) and if the aspect has no real color table entry.
	 */
	public static void focusFlash(Level level, Vec3 pos, EnumAspect aspect)
	{
		focusFlash(level, pos, aspect, org.wilkretawesomesauce.minestuckuniverseported.client.render.FociFlashRenderer.DEFAULT_SIZE,
				org.wilkretawesomesauce.minestuckuniverseported.client.render.FociFlashRenderer.DEFAULT_LIFETIME_TICKS);
	}

	/**
	 * Same as {@link #focusFlash(Level, Vec3, EnumAspect)}, but with an explicit icon size (world-space
	 * width/height, in blocks) and fade-out duration (in ticks) instead of
	 * {@code FociFlashRenderer}'s own defaults - for a tech that wants a bigger/smaller or longer/shorter
	 * flash than the standard one.
	 */
	public static void focusFlash(Level level, Vec3 pos, EnumAspect aspect, float size, int lifetimeTicks)
	{
		if(!(level instanceof ServerLevel serverLevel) || AspectColorHandler.get(aspect) == null)
			return;

		PacketDistributor.sendToPlayersInDimension(serverLevel, new FociFlashPacket(pos.x, pos.y, pos.z, aspect.ordinal(), size, lifetimeTicks));
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
		int[] colors = AspectColorHandler.get(aspect);
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
