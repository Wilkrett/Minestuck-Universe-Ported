# Minestuck Universe Ported - Project Handoff

Port of the **MinestuckUniverse** addon (Forge 1.12.2) to **NeoForge 1.21.1**, built as an addon
on top of the base **Minestuck** mod (also ported to 1.21.1 independently by its own team - that
port is a *dependency*, not something this project maintains).

Package root: `org.wilkretawesomesauce.minestuckuniverseported`
Mod ID: `minestuckuniverseported`

This file is a handoff summary from a long prior conversation (Claude, web chat) that built most
of what's here. It exists so a fresh session doesn't have to rediscover the reasoning, the
gotchas, or re-introduce bugs that were already found and fixed once. Read the "Recurring bug
patterns" section before making changes to anything involving entity movement, attachments, or
multi-file registrations - those are the categories that bit us more than once.

## How this project was built

Every subsystem here was ported by reading the **actual original 1.12.2 source** and the **actual
modern NeoForge/Minestuck source** (via decompiled jars and Minestuck's own already-ported
codebase) to confirm real APIs before writing code - not guessed from general knowledge. Where
something was simplified or skipped rather than fully ported, that's called out explicitly on the
relevant class, usually under a "Scope note" or "Known limitation" heading. **Those comments are
load-bearing** - if you're about to "improve" something that looks incomplete, read the doc
comment first; it likely explains why.

## Completed subsystems

The entries below are deliberately terse - what exists, and any *still-open* known gap or trap.
Implementation history, bug-fix narratives, and reasoning have been trimmed; if you need the full
story behind a specific decision, check git history or ask before assuming something was an
oversight.

### Strife Specibus (`strife` package)
Weapon-loadout system: `StrifePortfolio` (10-slot inventory of `KindAbstratus`-tagged
`StrifeSpecibus` decks), `StrifeCardItem`, full GUI (`MSUStrifePortfolioScreen`) + HUD
quickswitcher, data-driven kinds via `strife_kinds/*.json`. **Known gap**: no custom 3D icon
rendering; strife card textures still use the older `overrides`-predicate system.

### Abilitech framework (`skills.abilitech` package)
Equip-3-techs-in-slots ability system: `Abilitech`/`Skill` base classes, `AbilitechLoadout` (3
slots, press/hold/release key-state machine), `MSUAbilitechRegistry`, GUI via the real
16-position `AbilitechnosynthBlock` multiblock. Unlock gating is now real (see the boondollar
economy entry below), superseding this system's original sandbox-everything-unlocked state.
`TechTimeParallelAction` spawns a real fighting clone (`MSUFakePlayer`) - see "Recurring bug
patterns" below for the class of bugs (gravity, attack-strength ticker, invulnerability, damage
gating) any future acting fake-player needs to be checked against.

### Abilitechnosynth real multiblock
`AbilitechnosynthBlock` is a real, ported 16-position multiblock (one class, `FACING`+`PART`
properties, real per-sub-box collision `VoxelShape`s; `AbilitechnosynthItem` places all 16 at
once). GUI only opens once `isValid` confirms the structure is intact. **Known gap**: the
original's separate, never-modeled single-block "mini" variant was never built - it had no real
1.12.2 assets to port either.

### Blood Aspect (`skills.abilitech.heroAspect.blood`)
4 techs: Transfusion, Bleeding (own `BleedingEffect`), Bubble, Reformer. All originally-simplified
pieces (the bubble, the Reformer AI) were later de-simplified to real mechanics - see "Real
EntityBubble" below.

### Breath Aspect (`skills.abilitech.heroAspect.breath`)
8 techs (Gale, Knockback, Fall Proof, Speed, Bubble, Wind Vessel, Liberate, Constrain). Wind Vessel does
real render-cancellation + movement-input dampening. **Known gap**: sub-block collision-phasing
(slipping through gaps) was never built - no modern NeoForge hook exists for overriding a real
connected player's own collision resolution without Mixin. Liberate/Constrain are new techs added for
the Freedom system below - see that section for what they actually manipulate.

### Breath Wind visuals - two layers, `WindEngine` (particles) + `WindRibbonRenderer`/`WindBurstRenderer` (mesh)
Original design for this project, no 1.12.2 counterpart - built across three standalone user-supplied
docs in sequence, each correcting the last. Real callers throughout: `TechBreathLiberate`/
`TechBreathConstrain`/`TechPageBreathFreeWill` only - the other 5 Breath techs (Gale/Knockback/Fall Proof/
Speed/Bubble/Wind Vessel) are untouched, since none of the docs specced them.

**Real detour, tried and reverted - worth knowing if this ever needs revisiting**: a first pass wired up
[Low-Drag-MC's Photon](https://github.com/Low-Drag-MC/Photon) (a real, actively-maintained Unity-style VFX
mod, confirmed via its own maven and decompiled classes to genuinely support NeoForge 1.21.1) as an
optional soft dependency - `FXHelper.getFX`/`EntityEffectExecutor` confirmed real and callable, a
`mandatory=false, side=CLIENT` `neoforge.mods.toml` entry, a network packet bridging server-authoritative
tech code to Photon's own client-only trigger API. **Reverted entirely**, for a real, confirmed reason:
Photon's actual effect definitions (particle graphs, timelines, materials) have no data-driven/JSON
authoring path anywhere in its own source tree - every one is built through its in-game `/photon_editor`
GUI, which nothing in this project's own toolchain can operate. Wiring the dependency would have shipped
real plumbing pointed at an effect that could never exist without a human opening Minecraft and using that
editor by hand. A user-supplied "Breath Visualizer Architecture Decision" doc then independently proposed
the actual real fix: don't rely on any external VFX authoring tool at all, build a genuine custom
*renderer* instead - real Java rendering code is something this project can actually write and compile-verify
itself. `gradle.properties`/`build.gradle` were fully reverted to their pre-Photon state; no trace of that
dependency remains.

