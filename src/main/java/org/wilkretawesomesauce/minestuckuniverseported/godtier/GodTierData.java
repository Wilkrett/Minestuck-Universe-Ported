package org.wilkretawesomesauce.minestuckuniverseported.godtier;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.wilkretawesomesauce.minestuckuniverseported.badges.Badge;

import java.util.HashMap;
import java.util.Map;

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
 */
public class GodTierData implements INBTSerializable<CompoundTag>
{
	private boolean ascended = false;
	private boolean canGodTier = true;
	private boolean climbedTheSpire = false;
	private boolean wereEffectsActive = false;
	private int staticKarma = 0;
	private int tempKarma = 0;

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

		ListTag badgeList = new ListTag();
		for(Map.Entry<ResourceLocation, Boolean> entry : badges.entrySet())
		{
			CompoundTag badgeTag = new CompoundTag();
			badgeTag.putString("Id", entry.getKey().toString());
			badgeTag.putBoolean("Enabled", entry.getValue());
			badgeList.add(badgeTag);
		}
		nbt.put("Badges", badgeList);

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
	}
}
