package org.wilkretawesomesauce.minestuckuniverseported.util;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.AbilitechLoadout;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.badgeEffects.BadgeEffects;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.beam.BeamData;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.consortCosmetics.ConsortHatsData;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.SkillKeyStates;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.mediumData.MediumData;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.game.GameData;
import org.wilkretawesomesauce.minestuckuniverseported.client.streak.StreakPreference;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.strife.StrifeData;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.doom.DoomData;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.freedom.FreedomData;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.mind.DecisionData;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.doom.DoomReleasePool;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.TimelineBranchRegistry;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.TimelineData;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.request.TimeRequestData;

import java.util.function.Supplier;

/**
 * Data attachment registration for this addon. Modern NeoForge equivalent of what 1.12.2 Forge
 * capabilities were used for in MinestuckUniverse (see e.g. {@code capabilities.MSUCapabilities}).
 */
public final class MSUAttachments
{
	public static final DeferredRegister<AttachmentType<?>> REGISTER =
			DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Minestuckuniverseported.MODID);

	// Attaches to any LivingEntity, mirroring the 1.12.2 capability which was attached to EntityLivingBase.
	// copyOnDeath() is required for a serializable attachment to survive a respawn at all (death,
	// dimension change, etc.) - without it NeoForge just resets it to a fresh default on the new player
	// entity, silently wiping the whole portfolio every respawn regardless of any config. Whether the
	// portfolio should then be deliberately *cleared* specifically on death is a separate, later decision
	// made in StrifePortfolioEvents based on Config.keepPortfolioOnDeath.
	public static final Supplier<AttachmentType<StrifeData>> STRIFE_PORTFOLIO = REGISTER.register(
			"strife_portfolio", () -> AttachmentType.serializable(StrifeData::new).copyOnDeath().build());

	// Genuinely new, no-original-counterpart scratch state only (slotHistory/dragonAuraActive/landEntryPos)
	// - the capabilities.badgeEffects.IBadgeEffects fields (tether, external-tech borrowing, warp point,
	// manipulated-matter corners, etc.) that briefly lived here (a user-requested consolidation) moved back
	// out to their own BADGE_EFFECTS attachment below per a later, explicit correction - see
	// capabilities.badgeEffects.BadgeEffects's own doc comment for why.
	public static final Supplier<AttachmentType<AbilitechLoadout>> ABILITECH_LOADOUT = REGISTER.register(
			"abilitech_loadout", () -> AttachmentType.serializable(AbilitechLoadout::new).copyOnDeath().build());

	// The capabilities.badgeEffects.IBadgeEffects fields - see BadgeEffects's own doc comment for the full
	// accounting of which original fields these are. copyOnDeath() for the same reason as
	// STRIFE_PORTFOLIO/ABILITECH_LOADOUT/GOD_TIER above - a warp point shouldn't silently vanish on respawn.
	public static final Supplier<AttachmentType<BadgeEffects>> BADGE_EFFECTS = REGISTER.register(
			"badge_effects", () -> AttachmentType.serializable(BadgeEffects::new).copyOnDeath().build());

	// Attaches to any LivingEntity, mirroring the 1.12.2 capability which was attached to EntityLivingBase -
	// see capabilities.keyStates.SkillKeyStates's own doc comment. copyOnDeath() for the same reason as
	// ABILITECH_LOADOUT above (this held the key-input state machine before moving here).
	public static final Supplier<AttachmentType<SkillKeyStates>> SKILL_KEY_STATES = REGISTER.register(
			"skill_key_states", () -> AttachmentType.serializable(SkillKeyStates::new).copyOnDeath().build());

	// Same copyOnDeath() reasoning as above - ascension status and spire progress should survive an
	// ordinary respawn, not silently reset.
	public static final Supplier<AttachmentType<GodTierData>> GOD_TIER = REGISTER.register(
			"god_tier", () -> AttachmentType.serializable(GodTierData::new).copyOnDeath().build());

	// Attaches to a Level, not an entity - NeoForge's Level class extends AttachmentHolder too. No
	// copyOnDeath() (doesn't apply to levels), and only doomPoints/totalRewinds actually persist -
	// see TimelineData's own doc comment for why the recorded history itself isn't serialized.
	public static final Supplier<AttachmentType<TimelineData>> TIMELINE = REGISTER.register(
			"timeline", () -> AttachmentType.serializable(TimelineData::new).build());

	// Only ever fetched from the Overworld (the Alpha Timeline) - see TimelineBranchRegistry's own doc
	// comment for why that's the one safe place for the whole branch tree to live, rather than one
	// registry per level.
	public static final Supplier<AttachmentType<TimelineBranchRegistry>> TIMELINE_BRANCHES = REGISTER.register(
			"timeline_branches", () -> AttachmentType.serializable(TimelineBranchRegistry::new).build());

	// Time Request / Doom System - see TimeRequestData's own doc comment for why this is deliberately
	// separate from TIMELINE above rather than folded into it. copyOnDeath() for the same reason as
	// STRIFE_PORTFOLIO/ABILITECH_LOADOUT/GOD_TIER: open paradoxes shouldn't silently vanish on respawn.
	public static final Supplier<AttachmentType<TimeRequestData>> TIME_REQUEST_DATA = REGISTER.register(
			"time_request_data", () -> AttachmentType.serializable(TimeRequestData::new).copyOnDeath().build());

	// Attaches to a Level, same as TIMELINE above - only ever meaningfully queried for a Land dimension
	// (see godtier.MediumData's own doc comment), but doesn't need conditional attachment to enforce that.
	public static final Supplier<AttachmentType<MediumData>> MEDIUM_DATA = REGISTER.register(
			"medium_data", () -> AttachmentType.serializable(MediumData::new).build());

	// Attaches to any LivingEntity, only ever meaningfully used on a Consort or Frog - see
	// capabilities.consortCosmetics.ConsortHatsData.
	public static final Supplier<AttachmentType<ConsortHatsData>> CONSORT_HATS_DATA = REGISTER.register(
			"consort_hats_data", () -> AttachmentType.serializable(ConsortHatsData::new).build());

	// Attaches to a Level, same as TIMELINE/MEDIUM_DATA above - but only ever fetched from the Overworld
	// specifically, mirroring the original capability's single dimension-0-only instance. See
	// itemvoid.GameData's own doc comment.
	public static final Supplier<AttachmentType<GameData>> ITEM_VOID = REGISTER.register(
			"item_void", () -> AttachmentType.serializable(GameData::new).build());

	// Attaches to a Level, every dimension (not just Overworld/Land) - see beam.BeamData's own doc comment.
	public static final Supplier<AttachmentType<BeamData>> BEAM_DATA = REGISTER.register(
			"beam_data", () -> AttachmentType.serializable(BeamData::new).build());

	// Attaches to any LivingEntity (same generic-attach-point convention as CONSORT_HATS_DATA), but
	// only ever meaningfully set for a real player via the /msustreak debug command - see
	// streak.StreakPreference's own doc comment. copyOnDeath() for the same reason as
	// STRIFE_PORTFOLIO/GOD_TIER above: a cosmetic toggle shouldn't silently reset on respawn.
	public static final Supplier<AttachmentType<StreakPreference>> STREAK_PREFERENCE = REGISTER.register(
			"streak_preference", () -> AttachmentType.serializable(StreakPreference::new).copyOnDeath().build());

	// Attaches to any LivingEntity - the universal Doom value (see mechanics.doom.DoomData's own doc comment).
	// Deliberately NOT copyOnDeath() - unlike every other LivingEntity attachment above, a fresh default
	// on the post-death entity instance (a respawned player, or nothing for a killed mob) is exactly
	// the wanted behavior: death releases bound Doom (see mechanics.doom.DoomReleaseEvents) rather than carrying
	// it forward into whatever comes next.
	public static final Supplier<AttachmentType<DoomData>> DOOM_DATA = REGISTER.register(
			"doom_data", () -> AttachmentType.serializable(DoomData::new).build());

	// Attaches to a Level, every dimension - same as BEAM_DATA above (deaths can happen anywhere,
	// unlike MEDIUM_DATA/ITEM_VOID's Overworld-only scope). See mechanics.doom.DoomReleasePool's own doc comment.
	public static final Supplier<AttachmentType<DoomReleasePool>> DOOM_RELEASE_POOL = REGISTER.register(
			"doom_release_pool", () -> AttachmentType.serializable(DoomReleasePool::new).build());

	// Attaches to any LivingEntity - the hidden Freedom value (see mechanics.freedom.FreedomData's own doc
	// comment). Deliberately NOT copyOnDeath(), same reasoning as DOOM_DATA above: a fresh, neutral value
	// on a respawned player's new instance is correct, not a bug.
	public static final Supplier<AttachmentType<FreedomData>> FREEDOM_DATA = REGISTER.register(
			"freedom_data", () -> AttachmentType.serializable(FreedomData::new).build());

	// Attaches to any LivingEntity - the hidden Decision State (see mechanics.mind.DecisionData's own doc
	// comment). Deliberately NOT copyOnDeath(), same reasoning as FREEDOM_DATA above.
	public static final Supplier<AttachmentType<DecisionData>> DECISION_DATA = REGISTER.register(
			"decision_data", () -> AttachmentType.serializable(DecisionData::new).build());

	private MSUAttachments()
	{
	}
}
