package org.wilkretawesomesauce.minestuckuniverseported.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.client.WindRibbonClientState;

import java.util.Map;

/**
 * The real primary Breath visual system - "Breath Visualizer Architecture Decision" design doc's own
 * "Wind Field" renderer (no 1.12.2 counterpart, original design for this project). Renders a genuine
 * procedural "lightning trail" mesh between caster and target (not vanilla particles, which the doc
 * explicitly demotes to secondary/atmospheric-only status - see
 * {@code skills.abilitech.heroAspect.breath.WindEngine}'s own doc comment for that half), plus a spiral
 * vortex around the target. Fed by {@code client.WindRibbonClientState}, populated by
 * {@code network.WindRibbonSyncPacket}.
 * <p>
 * <b>Real "lightning trail" layer</b> - a direct user request, pointed at a real reference mod (`ChestItem`,
 * NeoForge 1.21.1) whose own trail entities render through vanilla's real {@link RenderType#lightning()}
 * instead of a textured quad: untextured, additive-ish, glowing vertex-colored geometry, the same render
 * type vanilla itself uses for actual lightning bolts (confirmed against {@code LightningBoltRenderer}'s
 * own real source, not guessed - its vertex format is {@code POSITION_COLOR}, so a lightning-type vertex
 * only ever gets a position and a color, no UV/overlay/light/normal calls at all). {@link #renderLightningTube}
 * builds real 3D cylinder geometry ({@link #LIGHTNING_TUBE_SIDES} sides) rather than a flat billboard -
 * looks round and solid from any angle instead of paper-thin, matching the reference mod's own
 * {@code renderBlood}/{@code addSquare} technique - along an animated curve ({@link #ribbonPoint}, two
 * summed sine waves of different spatial frequency/phase, offset perpendicular to the caster-target line
 * and tapered to zero at both ends via {@code sin(t*PI)} so it anchors cleanly rather than flailing right
 * at the endpoints - the doc's own "Simple" turbulence method, "smooth curved movement... slight twisting
 * motion... should curve if the target moves").
 * <p>
 * {@link #renderLightningVortex} is the doc's own "Spiral Currents" - a gentle wrap around the target (angle
 * advancing with both arc-length and real time, so it visibly rotates), radius/density scaled by
 * {@link WindRibbonClientState.RenderRibbon#intensity()} (Liberate's own "gentle orbiting rings... grows with
 * Freedom" - see {@code TechBreathLiberate}'s own call site for how that value is derived). For
 * {@code inward} ribbons (Constrain), the spiral's radius shrinks toward the target along its own length
 * instead of holding constant - "air pressure compresses toward the target" made literal, and colored with
 * Breath's own second real palette entry (a deeper blue, never a darker/invented "evil" color - the design
 * doc's own explicit instruction).
 * <p>
 * <b>Real crash, caught from a live client report ("Crashed...") the very first time this layer actually
 * ran</b>: {@code IllegalStateException: Not building!} inside {@code VertexConsumer#addVertex}. Root cause
 * confirmed by reading {@code MultiBufferSource.java}'s real {@code getBuffer()} source, not guessed: this
 * used to also render a textured "quad-streak" style sharing one interleaved loop with this lightning
 * layer, and {@code entityTranslucent(TEXTURE)}/{@code lightning()} are both "shared-buffer" render types
 * (neither has its own dedicated fixed buffer) - {@code getBuffer()} unconditionally ends whichever
 * shared-buffer type was last active the instant a *different* shared-buffer type is requested, so fetching
 * the second consumer right after the first silently ended the first's batch before a single vertex had
 * been written to it. Fixed at the time by splitting into two fully sequential passes; the textured
 * quad-streak pass itself was later removed entirely (see below), so this class now only ever fetches one
 * render type per frame and the two-pass structure is no longer strictly needed - kept anyway as the
 * simplest, already-proven-safe shape. General rule for any future render type added here (or anywhere else
 * two shared-buffer types are used in the same frame): never hold two different shared-buffer
 * {@link VertexConsumer} references live across a {@code getBuffer()} call boundary.
 * <p>
 * <b>The textured "quad-streak" style (parallel wavy billboard bands) is gone entirely, a direct later user
 * request</b> ("it still uses streaks.. causing it to look quite jarring", from a live screenshot): this
 * class used to also draw {@code STREAK_COUNT}-many wide translucent billboard quads (camera-facing strips
 * textured with a reused {@code clear_beam.png}) alongside the lightning tube - first removed for Constrain
 * only (a separate, earlier request), then removed for Liberate too once the reused wide quads themselves
 * turned out to read as visually jarring even alone, not just relative to Constrain's cleaner look. Both
 * abilities now render identically: the lightning tube (this doc comment's own primary section) plus its
 * lightning vortex, and nothing else - {@code skills.abilitech.heroAspect.breath.WindEngine}'s own particle
 * trail (reworked to trace this exact same curve, see its own doc comment) is the only other visual layer
 * left. The former quad-billboard machinery ({@code renderRibbon}/{@code renderVortex}/{@code renderQuad}/
 * {@code vertex}, and the {@code TEXTURE}/{@code RADIUS}/{@code ALPHA}/{@code MIN_CAM_DISTANCE_SQR}/
 * {@code STREAK_*} constants it alone used) was deleted outright rather than left dead, since nothing else
 * in the project ever called it and this class's own history already shows that style was the repeat source
 * of every visual complaint (double-emission z-fighting, tight-spiral flicker, and now this).
 * <p>
 * <b>Multiple parallel trail strands, a direct later user request</b> ("there's only 1 trail instead of
 * multiple... I liked the thickness &amp; amount the streaks had"): removing the quad-streak style above
 * also removed its "several parallel bands" look, leaving a single thin tube that read as too sparse on its
 * own. {@link #renderLightningRibbon} now draws {@link #TRAIL_STRAND_COUNT} parallel tubes instead of one,
 * reusing the exact same "fixed untapered lateral offset + tapered animated wobble + phase/frequency
 * stagger per strand" shape the deleted quad-streak style used ({@link #ribbonPoint}'s restored
 * {@code strandOffset}/{@code phase}/{@code freqScale} parameters) - so the "amount" is back without
 * reintroducing the flat *billboard* geometry that actually caused the jarring look (a screen-facing quad
 * that goes edge-on/degenerate near the camera); each strand is still real, always-solid 3D tube geometry,
 * just several of them side by side.
 * <p>
 * <b>Flattened tube cross-section, a direct later user request</b> ("i think i need the trails somewhat
 * more flatter or stretched out... it doesnt really feel like a breath/wind effect"): a perfectly circular
 * tube read as a rigid round rope rather than flowing wind. {@link #renderLightningTube} now builds an
 * <i>elliptical</i> cross-section instead of a circular one - {@link #LIGHTNING_TUBE_WIDTH} (wide) along
 * the tube's own local basis axis, {@link #LIGHTNING_TUBE_THICKNESS} (thin) along the other - and that
 * "wide" axis is exactly the same one {@link #renderLightningRibbon} already spreads its parallel strands
 * along, so each strand's own flat side lines up with the "sheet" the strands together form, reading as
 * overlapping flat ribbons of wind rather than round rods or pipes. Deliberately still real 3D geometry
 * with a fixed world-space cross-section, not a revived camera-facing billboard (which is what actually
 * degenerated near the camera and needed a cutoff hack before) - a real ribbon's width can look thinner
 * from some angles than others, same as real cloth/wind would, which reads as natural rather than as the
 * earlier bug.
 * <p>
 * <b>Fade-out on release, a direct later user request</b> ("instead of instantly making the trail
 * disappear it should slowly fade out"): {@code client.WindRibbonClientState} used to remove a ribbon the
 * instant its ability released, so this renderer simply stopped seeing it the next frame - a hard pop, not
 * a fade. That class now mirrors its own {@code StreakClientState}'s live/fading-out map split (see its own
 * doc comment) and hands back a {@code fadeMultiplier} (1 while active, ramping to 0 over its fade window)
 * via {@link WindRibbonClientState.RenderRibbon#fadeMultiplier()} - multiplied into every alpha value this
 * class computes, at every {@link #renderLightningTube} call site, independent of (not a replacement for)
 * the existing {@code intensity} scalar.
 * <p>
 * <b>Strands repositioned to originate from the caster's sides, a direct later user request</b> ("should
 * come out from the sides instead of 3 begin points on the front" - a live screenshot showed all
 * {@link #TRAIL_STRAND_COUNT} strands bunched at nearly the same spot in front of the caster's face,
 * only ever separated by a small {@link #TRAIL_STRAND_SPACING}).
 * <p>
 * <b>Real per-vertex dark/light two-tone shading, from a reference gif</b> ("each wind thing should have
 * a dark + light side to it... dark is outer, light is inner") of a twisting wind burst whose crescent
 * shape alternates between a dark outer rim and a lighter inner highlight as it curls. {@link RenderType#lightning()}
 * vertices are {@code POSITION_COLOR} only (no vanilla lighting ever touches them - see this class's own
 * "Real crash" note above for why that render type's vertex format is this bare), so there's no lighting
 * engine to fake this with - the two-tone look is baked directly into each cross-section vertex's own
 * color instead. {@link #shadeFactor} picks a real fixed brightness multiplier by {@code cos(angle -
 * twistPhase)} around the {@link #LIGHTNING_TUBE_SIDES}-sided cross-section; {@link #shadeTwistPhase}
 * rotates that angle progressively along the tube's own length and over time, so the dark/light boundary
 * visibly spirals around the tube as it travels - a real twist, not a static painted-on stripe.
 * {@link #renderLightningVortex} reuses its own already-computed spiral {@code angle} directly as the
 * twist phase; {@link #renderLightningRibbon} computes a synthetic one instead via {@link #shadeTwistPhase}.
 * <p>
 * <b>Behind-the-caster start + random vertical offset per strand, a direct later user request</b> - see
 * {@link #START_BEHIND_CASTER_DISTANCE}/{@link #START_RANDOM_Y_RANGE}'s own doc comment for the full story,
 * including the later {@code spawnTick}-based correction ({@code client.WindRibbonClientState}) so the
 * random offset re-rolls per cast instead of staying fixed for a caster's whole session.
 * <p>
 * <b>Real allocation-elimination pass, a direct user request</b> ("Isn't 2k vertices extremely
 * unoptimized" / "Optimize this"): the raw {@link #LIGHTNING_TUBE_SIDES}-sided vertex count itself was
 * never the actual cost (GPUs eat thousands of vertices for breakfast) - the real problem was that every
 * one of those ~2,000 vertices/ribbon/frame was being built through Minecraft's <i>immutable</i>
 * {@link Vec3} (every {@code add}/{@code subtract}/{@code scale}/{@code cross}/{@code normalize} call
 * allocates a brand new object) plus a fresh {@code float[3]} from the old array-returning shade helper,
 * all inside a hook ({@code RenderLevelStageEvent.AFTER_PARTICLES}) that runs once per <i>rendered frame</i>
 * (not once per game tick) - at 144fps that's ~144 full geometry rebuilds/second, each generating several
 * thousand short-lived objects. Every hot-path method below ({@link #ribbonPoint}/{@link #computeBasis}/
 * {@link #renderLightningTube}/{@link #renderLightningVortex}/{@link #lightningVertex}) now works purely in
 * raw {@code double}/{@code float} components instead of {@link Vec3}, and {@link #shadeFactor} returns a
 * single {@code float} multiplier instead of a {@code float[]} - the only remaining {@link Vec3} use in the
 * whole render pass is the two per-ribbon {@code start}/{@code end} lookups in {@link #onRenderLevel} itself
 * (real entity position reads, once per ribbon per frame, not once per vertex). {@link #BASIS_SCRATCH}/
 * {@link #POINT_SCRATCH} are real mutable scratch buffers (not thread-safe, deliberately - see their own
 * doc comment for why that's fine here) replacing what used to be a fresh {@code Vec3[]}/{@code Vec3} per
 * call; {@link #TUBE_COS}/{@link #TUBE_SIN} precompute the {@link #LIGHTNING_TUBE_SIDES} cross-section
 * angles' sine/cosine once at class-init (they're compile-time-fixed angles, unlike the live twist-phase
 * trig {@link #shadeFactor} still has to compute fresh every call), cutting real redundant
 * {@link Math#cos}/{@link Math#sin} calls out of the tube-shape geometry specifically. No formula or
 * visual output changed in this pass - every value this class produces is bit-for-bit the same as before,
 * only how it gets computed changed.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class WindRibbonRenderer
{
	private static final int OUTWARD_COLOR = 0x006EE9;
	private static final int INWARD_COLOR = 0x10E0FF;

	private static final int SEGMENTS = 20;

	// Frequency roughly halved and amplitude reduced relative to spacing from the first pass - long, lazy
	// curves rather than a tight zigzag, matching the reference's own broad, slow waves and keeping the
	// per-segment bend shallow (less faceting - camera-facing/orientation-recomputed geometry only looks
	// smooth when consecutive segments don't turn too sharply).
	private static final double TWIST_FREQ_1 = 0.4;
	private static final double TWIST_FREQ_2 = 0.7;
	private static final double TIME_SPEED_1 = 0.08;
	private static final double TIME_SPEED_2 = -0.05;
	private static final double TWIST_AMPLITUDE = 0.3;

	// Well under one full turn (was 1.5) at a wider radius - a gentle wrap around the target, not a tight
	// coil - see this class's own doc comment for why the tight version was a real flicker source.
	private static final int VORTEX_SEGMENTS = 24;
	private static final double VORTEX_TURNS = 0.55;
	private static final double VORTEX_SPIN_SPEED = 0.05;
	private static final double VORTEX_HEIGHT = 1.6;
	private static final double VORTEX_RADIUS_MIN = 0.7;
	private static final double VORTEX_RADIUS_MAX = 1.7;

	// The glowing "lightning core" layer - see this class's own doc comment. Elliptical, not circular -
	// flattened along the tube's own local basis[0] (the same axis strands are spread apart along, see
	// ribbonPoint/renderLightningRibbon) so each strand reads as a flat ribbon rather than a round rope.
	private static final int LIGHTNING_TUBE_SIDES = 6;
	private static final float LIGHTNING_TUBE_WIDTH = 0.18F;
	private static final float LIGHTNING_TUBE_THICKNESS = 0.03F;
	// Lowered from 0.55 - a direct later user request pivoted the *primary* "this looks like wind" visual to
	// a soft particle swarm (WindEngine's new wind-wisp system); this mesh is now meant to read as a faint
	// accent thread underneath that swarm, not compete with it for attention. Not deleted - still a real,
	// crash-tested, working visual - just de-emphasized. (Later raised back to full opacity by a direct
	// user edit - see git history if the exact reasoning for that specific change is ever needed.)
	private static final float LIGHTNING_ALPHA = 1.0F;
	private static final float LIGHTNING_PHASE = 5.0F;

	// Precomputed cos/sin for the LIGHTNING_TUBE_SIDES cross-section angles - see this class's own doc
	// comment ("Real allocation-elimination pass") for why these are safe to precompute once: the angles
	// themselves (2*PI*i/SIDES) never depend on any runtime state, only the live twist phase they're later
	// compared against does (that part - shadeFactor - still computes cos() fresh every call, since it
	// genuinely can't be precomputed).
	private static final double[] TUBE_COS = new double[LIGHTNING_TUBE_SIDES];
	private static final double[] TUBE_SIN = new double[LIGHTNING_TUBE_SIDES];
	private static final double[] TUBE_ANGLE = new double[LIGHTNING_TUBE_SIDES];

	static
	{
		for(int i = 0; i < LIGHTNING_TUBE_SIDES; i++)
		{
			double angle = (2.0 * Math.PI * i) / LIGHTNING_TUBE_SIDES;
			TUBE_ANGLE[i] = angle;
			TUBE_COS[i] = Math.cos(angle);
			TUBE_SIN[i] = Math.sin(angle);
		}
	}

	// Multiple parallel strands rather than one, a direct later user request ("I liked the thickness &
	// amount the streaks had") - see this class's own doc comment. Same shape the deleted quad-streak style
	// used to use (offset spacing that doesn't taper, so strands stay visibly spread rather than pinching
	// back to one point at the endpoints; phase stagger + a small per-strand frequency bump so they wave
	// independently instead of moving in lockstep).
	private static final int TRAIL_STRAND_COUNT = 3;
	private static final double TRAIL_STRAND_SPACING = 1.0;
	private static final float TRAIL_STRAND_PHASE_STAGGER = 1.7F;
	private static final double TRAIL_STRAND_FREQ_VARIATION = 0.09;

	// Starts each strand a bit behind the caster (opposite the target) instead of exactly at their eye
	// position, and gives each strand its own small stable random vertical offset there - a direct user
	// request, mirroring WindEngine's own particle trail treatment (see that class's own
	// RIBBON_BEHIND_CASTER_DISTANCE). The random offset is seeded from caster id + WindRibbonClientState's
	// own real spawnTick + strand index (stableRandom), not Math.random() and not caster id alone - a
	// direct user correction ("this is only random on game launch... should be done when spawning in the
	// wind engine"), since seeding off caster id alone re-derived the exact same offset for that player's
	// every cast for their whole session rather than a fresh one each time the ribbon actually spawns. See
	// renderLightningRibbon's own doc comment for the full story.
	private static final double START_BEHIND_CASTER_DISTANCE = 2.5;
	private static final double START_RANDOM_Y_RANGE = 2.0;

	// Real per-vertex dark/light two-tone shading - see this class's own doc comment. A single fixed
	// brightness multiplier per side of the twist boundary (SHADE_DARK_FACTOR/SHADE_LIGHT_FACTOR), not a
	// blend toward black/white or toward some other base color; SHADE_TWIST_TURNS_PER_LENGTH/
	// SHADE_TWIST_SPIN_SPEED rotate the angle that boundary is measured from, along the tube's own length
	// and over real time, so it reads as a slow physical twist rather than a fixed painted-on stripe.
	private static final float SHADE_DARK_FACTOR = 0.65F;
	private static final float SHADE_LIGHT_FACTOR = 1.15F;
	private static final double SHADE_TWIST_TURNS_PER_LENGTH = 0.18;
	private static final double SHADE_TWIST_SPIN_SPEED = 1.2;

	// Reused scratch buffers for the hot geometry path - see this class's own doc comment ("Real
	// allocation-elimination pass") for the full reasoning. Deliberately NOT thread-safe: client rendering
	// (RenderLevelStageEvent) only ever runs on the single client render thread, never concurrently and
	// never re-entrantly, and every caller fully reads its result out into local primitives before any
	// nested call could possibly overwrite it again - see computeBasis/ribbonPoint's own doc comments for
	// the exact "write, then immediately unpack to locals" discipline that makes this safe.
	private static final double[] BASIS_SCRATCH = new double[6];
	private static final double[] POINT_SCRATCH = new double[3];

	private WindRibbonRenderer()
	{
	}

	@SubscribeEvent
	private static void onRenderLevel(RenderLevelStageEvent event)
	{
		if(event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
			return;

		Map<Integer, WindRibbonClientState.RenderRibbon> ribbons = WindRibbonClientState.getRenderRibbons();
		if(ribbons.isEmpty())
			return;

		Minecraft mc = Minecraft.getInstance();
		if(mc.level == null)
			return;

		PoseStack poseStack = event.getPoseStack();
		Vec3 camPos = event.getCamera().getPosition();
		MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		float time = (mc.level.getGameTime() + partialTick) / 20.0F;

		poseStack.pushPose();
		poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
		PoseStack.Pose pose = poseStack.last();

		VertexConsumer lightningConsumer = bufferSource.getBuffer(RenderType.lightning());
		for(Map.Entry<Integer, WindRibbonClientState.RenderRibbon> entry : ribbons.entrySet())
		{
			Entity caster = mc.level.getEntity(entry.getKey());
			Entity target = mc.level.getEntity(entry.getValue().targetId());
			if(!(caster instanceof LivingEntity livingCaster) || !livingCaster.isAlive()
					|| !(target instanceof LivingEntity livingTarget) || !livingTarget.isAlive())
				continue;

			boolean inward = entry.getValue().inward();
			float intensity = entry.getValue().intensity();
			float fadeMultiplier = entry.getValue().fadeMultiplier();
			int color = inward ? INWARD_COLOR : OUTWARD_COLOR;
			float r = ((color >> 16) & 0xFF) / 255F;
			float g = ((color >> 8) & 0xFF) / 255F;
			float b = (color & 0xFF) / 255F;

			Vec3 start = livingCaster.getPosition(partialTick).add(0, livingCaster.getEyeHeight() * 0.8, 0);
			Vec3 end = livingTarget.getPosition(partialTick).add(0, livingTarget.getBbHeight() * 0.5, 0);

			renderLightningRibbon(lightningConsumer, pose, start.x, start.y, start.z, end.x, end.y, end.z,
					time, r, g, b, fadeMultiplier, livingCaster.getId(), entry.getValue().spawnTick());
			renderLightningVortex(lightningConsumer, pose, end.x, end.y, end.z, time, inward,
					Math.max(0.2F, intensity), r, g, b, fadeMultiplier);
		}
		bufferSource.endBatch(RenderType.lightning());

		poseStack.popPose();
	}

	/**
	 * A point on one strand's animated curve - a fixed (untapered) lateral offset from the caster-target
	 * centerline, plus a tapered, time-animated wobble on top: the fixed offset itself doesn't taper (keeps
	 * the strands visibly spread the whole way rather than all pinching back together at the caster/target),
	 * while the wobble does (zeroed at both ends via {@code sin(t*PI)} so each strand anchors cleanly rather
	 * than flailing right at the endpoints) - see this class's own doc comment.
	 * <p>
	 * Writes its result into {@code out} (expected length 3) rather than returning a new {@link Vec3} - see
	 * this class's own doc comment ("Real allocation-elimination pass"). Safe to call repeatedly against
	 * the same {@code out} array (e.g. {@link #POINT_SCRATCH}) as long as the caller fully reads {@code out}
	 * before the next call - this method never reads {@code out} itself, only writes it.
	 */
	private static void ribbonPoint(double startX, double startY, double startZ, double endX, double endY, double endZ,
			float t, float length, float time, double b0x, double b0y, double b0z, double b1x, double b1y, double b1z,
			double strandOffset, float phase, double freqScale, double[] out)
	{
		double baseX = startX + (endX - startX) * t;
		double baseY = startY + (endY - startY) * t;
		double baseZ = startZ + (endZ - startZ) * t;

		double taper = Math.sin(t * Math.PI);
		double phase1 = t * length * TWIST_FREQ_1 * freqScale + time * TIME_SPEED_1 * 20.0 + phase;
		double phase2 = t * length * TWIST_FREQ_2 * freqScale + time * TIME_SPEED_2 * 20.0 + phase * 0.6;

		double offset1 = strandOffset + Math.sin(phase1) * TWIST_AMPLITUDE * taper;
		double offset2 = Math.cos(phase2) * TWIST_AMPLITUDE * 0.6 * taper;

		out[0] = baseX + b0x * offset1 + b1x * offset2;
		out[1] = baseY + b0y * offset1 + b1y * offset2;
		out[2] = baseZ + b0z * offset1 + b1z * offset2;
	}

	/**
	 * {@link #TRAIL_STRAND_COUNT} parallel glowing lightning-style tubes along the ribbon - see this
	 * class's own doc comment. Each strand's own base line runs from {@link #START_BEHIND_CASTER_DISTANCE}
	 * behind the caster (opposite the target) to the target, with a small stable random vertical offset
	 * ({@link #stableRandom}, seeded off {@code casterId} + {@code spawnTick} + strand index) applied at
	 * that behind-the-caster origin - see this class's own doc comment on
	 * {@link #START_BEHIND_CASTER_DISTANCE}. Seeding off {@code spawnTick} (the real tick this specific
	 * ribbon started on, from {@code client.WindRibbonClientState}) rather than {@code casterId} alone is a
	 * direct user correction ("this is only random on game launch... should be done when spawning in the
	 * wind engine") - {@code casterId} alone re-derives the exact same offset for every cast a given player
	 * ever makes, for their whole session, instead of a fresh one each time the ribbon actually spawns.
	 */
	private static void renderLightningRibbon(VertexConsumer consumer, PoseStack.Pose pose,
			double sx, double sy, double sz, double ex, double ey, double ez, float time,
			float r, float g, float b, float fadeMultiplier, int casterId, long spawnTick)
	{
		double axisX = ex - sx, axisY = ey - sy, axisZ = ez - sz;
		double length = Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);
		if(length < 1.0E-4)
			return;

		double invLength = 1.0 / length;
		double dirX = axisX * invLength, dirY = axisY * invLength, dirZ = axisZ * invLength;

		computeBasis(dirX, dirY, dirZ, BASIS_SCRATCH);
		double b0x = BASIS_SCRATCH[0], b0y = BASIS_SCRATCH[1], b0z = BASIS_SCRATCH[2];
		double b1x = BASIS_SCRATCH[3], b1y = BASIS_SCRATCH[4], b1z = BASIS_SCRATCH[5];

		double originX = sx - dirX * START_BEHIND_CASTER_DISTANCE;
		double originY = sy - dirY * START_BEHIND_CASTER_DISTANCE;
		double originZ = sz - dirZ * START_BEHIND_CASTER_DISTANCE;
		double extendedLength = length + START_BEHIND_CASTER_DISTANCE;

		for(int strand = 0; strand < TRAIL_STRAND_COUNT; strand++)
		{
			double strandOffset = (strand - (TRAIL_STRAND_COUNT - 1) / 2.0) * TRAIL_STRAND_SPACING;
			float phase = LIGHTNING_PHASE + strand * TRAIL_STRAND_PHASE_STAGGER;
			double freqScale = 1.0 + strand * TRAIL_STRAND_FREQ_VARIATION;
			int seed = casterId * 31 + strand + (int) (spawnTick * 0x2545F491L);
			double randomY = (stableRandom(seed) - 0.5) * START_RANDOM_Y_RANGE;
			double strandOriginX = originX, strandOriginY = originY + randomY, strandOriginZ = originZ;

			ribbonPoint(strandOriginX, strandOriginY, strandOriginZ, ex, ey, ez, 0F, (float) extendedLength, time,
					b0x, b0y, b0z, b1x, b1y, b1z, strandOffset, phase, freqScale, POINT_SCRATCH);
			double prevX = POINT_SCRATCH[0], prevY = POINT_SCRATCH[1], prevZ = POINT_SCRATCH[2];
			double prevTwist = shadeTwistPhase(0F, (float) extendedLength, time, phase);

			for(int i = 1; i <= SEGMENTS; i++)
			{
				float t = (float) i / SEGMENTS;
				ribbonPoint(strandOriginX, strandOriginY, strandOriginZ, ex, ey, ez, t, (float) extendedLength, time,
						b0x, b0y, b0z, b1x, b1y, b1z, strandOffset, phase, freqScale, POINT_SCRATCH);
				double curX = POINT_SCRATCH[0], curY = POINT_SCRATCH[1], curZ = POINT_SCRATCH[2];
				double twist = shadeTwistPhase(t, (float) extendedLength, time, phase);
				renderLightningTube(consumer, pose, prevX, prevY, prevZ, curX, curY, curZ, r, g, b,
						LIGHTNING_ALPHA * fadeMultiplier, prevTwist, twist);
				prevX = curX; prevY = curY; prevZ = curZ;
				prevTwist = twist;
			}
		}
	}

	/** Deterministic pseudo-random in [0, 1) from an integer seed - not {@link java.util.Random}, so the same seed always gives the same value across frames without needing to store any per-ribbon state. */
	private static double stableRandom(int seed)
	{
		int h = seed * 0x9E3779B1;
		h ^= h >>> 15;
		h *= 0x85EBCA6B;
		h ^= h >>> 13;
		return (h & 0xFFFFFF) / (double) 0xFFFFFF;
	}

	/** The glowing lightning-style core along the vortex's own path - see this class's own doc comment. */
	private static void renderLightningVortex(VertexConsumer consumer, PoseStack.Pose pose,
			double cx, double cy, double cz, float time, boolean inward, float intensity,
			float r, float g, float b, float fadeMultiplier)
	{
		boolean hasPrev = false;
		double prevX = 0, prevY = 0, prevZ = 0, prevAngle = 0;

		for(int i = 0; i <= VORTEX_SEGMENTS; i++)
		{
			float t = (float) i / VORTEX_SEGMENTS;
			double angle = t * VORTEX_TURNS * 2.0 * Math.PI + time * VORTEX_SPIN_SPEED * 20.0;
			double radius = (VORTEX_RADIUS_MIN + (VORTEX_RADIUS_MAX - VORTEX_RADIUS_MIN) * intensity) * (inward ? (1.0 - t * 0.7) : 1.0);
			double height = (t - 0.5) * VORTEX_HEIGHT * intensity;

			double pointX = cx + Math.cos(angle) * radius;
			double pointY = cy + height;
			double pointZ = cz + Math.sin(angle) * radius;

			if(hasPrev)
				renderLightningTube(consumer, pose, prevX, prevY, prevZ, pointX, pointY, pointZ, r, g, b,
						LIGHTNING_ALPHA * intensity * fadeMultiplier, prevAngle, angle);
			prevX = pointX; prevY = pointY; prevZ = pointZ;
			prevAngle = angle;
			hasPrev = true;
		}
	}

	/**
	 * Real 3D <i>elliptical</i> tube geometry ({@link #LIGHTNING_TUBE_SIDES} sides) between two points,
	 * rendered through vanilla's real {@link RenderType#lightning()} - see this class's own doc comment for
	 * why that render type (untextured, {@code POSITION_COLOR} only) needs a genuinely different
	 * vertex-building technique than a camera-facing billboard, and why a real 3D tube needs no near-camera
	 * degenerate-quad cutoff - its cross-section is fixed in world space, not scaled by distance to camera.
	 * Flattened ({@link #LIGHTNING_TUBE_WIDTH} &gt; {@link #LIGHTNING_TUBE_THICKNESS}) along the tube's own
	 * local basis axis rather than a perfect circle - see this class's own doc comment for why.
	 * <p>
	 * {@code twistStart}/{@code twistEnd} drive the real dark/light two-tone shading (see this class's own
	 * doc comment) - each ring's own {@link #shadeFactor} call is offset by whichever twist phase applies
	 * to that ring specifically (start ring uses {@code twistStart}, end ring uses {@code twistEnd}), so
	 * two adjacent tube segments that share a ring always agree on that ring's color instead of seaming.
	 */
	private static void renderLightningTube(VertexConsumer consumer, PoseStack.Pose pose,
			double startX, double startY, double startZ, double endX, double endY, double endZ,
			float r, float g, float b, float alpha, double twistStart, double twistEnd)
	{
		double axisX = endX - startX, axisY = endY - startY, axisZ = endZ - startZ;
		double lenSqr = axisX * axisX + axisY * axisY + axisZ * axisZ;
		if(lenSqr < 1.0E-6)
			return;

		double invLen = 1.0 / Math.sqrt(lenSqr);
		double dirX = axisX * invLen, dirY = axisY * invLen, dirZ = axisZ * invLen;

		computeBasis(dirX, dirY, dirZ, BASIS_SCRATCH);
		double b0x = BASIS_SCRATCH[0], b0y = BASIS_SCRATCH[1], b0z = BASIS_SCRATCH[2];
		double b1x = BASIS_SCRATCH[3], b1y = BASIS_SCRATCH[4], b1z = BASIS_SCRATCH[5];

		for(int i = 0; i < LIGHTNING_TUBE_SIDES; i++)
		{
			int next = (i + 1) % LIGHTNING_TUBE_SIDES;

			double cos1 = TUBE_COS[i] * LIGHTNING_TUBE_WIDTH, sin1 = TUBE_SIN[i] * LIGHTNING_TUBE_THICKNESS;
			double cos2 = TUBE_COS[next] * LIGHTNING_TUBE_WIDTH, sin2 = TUBE_SIN[next] * LIGHTNING_TUBE_THICKNESS;

			double off1x = b0x * cos1 + b1x * sin1, off1y = b0y * cos1 + b1y * sin1, off1z = b0z * cos1 + b1z * sin1;
			double off2x = b0x * cos2 + b1x * sin2, off2y = b0y * cos2 + b1y * sin2, off2z = b0z * cos2 + b1z * sin2;

			double angle1 = TUBE_ANGLE[i], angle2 = TUBE_ANGLE[next];
			float shadeStart1 = shadeFactor(angle1 - twistStart);
			float shadeStart2 = shadeFactor(angle2 - twistStart);
			float shadeEnd1 = shadeFactor(angle1 - twistEnd);
			float shadeEnd2 = shadeFactor(angle2 - twistEnd);

			lightningVertex(consumer, pose, startX + off1x, startY + off1y, startZ + off1z,
					Math.min(r * shadeStart1, 1F), Math.min(g * shadeStart1, 1F), Math.min(b * shadeStart1, 1F), alpha);
			lightningVertex(consumer, pose, startX + off2x, startY + off2y, startZ + off2z,
					Math.min(r * shadeStart2, 1F), Math.min(g * shadeStart2, 1F), Math.min(b * shadeStart2, 1F), alpha);
			lightningVertex(consumer, pose, endX + off2x, endY + off2y, endZ + off2z,
					Math.min(r * shadeEnd2, 1F), Math.min(g * shadeEnd2, 1F), Math.min(b * shadeEnd2, 1F), alpha);
			lightningVertex(consumer, pose, endX + off1x, endY + off1y, endZ + off1z,
					Math.min(r * shadeEnd1, 1F), Math.min(g * shadeEnd1, 1F), Math.min(b * shadeEnd1, 1F), alpha);
		}
	}

	/** Progressively rotating angle for {@link #shadeFactor}'s twist boundary - see this class's own doc comment for why this needs to be a rotating angle rather than a fixed one. */
	private static double shadeTwistPhase(float t, float length, float time, float phaseOffset)
	{
		return t * length * SHADE_TWIST_TURNS_PER_LENGTH * 2.0 * Math.PI + time * SHADE_TWIST_SPIN_SPEED + phaseOffset;
	}

	/**
	 * A single fixed brightness multiplier by {@code cos(angle)} - {@link #SHADE_DARK_FACTOR} on one side
	 * of the twist boundary, {@link #SHADE_LIGHT_FACTOR} on the other - see this class's own doc comment
	 * for the real dark/light two-tone shading this drives. Returns a lone {@code float} rather than a
	 * {@code float[]} (see this class's own doc comment, "Real allocation-elimination pass") - the caller
	 * applies it to r/g/b directly, clamping at 1 itself.
	 */
	private static float shadeFactor(double angle)
	{
		return Math.cos(angle) < 0.0 ? SHADE_DARK_FACTOR : SHADE_LIGHT_FACTOR;
	}

	/** A {@code POSITION_COLOR} vertex - no UV/overlay/light/normal, confirmed against vanilla's own {@code LightningBoltRenderer} that {@link RenderType#lightning()} vertices only ever get position + color. */
	private static void lightningVertex(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z, float r, float g, float b, float alpha)
	{
		consumer.addVertex(pose, (float) x, (float) y, (float) z)
				.setColor(r, g, b, alpha);
	}

	/**
	 * Two vectors perpendicular to {@code dir} (and to each other), written into {@code out} (expected
	 * length 6: {@code out[0..2]} is the first, {@code out[3..5]} the second) - functionally the same
	 * cross-product construction {@code skills.abilitech.heroAspect.breath.WindEngine#perpendicularBasis}
	 * already uses (duplicated rather than shared for the same reason documented there: that one operates
	 * purely server-side, this one is client-render-only), just written in raw {@code double}s instead of
	 * {@link Vec3} - see this class's own doc comment ("Real allocation-elimination pass"). Same "write,
	 * then the caller immediately unpacks to locals" contract as {@link #ribbonPoint}.
	 */
	private static void computeBasis(double dirX, double dirY, double dirZ, double[] out)
	{
		double refX, refY, refZ;
		if(Math.abs(dirY) > 0.9)
		{
			refX = 1.0; refY = 0.0; refZ = 0.0;
		}
		else
		{
			refX = 0.0; refY = 1.0; refZ = 0.0;
		}

		double p1x = dirY * refZ - dirZ * refY;
		double p1y = dirZ * refX - dirX * refZ;
		double p1z = dirX * refY - dirY * refX;
		double p1len = Math.sqrt(p1x * p1x + p1y * p1y + p1z * p1z);
		p1x /= p1len; p1y /= p1len; p1z /= p1len;

		double p2x = dirY * p1z - dirZ * p1y;
		double p2y = dirZ * p1x - dirX * p1z;
		double p2z = dirX * p1y - dirY * p1x;
		double p2len = Math.sqrt(p2x * p2x + p2y * p2y + p2z * p2z);
		p2x /= p2len; p2y /= p2len; p2z /= p2len;

		out[0] = p1x; out[1] = p1y; out[2] = p1z;
		out[3] = p2x; out[4] = p2y; out[5] = p2z;
	}
}
