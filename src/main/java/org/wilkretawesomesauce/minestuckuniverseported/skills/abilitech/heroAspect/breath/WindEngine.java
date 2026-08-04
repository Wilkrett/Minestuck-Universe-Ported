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
 * scatter, on top of the same real {@link MSUParticles#spawnPowerParticle}/{@link MSUParticles#spawnWindWisp}
 * primitives every other aspect's own particle calls (or, for {@link #ribbon}/{@link #windSwirl}, this
 * class's own newer wind-specific particle) already use (no hand-drawn placeholder art anywhere - see each
 * primitive's own doc comment for which real vanilla art it reuses).
 * <p>
 * <b>{@link #ribbon}/{@link #windSwirl} switched to {@link MSUParticles#spawnWindWisp}, a direct later user
 * request</b> ("I want something like this [a reference screenshot of soft, blurred, curling smoke-ring
 * wisps]... though keep the color blue"): {@link #spiralAroundTarget}/{@link #pressureInward}/
 * {@link #expandingBurst} still use the older, sharper {@code spawnPowerParticle} (vanilla firework-spark
 * art) unchanged - out of scope for this pass, still correct for what they visualize. Only the two methods
 * that needed to read as "soft natural wind" switched to the new wisp particle - see
 * {@code client.particles.WindWispParticle}'s own doc comment for the full reasoning and the real vanilla
 * Wind Charge/Breeze art (`gust_0`-`gust_11`) it reuses.
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
	private static final int RIBBON_PARTICLES_PER_TICK = 3;
	private static final double RIBBON_JITTER = 0.12;
	private static final float RIBBON_WISP_SCALE = 0.5F;

	// Mirrors client.render.WindRibbonRenderer's own TWIST_FREQ_1/2, TIME_SPEED_1/2, TWIST_AMPLITUDE,
	// LIGHTNING_PHASE exactly - see this class's own ribbon() doc comment for why this is a deliberate
	// server-side duplicate of that renderer's private ribbonPoint math, not a shared extraction.
	private static final double TWIST_FREQ_1 = 0.4;
	private static final double TWIST_FREQ_2 = 0.7;
	private static final double TIME_SPEED_1 = 0.08;
	private static final double TIME_SPEED_2 = -0.05;
	private static final double TWIST_AMPLITUDE = 0.3;
	private static final float TRAIL_PHASE = 5.0F;

	private static final int SWIRL_PARTICLES_PER_TICK = 4;
	private static final float SWIRL_WISP_SCALE = 1.4F;
	private static final double SWIRL_TANGENT_SPEED = 0.03;
	private static final double SWIRL_BOB_AMPLITUDE = 0.4;
	private static final double SWIRL_BOB_SPEED = 0.6;

	private WindEngine()
	{
	}

	/**
	 * "Wind Ribbons" - the doc's own primary Breath visual: translucent flowing particles curving from
	 * {@code from} toward {@code to}. Deliberately low particle count per tick (the doc's own explicit
	 * instruction) - a random sample of points along the path each tick, not the whole path at once, so the
	 * flow reads as continuous across several ticks rather than a single dense burst. Curves toward wherever
	 * {@code to} <i>currently</i> is - called fresh every tick with the target's live position, so a moving
	 * target naturally bends the ribbon instead of it snapping to a stale straight line (the doc's own
	 * "should curve if the target moves").
	 * <p>
	 * <b>Reworked to trace the mesh's own lightning-trail curve, a direct later user request</b> ("reuse
	 * windengine but wire it to be using the trails instead of the streaks" - i.e. this method's own
	 * independent single cos/sin spiral-twist path was replaced with the exact same tapered, two-summed-sine
	 * curve {@code client.render.WindRibbonRenderer}'s lightning tube already animates along
	 * ({@code ribbonPoint}, {@code streakOffset=0}, {@code phase=LIGHTNING_PHASE}) - so these particles now
	 * visually hug the mesh's own glowing core instead of tracing a separate, disconnected line. A real
	 * server-side duplicate of that private client-only math, not a shared extraction: this method runs on
	 * the server tick loop (via {@code TechBreathLiberate}/{@code TechBreathConstrain}), and
	 * {@code WindRibbonRenderer} is {@code @EventBusSubscriber(..., value = Dist.CLIENT)} - importing it here
	 * would pull client rendering code onto the dedicated-server classpath. Same reasoning already documented
	 * on both classes' identical, already-duplicated {@code perpendicularBasis} helper.
	 * <p>
	 * <b>Switched to {@link MSUParticles#spawnWindWisp} with a small perpendicular jitter, a direct later
	 * user request</b> (soft-wind reference screenshots, "I liked the thickness &amp; amount the streaks
	 * had"): a precise spark riding exactly on the curve read as a crisp line of motes, not a soft drifting
	 * stream. Each spawn point now gets a small random offset off the curve (still centered on it, just not
	 * pinned to it) so the wisps read as a loose cloud following the trail rather than a thin string of
	 * particles.
	 */
	public static void ribbon(Level level, Vec3 from, Vec3 to, float time, int color, float intensity)
	{
		if(!(level instanceof ServerLevel serverLevel))
			return;

		Vec3 delta = to.subtract(from);
		double length = delta.length();
		if(length < 0.5)
			return;

		Vec3 dir = delta.scale(1.0 / length);
		Vec3[] basis = perpendicularBasis(dir);
		RandomSource random = serverLevel.getRandom();

		int samples = Math.max(1, (int) (length / RIBBON_SAMPLE_SPACING));
		int toSpawn = Math.min(RIBBON_PARTICLES_PER_TICK, samples);

		for(int i = 0; i < toSpawn; i++)
		{
			float t = random.nextFloat();
			Vec3 point = curvePoint(from, to, t, (float) length, time, basis);
			Vec3 jitter = basis[0].scale((random.nextDouble() - 0.5) * RIBBON_JITTER)
					.add(basis[1].scale((random.nextDouble() - 0.5) * RIBBON_JITTER));
			point = point.add(jitter);
			Vec3 vel = dir.scale(0.05 * intensity);

			MSUParticles.spawnWindWisp(level, point.x, point.y, point.z, vel.x, vel.y, vel.z, 14 + random.nextInt(10), color, RIBBON_WISP_SCALE);
		}
	}

	/** Server-side twin of {@code WindRibbonRenderer#ribbonPoint} with {@code streakOffset=0}/{@code phase=TRAIL_PHASE}/{@code freqScale=1} baked in - see {@link #ribbon}'s own doc comment for why this is duplicated rather than shared. */
	private static Vec3 curvePoint(Vec3 start, Vec3 end, float t, float length, float time, Vec3[] basis)
	{
		Vec3 base = start.add(end.subtract(start).scale(t));

		double taper = Math.sin(t * Math.PI);
		double phase1 = t * length * TWIST_FREQ_1 + time * TIME_SPEED_1 * 20.0 + TRAIL_PHASE;
		double phase2 = t * length * TWIST_FREQ_2 + time * TIME_SPEED_2 * 20.0 + TRAIL_PHASE * 0.6;

		double offset1 = Math.sin(phase1) * TWIST_AMPLITUDE * taper;
		double offset2 = Math.cos(phase2) * TWIST_AMPLITUDE * 0.6 * taper;

		return base.add(basis[0].scale(offset1)).add(basis[1].scale(offset2));
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
	 * A soft, curling ring of {@link MSUParticles#spawnWindWisp} wisps orbiting {@code center} - a direct
	 * later user request, replacing the geometric mesh's role as the primary "this looks like wind" visual
	 * with the technique a reference screenshot actually used (soft blurred particles curling around the
	 * caster in overlapping bands, not a precise line). Reuses {@link #spiralAroundTarget}'s own orbiting
	 * shape (angle/radius/tangential velocity) but denser, bigger ({@link #SWIRL_WISP_SCALE}), slower
	 * ({@link #SWIRL_TANGENT_SPEED}), and with a gentle vertical bob ({@code sin(time * SWIRL_BOB_SPEED)})
	 * layered on top of the usual random height jitter, so it reads as a soft curling ring/aura rather than
	 * a thin fast vortex of motes.
	 */
	public static void windSwirl(Level level, Vec3 center, double radius, float time, int color, float intensity)
	{
		if(!(level instanceof ServerLevel serverLevel))
			return;

		RandomSource random = serverLevel.getRandom();
		int count = Math.max(1, Math.round(SWIRL_PARTICLES_PER_TICK * intensity));
		double bob = Math.sin(time * SWIRL_BOB_SPEED) * SWIRL_BOB_AMPLITUDE;

		for(int i = 0; i < count; i++)
		{
			double angle = random.nextDouble() * 2.0 * Math.PI;
			double height = bob + (random.nextDouble() - 0.5) * 0.6;
			Vec3 offset = new Vec3(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
			Vec3 point = center.add(offset);
			Vec3 tangent = new Vec3(-Math.sin(angle), 0.0, Math.cos(angle)).scale(SWIRL_TANGENT_SPEED * intensity);

			MSUParticles.spawnWindWisp(level, point.x, point.y, point.z, tangent.x, tangent.y, tangent.z, 20 + random.nextInt(10), color, SWIRL_WISP_SCALE);
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
