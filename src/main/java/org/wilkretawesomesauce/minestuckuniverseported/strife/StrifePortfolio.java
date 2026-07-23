package org.wilkretawesomesauce.minestuckuniverseported.strife;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code capabilities.strife.StrifeData}/{@code IStrifeData}
 * Forge capability. In NeoForge 1.21.1, Forge capabilities-on-entities were replaced by the data
 * attachment system, so this is now a plain {@link INBTSerializable} attached to entities via
 * {@link MSUAttachments#STRIFE_PORTFOLIO} instead of a capability interface + provider pair.
 * <p>
 * Holds the "portfolio": the set of strife specibi (weapon decks) assigned to an entity, which one/slot
 * is currently selected, whether a weapon is currently armed (held), and the handful of related flags
 * the original tracked (strife card drop cooldown, abstrata switcher unlock, etc).
 * <p>
 * Bugfix vs. the original: {@code isPortfolioEmpty()} in 1.12.2 unconditionally returned {@code false}
 * (its final line was {@code return false;} instead of {@code return true;}), so it never actually
 * reported an empty portfolio. Fixed here.
 */
public class StrifePortfolio implements INBTSerializable<CompoundTag>
{
	public static final int PORTFOLIO_SIZE = 10;

	private StrifeSpecibus[] portfolio = new StrifeSpecibus[PORTFOLIO_SIZE];
	private int selectedSpecibusIndex = -1;
	private int selectedWeaponIndex = -1;
	private boolean armed = false;

	private int droppedCards = 0;
	private int prevSelectedSlot = 0;
	private boolean abstrataSwitcherUnlocked = false;
	private boolean strifeEnabled = false;

	public StrifeSpecibus[] getPortfolio()
	{
		return portfolio;
	}

	/** Non-null, non-empty (has contents, or is an assigned fist kind) specibi only. */
	public StrifeSpecibus[] getNonEmptyPortfolio()
	{
		List<StrifeSpecibus> result = new ArrayList<>();
		for(StrifeSpecibus specibus : portfolio)
			if(specibus != null && specibus.isAssigned()
					&& (specibus.getKindAbstratus().isFist() || !specibus.getContents().isEmpty()))
				result.add(specibus);
		return result.toArray(new StrifeSpecibus[0]);
	}

	public boolean isPortfolioFull()
	{
		for(StrifeSpecibus specibus : portfolio)
			if(specibus == null)
				return false;
		return true;
	}

	public boolean isPortfolioEmpty()
	{
		for(StrifeSpecibus specibus : portfolio)
			if(specibus != null)
				return false;
		return true;
	}

	public boolean portfolioHasAbstratus(@Nullable KindAbstratus kindAbstratus)
	{
		if(kindAbstratus == null)
			return false;
		for(StrifeSpecibus specibus : portfolio)
			if(specibus != null && specibus.getKindAbstratus() == kindAbstratus)
				return true;
		return false;
	}

	public boolean addSpecibus(StrifeSpecibus specibus)
	{
		if(isPortfolioFull() || (specibus.isAssigned() && portfolioHasAbstratus(specibus.getKindAbstratus())))
			return false;

		for(int i = 0; i < portfolio.length; i++)
			if(portfolio[i] == null)
			{
				portfolio[i] = specibus;
				if(selectedSpecibusIndex < 0)
					selectedSpecibusIndex = i;
				return true;
			}
		return false;
	}

	@Nullable
	public StrifeSpecibus removeSpecibus(int index)
	{
		if(index < 0 || index >= portfolio.length || portfolio[index] == null)
			return null;
		StrifeSpecibus removed = portfolio[index];
		portfolio[index] = null;
		if(selectedSpecibusIndex == index)
		{
			selectedSpecibusIndex = -1;
			armed = false;
		}
		return removed;
	}

	public void setSpecibus(StrifeSpecibus specibus, int index)
	{
		portfolio[index] = specibus;
	}

	public void clearPortfolio()
	{
		for(int i = 0; i < portfolio.length; i++)
			portfolio[i] = null;
	}

	public int getSpecibusIndex(StrifeSpecibus specibus)
	{
		for(int i = 0; i < portfolio.length; i++)
			if(portfolio[i] == specibus)
				return i;
		return -1;
	}

	public int getSelectedSpecibusIndex()
	{
		return selectedSpecibusIndex;
	}

	public void setSelectedSpecibusIndex(int index)
	{
		if(selectedSpecibusIndex != index)
		{
			selectedWeaponIndex = 0;
			selectedSpecibusIndex = index;
		}
	}

	public int getSelectedWeaponIndex()
	{
		return selectedWeaponIndex;
	}

	public void setSelectedWeaponIndex(int index)
	{
		this.selectedWeaponIndex = index;
	}

	public boolean isArmed()
	{
		return armed;
	}

	public void setArmed(boolean armed)
	{
		this.armed = armed;
	}

	public int getDroppedCards()
	{
		return droppedCards;
	}

	public void setDroppedCards(int droppedCards)
	{
		this.droppedCards = droppedCards;
	}

	public int getPrevSelectedSlot()
	{
		return prevSelectedSlot;
	}

	public void setPrevSelectedSlot(int slot)
	{
		this.prevSelectedSlot = slot;
	}

	public boolean abstrataSwitcherUnlocked()
	{
		return abstrataSwitcherUnlocked;
	}

	public void unlockAbstrataSwitcher(boolean unlocked)
	{
		this.abstrataSwitcherUnlocked = unlocked;
	}

	public boolean canStrife()
	{
		return strifeEnabled;
	}

	public void setStrifeEnabled(boolean canStrife)
	{
		this.strifeEnabled = canStrife;
	}

	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider provider)
	{
		CompoundTag nbt = new CompoundTag();

		ListTag list = new ListTag();
		for(StrifeSpecibus specibus : portfolio)
			list.add(specibus == null ? new CompoundTag() : specibus.serializeNBT(provider));
		nbt.put("Portfolio", list);

		nbt.putInt("SelectedSpecibus", selectedSpecibusIndex);
		nbt.putInt("SelectedWeapon", selectedWeaponIndex);
		nbt.putBoolean("Armed", armed);
		nbt.putInt("DroppedCards", droppedCards);
		nbt.putBoolean("AbstrataSwitcherUnlocked", abstrataSwitcherUnlocked);
		nbt.putBoolean("CanStrife", strifeEnabled);
		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt)
	{
		portfolio = new StrifeSpecibus[PORTFOLIO_SIZE];

		if(nbt.contains("Portfolio"))
		{
			ListTag list = nbt.getList("Portfolio", Tag.TAG_COMPOUND);
			for(int i = 0; i < list.size() && i < portfolio.length; i++)
			{
				CompoundTag tag = list.getCompound(i);
				if(!tag.isEmpty())
				{
					StrifeSpecibus specibus = StrifeSpecibus.empty();
					specibus.deserializeNBT(provider, tag);
					portfolio[i] = specibus;
				}
			}
		}

		selectedSpecibusIndex = nbt.getInt("SelectedSpecibus");
		selectedWeaponIndex = nbt.getInt("SelectedWeapon");
		armed = nbt.getBoolean("Armed");
		droppedCards = nbt.getInt("DroppedCards");
		abstrataSwitcherUnlocked = nbt.getBoolean("AbstrataSwitcherUnlocked");
		strifeEnabled = nbt.getBoolean("CanStrife");
	}
}
