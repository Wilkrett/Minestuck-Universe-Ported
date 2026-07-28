package org.wilkretawesomesauce.minestuckuniverseported.client.streak;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nullable;

/**
 * Per-entity state backing the {@code /msustreak} debug/demo command (see {@code command.StreakCommand}) -
 * whether the streak ribbon + sprint-ghost effect (ported from iChun's Streak, see
 * {@link StreakFlavours}'s own doc comment) is currently toggled on, and which flavour it uses.
 * Attached to any {@code LivingEntity} (same generic-attach-point convention as
 * {@code capabilities.consortCosmetics.ConsortHatsData}), but only ever meaningfully set for a real player via that
 * command - unlike the original mod, this isn't an always-on cosmetic every player automatically gets.
 */
public class StreakPreference implements INBTSerializable<CompoundTag>
{
	private boolean enabled;
	@Nullable
	private String favouriteFlavour;

	// Real gameplay reuse of this debug system, not just the /msu streak command anymore - e.g.
	// heroAspect.time.TechTimeAccelerateSelf ("Accelerate") wants the ghost-afterimage half only, no
	// ribbon, tinted red, and showing regardless of whether the caster is actually sprinting. All three
	// default to the plain debug-toggle's existing look (ribbon shown, ghosts sprint-gated, no tint) so
	// `/msu streak toggle` itself is completely unaffected.
	private boolean hideTrail;
	private boolean ghostsIgnoreSprint;
	private int ghostTint = 0xFFFFFF;

	public boolean isEnabled()
	{
		return enabled;
	}

	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

	public boolean isHideTrail()
	{
		return hideTrail;
	}

	public void setHideTrail(boolean hideTrail)
	{
		this.hideTrail = hideTrail;
	}

	public boolean isGhostsIgnoreSprint()
	{
		return ghostsIgnoreSprint;
	}

	public void setGhostsIgnoreSprint(boolean ghostsIgnoreSprint)
	{
		this.ghostsIgnoreSprint = ghostsIgnoreSprint;
	}

	/** Packed RGB (no alpha channel), multiplied into the ghost afterimages' own fade alpha. {@code 0xFFFFFF} = no tint. */
	public int getGhostTint()
	{
		return ghostTint;
	}

	public void setGhostTint(int ghostTint)
	{
		this.ghostTint = ghostTint;
	}

	@Nullable
	public String getFavouriteFlavour()
	{
		return favouriteFlavour;
	}

	public void setFavouriteFlavour(@Nullable String favouriteFlavour)
	{
		this.favouriteFlavour = favouriteFlavour;
	}

	/** The flavour to actually render with - falls back to the first registered flavour if none was ever chosen. */
	public String resolveFlavour()
	{
		return favouriteFlavour != null && StreakFlavours.isValid(favouriteFlavour) ? favouriteFlavour : StreakFlavours.NAMES.get(0);
	}

	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider provider)
	{
		CompoundTag nbt = new CompoundTag();
		nbt.putBoolean("Enabled", enabled);
		if(favouriteFlavour != null)
			nbt.putString("FavouriteFlavour", favouriteFlavour);
		nbt.putBoolean("HideTrail", hideTrail);
		nbt.putBoolean("GhostsIgnoreSprint", ghostsIgnoreSprint);
		nbt.putInt("GhostTint", ghostTint);
		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt)
	{
		enabled = nbt.getBoolean("Enabled");
		favouriteFlavour = nbt.contains("FavouriteFlavour") ? nbt.getString("FavouriteFlavour") : null;
		hideTrail = nbt.getBoolean("HideTrail");
		ghostsIgnoreSprint = nbt.getBoolean("GhostsIgnoreSprint");
		ghostTint = nbt.contains("GhostTint") ? nbt.getInt("GhostTint") : 0xFFFFFF;
	}
}
