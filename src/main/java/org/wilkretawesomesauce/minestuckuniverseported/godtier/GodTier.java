package org.wilkretawesomesauce.minestuckuniverseported.godtier;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code capabilities.godTier.GodTierData}/{@code IGodTierData}
 * Forge capability - the ascension-only slice of it. Like {@code StrifePortfolio}, this is a data
 * attachment (see {@link org.wilkretawesomesauce.minestuckuniverseported.MSUAttachments#GOD_TIER})
 * rather than a capability, since NeoForge 1.21.1 replaced entity capabilities with attachments.
 * <p>
 * <b>Scope note - read this before extending it.</b> The original {@code IGodTierData} bundles God Tier
 * ascension together with an entire separate bespoke RPG layer: skills, badges, a "master badge", an
 * "abilitech" equipment-slot loadout system, per-stat XP/leveling with attribute modifiers, karma,
 * consort type, and lunar sway - over 100 files worth, in the original. None of that is part of this
 * pass; this class is deliberately just the ascension state itself, matching how the Strife Specibus
 * port started with just {@code StrifePortfolio}/{@code StrifeSpecibus} before any handler, networking,
 * or GUI layer existed.
 * <p>
 * One real behavioural simplification worth knowing about: in the original, {@code isGodTier()} isn't
 * an independent flag - it's <i>derived</i> from the (unported) stat-leveling system, specifically
 * {@code generalStat.level > 0}. Since that system doesn't exist here, {@link #isAscended()} is a plain
 * boolean instead. If the stat-leveling system gets ported later, this may need to change to match.
 * <p>
 * TODO(next phase): once anything client-side actually reads this (a GUI, HUD, etc.), remember to add a
 * login/respawn sync packet - see {@code StrifePortfolioEvents}. Skipping that was the exact cause of a
 * real bug on the strife side (portfolio appeared empty after a world reload until some unrelated
 * mutation happened to trigger a sync) - attachments are not automatically synced to the client by
 * NeoForge, and that bug won't be visible here until something is actually reading this data client-side.
 */
public class GodTier implements INBTSerializable<CompoundTag>
{
	private boolean ascended = false;
	private boolean climbedTheSpire = false;
	private boolean wereEffectsActive = false;

	public boolean isAscended()
	{
		return ascended;
	}

	public void setAscended(boolean ascended)
	{
		this.ascended = ascended;
	}

	/** Whether the player has climbed their Land's spire - tracked separately from ascension itself. */
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
		nbt.putBoolean("ClimbedTheSpire", climbedTheSpire);
		nbt.putBoolean("WereEffectsActive", wereEffectsActive);
		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt)
	{
		ascended = nbt.getBoolean("Ascended");
		climbedTheSpire = nbt.getBoolean("ClimbedTheSpire");
		wereEffectsActive = nbt.getBoolean("WereEffectsActive");
	}
}
