package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.breath;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUParticles;

/**
 * The "Wind Engine" from the "Breath Wind Engine Visualizer Design" doc - original design for this
 * project, no 1.12.2 counterpart. Real callers: {@code TechBreathLiberate}/{@code TechBreathConstrain}/
 * {@code heroClass.page.breath.TechPageBreathFreeWill}, the exact three abilities the source doc gives
 * its own worked visual spec for - see each tech's own doc comment for which method(s) it calls and why.
 * <p>
 * <b>Deliberately not a persistent, stateful {@code WindField} object</b>, despite the doc's own sketched
 * class diagram (<code>Origin/Direction/Target/Strength/Radius/Duration</code>) - every real particle-effect
 * caller in this project (see {@code skills.abilitech.MSUAbilitechParticles}) is a stateless method called
 * fresh every tick an effect is active, with "duration" already provided for free by however many ticks the
 * caster keeps holding the key. Reproducing the doc's own object graph as a tracked, ticking entity would be
 * new infrastructure this project's particle system has never needed anywhere else - these methods follow
 * the same call-every-active-tick shape instead, just with wind-specific motion math instead of generic
 * scatter, on top of the same real {@link MSUParticles#spawnPowerParticle} primitive every other
 * aspect's own particle calls already use (no new particle type/texture - see that class's own doc comment
 * for why the existing one is real, tinted art rather than a placeholder needing replacement).
 * <p>
 * <b>The doc's own "Environmental Reactions" list is mostly NOT implemented, for real, confirmed
 * technical reasons, not oversight</b> - leaves/grass/flowers swaying, smoke bending, campfire flames
 * leaning, snow drifting, and clouds swirling would all require either replacing vanilla's own static
 * block/particle rendering per-instance (no such per-location override hook exists in modern NeoForge) or
 * a custom renderer/Mixin this project doesn't use, the same category of gap as
 * {@code TechBreathWindVessel}'s own documented collision-phasing limitation. "Arrows wobble" is skipped
 * too, but for a different reason: an {@code Arrow} has no separate "visual-only" transform channel
 * reachable without a custom renderer, so making one visibly wobble would mean nudging its <i>real</i>
 * flight path - the doc explicitly says environmental reactions should be visual only, so faking this with
 * an actual (if minor) trajectory change would violate that instruction, not satisfy it. <b>Only "dropped
 * items shift slightly" is real</b> ({@link #nudgeNearbyItems}) - a real vanilla {@link ItemEntity} is an
 * ordinary entity with ordinary velocity, the one item on the doc's own list with a real, non-Mixin lever
 * to pull.
 */
public final class WindEngine
{
	private static final double RIBBON_SAMPLE_SPACING = 0.75;
	private static final double RIBBON_TWIST_AMPLITUDE = 0.22;
	private static final double RIBBON_TWIST_TURNS_PER_BLOCK = 0.35;
	private static final int RIBBON_PARTICLES_PER_TICK = 3;

	private WindEngine()
	{
	}

	/**
	 * "Wind Ribbons" - the doc's own primary Breath visual: translucent flowing particles curving from
	 * {@code from} toward {@code to}, with a slight twisting motion along the way ("smooth curved
	 * movement... slight twisting motion... should look like moving air, not magic particles"). Deliberately
	 * low particle count per tick (the doc's own explicit instruction) - a random sample of points along the
	 * path each tick, not the whole path at once, so the flow reads as continuous across several ticks
	 * rather than a single dense burst. Curves toward wherever {@code to} <i>currently</i> is - called fresh
	 * every tick with the target's live position, so a moving target naturally bends the ribbon instead of
	 * it snapping to a stale straight line (the doc's own "should curve if the target moves").
	 */
	public static void ribbon(Level level, Vec3 from, Vec3 to, int color, float intensity)
	{
		if(!(level instanceof ServerLevel serverLevel))
			return;

		Vec3 delta = to.subtract(from);
		double distance = delta.length();
		if(distance < 0.5)
			return;

		Vec3 dir = delta.scale(1.0 / distance);
		Vec3[] basis = perpendicularBasis(dir);
		RandomSource random = serverLevel.getRandom();

		int samples = Math.max(1, (int) (distance / RIBBON_SAMPLE_SPACING));
		int toSpawn = Math.min(RIBBON_PARTICLES_PER_TICK, samples);

		for(int i = 0; i < toSpawn; i++)
		{
			double t = random.nextDouble();
			Vec3 base = from.add(delta.scale(t));

			double angle = t * distance * RIBBON_TWIST_TURNS_PER_BLOCK * 2.0 * Math.PI;
			double amplitude = RIBBON_TWIST_AMPLITUDE * intensity;
			Vec3 twist = basis[0].scale(Math.cos(angle) * amplitude).add(basis[1].scale(Math.sin(angle) * amplitude));
			Vec3 point = base.add(twist);
			Vec3 vel = dir.scale(0.08 * intensity);

			MSUParticles.spawnPowerParticle(level, point.x, point.y, point.z, vel.x, vel.y, vel.z, 12 + random.nextInt(8), color);
		}
	}

