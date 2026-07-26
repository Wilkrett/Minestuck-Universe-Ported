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
6 techs (Gale, Knockback, Fall Proof, Speed, Bubble, Wind Vessel). Wind Vessel does real
render-cancellation + movement-input dampening. **Known gap**: sub-block collision-phasing
(slipping through gaps) was never built - no modern NeoForge hook exists for overriding a real
connected player's own collision resolution without Mixin.

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