**`client.render.WindRibbonRenderer`/`WindBurstRenderer` - the real primary visual**, per that doc's own
explicit rule ("do not implement Breath visuals primarily through vanilla particle spawning... the primary
Breath visual should be a custom renderer"). Directly extends `client.render.TetherBondRenderer`'s already-proven
technique rather than inventing new rendering machinery: same `RenderLevelStageEvent.Stage.AFTER_PARTICLES`
hook, same per-segment camera-facing billboard quad (width perpendicular to both the segment's own axis
and the camera direction - the standard textured-line-with-no-real-geometry trick), same reused
`textures/entity/projectiles/clear_beam.png` tintable strip (no new art needed). What's genuinely new:
`WindRibbonRenderer#ribbonPoint` replaces `TetherBondRenderer`'s single static bow with a *time-animated*
curve (two summed sine waves at different frequency/phase, tapered to zero at both ends so the ribbon
always anchors cleanly to caster and target) - the doc's own "Simple" turbulence method; real Perlin noise
(the doc's "Better" method) was deliberately left for a later pass, since a subtle noise bug in a system
that can't be tested in a live client this session was a real risk not worth taking for a first cut.
**Real follow-up, a direct user request** ("wavy blue streaks" for Liberate/Constrain specifically,
matching a reference image's own look of several independently-undulating parallel bands rather than one
line): `renderRibbon` now draws `STREAK_COUNT` parallel streaks instead of one, each offset from the
caster-target centerline by a *fixed, untapered* spacing - only each streak's own wave wobble tapers to
zero at the endpoints, the fixed spacing itself doesn't, so the streaks stay visibly spread the whole way
rather than all pinching back together at the caster/target - and each carries its own phase stagger and a
small frequency variation so they wave independently instead of moving in lockstep.
`renderVortex` is the doc's own "Spiral Currents" - a gentle wrap around the target, angle advancing
with both arc-length and real time so it visibly rotates, radius/density scaled by the same intensity value
`WindEngine`'s own particle calls already use. `WindBurstRenderer` (Free Will's activation) is the doc's
own "expanding spherical pressure wave" - deterministic-per-burst radial billboard spokes (seeded from
caster id + spawn tick, regenerated fresh every frame from that seed rather than stored, so the shell reads
as one coherent wave instead of flickering noise) expanding via an ease-out curve while fading to 0.

**Two real bugs, caught from a live screenshot** (the first actual in-game look at this system, and a
direct user report: "it flickers a lot... you can see the edges"): both `WindRibbonRenderer#renderQuad`
and `WindBurstRenderer#renderQuad` were emitting every quad *twice*, in both winding orders, copying
`TetherBondRenderer`'s own defensive-but-explicitly-unconfirmed habit of doing that "in case the render
type ever turns out to cull backfaces." Checked for real this time (`RenderType.entityTranslucentCull`
exists as a *separate* method from the plain `entityTranslucent` this project uses, confirming the plain
one doesn't cull) - so the second copy was drawing identical translucent triangles at the identical
position a second time, a textbook cause of both symptoms actually reported: z-fighting flicker
(floating-point depth precision doesn't reliably agree with itself on truly coincident geometry) and
doubled/harder-looking alpha compositing at the edges. Fixed in both classes by emitting each quad once.
`TetherBondRenderer` itself very likely has the same latent bug - not touched, since fixing it wasn't part
of what was asked and it hasn't been reported as visibly wrong, but worth revisiting together if the
tether ever gets its own bug report. Second, the original vortex used a tight multi-turn spiral (1.5 turns
in a ~1-block radius); camera-facing billboard segments recompute their own orientation every frame from
the live camera position, so wherever the curve bends sharply between segments - exactly what a tight
spiral does - adjacent segments' orientations diverge a lot frame-to-frame, reading as visible faceting/
shimmer even with the z-fight fixed. Softened to well under one full turn at a wider radius (still wraps
around the target - the one part of the original screenshot the user explicitly liked - just gently), and
the streaks/vortex were both substantially widened, slowed, and given lower-frequency waves to match the
reference image (thick, clearly-separated, lazily-undulating bands, not fine fast-zigzagging threads).
Server-to-client bridging is real networking, not the removed Photon packet: `network.WindRibbonSyncPacket`
(sent once on lock-on, then every `RIBBON_RESYNC_INTERVAL_TICKS` while held so the vortex can visibly grow
with the target's *current* Freedom without spamming a packet every tick, and once more on release to
clear it) and `network.WindBurstPacket` (fire-and-forget, mirrors `TetherBondImpactPacket`'s exact shape).
Client-side state (`client.WindRibbonClientState`) mirrors `TetherBondClientState` exactly.

**Real "lightning trail" layer, added on top of the streaks (a direct, explicit user request - not a
replacement of the wavy-band system)**: pointed at a real reference mod (`ChestItem`, NeoForge 1.21.1)
whose own trail entities render through vanilla's real `RenderType.lightning()` instead of a textured
quad - confirmed by reading that mod's actual renderer source, not guessed. `WindRibbonRenderer` now also
builds real 3D cylinder geometry (`renderLightningTube`, 6-sided) along the exact same animated curve
math the streaks already use (a distinct phase offset so it doesn't sit exactly on top of streak 0),
drawn through a second, genuinely separate `VertexConsumer`/render-type batch using vanilla's own
`RenderType.lightning()` - the same untextured, additive-ish, glowing render type vanilla itself uses for
actual lightning bolts (confirmed against `LightningBoltRenderer`'s own real source: its vertex format is
`POSITION_COLOR`, so a lightning vertex only ever gets a position and a color - no UV/overlay/light/normal
calls, a genuinely simpler vertex-building path than the textured streaks). No new shader/texture assets
needed - `RenderType.lightning()` is a public vanilla constant, not something the reference mod had to
build itself (that mod's own copy layers a custom Mixin-injected render-target system on top for a
screen-distortion effect - explicitly not replicated here, out of scope and against this project's own
no-Mixin policy - only the plain vanilla render type was adopted). Real 3D tube geometry also sidesteps
`renderQuad`'s own near-camera degenerate-quad cutoff entirely - a fixed-radius cylinder doesn't have a
camera-facing billboard's "width grows unbounded as distance to camera shrinks" problem.

**Real crash, caught from a live client report ("Crashed...") the very first time this layer actually
ran**: `IllegalStateException: Not building!` inside `VertexConsumer#addVertex`, thrown from
`WindRibbonRenderer#vertex`/`renderQuad`/`renderRibbon`. Root cause confirmed by reading
`MultiBufferSource.java`'s real `getBuffer()` source, not guessed: `entityTranslucent(TEXTURE)` and
`lightning()` are both "shared-buffer" render types (neither has its own dedicated fixed buffer), and
`getBuffer()` unconditionally ends whichever shared-buffer type was last active (`endBatch(lastSharedType)`)
the instant a *different* shared-buffer type is requested. The original `onRenderLevel` fetched both
consumers up front, then interleaved streak and lightning draw calls in one loop - so fetching
`lightningConsumer` right after `consumer` silently ended `consumer`'s batch before a single vertex had been
written to it, and the very first streak `addVertex()` call inside the loop crashed. Fixed by splitting into
two fully sequential passes over `ribbons.entrySet()` - fetch `entityTranslucent(TEXTURE)`, draw every
ribbon's streaks/vortex, `endBatch` it; only then fetch `lightning()`, draw every ribbon's lightning
tubes/vortex, `endBatch` it. General rule for any future render type added here (or anywhere else two
shared-buffer types are used in the same frame): never hold two different shared-buffer `VertexConsumer`
references live across a `getBuffer()` call boundary - one type's whole pass has to fully finish (including
its `endBatch`) before the next type is ever fetched.

**Fade-out on release, a direct later user request** ("instead of instantly making the trail disappear it
should slowly fade out"): `client.WindRibbonClientState` used to `Map.remove` a ribbon the instant
`WindRibbonSyncPacket` arrived with `targetId=-1` (release), so `WindRibbonRenderer` simply stopped seeing
it the very next frame - a hard pop. Fixed by mirroring a pattern this exact codebase already has for a
different feature: `client.StreakClientState`'s own `active`/`fadingOut` live/fading map split (built for
its ghost afterimages, not its ribbon trail - this is the first reuse of that shape elsewhere).
`WindRibbonClientState.clearRibbon` now moves the released ribbon into a second `fadingRibbons` map with a
start tick instead of discarding it; a new `getRenderRibbons()` (replacing the old `getRibbons()`, its only
caller) hands the renderer a combined snapshot each frame - live ribbons at full strength, fading ones with
a `fadeMultiplier` ramping `1F -> 0F` over `FADE_OUT_TICKS` (20 ticks/1 second, self-pruned once elapsed).
`WindRibbonRenderer` threads that multiplier through `renderRibbon`/`renderVortex`/`renderLightningRibbon`/
`renderLightningVortex`, multiplied into whatever alpha value each already computes (independent of, not a
replacement for, the existing `intensity` scalar) - so every visual element (streaks, vortex, lightning
tube, lightning vortex) now fades together smoothly instead of vanishing instantly.

**Per-ability visual split, two direct later user requests** ("remove the streak from Stifling Calm and
use the trail instead" / "remove the particles from LiberatingZephyr and use the trail instead") - the two
abilities briefly no longer shared an identical visual, each leaning on a different half of what this
renderer built at the time: Constrain's pass 1 (the quad-streak style) was skipped for `inward=true`
ribbons only, while Liberate still got both mesh styles. **Superseded by the next entry below** - the
quad-streak style is now gone for both abilities, not just Constrain.

**The quad-streak style removed entirely, a direct later user request** ("it still uses streaks.. causing
it to look quite jarring", from a live screenshot of Liberate): the wide translucent billboard quads
(`renderRibbon`'s `STREAK_COUNT` parallel bands + `renderVortex`, both built on `renderQuad`/`vertex` and
the reused `clear_beam.png` texture) read as jarring even for Liberate alone once seen live, not just
relative to Constrain's already-cleaner look. Removed outright for both abilities - `renderRibbon`,
`renderVortex`, `renderQuad`, `vertex`, and every constant only they used (`TEXTURE`, `RADIUS`, `ALPHA`,
`MIN_CAM_DISTANCE_SQR`, `STREAK_COUNT`/`STREAK_SPACING`/`STREAK_PHASE_STAGGER`/`STREAK_FREQ_VARIATION`)
were deleted from `WindRibbonRenderer` rather than left dead, since nothing else in the project called them
and this style was the repeat source of every real visual complaint in this feature's history (double-
emission z-fighting, tight-spiral flicker, and now this). Both abilities now render identically: the
lightning tube + lightning vortex (this renderer's only remaining visual) plus `WindEngine.ribbon`'s
particle trail (below) - no per-ability mesh-style difference left at all. `onRenderLevel` also dropped
back to a single render-type pass now that only `lightning()` is ever fetched (the two-pass structure from
the earlier crash fix is no longer strictly required, but was kept as the simplest already-proven-safe
shape rather than un-splitting it for no real benefit).

**Multiple parallel trail strands, a direct later user request** ("there's only 1 trail instead of
multiple... I liked the thickness & amount the streaks had"): removing the quad-streak style also removed
its "several parallel bands" look, and a single thin lightning tube alone read as too sparse. Rather than
reviving any flat billboard geometry, `renderLightningRibbon` now draws `TRAIL_STRAND_COUNT` (3) parallel
*tubes* instead of one, reusing the exact same shape the deleted quad streaks used - `ribbonPoint` got its
`strandOffset`/`phase`/`freqScale` parameters back (a fixed, untapered lateral offset per strand so they
stay visibly spread rather than pinching together at the endpoints, plus a phase stagger and small
per-strand frequency bump so they wave independently instead of in lockstep) - just applied to real round
cylinder geometry instead of flat quads, so the "amount" is back without the jarring flat-panel look coming
back with it. The vortex (`renderLightningVortex`) is untouched - still a single spiral tube, not
multi-stranded, since this request was specifically about the connecting trail.

**Flattened, elliptical tube cross-section, a direct later user request** ("i think i need the trails
somewhat more flatter or stretched out... it doesnt really feel like a breath/wind effect"): a perfectly
round tube read as a rigid rope rather than flowing wind even with multiple strands. `renderLightningTube`
now builds an elliptical cross-section - a new `LIGHTNING_TUBE_WIDTH` (0.18, wide) along `basis[0]` and
`LIGHTNING_TUBE_THICKNESS` (0.03, thin) along `basis[1]`, replacing the old single symmetric
`LIGHTNING_TUBE_RADIUS` - rather than a circle. `basis[0]` is exactly the same axis the parallel strands
above are already spread apart along, so each strand's own flat side lines up with the "sheet" the strands
form together, reading as overlapping flat ribbons rather than round rods. Deliberately still real 3D
geometry with a fixed world-space cross-section (each strand's own local tangent frame, recomputed per
segment along the curve), not a revived camera-facing billboard - the earlier "jarring" billboard problems
were about a screen-facing quad degenerating near the camera and z-fighting when double-drawn, not simply
about being flat, so flattening the already-working, already-proven tube geometry sidesteps that whole bug
class while still reading as ribbon-like.

**Real technique pivot to a soft particle-swarm "wind wisp" system, from fresh reference screenshots** ("I
want something like this... though keep the color blue" - a different modpack's Photon-based spell-charging
effect: soft, blurred, translucent smoke-ring wisps curling around the caster, nothing like a precise
geometric line). After several rounds of tuning the mesh's thickness/flatness, the user confirmed (via two
direct questions) that the mesh's line geometry was never going to read as "natural wind" regardless of
shape, and that the real fix was a technique pivot to a denser, softer particle swarm rather than continued
mesh tuning.

**Photon investigated a second time and still not reintroduced** - the user's "I think we might have to use
Proton" turned out to mean Photon (the reference modpack's own mod list, `E:\Twitch\Instances\magic evo 2
\mods`, includes `photon-forge-1.20.1-1.1.17.jar`, and the swirl screenshots are literally one of its
bundled effects, `assets/photon/fx/windcasting.fx` inside that pack's `magic_evolved_two` mod). Inspecting
that file for real (gzip-decompressed, not guessed) found it's actually a real, structured NBT-like data
format (`particle`/`trails`/`colorOverTrail` gradient/`material shader photon:circle` fields, etc.) - a real
correction to this doc's own earlier claim that Photon has "no data-driven/JSON authoring path anywhere in
its own source tree." It does have an underlying file format. That correction doesn't change the outcome,
though, for two separate real reasons: reusing that modpack's actual bundled `.fx` file in this project
would mean redistributing another modder's authored creative work without permission (a licensing concern,
not a technical one), and hand-authoring a *new* correct effect in that binary format completely blind (no
live Minecraft client available in this session to render/iterate against) remains impractically high-risk
- one wrong field in a particle/shader graph and it silently renders nothing. Photon stays out. What *was*
worth taking from this investigation: the underlying **technique** (soft round particle sprites moving along
spiral/trail paths with a color gradient) is fully achievable with tools this project already has.

**`skills.abilitech.heroAspect.breath.WindEngine`'s new wind-wisp particles - real vanilla art reuse, not
new placeholder art**: confirmed via direct inspection of the actual vanilla 1.21.1 client jar
(`neoformruntime`'s cached `minecraft_1.21.1_client.jar`), vanilla ships its own real "Gust" Wind Charge/
Breeze particle art - `textures/particle/gust_0.png` through `gust_11.png` (12 frames), each genuinely soft,
blurred, and translucent (visually confirmed by rendering several frames - `gust_0` is a soft round blur
dot, `gust_6`/`gust_10` are soft curling comma/spiral-ring shapes) - and its own `particles/gust.json` lists
them exactly the same way this project's own `particles/power.json` already lists vanilla's `spark_0`-
`spark_7` for `PowerParticle`. Same established convention (`PowerParticle`'s own doc comment), a
thematically perfect zero-new-art fit this time. New: `util.WindWispParticleOption` (color + maxAge + a new
`scale` field, modeled on `PowerParticleOption`), `client.particles.WindWispParticle` (modeled on
`PowerParticle` - same `PARTICLE_SHEET_TRANSLUCENT`/"always half-lit" glow trick - but with two real
additions `PowerParticle` deliberately doesn't have: `tick()` now actively animates both `alpha` (ease-in
over the first 15% of life, ease-out to 0 after - so it fades in and out instead of popping/vanishing) and
`quadSize` (a mild growth over its life, a "puffing outward" feel) - kept as a genuinely separate class
rather than added to `PowerParticle` itself, since that class is shared infrastructure every other aspect's
own particle calls still go through unchanged), a new `MSUParticles.WIND_WISP`/`spawnWindWisp` pair
(registered/wired the same way `POWER`/`INK` already are, including in `client.MSUClientSetup`), and a new
`particles/wind_wisp.json` listing the real `gust_0`-`gust_11` frames.
- `WindEngine.ribbon` switched from `spawnPowerParticle` to `spawnWindWisp` (small `scale`) with a small
  random perpendicular jitter per spawn point - a precise spark sitting exactly on the curve read as a crisp
  line of motes, not a soft drifting stream; the jitter turns it into a loose cloud following the trail.
- New `WindEngine.windSwirl(Level, Vec3 center, double radius, float time, int color, float intensity)` -
  reuses `spiralAroundTarget`'s own orbiting shape (angle/radius/tangential velocity) but denser, bigger,
  slower, with a gentle vertical bob layered on top, so it reads as a soft curling ring/aura around the
  target rather than a thin fast vortex of motes - the piece that actually reproduces the reference
  screenshots' "curling wing/ring" look. Called from both `TechBreathLiberate` (radius grows with
  `freedomFraction`, matching the mesh's own vortex) and `TechBreathConstrain` (radius *shrinks* as
  compression increases instead, mirroring `pressureInward`'s own inward motif rather than Liberate's
  outward one). `spiralAroundTarget`/`pressureInward`/`expandingBurst` stay on the older, sharper
  `spawnPowerParticle` unchanged - out of scope, still correct for what they visualize.
- Color needed no new logic anywhere - every new call passes through the exact same `MSUAspectColors.get(EnumAspect.BREATH)`
  values (`0x47E2FA`/`0x4379E6`) the existing calls at each site already used.
- `client.render.WindRibbonRenderer`'s mesh is de-emphasized, not deleted: `LIGHTNING_ALPHA` lowered from
  `0.55F` to `0.25F` so it reads as a faint accent thread under the new particle swarm instead of competing
  with it - a one-constant, easily-revertable change, since the mesh itself is still real, crash-tested,
  working code.

**`WindEngine.ribbon` reintroduced and reworked to trace the trail curve, from a live screenshot report**
("it only shows 1 measly wind effect... reuse windengine but wire it to be using the trails instead of the
streaks"): the particle-removal change above was a real overcorrection - the mesh's lightning tube alone
read as too sparse while held. `WindEngine.ribbon` (previously removed from every call site, `dead code
with no caller`) is back, called every active tick from **both** `TechBreathLiberate` and
`TechBreathConstrain` (the latter alongside its existing `pressureInward`, not replacing it) - a direct
user confirmation that both abilities should get it, not just the one in the screenshot. Its own internal
math changed too, not just its call sites: it used to trace an independent single cos/sin spiral-twist path
(`RIBBON_TWIST_AMPLITUDE`/`RIBBON_TWIST_TURNS_PER_BLOCK`, both removed) unrelated to anything the mesh
renders; it now samples the exact same tapered, two-summed-sine curve `WindRibbonRenderer`'s lightning tube
already animates along (`ribbonPoint`'s own math, `streakOffset=0`/`phase=LIGHTNING_PHASE`) via a new
private `curvePoint` - a deliberate server-side duplicate (mirroring `TWIST_FREQ_1/2`/`TIME_SPEED_1/2`/
`TWIST_AMPLITUDE`/`LIGHTNING_PHASE` under a local `TRAIL_PHASE` name), since `WindRibbonRenderer` is
`@EventBusSubscriber(..., value = Dist.CLIENT)` and can't be imported from this server-tick code without
pulling client rendering onto the dedicated-server classpath - the same reasoning both classes' already-
duplicated `perpendicularBasis` helpers document. Net effect: the particle stream now visually hugs the
mesh's own glowing lightning core instead of tracing an unrelated line, reading as a much denser, fuller
"wind" layered directly on the thin tube. `spiralAroundTarget`/`expandingBurst`/`nudgeNearbyItems`/
`nudgeItemsOutward` are unchanged and still only used where they always were (Free Will/general).
**Most of the doc's own "Environmental Reactions" list (leaves/grass/flowers swaying, smoke bending,
campfire flames leaning, snow drifting, clouds swirling) is NOT implemented, for confirmed technical
reasons, not oversight**: none of it has a real per-location override hook in modern NeoForge without
Mixin. "Arrows wobble" is skipped for a different, deliberate reason: an `Arrow` has no separate
visual-only transform channel, so faking a wobble would mean nudging its *real* flight path - the docs
explicitly want environmental reactions to be visual-only, so doing that would violate the instruction
rather than satisfy it. **Only "dropped items shift slightly" is real** (`nudgeNearbyItems`/
`nudgeItemsOutward`) - a real vanilla `ItemEntity` is an ordinary entity with ordinary velocity, the one
item on that list with an actual lever to pull.

**Not yet manually verified in a real client** - same reasoning as every other item in "Suggested next
steps": this is the single highest-risk unverified item in the whole project alongside Space Salt's own
multiblock relocation - real vertex-animated world-space mesh rendering is more fragile to get right blind
than anything else built this session (a wrong render-type, matrix transform, or winding order can look
broken or silently render nothing, and none of it has been seen in an actual client). Needs a real client
to confirm the ribbon/vortex/burst actually read as "flowing air" rather than a visual mess, that the curve
genuinely bends when a Liberate/Constrain target moves instead of snapping or breaking, and that
`WindBurstRenderer`'s billboard spokes don't degenerate at the near-camera cutoff.

### Freedom system (`mechanics.freedom` package)
Original design for this project, no 1.12.2 counterpart (same category as `mechanics.doom.DoomData`) -
built from a standalone user-supplied design doc ("Minestuck - Breath Aspect Mechanic"). Every
`LivingEntity` carries a hidden 0-100 `FreedomData` value (50 = neutral, real attachment
`MSUAttachments#FREEDOM_DATA`, deliberately not `copyOnDeath()` - same reasoning as `DOOM_DATA`) that
represents how much behavioral slack an entity has, not a resource to spend - explicitly **not** mind
control, the entity still wants what it always wanted. `FreedomEvents` applies it to real hooks:
continuous movement-speed/jump-strength `AttributeModifier`s scaled off distance from the 50 baseline;
reduced incoming knockback and a chance to resist Slowness outright above the "High" (70+) threshold;
leashing a High-freedom mob has a chance to fail, and an already-leashed one has a small periodic chance
to snap its own leash; at "Extremely Low" (≤20) a `Mob`'s own dodge/flee/wander goals (`AvoidEntityGoal`/
`PanicGoal`/`RandomStrollGoal`/`WaterAvoidingRandomStrollGoal`) are spliced out of its `goalSelector` on
bracket entry and restored on exit, the same real goal-splicing idiom `heroAspect.rage.RageAI` already
established. Breath's own two new techs (`TechBreathLiberate`/`TechBreathConstrain`, "Tailwind"
(renamed from "Liberating Zephyr")/"Stifling Calm") are the player-facing trigger: hold and aim at a target to raise or lower their
Freedom over time, matching the source doc's own "Breath users manipulate Freedom instead of directly
controlling entities" framing.
**Two categories deliberately left unmodeled, stated plainly rather than faked** (see `FreedomEvents`'
own doc comment for the full reasoning): the source doc's "more varied AI decisions"/"improvised
alternate routes"/"the entity appears creative" language has no generic engine hook to attach to -
vanilla's own A* pathfinder always computes the objectively shortest path to whatever a goal picked,
there's no "path diversity" knob anywhere in it; a periodic target-clear + forced `recomputePath()` at
High freedom is the closest real approximation, not a literal implementation. And "resistance to webs"
specifically (one item in the doc's "resistance to Slowness, webs, knockback" list) has no real hook
either - `Entity#stuckSpeedMultiplier` is a protected field re-set every tick from inside cobweb's own
`entityInside`, with no public mutator reachable before movement consumes it the same tick, same
no-Mixin-policy gap as `TechBreathWindVessel`'s own documented collision-phasing limitation. The goal
splicing at Extremely Low is also a heuristic, not exhaustive - it matches by exact vanilla goal class
only, so a modded mob's own equivalent goal won't be recognized.
**Not yet manually verified in a real client** - same reasoning as every other item in "Suggested next
steps" below (needs a real client and a live mob to watch react): confirm the movement-speed/jump-height
shift is actually visible/felt at both extremes; confirm a High-freedom mob's goals actually get spliced
back correctly after a chunk unload/reload mid-suppression (untested edge case - `lastAppliedLevel`
resets to unknown on reload, so a mob that unloads while suppressed and reloads already Extremely Low
will silently re-suppress a second time against an already-goal-less selector, which should be harmless
but was never actually watched happen); confirm Liberate/Constrain's tether survives a target walking
out of range and reacquires correctly like `heart.TechHeartBond`'s own tether does.

### Freedom/Doom/Relationship cross-system interactions
Built from a second, separate user-supplied doc ("Minestuck Systems Overview") describing how Freedom
should relate to the already-existing Doom and Relationship systems - same "original design, no 1.12.2
counterpart" category as all three. Deliberately additive, one-way-dependency listener classes (Doom/
Relationship stay generic, unaware of Freedom), the same shape `mechanics.doom.RelationshipDoomEvents`
already established for Doom-reacts-to-Relationship:
- **`mechanics.doom.FreedomDoomEvents`** - the one quadrant of the doc's own Freedom/Doom four-quadrant
  matrix that needed real code ("Low Freedom + High Doom: trapped by circumstances, events feel
  inevitable"): a second, independent damage multiplier on top of `DoomDamageEvents`' own, scaled by how
  far below neutral (50) an entity's Freedom sits, gated on `doom > 0`. The other three quadrants are
  intentionally *not* separately coded - they already emerge for free from `FreedomEvents`/
  `DoomDamageEvents` running side by side (e.g. "can escape... but every choice carries risk" is just
  Freedom's own knockback/leash resistance plus Doom's own existing damage amplification, no glue needed).
- **`mechanics.freedom.FreedomRelationshipEvents`** - the doc's own flagship example, implemented for
  real: "A Page of Breath does not force a mob to follow them. They increase its Freedom until it chooses
  to follow." A `Mob` sustained at "High" Freedom by a specific player (`TechBreathLiberate`, which now
  also records `FreedomData#setLastLiberatedBy`) gets a real trust/affinity boost toward that player;
  only if that's enough to derive a real `FRIENDSHIP`/`LOYALTY` relationship (never forced, blocked
  outright by a standing `HOSTILE` one) does the mob actually start following
  (`FreedomData#setFollowing`, driven by a new generic `FreedomFollowGoal` - the `Mob`-agnostic,
  UUID-driven equivalent of `entity.HopeGolemEntity`'s own private `FollowOwnerGoal`, re-injected on
  world load the same way `heroAspect.rage.RageMobEvents` already re-injects its own goals). Dropping back
  to Low/Extremely Low Freedom breaks an existing follow bond. Also drives a slow ambient relationship
  stability drift (stronger at High Freedom, more fragile at Low) - the doc's own "based on choice, not
  control" framing.
- **`TechBreathLiberate`'s own potency now scales with the caster-target relationship** (the doc's
  "Potential Relationship Effects" section): a positive relationship boosts the Freedom gain rate by up
  to 50% at full trust; a `HOSTILE` one (the closest existing analog to the doc's own unmodeled "Fear"
  value) heavily dampens it; low trust otherwise mildly weakens it. **"Respect"/"Fear" were deliberately
  not added as new `Relationship` fields** - mapped onto what the existing `Relationship`
  class already tracks (`LOYALTY`'s own trust/strength thresholds for "Respect", a standing `HOSTILE`
  relationship for "Fear") rather than growing that class for one flavor doc, matching the restraint
  `RelationshipManager`/`RelationshipDoomEvents` already show toward their own source docs.
**Not yet manually verified in a real client**, same reasoning as the Freedom system above - needs a real
mob, a real second player to be liberated toward, and time to watch the following conversion (or its
breakdown) actually happen.

**Real correction pass, from a third, later user-supplied doc** ("Minestuck Relationship System
Interaction: Breath Aspect") that gave this whole area a stricter Core Design Rule: "Blood creates the
relationship. Breath determines whether the relationship is chosen." / "Breath does not create
relationships." That directly contradicted `FreedomRelationshipEvents`' own first version, which used
`RelationshipManager#getOrCreate` in `tryFormWillingFollowership` - a Page of Breath could conjure real
followership out of a total stranger mob with zero prior connection. **Fixed for real**: now uses
`RelationshipManager#get` (never creates) - a mob only ever converts an *already-existing* relationship
(vanilla taming's own `OWNERSHIP`, an organically-formed `FORMING`/`RIVALRY`, `KINSHIP`, etc.) into a
chosen one; a genuine stranger gains nothing from sustained Liberation, matching the doc's own wolf
example correctly (the wolf already has a taming bond *before* Breath ever touches it). Same doc also
named two real, distinct relationship *events* Breath causes (as opposed to Blood's own event set),
both now real and both respecting the same never-create rule:
- **Liberation** (`TechBreathLiberate`) - fires once, the exact tick a target's Freedom crosses from
  Low/Extremely Low up into High as a direct result of the ability (a real threshold-crossing event, not
  a per-tick trickle): +Trust/+Affinity/+Stability on top of the ability's own ordinary per-tick gain.
- **Forced Freedom** (`TechBreathLiberate`) - a small periodic chance, only while the relationship is
  `HOSTILE`, of the opposite outcome instead of any gain: -Trust/+Conflict, "Freedom cannot be forced."

**New tech, same doc's own "Ability Concept: Free Will"**: `heroClass.page.breath.TechPageBreathFreeWill`
("Free Will" - `[Page] [Breath] [Utility]`), Page of Breath's real class+aspect tech (same
`EnumClass.PAGE` + `requiredAspect = EnumAspect.BREATH` shape `page.doom.TechPageDoomReservoir` already
established). Passive: nearby entities gain a slow Freedom trickle, doubled for anyone with a real
(never-created) relationship to the Page. Activation (press): an instant burst - grants nearby entities a
real chunk of Freedom, snaps any nearby mob's leash (real `Leashable#dropLeash`, the concrete reading of
"allows entities to leave forced situations"), and reduces Instability on relationships touching a nearby
entity (this project's own real "relationship manipulation" mechanic is Crimson Discord's Instability
system, so "reduces relationship manipulation effects" is read as a direct counter to that specific
existing mechanic). Also implements the doc's own **Shared Freedom** event for real: any two entities
caught in the same burst that already have a relationship *with each other* gain
Familiarity/Trust/Affinity from it. **Deliberately not modeled**: "makes loyalty based more on Trust than
dependency" has no concrete mechanical anchor in the real `Relationship` fields and was left as flavor
text rather than forced into an arbitrary implementation - same honesty convention as this project's other
stated gaps.
**Not yet manually verified in a real client** - same reasoning as everything else in this section; Free
Will additionally needs a real group of nearby entities (ideally some already related to each other) to
watch the Shared Freedom event actually fire correctly.

**New tech, user-requested directly (not from any of the three Breath/Freedom docs)**:
`heroClass.mage.breath.TechMageBreathInsight` ("Breath Insight" - `[Mage] [Breath] [Utility]`), a fourth
sibling to `mage.blood.TechMageBloodInsight`/`mage.doom.TechMageDoomInsight` (same "the Mage cannot
create/manipulate, only understand" role, same 100-boondollar cheap-informational-read price, same
press-while-aiming-to-report shape). Reads a target's `FreedomData` - raw 0-100 value, its `FreedomLevel`
bracket (color-coded green/white/gold/red for High/Neutral/Low/Extremely Low), who last raised it via
Liberate, and who it's currently willingly following, if anyone - purely read-only, no side effects.

### Real EntityBubble (`entity` package) - de-simplification pass
Shared, renderable `BubbleEntity` (real port) backs all three aspects' bubble techs
(Blood/Breath/Doom). Containment/repulsion uses manual per-tick position correction, not a
collision-box event (none exists in modern NeoForge). Same pass also built: `SoulData` (real,
backs Soul Switcher), `HopeGolemEntity` (real standalone IronGolem-based ally, backs Willed
Alliance), reuse of Minestuck's own `DecoyEntity` (backs Astral Projection), real Reformer AI,
real Wind Vessel render/input hooks, a real Soul Shock GUI takeover (`SoulShockScreen`), and a
real Hopeful Outburst input hook. **Two permanent, confirmed gaps**: `EntityBubble`'s
fireball-acceleration-reversal nicety, and Wind Vessel's sub-block collision-phasing (both need
Mixin or a nonexistent modern hook).

### Doom, Heart, Hope Aspects
6 Doom techs (Bind/Chain/Decay/Demise/DemiseAoE/VoidBubble), ported closely - the two
unconditional-death ultimates use vanilla `LivingEntity#kill()`. Heart (4) and Hope (4) were
heavily simplified originally, then fully de-simplified in the "Real EntityBubble" pass above -
nothing here remains a stand-in.

### Space, Void, Mind, Rage, Light, Life Aspects
Closes out the full original `heroAspect` tech list (31 techs across 6 aspects). Notable real
pieces: Space Salt repositions real placed SBURB machines via Minestuck's own `MachineMultiblock`
API; Matter Manipulator captures/places via a real `StructureTemplate`; Void Grasp is the real
trigger for the Item Void GUI; Mind Control does real possession of both mobs and real connected
players (full client-relay-server-relay-client input netcode); Mind Cloak does real disguise
rendering + visibility hooks; Rage Berserk/Frenzy splice real `GoalSelector` entries (no reflection
needed, unlike the original); Light Insight wires a real 5x Juju drop-chance loot condition; Life
Chloroball is a real block using vanilla random-tick fertilizing. Per-tech known gaps are
documented inline in code; see "Suggested next steps" for the manual-verification checklist.

**Real bug fix, from a live report ("void step doesn't work")**: `TechVoidStep` used to set
`player.noPhysics = true` directly, and only on the server's own `Player` instance - the whole Abilitech
tick framework (`AbilitechEvents#onPlayerTick`) is explicitly server-only. `Entity#noPhysics` is a plain,
*unsynced* field (confirmed against real vanilla source, not guessed) - it only works for spectator mode
because both client and server independently compute `noPhysics = isSpectator()` from the same
already-synced gamemode inside `Player#tick()` (which runs on both logical sides), not because that value
is itself pushed over the network. So Void Step's server-side field flip did nothing for how a real
connected player's own client resolves its own local collision - the client never learned Void Step was
active at all. Fixed the same real way `breath.TechBreathWindVessel` already had to solve this exact class
of problem: a new marker `MobEffect` (`voidAspect.VoidStepEffect`, auto-synced to every observing client
for free like any potion effect, refreshed every held tick) plus a new client-side event hook
(`voidAspect.VoidStepClientEvents`, `PlayerTickEvent.Post` on the client only - timed to run *after*
`Player#tick()`'s own reset for that tick, so it actually sticks) that sets the client's own copy of
`noPhysics` too. Checked for the same bug class elsewhere in the project (any other server-only tick code
directly mutating an unsynced `Entity`/`Player` field like `noPhysics`/`abilities.flying`) - found nothing
else matching it; every other passive tech either uses real vanilla potion effects (auto-synced, e.g.
`TechBreathSpeed`) or already has its own correct marker-effect-plus-client-hook pair.

### Mind Decision system (`mechanics.mind` package)
Original design for this project, no 1.12.2 counterpart (same category as `mechanics.freedom`/
`mechanics.doom`) - built from a standalone user-supplied design doc ("Mind Aspect System Design"). Core
rule: Mind governs *decisions*, not thoughts/personality/knowledge/relationships (those stay Heart/Light/
Blood's job) - "Mind determines what an entity chooses to do." Every `LivingEntity` carries a hidden
`DecisionData` (`MSUAttachments#DECISION_DATA`, not `copyOnDeath()`, same reasoning as `FREEDOM_DATA`):
four 0-100/50-neutral attributes (Certainty, Hesitation, Adaptability, Resolve - the doc gives no numeric
scale of its own, this mirrors Freedom's convention for consistency) plus a `DecisionType` (Attack/
Protect/Flee/Wander/Follow/Breed/Search/Harvest/Guard) + target UUID + capped history.
`mechanics.mind.DecisionManager` is the doc's own named class ("owns behavioral choices") - real
operations matching the doc's own vocabulary: `commit`/`reconsider` (direct, unresisted), `tryRedirect`
(Resolve-resisted - "harder to redirect... less vulnerable to Mind abilities" - the doc's own explicit
tie between Resolve and being manipulated), `delay` (schedules a hesitation pause), `reinforceConfidence`/
`weakenConfidence` (both just adjust Certainty). **Architectural dependency is real and enforced by
omission, exactly as the doc's own diagram** (`RelationshipManager -> DecisionManager`, never reverse):
`DecisionManager.evaluatePriority` reads `RelationshipManager` (Ownership/Friendship/Loyalty/Family/
Kinship score by Trust, Hostile/Rivalry score by Conflict) but nothing in the file has a code path to
write back to it - the doc's own Design Boundary ("Mind should never directly modify relationship values")
holds because there's simply no method that does that, not because of a runtime guard.
`mechanics.mind.DecisionEvents` is the real behavior-wiring half (mirrors `FreedomEvents`' own split
between data-holder and interpreter), `Mob`-only (no player-relevant hooks exist for this system, unlike
Freedom's own attribute modifiers): Certainty resists a `Mob`'s own natural vanilla retargeting above the
50 baseline (a real `Mob#setTarget` override), *unless* `evaluatePriority` says the new candidate target
is dramatically more relevant than the old one - a real, live implementation of the doc's own worked
example (a Hostile threat overriding a merely-committed decision even for a high-Certainty entity), and
the one place Relationship data actually reaches a live AI decision in this pass. Adaptability scales how
quickly a stale (dead/gone) decision target actually clears, from a fast 20-tick check at high Adaptability
up to a slow 200-tick one at low ("tunnel vision"). Hesitation splices real `MeleeAttackGoal` instances out
of a mob's `goalSelector` for the scheduled pause duration and restores them after - deliberately *only*
the attack goal, so a hesitating mob can still move/look/flee/wander normally, a real distinction from a
stun (the doc's own explicit "this is not a stun effect").
**Real Resolve payoff, wired into already-built content** rather than left as unconsumed infrastructure:
`TechMindControl`'s possession attempt and `TechMindConfusion`'s effect application both now roll
`DecisionManager#resistsInfluence` (a shared, above-50-baseline-only resistance formula) before doing
anything - a resisted attempt costs no food/resource and sends `status.mindResisted`.
**"Predict" has a real reader now**: `heroClass.mage.mind.TechMageMindInsight` ("Mind Insight",
`[Mage] [Mind] [Utility]`, cost 100 like its three siblings) - press while aiming at a living entity to
read out its Certainty/Hesitation/Adaptability/Resolve, current `DecisionType` + target if any, and its
most recent history entry. Same "the Mage cannot create/manipulate, only understand" role and same
read-only shape as `mage.breath.TechMageBreathInsight`/`mage.blood.TechMageBloodInsight`/
`mage.doom.TechMageDoomInsight` - `IDecisionData`'s plain getters were exactly the read-only surface this
needed, no dedicated "predict" method required on `DecisionManager` itself.
**Real bug fix, caught from a live report** ("entities targeting you don't show that they're targeting
you"): `DecisionEvents.processTargetTracking` was always syncing `currentDecisionTarget` correctly
(including to the player), but `currentDecision` (the `DecisionType` label) is only ever set by a
deliberate `DecisionManager#commit`/`tryRedirect` call - nothing an ordinary vanilla-AI-hunted mob would
ever trigger. `TechMageMindInsight`'s report gated its whole "Currently: ..." line behind
`currentDecision != null`, so a mob correctly tracking the player as its target still reported nothing at
all. Two real fixes, not a workaround: the report now shows the target line whenever a target exists,
independent of whether a decision type is set (labeling an untyped one `(untyped)`, and calling the
player out by name as "you" in red rather than their in-game name); and `DecisionEvents` gained a new
`syncTarget` helper that labels a synced natural vanilla combat target as `DecisionType.ATTACK` (accurate,
since `Mob#getTarget()` *is* vanilla's own attack target) and clears the label alongside a lost target, so
the common case now reads as a real "Currently: ATTACK targeting you" instead of the technically-correct
but useless "(untyped)". **Known limitation, not fully solved**: `currentDecisionTarget` is genuinely
overloaded (vanilla's own attack target vs. whoever a deliberately committed non-combat decision like
`PROTECT` is about) - nothing currently calls `commit` with a non-`ATTACK` decision that also sets a
target, so this has never actually collided in practice, but a future caller that does would have its
own committed target silently overwritten the next time natural target-tracking runs. Worth a second,
dedicated field if/when a real non-combat target-setting caller exists - see `DecisionEvents.syncTarget`'s
own doc comment.
`evaluatePriority` is deliberately a modest single consult point, not a full
replacement targeting AI - the doc's own worked example (Player→Ownership/Wolf→Kinship/Zombie→Hostile
driving Protect/Follow/Attack/Retreat) is illustrative of the *kind* of reasoning Relationship should
inform, not a spec for a general-purpose utility-AI system, and building one would risk real conflicts
with vanilla AI for a doc that explicitly says "build on existing decisions rather than replacing
Minecraft AI entirely."
**Not yet manually verified in a real client** - same reasoning as every other item in "Suggested next
steps": needs real mobs with real vanilla AI to watch retarget (or resist retargeting), a real Hostile
threat appearing mid-combat to confirm the relationship override actually fires, and a real hesitation
pause to confirm only the attack goal pauses while the mob keeps moving/looking around normally.

### Timeline system (`timeline` package)
From-scratch design (not ported from the original mod). `TimelineRecorder` always-on records
block changes + full entity snapshots per tick into a rolling per-`Level` history. Two destructive
rewind paths exist: `TechTimelineRewind` (gradual, clone-accompanied undo) and
`/msu timeline travel backwards` (instant undo, no clone). `TechRetrocognition` is real,
non-destructive past-vision - packet-only ghost blocks/entities sent to one observer, never
touching the real world or the caster's own body/position. **Known limits**: ghost fidelity is
position/rotation/equipment only; other real players are never ghosted. Doom Points here are an
unattached placeholder (a separate, real DP system exists under Time Request, below).

### Parallel Timeline Branches
Real, not a stand-in: each branch is a real dimension created via Infiniverse
(`InfiniverseAPI.getOrCreateLevel`), forked by force-saving + a raw on-disk folder copy
(`timeline.BranchForker`), dormant while unoccupied, deletable/auto-prunable recursively.
Player-facing trigger is `TechTimelineBranch` (hold-duration tiers: fork / step to parent / jump
to Alpha). **Known gap**: no in-game branch-picker GUI (arbitrary-branch browsing is command-only).
**Unproven, worth re-checking if branch data ever looks wrong**: whether a pre-populated forked
dimension folder actually loads correctly rather than silently regenerating - the single
highest-risk assumption in this feature, never exercised by Minestuck's own (always-fresh-gen)
Lands.

### Time Loop Abilitechs (`timeline.loop`)
`TechTimeLoop`/`TechTimeLoopNested` create a radius-scoped zone that repeatedly replays a captured
window of history on loop, with a dedicated repeating `DoomedTimelineClone` puppeting the caster.
`INDEPENDENT` zones can conflict on overlap (last-write-wins, undetected); `NESTED` zones layer
parent-before-child deterministically. **Known limits**: no player-facing radius/duration picker;
no cap on concurrent zone count.

### Time Request / Doom System (`timeline.request`)
Doom Points' real, intentional design (separate bookkeeping from Timeline's own DP placeholder
above). `TechFutureRequest` (hold-tier category picker) borrows a progression-appropriate item
tagged with a `BORROWED_REQUEST_ID` component; repayment happens at the new
`TemporalSendificatorBlock` (a real menu-based GUI, unrelated to Minestuck's own Sendificator). DP
accrues per open request and drives weighted-random `DoomEventPool` events; only clears on actual
repayment. **Known gaps**: "Armor" resolves to one representative item, not per-slot gear; sending
to other players isn't built; the category picker is a hold-duration stand-in for a real GUI.

### Streak debug/demo effect (`streak` package)
Ported from a *different* mod (iChun's Streak, decompiled), not MinestuckUniverse. Scope, after a
correction: a cheats-gated debug/demo command (`/msu streak ...`), not an always-on cosmetic. Its
ghost-afterimage half is real, reusable infrastructure - first real consumer is
`TechTimeAccelerateSelf`'s charge-and-release dash. All tuning is developer-only
(`client.util.StreakSettings`), not player-facing.

### Real potion/status effects from the original source (`doom`/`heart`/`mind`/`voidAspect`, `godtier`)
Replaced several vanilla-effect stand-ins with the original's real custom potions
(`EarthboundEffect`, `BuildInhibitEffect`, `DecayEffect`, `GodTierLockEffect`,
`MindFortitudeEffect`, `ConcealEffect`, `GodTierComebackEffect`), scoped to only the ones with a
real in-project producer or consumer. A real bug was fixed along the way: neither original
ability-suppressing potion ever restored what it flipped on removal (a permanent soft-lock risk) -
now restored via a tick-watcher (`doom.DoomAbilityEvents`).

### Real skills/boondollar unlock economy (`skills.TechBoondollarCost`, `/msu` commands)
Real class hierarchy (`Skill` → `TechBoondollarCost` → `TechHeroAspect`/`TechHeroClass`), every
tech given its real ported boondollar cost from the original's `MSUSkills.java`, real persisted
per-player `unlockedTechs` gating equip server-side. `client.gui.SkillShopScreen` +
server-authoritative purchase packets are built; reachable via `/msu shop` or a real Consort
dialogue trigger. **Still not built**: the real `Badge` item hierarchy beyond the 4 concrete
badges `heroClass` techs actually read (see the repair-pass entry below).

### `heroClass` package - Title-Class-specific techs
All 14 Title classes' real class-specific techs (23 tech classes + shared `TechHeroClass` base),
one folder per class, real per-tech boondollar costs sourced from the actual upstream GitHub repo.
Shared infra: `MSUClassColors`, `MSUAspectAmbientEffects` (real ambient Title-Aspect buff table),
`MSUNegativeAspectEffects`, `AbilitechTargetedEvent`, and two new `AbilitechLoadout` scratch
fields (`externalTech[]`, `lastSeerDodgeTick`) backing the borrow-another-tech mechanic three
techs share.

### `heroClass` repair pass
Every originally-simplified `heroClass` tech was revisited; all but two are now real, including a
genuine `badges` package (`Badge`/`BadgeLevel` + 4 concrete badges: Karma, EffectBuff, Page,
Overlord) and real `GodTierData` badge/skill-level tracking. **Two permanent, confirmed API gaps**
(verified via `javap`): `TechWitchTrap`'s planted cloud can't be force-tinted (modern
`AreaEffectCloud` has no color method), and `TechThief`'s stolen lock effect can still be
milk-cured (modern `MobEffectInstance` has no curative-item override point).

### `capabilities` folder audit + close-out pass + restructure
Full field-by-field audit of every original 1.12.2 `capabilities.*` package (excluding `godTier`,
deliberately out of scope for *new feature work* below - the restructure still relocates its one
data class for structural consistency) against this project's NeoForge Data Attachment equivalents
(`util.MSUAttachments`). Result: `consortCosmetics`, `beam`, `strife`, `keyStates`, `mediumData`,
and `game` were already fully and faithfully ported; `badgeEffects`' ~20 fields were already
faithfully redistributed across `AbilitechLoadout` and dedicated marker `MobEffect`s (documented
gaps only, no silent drops) **except three concrete techs that were never ported at all**, since
they're generic (`Abilitech -> TechBoondollarCost` directly, no `TechHeroAspect`/`TechHeroClass` in
between) and so fell outside every earlier aspect/class-by-class pass:
- `skills.abilitech.TechDragonAura` ("Draconic Aura") - heal-over-time/food-drain while held,
  retaliation nova + `GodTierLockEffect` on `LivingDamageEvent.Post` while active (a plain
  `AbilitechLoadout#isDragonAuraActive()` scratch flag, not a registered `MobEffect` - a later backend
  pass found it was the one truly single-consumer "marker effect" tech in the whole project, with zero
  other code ever querying it, unlike every sibling marker-effect tech which turned out to have a real
  second consumer; real `MSUItems#DRAGON_GEL` unlock-gate item).
- `skills.abilitech.TechReturn` ("Return Jump") - hold to teleport back to your own Land. Real
  substitute for the original's dead Skaianet API: `SburbPlayerData#getLandDimensionIfEntered()`.
  **Real bugfix, found via a live player report**: destination position originally used the target
  Land's own `getSharedSpawnPos()` (no modern `getRandomizedSpawnPoint()` equivalent exists) - this
  was landing everyone at the Land's raw dimension origin (0,0,0-ish), not anywhere near their real
  base, because Minestuck's actual Land-entry code (`com.mraof.minestuck.entry.EntryProcess`,
  confirmed via `javap`) copies the player's Overworld structure into the Land at a computed offset
  and teleports them there directly - it never sets the level's own shared spawn at all. Real fix:
  `AbilitechLoadout` gained a new persisted `landEntryPos`/`landEntryDim` pair (see that class's own
  doc comment), recorded by a new `TechReturn#onEntry` listener on Minestuck's real
  `com.mraof.minestuck.event.OnEntryEvent` the instant entry finishes (that event only carries the
  player, not a position, and nothing in Minestuck exposes "where did entry actually put me" any
  other way) - `getSharedSpawnPos()` is now only a last-resort fallback for a player who entered
  before this fix ever ran once.
- `skills.abilitech.TechSling` ("Sylladex Sling") - hold to FOV-zoom (`SlingChargeEffect` +
  `client.SlingZoomEvents`, a real `ViewportEvent.ComputeFov` hook), release to throw your
  Captchalogue Modus's top item via `entity.MSUThrowableEntity` (new, generic `ThrowableItemProjectile`
  infrastructure - real "plain hit" damage path only, no `IPropertyThrowable` weapon-property hooks,
  since none of this project's still-partial Strife weapon-property system was needed here - see
  that entity's own doc comment for the full scope note).

Also closed a real, separately-flagged gap while here: **`badges.BadgeBuilder`** (the 5th badge,
previously just "still real, ready future work" in `badges.Badge`'s own doc comment) is now real -
real cost (`battlepickOfZillydew` + 20000 Build grist, using the same `GristCache` API
`BadgeKarma` established), and a real drag-select cuboid block-fill tool
(`client.BadgeBuilderClientEvents` + `network.BadgeBuilderFillPacket`) usable outside Minestuck's
own Edit Mode. Needed the project's first-ever client-side badge-state signal
(`network.BuilderBadgeSyncPacket`/`client.BuilderBadgeClientState`), since `MSUAttachments#GOD_TIER`
itself still isn't synced to the client and every other badge only ever needed server-side reads.
**Known gaps, both documented on the relevant class**: the original's own Edit-Mode-specific
per-deploy-list-entry grist gate isn't reproduced (reflection into a private Minestuck field, not
worth it for a peripheral bonus check); `battlepickOfZillydew`'s real Blockbench model references
10 texture files that don't exist anywhere in this project's resources (genuine missing art, same
category as `temporal_sendificator`'s) - it's a plain `PickaxeItem` with a vanilla diamond-pickaxe
texture standing in for now, real stats via a new `MSUToolTiers` (approximate 1.12.2 parity, not
exact - see that class's own doc comment for why exactness doesn't matter for what's now purely an
unlock-gate item). Every badge in this project (not just the new one) is still only reachable via
`/msu godtier badge <id>` - `SkillShopScreen` has never listed badges, a pre-existing gap this pass
didn't expand scope to fix.

**Structural follow-up, same pass**: a real `capabilities` package now exists
(`capabilities.{badgeEffects,beam,consortCosmetics,game,godTier,keyStates,mediumData,strife}`),
mirroring the original's own `capabilities.*` layout - a prior session had eliminated this package
entirely (scattering its contents by feature area instead, e.g. `BeamData` living directly under
`beam`, `GodTierData` under `godtier`), which was never restored until now. Moved back:
`Beam`/`BeamData` (from `beam`), `StrifePortfolio` (from `strife` - the original's own separate,
non-capability `strife` package stays exactly where it was, only the one class that was genuinely
part of `capabilities.strife` moved), `GodTierData` (from `godtier`, moved for structural
consistency even though new *feature* work there is still out of scope) and `MediumData` (also from
`godtier`, into its own `mediumData` subpackage - two different original capabilities that
happened to share a modern folder), `ItemVoidData` (from
`gui.itemvoid`, into `game` - the modern name for the original's `GameData`), and `AbilitechKey`/
`AbilitechKeyState` (from `skills.abilitech`, into `keyStates` - the original's nested
`SkillKeyStates.Key`/`SkillKeyStates.KeyState` enums). The actual key-state *machine logic* stays on
`AbilitechLoadout` (a deliberate merge with `GodTierData`'s own tech-equip-slot half - see that
class's own doc comment), not un-merged back into a standalone `SkillKeyStates` class - only the two
enums round-tripped back to a real file location. `badgeEffects` had no surviving 1:1 class at this
point in the session (a real `BadgeEffects` class was added in a later pass - see that entry further
down). Every other file that referenced a moved class via same-package access before (no import
needed) got a real new `import` line added - this was a straight mechanical move, not a re-design;
nothing about any of these classes' actual behavior
changed.

**Follow-up in the same session**: `AbilitechLoadout` turned out to be a real three-way merge, not
just the keyStates one described above - it also absorbed `GodTierData`'s own tech-equip-slot half
(equipped techs, per-slot passive toggle, the real unlock-tracking set) and several individual
`IBadgeEffects` scratch fields. The tech-equip-slot half moved back to `capabilities.godTier
.GodTierData` (its real original home) - `equipped[]`/`passiveEnabled[]`/`unlockedTechs` and their
accessors (`getTech`/`getTechSlots`/`isTechEquipped`/`isPassiveEnabledFor`/`equipTech`/`unequipTech`/
`isPassiveEnabled`/`setPassiveEnabled`/`isUnlocked`/`markUnlocked`/`revokeUnlocked`/
`clearUnlockedTechs`) now live on `GodTierData`, which gained its own `TECH_SLOTS = 3` constant
(deliberately not shared with `AbilitechLoadout.SLOTS` - two independent attachments that just happen
to agree on the same small number, not worth a cross-package dependency to deduplicate). Touched
~20 files across GUI/command/network/tech code - every call site either fully switched to
`MSUAttachments.GOD_TIER` or (where a single method genuinely needed both attachments, e.g.
`TechSeerDodge`'s passive-check-plus-cooldown-tracking) fetches both. `network.AbilitechLoadoutSyncPacket`
now carries two `CompoundTag`s instead of one (kept as a single combined packet rather than building
`GodTierData` its own separate sync path - see that packet's own doc comment) since `GodTierData`
itself still isn't NeoForge-auto-synced and several client screens (`MSUAbilitechScreen`,
`SkillShopScreen`) read the moved fields. The badgeEffects-derived scratch fields (`externalTech`,
`lastSeerDodgeTick`, `cloakType`, `warpPointPos/Dim`, `manipulatedPos1/2`, `savingGraceTargets`) and
the key-input state machine itself stayed on `AbilitechLoadout` - only the tech-equip-slot half moved
this round.

**Second follow-up, same session**: `ConsortHatCooldown` (the pickup-delay-only class from the first
restructure above) is gone, replaced by a real, full `ConsortHatsData`/`IConsortHatsData` pair -
exact original names, exact original file count (2), and the worn hat itself is real capability data
again (an `ItemStack` field), not a vanilla equipment slot. The equipment-slot approach was an
earlier session's deliberate modernization (documented reasoning: "free" vanilla sync/death-drop) -
that reasoning didn't actually hold up, since Consorts/Frogs render via a custom GeckoLib model that
never consumed the vanilla equipment slot into anything visible anyway, so the "free" benefit was
never real. Reverted for real: `network.ConsortHatSyncPacket` (new, mirrors the original's own
`MSUPacket.Type.UPDATE_HATS`) broadcasts the worn hat to trackers on change and to a player the
instant they start tracking an already-hatted entity; `client.ConsortHatClientState` is the client-side
cache. **No longer a permanent gap** - see "Real Consort/Frog hat render layer" further down for the
render layer that now actually consumes this cache. The wizard hat's magic-damage-resist handler, previously
folded into the old `consort.ConsortHatEvents` (a class it had no real original connection to), moved
to a new, correctly-named `events.handlers.ArmorEventHandler` (the real original package/class) -
only the wizard hat's own check is ported; the original's sibling spiked-helmet/archmage-hat checks
aren't, since neither item is registered in this project yet.

### Real Consort/Frog hat render layer (`client.render` package)
Closes the render-side gap `ConsortHatsData`'s own doc comment called out. **Two real wrong attempts before
this one, both caught the hard way (real screenshots, not reasoning)**: a first version rendered the worn
`ItemStack` directly via GeckoLib's `BlockAndItemGeoLayer`/`ItemDisplayContext.HEAD`, which produces a flat,
camera-facing GUI-style icon for a plain `ArmorItem` (no special head-context model exists for ordinary
armor, only for vanilla skulls) - a floating helmet, not a worn one. A second version switched to GeckoLib's
own `ItemArmorGeoLayer` (its built-in vanilla-armor-on-a-bone layer, chosen because the real original 1.12.2
source - `client.layers.LayerConsortCosmetics`, read directly, not from memory - renders every worn hat's
real *armor* head model, since every entry in `ConsortHatsData#HAT_SPAWN_POOL` is an `ItemArmor`) - but that
rendered nothing at all; its internal branching (whole-model-vs-`GeoArmorRenderer` detection,
`IClientItemExtensions`-routed model resolution) is built around GeckoLib's own animated-armor-item
ecosystem and never produced visible output for a plain vanilla `ArmorItem` here, and the exact failure
point wasn't worth fully reverse-engineering.
- `ConsortHatGeoLayer`/`ConsortHatRenderEvents`: real fix, third attempt - extends the bare `GeoRenderLayer`
base directly (no GeckoLib armor-layer indirection at all) and manually renders vanilla's own generic
`ModelLayers#PLAYER_OUTER_ARMOR` head shape, baked once, at the bone's position via GeckoLib's own real
`RenderUtil#translateAndRotateMatrixForBone` utility (the same one `BlockAndItemGeoLayer`/
`ItemArmorGeoLayer` use internally, confirmed via `javap`), textured with the item's real `ArmorMaterial`
outer-layer texture - full manual control, same technique `FrogHatLayer` (below) already used successfully.
Attached via GeckoLib's real `GeoRenderEvent.Entity.CompileRenderLayers` extension point - confirmed via
`javap` this fires on `NeoForge.EVENT_BUS` (the GAME bus) exactly once per `GeoEntityRenderer` construction,
i.e. once per Consort species at client startup, not per-frame. Targets the `"head"` bone, confirmed present
under that exact name in all four real Consort species geo models
(`assets/minestuck/geo/entity/consort/*.geo.json` - iguana/nakagator/salamander/turtle all read directly,
not assumed from one).
- `FrogHatLayer`: Frogs render through a plain vanilla `MobRenderer`/`FrogModel`
(`com.mraof.minestuck.client.renderer.entity.frog.FrogRenderer` - confirmed via `javap` it does NOT extend
GeckoLib's `GeoEntityRenderer` like `ConsortRenderer` does), so no GeckoLib extension point applies at all;
real vanilla `RenderLayer<FrogEntity, FrogModel<FrogEntity>>` added via `EntityRenderersEvent.AddLayers` in
`client.MSUClientSetup` instead. Vanilla's own `HumanoidArmorLayer` can't be reused directly either
(hard-bound to a `RenderLayerParent<T, ? extends HumanoidModel<T>>` parent, and `FrogModel` is a
`HierarchicalModel`, not a `HumanoidModel`) - same real, deliberately-scoped substitute as
`ConsortHatGeoLayer` above: bakes vanilla's own generic `ModelLayers#PLAYER_OUTER_ARMOR` head shape once
(via the `EntityModelSet` `AddLayers` already provides) and renders it fitted at the Frog's real `"head"`
bone (confirmed via the class's own bytecode constant pool, not guessed) using vanilla's own
`ModelPart#translateAndRotate` instead of GeckoLib's bone utility, textured with the item's real
`ArmorMaterial` outer-layer texture - same real vanilla armor asset, just without trim/dye/glint layering
(vanilla's own trim/dye pipeline lives entirely inside `HumanoidArmorLayer`'s private methods, not reusable
standalone, and no entry in `HAT_SPAWN_POOL` uses either anyway).
**Known gap, honestly stated**: Frogs never wore hats in the original 1.12.2 mod at all (Consort-only
there), so `FrogHatLayer`'s approach has no original numbers to match - a reasonable, self-consistent
"fit vanilla's own generic armor head shape onto a small mob" call, not a port.

**Fourth real bug, same category**: the hat rendered the correct shape/texture but upside-down on Consorts
(Frog wasn't reported broken). Root cause confirmed via `javap` against GeckoLib's own (otherwise unused
here) `ItemArmorGeoLayer`: it applies a `poseStack.scale(-1, -1, 1)` correction before touching a vanilla
`HumanoidModel` part, because GeckoLib bone space is mirrored on X/Y relative to vanilla `ModelPart` space -
`ConsortHatGeoLayer#renderForBone` now applies the same correction. `FrogHatLayer` needed no equivalent fix
since it positions itself via vanilla's own `ModelPart#translateAndRotate`, not a GeckoLib bone, so there's
no coordinate-space mismatch there to begin with.

**Real, deliberate, project-original quirk (no original 1.12.2 counterpart)**: a 0.1% chance, rolled once
per real hat equip (`ConsortHatsData#equip`, not a per-frame roll), for a given wearer's hat to render
upside-down on purpose - `IConsortHatsData#isHatUpsideDown`/`setHatUpsideDown`, persisted, synced via the
now-3-field `network.ConsortHatSyncPacket`, cached client-side in `client.ConsortHatClientState`. On
Consorts this is implemented by *skipping* the coordinate-space correction above (reproduces exactly the
look the third bug accidentally had); on Frogs (no correction to skip) it's implemented by *adding* the
same `scale(-1, -1, 1)` flip instead. Given three real rendering bugs already caught only via screenshots in
this one feature, treat all of this - orientation fix included - as unconfirmed until actually seen in a
real client, for both Consorts and Frogs, ideally with a `/msu` debug way to force-roll the rare case rather
than waiting on real 1-in-1000 odds (no such command exists yet).

**User-applied follow-up, directly in `ConsortHatGeoLayer` (not this session's own edit - noted for the
record, not re-derived)**: two real changes on top of the above. First, a small cosmetic tilt -
`poseStack.mulPose(Axis.XP.rotationDegrees(15.0F))`, inserted right after
`RenderUtil#translateAndRotateMatrixForBone` and before the mirroring correction, so the tilt itself isn't
affected by which branch (normal vs. upside-down) runs after it. Second, a real retarget of which bone each
variant renders on: the normal case now renders on the `"face"` bone (previously `"head"`) with the same
`scale(-1, -1, 1)` correction as before; the upside-down variant now renders on a `"waist"` bone instead,
using a `poseStack.translate(0, 0.25F, 0)` offset in place of the mirroring scale (no longer skipping a
correction - it's a genuinely different bone/positioning path now, not just an unflipped render of the same
one). **Known gap worth flagging**: of the four real Consort species geo models read earlier in this same
section (`assets/minestuck/geo/entity/consort/*.geo.json`), only `turtle.geo.json` actually has a `"waist"`
bone - iguana/nakagator/salamander don't. Since `renderForBone` only proceeds when the bone name matches the
selected target, the upside-down variant as currently wired will only ever actually render on Turtle
Consorts; on the other three species a wearer that rolls the rare case will simply show no hat at all
(silent, not a crash) until either a species-appropriate bone is chosen or this is intentional and accepted
as-is.

**Third follow-up, same session**: the last real merge left inside `AbilitechLoadout` is gone too - a
real `capabilities.badgeEffects.BadgeEffects`/`IBadgeEffects` pair now exists, holding the handful of
`AbilitechLoadout`-hosted fields that were genuine ports of the original's own (much larger, ~40-method)
`IBadgeEffects`: per-slot tethers (`getTether`/`setTether`/`clearTether`, matching the original's real
`tether(int slot)` field - not the same thing as `AbilitechLoadout`'s own still-remaining `slotHistory`,
which is genuinely new, no original counterpart), external-tech borrowing, the seer-dodge cooldown, the
mind-cloak type, the space warp point, the matter-manipulator corner selection, and saving-grace
targets. Deliberately scoped, not a full revert: the already-redistributed marker-`MobEffect`-based
fields (conceal, time-stop, rage, mindflayer, soul-shock, soul-link, FOV, tick-up stacks, movement
puppeting, power-particle tracking) stay exactly as they were, real synced effects, not pulled back
into this class - see `IBadgeEffects`'s own doc comment for the full accounting. Touched ~28 files;
`AbilitechLoadout` itself is now left with only the key-input state machine and `slotHistory` - its
`serializeNBT`/`deserializeNBT` are now permanently empty (nothing left to persist), so
`network.AbilitechLoadoutSyncPacket` (despite its name) now carries only `GodTierData`'s NBT, not
`AbilitechLoadout`'s - see that packet's own doc comment for why the name wasn't churned along with it.

**Fourth follow-up, same session - closes out `AbilitechLoadout`'s merges entirely**: the key-input
state machine itself (not just its two enums) moved to a real `capabilities.keyStates.SkillKeyStates`,
a new `MSUAttachments#SKILL_KEY_STATES` attachment. Unlike the previous three moves, this one also
restored real behavior the merged version had silently dropped: the original's own `SkillKeyStates`
had real `writeToNBT`/`readFromNBT` persistence, but `AbilitechLoadout`'s merged version unconditionally
called `resetKeyStates()` on every `deserializeNBT` regardless of what NBT it was handed - real
persistence is back (matching every other attachment in `util.MSUAttachments`, all real
`.serializable()`, no exceptions), though the original's own `onWorldJoin` handler *also* unconditionally
resets on every level join regardless of what was just loaded, so the observable behavior is unchanged
either way - restored for consistency with the rest of this project's attachments, not because it
changes what a player experiences. Touched 3 files (`AbilitechKeyPacket`, `AbilitechEvents`,
`TechMageStudy`). `AbilitechLoadout` is now down to exactly one field, `slotHistory` - the one piece of
this whole three-original-capability merge that was genuinely new all along, with no original
counterpart to move back to.

**Final sweep, same session - closes out the `capabilities` restructure entirely**: a file-by-file diff
against the real extracted 1.12.2 source (not memory) turned up two more real gaps the earlier passes
missed. **Two class-name mismatches**, same category as the earlier `ConsortHatCooldown` one:
`StrifePortfolio` renamed to `StrifeData` (matching the original's real `capabilities.strife.StrifeData`;
its own `strife` package - `KindAbstratus`/`StrifeSpecibus`/etc. - is a genuinely separate original
package and wasn't touched), and `ItemVoidData` renamed to `GameData` (matching
`capabilities.game.GameData`). Both touched ~15-20 files each, all mechanical `\bTypeName\b`-bounded
renames (protects sibling classes that merely contain the old name as a substring, e.g.
`StrifePortfolioEvents`/`MSUStrifePortfolioScreen`, which are real, separate, correctly-named classes of
their own, not the thing being renamed). **Five missing interfaces added**: `IStrifeData`, `IGameData`,
`IBeamData`, `IMediumData`, `ISkillKeyStates` - each declaring only the real, currently-implemented
method set (adapted signatures where a modern API genuinely needs an explicit `Level`/`ServerLevel`
parameter the original didn't - see each interface's own doc comment), not the original's full method
list where parts of it were already known-dead in the original itself. `godTier/IGodTierData` is still
missing - `godTier` stays explicitly out of scope. **One genuinely missing method restored**:
`StrifeData#canDropCards(ServerPlayer)`, the original's real mob-kill strife-card-drop cap - its config
option (`Config.strifeCardMobDrops`) already existed with a code comment noting it "wasn't wired up
yet"; the method is real now but still has no caller (nothing in this project currently drops a
`StrifeCardItem` from a mob kill at all). **One confirmed dead end, not ported**:
`badgeEffects/IBadgeEffect.java` (singular - a generic tagged-union value-boxing type backing
`IBadgeEffects#receive(String, IBadgeEffect)`) - grepped the original's own real source and confirmed no
tech anywhere ever calls `receive()`; unused infrastructure in the original itself, not a hole in this
port.

### Dedicated-server crash fix
`AbilitechnosynthBlock`/`StrifeCardItem` used to inline a client-only `Screen` reference in common
code, crashing dedicated-server class-loading regardless of the `isClientSide()` runtime guard
(bytecode verification doesn't care about the guard). Fixed by moving the
`Minecraft.getInstance().setScreen(...)` call into a static `open(...)` method on the screen class
itself. **Not yet verified** against a real `gradlew runServer`.

### New "Minestuck Universe" creative tab
Replaced the old piecemeal `addCreative`-into-vanilla-Combat-tab approach with a real dedicated
`CreativeModeTab`, placed after Minestuck's own Weapons tab.

### Folder structure cleanup pass (ongoing)
`MSUAttachments`/`MSUParticles` moved from the project root into `util`, matching Minestuck's own
convention. **Standing exceptions - do not "fix" these**: `gui.itemvoid` stays nested under `gui`
(a move out of it was already tried and reverted); `items`/`blocks` stay plural; the duplicate
`block`/`blocks` packages stay unmerged - all explicit, repeated user corrections, not oversights.

### Real particle system (`util.MSUParticles` + `client.particles.PowerParticle`/`InkParticle`)
Real custom `ParticleType`s (not the vanilla `ENTITY_EFFECT` swirl stand-in), ported from the
original's actual particle source, broadcast server-side via `ServerLevel#sendParticles`.
`PowerParticle`'s art was originally a hand-authored placeholder (a guess at "real pixel art wasn't
available"), but that guess turned out to be unnecessary: the original's `setParticleTextureIndex(160 +
...)` referenced a region of 1.12.2's shared particle atlas that's vanilla's own real firework-spark
sprite (confirmed by matching the checkerboard-cross pattern against a real gameplay screenshot of the
original mod) - this project now references vanilla's actual `minecraft:spark_0`-`spark_7` textures
directly (`particles/power.json`) instead of placeholder art, since the original almost certainly reused
that same shared-atlas region rather than drawing new frames. `InkParticle` still has no current caller
anywhere in the project (ready infrastructure) and still uses its own placeholder texture - no equivalent
real-vanilla-asset match was found for it.

### Resource-reference bug sweep
Found and fixed two classes of broken texture/model references across every actually-registered
item/block: stale `minestuckuniverse:` (missing "ported") namespaces, and textures sitting in the
wrong singular/plural folder for the atlas to see. **One real remaining gap**, not a reference bug:
`temporal_sendificator` has zero resource files (blockstate/model/texture) in any namespace - needs
real new art, not a redirect.

### Real atlas-visibility fix (`assets/minecraft/atlases/blocks.json`)
Vanilla's default atlas only scans each namespace's singular `textures/block/`/`textures/item/`;
this project's textures live under plural `blocks`/`items`/`machines` folders by deliberate user
choice. Fixed via a full override copy of vanilla's atlas config plus 3 added `directory` sources,
not by renaming the folders. **Stated risk**: this file completely replaces vanilla's
`blocks.json` for anyone running this mod - another mod shipping its own override of the same path
will silently win-or-lose entirely against this one, whichever loads last.

### Real `skills` package reorg
`Abilitech`/`AbilitechLoadout`/every `heroAspect`/`heroClass` tech moved to `skills.abilitech`;
`Skill`/`TechBoondollarCost` moved to bare `skills`; the old `MSUAbilitechs`/`MSUBadges` split was
consolidated into one real `MSUSkills` class, matching the original's actual structure.

## Recurring bug patterns (read before touching movement/attachments/registration)

These aren't hypothetical - each was a real bug found and fixed in this session, sometimes twice
in different classes before the pattern was recognized:

1. **`ServerPlayer#teleportTo(double,double,double)` is NOT what you think.** It's overridden to
   route entirely through `this.connection.teleport(...)` with no fallback - for a normal `Mob`
   that's irrelevant, but for `MSUFakePlayer` (dummy connection, no real client) it was a silent
   no-op. Use `moveTo(x,y,z,yaw,pitch)` (5-arg) instead - confirmed NOT overridden by `ServerPlayer`,
   falls through to `Entity`'s base implementation regardless of entity type. `setPos(x,y,z)` is
   also safe (not overridden). For a *real* connected `ServerPlayer`, `teleportTo` is fine and
   correct - this only matters for fake/dummy-connection entities.
2. **Fake/dummy-connection entities don't get normal movement sync to other clients.** Setting
   position server-side is not enough - broadcast a `ClientboundTeleportEntityPacket` manually
   after every position change (see `MSUFakePlayer.broadcastMovement()`). `swing()` does NOT have
   this problem (doesn't route through the connection), no special handling needed there.
2. **Duplicate classes from context loss.** At least twice, two versions of essentially the same
   class got created under different names in the same turn without one being visible against the
   other (`GodTier`/`GodTierData`, `EnumTechType`/`MSUTechType`) - caused real duplicate-symbol
   compile errors. If something looks like it might already exist under a slightly different name,
   grep for it before creating a new file.
3. **Zip extraction doesn't delete files.** When a file gets deleted and the project re-zipped,
   extracting on top of an existing folder leaves the deleted file behind, stale, still compiling.
   This caused at least one confusing "already fixed this" bug report. Always fully replace the
   project folder on extraction, don't overwrite-merge. (Doesn't apply going forward if working
   directly via Claude Code on the real files.)
4. **Legacy-256 blit convention.** Several original 1.12.2 textures/GUIs assume `drawTexturedModalRect`-style
   256x256 UV normalization regardless of the real texture's actual size. When porting a
   `blit()` call, check whether the original used this convention (usually recognizable by
   `0, 0, 256, 256` u/v/w/h args in the decompiled source) before assuming the modern 9-arg
   "real pixel size" blit overload is a safe substitute for the 7-arg legacy one - mixing the two
   in the same screen caused visibly-broken (near-invisible) icons at least once.
5. **Trace GL/pose-stack scale math against the source line-by-line, don't re-derive by
   intuition.** The strife card GUI had a real off-by-a-factor bug (icon position scaled by the
   wrong active transform) that only got caught by literally re-reading the original's
   `setScale`/`drawTexturedModalRect` call sequence and matching each multiplication term, not by
   reasoning about what "should" be right.
6. **Never hold two different shared-buffer `VertexConsumer`s live across a `getBuffer()` call.**
   `MultiBufferSource.BufferSource#getBuffer` ends whichever shared-buffer render type
   (`RenderType.entityTranslucent`, `RenderType.lightning()`, etc. - any type without its own fixed
   buffer) was last active the instant a *different* shared-buffer type is requested, even if not a
   single vertex was written to it yet. `WindRibbonRenderer` crashed a real client
   (`IllegalStateException: Not building!`) by fetching two such consumers up front and interleaving
   draw calls between them in one loop - fetching the second silently ended the first's batch before
   it had any vertices. Fix: do one type's whole pass (fetch → draw everything → `endBatch`) before
   ever fetching the next type - see "Real crash" note under Breath Wind visuals below for the full
   diagnosis.

## Config reference

All under `Config.java`, categories: `strife`, `godTier`, `timeline`, `timeRequest`. Notable `godTier`
options: `questBedSpawnDistance`/`questBedSpawnArea` (feed `godtier.MediumData`'s quest-bed chunk
seeding - see that class's own doc comment; nothing consumes the resulting position yet, see
Suggested next steps). Notable timeline options: `timelineHistoryTicks` (rolling recording window, default **6000 = 5 minutes** -
raised from the original 30s default specifically to give Retrocognition's own 5-minute default
window enough history to actually draw on, a deliberate ~10x baseline recording-cost increase, see
Retrocognition above), `timelineRewindPlaybackSpeed` / `timelineCloneReplaySpeed` (deliberately
decoupled - world-undo can fast-forward, the clone defaults to real-time so it reads as a believable
re-enactment), `timelineDoomPointsPerTick`, `timeLoopMaxDurationTicks` / `timeLoopWindowTicks`
(Time Loop's total lifetime vs. its separate, fixed per-pass replay length - see Time Loop Abilitechs
above for why conflating these two was a real bug once), `timeLoopRadius`,
`retrocognitionObserveTicks` / `retrocognitionOverlayRadius` (Retrocognition's own window length and
live-following overlay radius). Notable `timeRequest` options (unrelated bookkeeping from the above,
see Time Request / Doom System): `timeRequestDoomPerTickBase` / `timeRequestDoomMultiplierCap`
(per-request DP accrual and its simultaneous-requests-open cap), `timeRequestDoomCheckInterval` /
`timeRequestEventCooldownTicks` (Doom Event spend timing), `timeRequestCooldownTicks` (the
borrowing Abilitech's own cooldown).

### `MSUAbilitechScreen` description panel - real scrolling, not overflow
The panel's own doc comment used to claim it "clips at the panel edge if it overflows" - it never
actually did; nothing enforced any height limit, so a long enough description (most techs, in practice)
just kept drawing past the box and over the rest of the screen/hotbar. Real fix, not a guess: the
original's actual `GuiFraymachine` has a genuine scrolling text region (`textBoxHeight`/`scrollPos`/
`descLines` fields, a real scrollbar quad on the same `abilitechnosynth.png` atlas at UV `(28or38, 241)`,
mouse-wheel-driven) - ported for real instead of built from scratch. Name + tags + tooltip are flattened
into one combined line list; only `DESC_BOX_HEIGHT / font.lineHeight` (~12) lines are ever drawn per
frame, and a real `scrollPos` field (reset to 0 whenever the described tech changes, adjusted via a new
`mouseScrolled` override) picks which window is visible - functionally identical to the original's own
two-separately-offset-`drawSplitString`-calls-sharing-a-padded-string trick, just flattened into one list
instead, which is simpler to get right in modern code without changing what's actually seen on screen.
Also closed a real gap found while touching this method: `heroClass` tech tags (class + every
`MSUTechType`) were never shown at all before this fix (only `heroAspect` techs had tag rendering wired
up) - both now render the same way.

### Cheats-gated debug commands, not creative-mode-gated
Every debug/testing command in this project (`/msutimeline branch`, `/msustreak`, `/msu unlock`,
`/msu abilitech revoke|grant user`, `/msu godtier`) used to gate on
`source.getPlayer() != null && source.getPlayer().isCreative()` - a real user-reported bug: that ties
access to the executing player's current *game mode*, not to whether cheats/operator access is actually
available, so a survival-mode player on a cheats-enabled world (or an op on a server who isn't personally
in creative) couldn't reach any of them. All of them now use the same real vanilla permission-level check
other operator-only commands use: `.requires(source -> source.hasPermission(2))` - permission level 2 is
exactly what "Allow Cheats" grants the singleplayer world owner, and what server operators already have,
decoupled from game mode entirely. If a future debug command is added, gate it the same way, not with an
`isCreative()` check.

## Commands

All of these nest under the shared `/msu` root - see "Real skills/boondollar unlock economy"'s own
"/msu command restructure" note above for why (this list was written before that restructure and is
corrected here, not re-describing a separate later change).

Debug/testing-only commands (`itemvoid`, `juju`, `shop`, `streak`, `unlock`) were moved a second time,
under a real `/msu debug` sub-literal - a later, separate user-requested restructure, splitting them out
from the two that stayed direct `/msu` children (`abilitech`, `godtier` - neither was named in that
request).

`/msu timeline rewind <seconds>` - gradual destructive rewind + doomed clone.
`/msu timeline travel backwards <seconds>` - instant destructive rewind, no clone.
`/msu debug itemvoid` - opens the real Item Void GUI (`gui.itemvoid.ItemVoidMenu`) - see that package's own
CLAUDE.md section for why a command, not a block, is the real trigger here.
`/msu debug juju link` / `/msu debug juju unlink` / `/msu debug juju stash` - link to the nearest player
with an unlinked Juju Modus, break an existing link, or open the real GUI showing (and letting you
withdraw from) your linked partner's stash - see the `juju` package's own CLAUDE.md section.
`/msu debug streak toggle` - plain on/off toggle (unaffected by the rest of this bullet).
`/msu debug streak toggle <name>` - a real shortcut added later: picking a trail name that's already the
active, enabled one turns the effect off entirely (a genuine toggle); picking any other registered
name switches to it and turns the effect on. Combines what used to require a separate toggle call
plus `/msu debug streak flavour <name>` into one command - both of those still work unchanged on their own.
`/msu debug shop` - opens the real Skill Shop screen directly (see `command.SkillShopCommand`'s own doc
comment - also the real Consort dialogue trigger target).
`/msu debug unlock <tech>` / `/msu debug unlock all` - debug-grants boondollar-gated unlock(s) without
actually spending currency.

## Suggested next steps (not started, roughly in order of "probably wanted next")

1. ~~Design what Doom Points actually *does* once it accumulates~~ - done for the *new*
   `timeRequest` DP (see Time Request / Doom System); the original rewind/branch DP under Timeline
   system is still an undesigned placeholder, unchanged.
2. ~~Build the client-only fake-update rendering layer for Retrocognition~~ - done (`timeline.vision`
   package). Fidelity gaps remain (fire/invisible/glowing/pose not replicated on ghosts, other real
   players never ghosted) - see Retrocognition's own section above for the full list.
3. Decide on God Tier's worn-armor visuals (flat textures vs. real custom models).
4. ~~Consider the Abilitechnosynth's real multiblock geometry if the single-block stand-in ever
   feels wrong, or a placement item to accompany it~~ - done (see "Abilitechnosynth real multiblock"
   above). Not yet manually verified in a real client (place it, confirm all 16 positions render/
   collide correctly in all 4 facings, confirm the GUI only opens once the structure is intact,
   confirm breaking one segment doesn't drop anything) - needs a real client, same reasoning as the
   other not-yet-verified items below.
5. Manually verify the parallel timeline branch feature in a real client (fork-and-reload
   roundtrip, tree behavior, dormancy, pruning, DP gating - see the feature's own section above and
   its original design plan for the full verification checklist). This couldn't be driven
   end-to-end from an automated/headless session - forking requires a real creative-mode player,
   and the Abilitech needs an equipped key press, neither of which RCON/console can do.
6. ~~Fix the dedicated-server startup crash noted above (`AbilitechnosynthBlock`/`StrifeCardItem`
   referencing the client-only `Screen` class from common code)~~ - done, see "Dedicated-server crash
   fix" above. Still not verified against a *real* dedicated server actually starting successfully
   (only reasoned about via `javap`-verified JVM verification behavior) - worth a real
   `gradlew runServer` smoke test before fully trusting this is closed.
7. Manually verify the Time Request / Doom System end-to-end in a real client (borrow across all 5
   categories/rungs, confirm DP accrual scales with simultaneous open requests, confirm Doom Events
   fire and respect cooldowns, confirm the Temporal Sendificator rejects the tagged original and
   accepts a fresh copy, confirm client sync survives a relog). Not driven end-to-end from this
   session for the same reason as item 5 - needs a real client and an equipped Abilitech key press.
8. Build a real category-picker GUI for `TechFutureRequest` (replacing the 5-tier hold-duration
   stand-in) and/or an in-game branch-picker GUI for `TechTimelineBranch` - both are documented as
   the same kind of stand-in, and a shared "simple list-picker" GUI pattern could serve both.
9. Manually verify the reworked Retrocognition end-to-end in a real client (break/place blocks and
   let a mob wander nearby, activate it, confirm the caster keeps normal control while nearby past
   state renders and updates as they walk around, confirm the real mob/blocks are exactly restored
   once the vision ends or things fall out of radius, confirm a second real player is never
   hidden/duplicated). Not driven end-to-end from this session for the same reason as item 5 - needs
   a real client, and this feature specifically needs a second real observer to check the
   multiple-simultaneous-observers case noted as untested in its own section above.
10. Consider ghost fidelity (fire/invisible/glowing/pose) and whether other real players should be
    representable somehow, if Retrocognition's current position/rotation/equipment-only ghosts and
    always-skip-other-players behavior ever feels like it's missing something important in practice.
11. Manually verify the whole leftover-capabilities pass end-to-end in a real client - none of it was
    driven end-to-end from this session for the same reason as item 5 (needs real connected players,
    not RCON/console): confirm a Consort spawns with a hat sometimes and Consorts/Frogs actually pick
    up/drop dropped headwear; confirm `/msuitemvoid` behaves correctly full-27-slots-and-overflowing
    (oldest item really falls out); fire a Needlewand and confirm the beam visibly grows, damages, and
    both auto-releases on full charge and releases early on letting go; confirm two real players can
    `/msujuju link`, that one player's `putItemStack` is visible to the other via `/msujuju stash`,
    and that withdrawing through that GUI doesn't desync from the real backing list.
12. Build the real Quest Bed structure (`godtier.MediumData` is ready, nothing calls it yet) and/or a
    Locator Eye-equivalent item - this project has no world-gen structure infrastructure at all yet,
    a genuinely new skill area for this project, not a quick follow-up.
13. ~~Build a real GeckoLib render layer for worn Consort/Frog hats~~ - done, see "Real Consort/Frog hat
    render layer" above. Not yet visually verified in a real client (place/confirm a wizard hat and a
    frog hat actually appear positioned on the head for all four Consort species and for a Frog, at a
    reasonable scale, and that they track head rotation correctly) - needs a real client, same reasoning
    as this project's other manual-verification items below.
14. Consider building more `beam.IPropertyBeam` weapon variants reusing the now-real `Beam`/`BeamData`
    infrastructure (the original had several: laser pointer, lit glitter beam transistor, thorn of
    Oglogoth, archmage daggers) - `beam.BeamWeaponItem`'s doc comment has the full list of siblings
    this pass didn't build, and a real camera-facing billboard renderer to replace
    `client.render.BeamRenderer`'s current plain-line rendering.
15. ~~Consider building the Void aspect~~ - done, along with Space/Mind/Rage/Light/Life (see that
    section above) - this closes out the full original `heroAspect` tech list, no aspects remain
    unported. `TechVoidGrasp` is now the real caller of `gui.itemvoid.ItemVoidData#addItem`.
16. Manually verify the whole Space/Void/Mind/Rage/Light/Life pass end-to-end in a real client - none of
    it was driven end-to-end from that session for the same reason as item 5 (needs a real connected
    player and an equipped Abilitech key press): confirm warp points survive a cross-dimension
    teleport; confirm Matter Manipulator's corner selection, size cap, and structure capture/placement
    round-trip correctly; confirm Mindflayer's Spell's mob possession actually pathfinds/attacks, that a
    possessed real player's camera visibly forces toward the controller **and that their movement is
    actually being puppeted** (needs two real clients, controller + target, to check at all); confirm
    Illusory Cloak's disguise actually renders correctly for a second observer (also needs two real
    clients) and that a late-joining third player sees the disguise immediately via
    `CloakTrackingEvents`; confirm Frenzied Mayhem/Anger Management's AI injection survives a chunk
    unload/reload; confirm Chloroball actually grows nearby crops over time; confirm Saving Grace
    actually prevents a death exactly once and that its Absorption burst (intentionally huge-amplitude,
    few-tick, per the original's own literal argument order - see that section's own note) looks like a
    burst rather than a bug; confirm Space Salt actually relocates a real placed Alchemiter/Cruxtruder/
    Totem Lathe/Punch Designix correctly and doesn't corrupt/duplicate blocks at the destination -
    this last one is the single highest-risk item in this whole pass (drives a third-party mod's
    internal multiblock API purely from decompiled signatures, never run live) and deserves a
    deliberate test before anyone relies on it.
17. ~~Give `block.ChloroballBlock` a real model/texture~~ - done, see "Resource-reference bug sweep"
    above (a real texture had already been bulk-imported, just misreferenced under the wrong
    namespace plus a stale blockstate variant key - not missing art after all).
18. Consider mirroring walk-cycle animation state and worn equipment onto `mind.TechMindCloak`'s
    disguise ghost entity (`client.CloakRenderEvents`) - currently only position/rotation are copied,
    same stated fidelity gap as Retrocognition's own ghosts.
19. Manually verify the Streak debug/demo effect in a real client (needs creative mode) - `/msustreak
    toggle`, confirm the ribbon trail renders/fades correctly and the flavour changes via `/msustreak
    flavour <name>`, confirm sprint ghosts appear while sprinting, confirm a second client sees the
    same state (late-join sync via `streak.StreakTrackingEvents`).
20. Manually verify the newly-real Doom/Heart/Mind/Void/God-Tier effects pass in a real client - none of
    it was driven end-to-end from that session for the same reason as other manual-verification items
    above: confirm Chains of Despair really disables flight/building (and restores both correctly after,
    via `doom.DoomAbilityEvents`) rather than just slowing; confirm Withering Whisper's damage visibly
    ramps up the longer it's continuously active; confirm an ascended God Tier player visibly
    regenerates and takes reduced damage (`godtier.GodTierComebackEvents`); confirm Wind Vessel refuses
    to activate while Earthbound; confirm Mindflayer's Spell possession is refused/released against a
    Mind-Fortitude target (test via `/effect give` - no in-scope producer exists yet, see that class's
    own doc comment) and that an observer with Mind Fortitude sees through Illusory Cloak; confirm Void
    Step suppresses its (newly-added) ambient particle aura while Conceal is active.
21. Manually verify the real unlock economy + Skill Shop end-to-end in a real client - not driven
    end-to-end this session for the same reason as the items above: confirm a fresh player genuinely
    can't equip a locked tech (try `/msu unlock <tech>` first to confirm the gate, then try equipping an
    still-locked one and confirm it's refused); confirm `/msu shop` (and talking to a GENERAL-merchant
    Consort - may take a few conversations, since it's just one topic among the existing real weighted
    pool) opens `SkillShopScreen` and that a real purchase there actually spends real boondollars and
    persists after relog; confirm the Buy button's enabled state matches `canUnlock` accurately for a
    tech with a real item-stack requirement too, not just a boondollar-only one.
22. Manually verify the whole `heroClass` pass end-to-end in a real client (needs several real Titled
    players, since most of this pass is either player-vs-player or player-vs-Title-Class-specific gating -
    not driven end-to-end this session for the same reason as the other manual-verification items above):
    confirm `bard.TechBardMetronome`/`mage.TechMageStudy`/`rogue.TechRogueSteal` can actually borrow and
    drive a real second player's own equipped ability; confirm `heir.TechHeir`'s two registered instances
    (`HEIR_WILL`/`UNIVERSAL_REVERSE`) retaliate independently based on which one's passive is toggled;
    confirm `knight.TechKnightWard`/`maid.TechMaid`/`sylph.TechSylph`/`sylph.TechSylphKarmaRestore`'s
    real per-slot tethers survive a target walking out of range and back; confirm
    `seer.TechSeerDodge`'s cooldown-gated full damage cancellation actually fires and its evasive hop
    doesn't feel broken (the exact original trig wasn't reconstructed, see that class's own doc comment);
    confirm `witch.TechWitchTrap`'s planted `AreaEffectCloud` renders/extends/expires correctly; confirm
    `lord.TechLord`'s 48-block AoE correctly excludes nearby real players but still hits distant
    opposite-Karma ones and any nearby hostile mob; confirm every real per-tech cost pulled from the
    original's live GitHub source (see this pass's own section above) actually matches in the Skill Shop.
23. ~~Verify the Abilitechnosynth's real multiblock still renders correctly in a real client~~ - done,
    see "Real atlas-visibility fix" above: a real custom atlas source config (`blocks`/`machines`/
    `items` directory sources added to `assets/minecraft/atlases/blocks.json`) was the approach chosen,
    confirmed via two real `gradlew runClient` log checks with zero "Missing textures in model" warnings
    remaining anywhere - including for every other registered item, a bigger pre-existing bug this same
    check happened to surface.
24. Build real art/models for `temporal_sendificator` - it currently has zero resource files at all
    (no blockstate, model, or texture in any namespace), a genuine missing-art gap rather than a
    reference bug, found during the "Resource-reference bug sweep" above.
25. Give `client.particles.PowerParticle`/`InkParticle` real art if/when it's ever available - the 7
    Power frames and 1 Ink frame currently shipped are new, procedurally-generated placeholders (see
    "Real particle system" above), the same category as this project's other stated placeholder-art
    gaps (Streak's flavour textures, Chloroball's now-fixed one).
26. Manually verify the new "Minestuck Universe" creative tab, the fixed dedicated-server crash (ideally
    via a real `gradlew runServer`), and the real Power/Ink particle system in a real client - none of
    this was driven end-to-end this session for the same reason as the other manual-verification items
    above.
27. Manually verify the `capabilities` close-out pass's new content in a real client (compiles clean, not
    driven end-to-end for the same reason as the other manual-verification items above - needs a real
    creative-mode/cheats-enabled player and equipped Abilitech key presses): confirm `TechDragonAura`'s
    retaliation nova/lock actually fires and its unlock gate genuinely requires holding Dragon Gel;
    confirm `TechReturn` actually lands on the real recorded entry point (not just "somewhere in the
    Land") across a real dimension change, including after a relog (tests the new persisted
    `AbilitechLoadout#landEntryPos`); confirm `TechSling`'s FOV zoom is visible and the thrown `MSUThrowableEntity` renders,
    damages, and drops/breaks correctly; confirm `/msu godtier badge builder_badge` genuinely requires
    the real grist+item cost, and that holding a block item afterward lets a drag-select cuboid actually
    place blocks (survival stack-count limit, creative 256-block cap, outline renders, normal single
    right-click placement is fully replaced not doubled) both via the badge and via real Minestuck Edit
    Mode.
28. Manually verify the new Freedom system (`mechanics.freedom`, see that section above) in a real client
    - not driven end-to-end this session, same reasoning as every item above: confirm a hostile mob
    driven to Extremely Low Freedom via `TechBreathConstrain` actually stops dodging/fleeing/wandering
    and that its goals correctly return once released; confirm a High-Freedom mob visibly moves/jumps
    faster, occasionally breaks its own leash, and resists a Slowness potion at least sometimes; confirm
    `TechBreathLiberate`/`TechBreathConstrain`'s tether behaves like `heart.TechHeartBond`'s own tether
    (survives the target moving, drops cleanly on release/unequip). Consider, if it ever feels needed in
    practice, a `/msu debug` command to directly set an entity's Freedom for testing rather than relying
    on holding down the new techs.
29. Manually verify the Freedom/Doom/Relationship cross-system interactions (see that section above) in a
    real client - not driven end-to-end this session, same reasoning as every item above: sustain
    `TechBreathLiberate` on a real hostile-to-neutral mob long enough to watch it actually flip to
    Friendship/Loyalty and start following, then watch it stop following after a `TechBreathConstrain`
    pass or natural Freedom decay; confirm the relationship stability drift is actually noticeable over a
    longer play session rather than just compiling clean; confirm a high-Doom, low-Freedom entity really
    does take visibly more damage than either factor alone would predict, without runaway values (both
    curves are supposed to saturate, but this was reasoned about, not watched).
30. Manually verify the "Breath does not create relationships" correction and `TechPageBreathFreeWill`
    ("Free Will", see the Freedom/Doom/Relationship section above) in a real client - not driven
    end-to-end this session, same reasoning as every item above: confirm a genuine stranger mob gains no
    followership from sustained `TechBreathLiberate` (the corrected, intended behavior) while an already-
    tamed/owned mob does; confirm the Liberation event actually fires once (not repeatedly) on the real
    threshold crossing; confirm Forced Freedom's Trust/Conflict shift is visible against a real Hostile
    mob; confirm Free Will's passive trickle, its activation burst (Freedom grant, leash-breaking,
    Instability reduction), and Shared Freedom's pairwise relationship boost all work correctly on a real
    group of nearby entities.
31. Manually verify the new Mind Decision system (`mechanics.mind`, see that section above) in a real
    client - not driven end-to-end this session, same reasoning as every item above: confirm a real
    high-Certainty mob visibly resists being retargeted away from its current opponent, and that a
    Hostile-relationship threat still overrides that resistance; confirm a low-Adaptability mob keeps
    "chasing" a target that already died noticeably longer than a high-Adaptability one; confirm a
    hesitating mob genuinely keeps moving/reacting but doesn't land an attack until the pause elapses;
    confirm `TechMindControl`/`TechMindConfusion`'s new Resolve resistance actually blocks an attempt
    against a real high-Resolve target and sends `status.mindResisted`. Consider, if it ever feels needed,
    a `/msu debug` command to directly set an entity's Decision attributes for testing.
32. Manually verify the Breath Wind visuals (`WindRibbonRenderer`/`WindBurstRenderer` + `WindEngine`, see
    that section above) in a real client - not driven end-to-end this session, same reasoning as every item
    above, but higher-stakes than most: this is real vertex-animated mesh rendering, never seen render at
    all. Confirm the ribbon mesh actually appears (right render type, right texture, not invisible/black/
    missing), that it visibly twists and bends when the target walks around instead of snapping or breaking
    apart; confirm the vortex spiral around the target rotates and grows with Freedom as intended; confirm
    Constrain's inward-shrinking vortex reads as compression, not "evil"; confirm `WindBurstRenderer`'s
    expanding billboard shell reads as one coherent wave, not scattered flickering spokes, and that the
    near-camera cutoff doesn't leave visible gaps; confirm `WindEngine`'s own particle layer still looks
    right as secondary decoration underneath the mesh, not fighting it visually. If the mesh rendering turns
    out too fragile/wrong in practice, the `WindEngine`-only particle version (this same section's own git
    history, before the mesh renderer pass) is a real, working fallback to revert to.