	/**
	 * "Spiral Currents" - small vortex motion orbiting {@code center} at {@code radius}, tangential
	 * velocity giving the particles an actual orbiting motion rather than just scattering. The doc's own
	 * "Uses: channeling abilities, high intensity attacks, Freedom increases" and Liberate's own "gentle
	 * orbiting rings appear around the target" that grow with Freedom - callers scale {@code radius}/
	 * {@code intensity} off whatever value they're actually visualizing.
	 */
	public static void spiralAroundTarget(Level level, Vec3 center, double radius, int color, float intensity)
	{
		if(!(level instanceof ServerLevel serverLevel))
			return;

		RandomSource random = serverLevel.getRandom();
		int count = Math.max(1, Math.round(2.0F * intensity));

		for(int i = 0; i < count; i++)
		{
			double angle = random.nextDouble() * 2.0 * Math.PI;
			double height = (random.nextDouble() - 0.5) * 1.2;
			Vec3 offset = new Vec3(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
			Vec3 point = center.add(offset);
			Vec3 tangent = new Vec3(-Math.sin(angle), 0.0, Math.cos(angle)).scale(0.06 * intensity);

			MSUParticles.spawnPowerParticle(level, point.x, point.y, point.z, tangent.x, tangent.y, tangent.z, 10 + random.nextInt(6), color);
		}
	}

	/**
	 * "Pressure Distortion", the inward-compressing half - Constrain's own explicit visual principle ("do
	 * not make this evil wind... air moves inward instead of outward... the air itself is restricting
	 * movement"): particles at {@code radius} around {@code center}, moving <i>toward</i> the center rather
	 * than away from it.
	 */
	public static void pressureInward(Level level, Vec3 center, double radius, int color, float intensity)
	{
		if(!(level instanceof ServerLevel serverLevel))
			return;

		RandomSource random = serverLevel.getRandom();
		int count = Math.max(1, Math.round(3.0F * intensity));

		for(int i = 0; i < count; i++)
		{
			double angle = random.nextDouble() * 2.0 * Math.PI;
			double height = (random.nextDouble() - 0.5) * 1.5;
			Vec3 offset = new Vec3(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
			Vec3 point = center.add(offset);
			Vec3 inward = offset.length() > 1.0E-4 ? offset.normalize().scale(-0.06 * intensity) : Vec3.ZERO;

			MSUParticles.spawnPowerParticle(level, point.x, point.y, point.z, inward.x, inward.y, inward.z, 10 + random.nextInt(6), color);
		}
	}

	/**
	 * Free Will's own activation visual - "create a large expanding pressure wave... transparent wind
	 * sphere expands outward." A one-shot shell of particles scattered roughly evenly across a sphere at
	 * {@code radius}, all moving further outward - called once on activation, not every tick (there's no
	 * ongoing hold state to re-call it from).
	 */
	public static void expandingBurst(Level level, Vec3 center, double radius, int color, int shellPoints)
	{
		if(!(level instanceof ServerLevel serverLevel))
			return;

		RandomSource random = serverLevel.getRandom();

		for(int i = 0; i < shellPoints; i++)
		{
			double theta = random.nextDouble() * 2.0 * Math.PI;
			double phi = Math.acos(2.0 * random.nextDouble() - 1.0);
			Vec3 dir = new Vec3(Math.sin(phi) * Math.cos(theta), Math.cos(phi) * 0.6, Math.sin(phi) * Math.sin(theta));
			Vec3 point = center.add(dir.scale(radius));
			Vec3 vel = dir.scale(0.15);

			MSUParticles.spawnPowerParticle(level, point.x, point.y, point.z, vel.x, vel.y, vel.z, 8 + random.nextInt(6), color);
		}
	}

	/**
	 * The one real "Environmental Reaction" from the source doc's own list - see this class's own doc
	 * comment for why the rest of that list isn't implemented. A genuine, if minor, physics nudge (not
	 * purely cosmetic - there's no separate visual-only channel for an {@link ItemEntity} to move through),
	 * broadcast the same way {@code heroAspect.breath.TechBreathGale}'s own launch already broadcasts a
	 * real connected entity's velocity change ({@code hurtMarked = true}).
	 */
	public static void nudgeNearbyItems(Level level, Vec3 center, double radius, Vec3 direction, double strength)
	{
		if(!(level instanceof ServerLevel serverLevel))
			return;

		AABB area = new AABB(center.x - radius, center.y - radius, center.z - radius, center.x + radius, center.y + radius, center.z + radius);
		for(ItemEntity item : serverLevel.getEntitiesOfClass(ItemEntity.class, area))
		{
			item.setDeltaMovement(item.getDeltaMovement().add(direction.scale(strength)));
			item.hurtMarked = true;
		}
	}

	/** Radial equivalent of {@link #nudgeNearbyItems} - each item gets pushed directly away from {@code center} instead of a single shared direction, for a burst/explosion-shaped effect rather than a linear flow. Real caller: {@code heroClass.page.breath.TechPageBreathFreeWill}'s activation ("dust is pushed outward"). */
	public static void nudgeItemsOutward(Level level, Vec3 center, double radius, double strength)
	{
		if(!(level instanceof ServerLevel serverLevel))
			return;

		AABB area = new AABB(center.x - radius, center.y - radius, center.z - radius, center.x + radius, center.y + radius, center.z + radius);
		for(ItemEntity item : serverLevel.getEntitiesOfClass(ItemEntity.class, area))
		{
			Vec3 away = item.position().subtract(center);
			Vec3 direction = away.length() > 1.0E-4 ? away.normalize() : new Vec3(0.0, 1.0, 0.0);
			item.setDeltaMovement(item.getDeltaMovement().add(direction.scale(strength)));
			item.hurtMarked = true;
		}
	}

	private static Vec3[] perpendicularBasis(Vec3 dir)
	{
		Vec3 reference = Math.abs(dir.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
		Vec3 perp1 = dir.cross(reference).normalize();
		Vec3 perp2 = dir.cross(perp1).normalize();
		return new Vec3[]{perp1, perp2};
	}
}
