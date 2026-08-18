package org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.badges.Badge;
import org.wilkretawesomesauce.minestuckuniverseported.network.MSUAbilitechPackets;
import org.wilkretawesomesauce.minestuckuniverseported.skills.TechBoondollarCost;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.Abilitech;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechRegistry;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code capabilities.godTier.GodTierData}/{@code IGodTierData}.
 * <p>
 * <b>Scope note:</b> the original bundled core God Tier ascension state together with a large, separate
 * skills/badges/abilitech RPG system and a per-stat XP/leveling system - {@code isGodTier()} itself was
 * literally defined as {@code godTierXp.get(StatType.GENERAL).level > 0}. That whole system is out of
 * scope for this pass (by explicit request), so this only carries the actual ascension state, as a plain
 * standalone flag instead of something derived from an unported leveling system.
 * <p>
 * Also not ported (yet): the original's consort type, lunar sway, and grist hoard bonus fields (all
 * fairly self-contained bonus trackers, could be added later without touching this class's shape), and
 * any gameplay *effects* of being God Tier (extra hearts, fall damage immunity, resurrection) - this
 * class only tracks the ascension state, nothing consumes it into a buff yet.
 * <p>
 * Karma ({@link #getStaticKarma()}/{@link #getTempKarma()}) <i>is</i> now ported, as real storage -
 * {@code abilitech.heroAspect.mind.TechMindKarmaHeal} ("Godhood's Justice") and
 * {@code abilitech.heroAspect.rage.TechRageOutburst} ("Vengeful Outburst") both read/write it for real
 * rather than assuming it's always zero. What isn't ported: the dozens of scattered, largely unrelated
 * places across the original's own codebase that changed Temp Karma as a side effect of other actions
 * (using various god-tier powers, certain quest events, etc.) - that's a genuinely separate, much larger
 * feature spanning far more than these two abilitechs, not something either of them is responsible for
 * on its own. Static Karma defaults to a player's zodiac-sign alignment in the original (via their
 * Title's class/aspect); this project has no such alignment table, so it simply starts at 0 for
 * everyone and only ever moves via {@code TechMindKarmaHeal}'s own passive nudge.
 * <p>
 * <b>Tech-equip-slot half restored (real port, not new)</b>: {@link #equipped}/{@link #passiveEnabled}/
 * {@link #unlockedTechs} and their accessors are the original's own {@code GodTierData} tech-slot fields -
 * these briefly lived merged into {@code skills.abilitech.AbilitechLoadout} (alongside the unrelated
 * {@code SkillKeyStates} capability) for a NeoForge-attachment-count-minimizing reason that turned out not
 * to be worth the resulting package-structure mismatch against the original. {@code AbilitechLoadout}
 * still owns the actual key-input state machine and several {@code IBadgeEffects}-derived per-slot scratch
 * fields - only the equip/unequip/unlock half moved back here. Synced to the client via the same
 * {@code network.AbilitechLoadoutSyncPacket} that already existed for {@code AbilitechLoadout} - see that
 * class's own doc comment for why one combined packet was kept rather than building this attachment its
 * own separate sync path.
 * <p>
 * {@link #onPlayerLoggedIn}/{@link #onPlayerRespawn} trigger that sync - genuinely new NeoForge-only
 * plumbing with no 1.12.2 counterpart at all (data attachments aren't automatically network-synced the way
 * old Forge capabilities implicitly were), the same category {@code strife.StrifePortfolioEvents}
 * already established for {@code StrifeData}'s own login/respawn sync - kept directly on this class rather
 * than a separate one-off "Events" file, since (unlike {@code StrifePortfolioEvents}, which also does
 * genuine per-tick reconciliation work) this attachment has nothing else that would justify a whole
 * sibling class.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public class GodTierData implements INBTSerializable<CompoundTag>
{
	private boolean ascended = false;
	private boolean canGodTier = true;
	private boolean climbedTheSpire = false;
	private boolean wereEffectsActive = false;
	private int staticKarma = 0;
	private int tempKarma = 0;

	// Real port of the original's own GodTierData#scrollsUsed - counts non-Super Skaian Scrolls actually
	// consumed, gated against Config#skaiaScrollLimit by item.SkaianScrollItem. See that class's own doc
	// comment for why "Super" scrolls (isSuperScroll in the original) don't increment this.
	private int scrollsUsed = 0;

	public static final int TECH_SLOTS = 3;

	private final Abilitech[] equipped = new Abilitech[TECH_SLOTS];
	private final boolean[] passiveEnabled = new boolean[TECH_SLOTS];

	// Real per-player unlock state for the boondollar-cost economy (see skills.TechBoondollarCost) -
	// genuinely new, the original had no equivalent single set (unlock state there was implicitly "does
	// IGodTierData#hasSkill(tech) return true", backed by its own separate skill-list field) - which is
	// exactly why this lives here rather than on AbilitechLoadout: it's this class's own modern
	// equivalent of that original field, not an AbilitechLoadout concept.
	private final Set<ResourceLocation> unlockedTechs = new HashSet<>();

	private static int clampSlot(int slot)
	{
		return Math.min(TECH_SLOTS - 1, Math.max(0, slot));
	}

	@Nullable
	public Abilitech getTech(int slot)
	{
		return equipped[clampSlot(slot)];
	}

	public int getTechSlots()
	{
		return TECH_SLOTS;
	}

	public boolean isTechEquipped(Abilitech tech)
	{
		for(Abilitech t : equipped)
			if(tech.equals(t))
				return true;
		return false;
	}

	/**
	 * Whether {@code tech} is both equipped in some slot and has that slot's passive toggle on - for techs
	 * whose actual behavior lives in a static event handler rather than {@code Abilitech#onPassiveTick}
	 * (e.g. {@code heroAspect.blood.TechBloodBleeding}, which only matters on someone else's
	 * {@code LivingDamageEvent}, not a per-tick check of its own owner).
	 */
	public boolean isPassiveEnabledFor(Abilitech tech)
	{
		for(int i = 0; i < TECH_SLOTS; i++)
			if(tech.equals(equipped[i]) && passiveEnabled[i])
				return true;
		return false;
	}

	public void equipTech(Level level, Player player, Abilitech tech, int slot)
	{
		int i = clampSlot(slot);
		equipped[i] = tech;
		tech.onEquipped(level, player, i);
	}

	public void unequipTech(Level level, Player player, int slot)
	{
		int i = clampSlot(slot);
		if(equipped[i] != null)
			equipped[i].onUnequipped(level, player, i);
		equipped[i] = null;
		passiveEnabled[i] = false;
	}

	public boolean isPassiveEnabled(int slot)
	{
		return passiveEnabled[clampSlot(slot)];
	}

	public void setPassiveEnabled(int slot, boolean enabled)
	{
		passiveEnabled[clampSlot(slot)] = enabled;
	}

	public boolean isUnlocked(Abilitech tech)
	{
		if(!(tech instanceof TechBoondollarCost cost))
			return true; // no cost concept at all for a plain Abilitech - always usable, matching this project's original "no gating" default for anything not under the real cost economy

		return (cost.cost == 0 && cost.requiredStacks.isEmpty()) || unlockedTechs.contains(tech.getId());
	}

	public void markUnlocked(Abilitech tech)
	{
		unlockedTechs.add(tech.getId());
	}

	/** Revokes one previously-unlocked tech - a no-op for a tech with no real cost concept (see
	 * {@link #isUnlocked}, those are always considered unlocked and can't be revoked). Used by
	 * {@code command.AbilitechUserCommand}'s debug "revoke" action. */
	public void revokeUnlocked(Abilitech tech)
	{
		unlockedTechs.remove(tech.getId());
	}

	/** Revokes every unlocked tech at once. */
	public void clearUnlockedTechs()
	{
		unlockedTechs.clear();
	}

	// Real port of the original's Badge half of IGodTierData#hasSkill/isBadgeActive/isBadgeEnabled -
	// badges.KARMA/EFFECT_BUFF/BADGE_PAGE/BADGE_OVERLORD need this to be real for heroClass techs to read
	// it correctly. Map value is the real per-badge enable/disable toggle (Skill#canDisable) - no toggle
	// UI exists yet, so every badge unlocks with it defaulted to true (matches "active immediately on
	// unlock", the only reachable state today). The original's MasterBadge special case isn't reproduced
	// (MasterBadge itself isn't ported).
	private final Map<ResourceLocation, Boolean> badges = new HashMap<>();

	public boolean hasBadge(Badge badge)
	{
		return badges.containsKey(badge.getId());
	}

	public boolean isBadgeEnabled(Badge badge)
	{
		return badges.getOrDefault(badge.getId(), false);
	}

	public void setBadgeEnabled(Badge badge, boolean enabled)
	{
		if(!hasBadge(badge) || !badge.canDisable())
			return;
		badges.put(badge.getId(), enabled);
	}

	/** Real port of {@code IGodTierData#isBadgeActive} - unlocked, and (if it can be disabled at all)
	 * currently enabled and still {@link Badge#canUse}. */
	public boolean isBadgeActive(Badge badge, Level level, Player player)
	{
		return hasBadge(badge) && (!badge.canDisable() || (isBadgeEnabled(badge) && badge.canUse(level, player)));
	}

	public void unlockBadge(Badge badge)
	{
		badges.put(badge.getId(), true);
	}

	// Real port of the original's GodTierData#getSkillLevel(StatType.GENERAL)/godTierXp - a single flat
	// level rather than the original's real per-StatType XP economy (level thresholds, attribute scaling,
	// what actions grant XP). Nothing reachable in this project ever consumes any StatType but GENERAL, so
	// collapsing to one field is a real, stated simplification, not a guess - same treatment this class's
	// own Karma fields already got (real storage, real consumers, no automatic in-game way to change it
	// yet beyond a debug command - see command.GodTierSkillCommand).
	private int skillLevel = 0;

	public int getSkillLevel()
	{
		return skillLevel;
	}

	public void setSkillLevel(int skillLevel)
	{
		this.skillLevel = skillLevel;
	}

	public int getScrollsUsed()
	{
		return scrollsUsed;
	}

	public void addScrollsUsed()
	{
		scrollsUsed++;
	}

	public int getStaticKarma()
	{
		return staticKarma;
	}

	public void setStaticKarma(int staticKarma)
	{
		this.staticKarma = staticKarma;
	}

	public int getTempKarma()
	{
		return tempKarma;
	}

	public void setTempKarma(int tempKarma)
	{
		this.tempKarma = tempKarma;
	}

	public boolean isAscended()
	{
		return ascended;
	}

	public void setAscended(boolean ascended)
	{
		this.ascended = ascended;
	}

	/**
	 * The original's equivalent gate could be permanently revoked (e.g. after a "make some other choice"
	 * type event); ported as a plain flag since that specific mechanic isn't implemented yet, but the gate
	 * itself is wired into {@link GodTierEvents} so it's ready for whatever revokes it later.
	 */
	public boolean canGodTier()
	{
		return canGodTier;
	}

	public void setCanGodTier(boolean canGodTier)
	{
		this.canGodTier = canGodTier;
	}

	public boolean climbedTheSpire()
	{
		return climbedTheSpire;
	}

	public void setClimbedTheSpire(boolean climbedTheSpire)
	{
		this.climbedTheSpire = climbedTheSpire;
	}

	/**
	 * Ported as-is from the original, which used this to remember whether God Tier's passive effects
	 * (health/attribute bonuses - not ported yet) were active before some other state change temporarily
	 * suppressed them, so they could be correctly restored afterward.
	 */
	public boolean wereEffectsActive()
	{
		return wereEffectsActive;
	}

	public void setWereEffectsActive(boolean wereEffectsActive)
	{
		this.wereEffectsActive = wereEffectsActive;
	}

	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider provider)
	{
		CompoundTag nbt = new CompoundTag();
		nbt.putBoolean("Ascended", ascended);
		nbt.putBoolean("CanGodTier", canGodTier);
		nbt.putBoolean("ClimbedTheSpire", climbedTheSpire);
		nbt.putBoolean("WereEffectsActive", wereEffectsActive);
		nbt.putInt("StaticKarma", staticKarma);
		nbt.putInt("TempKarma", tempKarma);
		nbt.putInt("SkillLevel", skillLevel);
		nbt.putInt("ScrollsUsed", scrollsUsed);

		ListTag badgeList = new ListTag();
		for(Map.Entry<ResourceLocation, Boolean> entry : badges.entrySet())
		{
			CompoundTag badgeTag = new CompoundTag();
			badgeTag.putString("Id", entry.getKey().toString());
			badgeTag.putBoolean("Enabled", entry.getValue());
			badgeList.add(badgeTag);
		}
		nbt.put("Badges", badgeList);

		for(int i = 0; i < TECH_SLOTS; i++)
		{
			if(equipped[i] != null)
				nbt.putString("Tech" + i, equipped[i].getId().toString());
			nbt.putBoolean("Passive" + i, passiveEnabled[i]);
		}
		ListTag unlockedList = new ListTag();
		for(ResourceLocation id : unlockedTechs)
			unlockedList.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));
		nbt.put("UnlockedTechs", unlockedList);

		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt)
	{
		ascended = nbt.getBoolean("Ascended");
		canGodTier = nbt.contains("CanGodTier") ? nbt.getBoolean("CanGodTier") : true;
		climbedTheSpire = nbt.getBoolean("ClimbedTheSpire");
		wereEffectsActive = nbt.getBoolean("WereEffectsActive");
		staticKarma = nbt.getInt("StaticKarma");
		tempKarma = nbt.getInt("TempKarma");
		skillLevel = nbt.getInt("SkillLevel");
		scrollsUsed = nbt.getInt("ScrollsUsed");

		badges.clear();
		if(nbt.contains("Badges"))
		{
			ListTag badgeList = nbt.getList("Badges", Tag.TAG_COMPOUND);
			for(int i = 0; i < badgeList.size(); i++)
			{
				CompoundTag badgeTag = badgeList.getCompound(i);
				badges.put(ResourceLocation.parse(badgeTag.getString("Id")), badgeTag.getBoolean("Enabled"));
			}
		}

		for(int i = 0; i < TECH_SLOTS; i++)
		{
			equipped[i] = nbt.contains("Tech" + i) ? MSUAbilitechRegistry.get(ResourceLocation.parse(nbt.getString("Tech" + i))) : null;
			passiveEnabled[i] = nbt.getBoolean("Passive" + i);
		}
		unlockedTechs.clear();
		if(nbt.contains("UnlockedTechs"))
		{
			ListTag unlockedList = nbt.getList("UnlockedTechs", Tag.TAG_STRING);
			for(int i = 0; i < unlockedList.size(); i++)
				unlockedTechs.add(ResourceLocation.parse(unlockedList.getString(i)));
		}
	}

	@SubscribeEvent
	private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
	{
		if(event.getEntity() instanceof ServerPlayer player)
			MSUAbilitechPackets.sendLoadoutSync(player);
	}

	@SubscribeEvent
	private static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event)
	{
		if(event.getEntity() instanceof ServerPlayer player)
			MSUAbilitechPackets.sendLoadoutSync(player);
	}
}
