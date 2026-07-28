package org.wilkretawesomesauce.minestuckuniverseported.capabilities.strife;
import org.wilkretawesomesauce.minestuckuniverseported.strife.StrifeSpecibus;
import org.wilkretawesomesauce.minestuckuniverseported.strife.KindAbstratus;

import com.mraof.minestuck.player.Echeladder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.wilkretawesomesauce.minestuckuniverseported.Config;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code capabilities.strife.StrifeData}/{@code IStrifeData}
 * Forge capability - real name match now (this class used to be called {@code StrifePortfolio}; renamed
 * for real, see {@link IStrifeData}'s own doc comment for why). In NeoForge 1.21.1, Forge
 * capabilities-on-entities were replaced by the data attachment system, so this is now a plain
 * {@link INBTSerializable} attached to entities via
 * {@link org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments#STRIFE_PORTFOLIO} instead of
 * a capability interface + provider pair.
 * <p>
 * Holds the "portfolio": the set of strife specibi (weapon decks) assigned to an entity, which one/slot
 * is currently selected, whether a weapon is currently armed (held), and the handful of related flags
 * the original tracked (strife card drop cooldown, abstrata switcher unlock, etc).
 * <p>
 * Bugfix vs. the original: {@code isPortfolioEmpty()} in 1.12.2 unconditionally returned {@code false}
 * (its final line was {@code return false;} instead of {@code return true;}), so it never actually
 * reported an empty portfolio. Fixed here.
 * <p>
 * {@link #canDropCards(ServerPlayer)} is a real port of the original's own mob-kill strife-card-drop cap
 * (gated on {@code Config.strifeCardMobDrops}, already present in this project's own {@code Config.java}
 * with a note that it "wasn't wired up yet") - the method itself is real now, but still has no real
 * caller: nothing in this project currently drops a {@code StrifeCardItem} from a mob kill at all (no
 * {@code LivingDropsEvent}/loot-table hook exists yet), so this is ready infrastructure, same category
 * as {@code godtier.MediumData}'s own Quest Bed position before anything consumed it.
 */
public class StrifeData implements IStrifeData, INBTSerializable<CompoundTag>
{
	public static final int PORTFOLIO_SIZE = 10;

	private StrifeSpecibus[] portfolio = new StrifeSpecibus[PORTFOLIO_SIZE];
	private int selectedSpecibusIndex = -1;
	private int selectedWeaponIndex = -1;
	private boolean armed = false;

	private int droppedCards = 0;
	private int prevSelSlot = 0;
	private boolean abstrataSwitcherUnlocked = false;
	private boolean strifeEnabled = false;

	@Override
	public StrifeSpecibus[] getPortfolio()
	{
		return portfolio;
	}

	/** Non-null, non-empty (has contents, or is an assigned fist kind) specibi only. */
	@Override
	public StrifeSpecibus[] getNonEmptyPortfolio()
	{
		List<StrifeSpecibus> result = new ArrayList<>();
		for(StrifeSpecibus specibus : portfolio)
			if(specibus != null && specibus.isAssigned()
					&& (specibus.getKindAbstratus().isFist() || !specibus.getContents().isEmpty()))
				result.add(specibus);
		return result.toArray(new StrifeSpecibus[0]);
	}

	@Override
	public boolean isPortfolioFull()
	{
		for(StrifeSpecibus specibus : portfolio)
			if(specibus == null)
				return false;
		return true;
	}

	@Override
	public boolean isPortfolioEmpty()
	{
		for(StrifeSpecibus specibus : portfolio)
			if(specibus != null)
				return false;
		return true;
	}

	@Override
	public boolean portfolioHasAbstratus(@Nullable KindAbstratus kindAbstratus)
	{
		if(kindAbstratus == null)
			return false;
		for(StrifeSpecibus specibus : portfolio)
			if(specibus != null && specibus.getKindAbstratus() == kindAbstratus)
				return true;
		return false;
	}

	@Override
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

	@Override
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

	@Override
	public void setSpecibus(StrifeSpecibus specibus, int index)
	{
		portfolio[index] = specibus;
	}

	@Override
	public void clearPortfolio()
	{
		for(int i = 0; i < portfolio.length; i++)
			portfolio[i] = null;
	}

	@Override
	public int getSpecibusIndex(StrifeSpecibus specibus)
	{
		for(int i = 0; i < portfolio.length; i++)
			if(portfolio[i] == specibus)
				return i;
		return -1;
	}

	@Override
	public int getSelectedSpecibusIndex()
	{
		return selectedSpecibusIndex;
	}

	@Override
	public void setSelectedSpecibusIndex(int index)
	{
		if(selectedSpecibusIndex != index)
		{
			selectedWeaponIndex = 0;
			selectedSpecibusIndex = index;
		}
	}

	@Override
	public int getSelectedWeaponIndex()
	{
		return selectedWeaponIndex;
	}

	@Override
	public void setSelectedWeaponIndex(int index)
	{
		this.selectedWeaponIndex = index;
	}

	@Override
	public boolean isArmed()
	{
		return armed;
	}

	@Override
	public void setArmed(boolean armed)
	{
		this.armed = armed;
	}

	@Override
	public int getDroppedCards()
	{
		return droppedCards;
	}

	@Override
	public void setDroppedCards(int droppedCards)
	{
		this.droppedCards = droppedCards;
	}

	/** Real port of the original's own mob-kill strife-card-drop cap - see this class's own doc comment
	 * for why it has no real caller yet. */
	@Override
	public boolean canDropCards(ServerPlayer owner)
	{
		if(owner instanceof FakePlayer)
			return false;
		return droppedCards < Math.max(Config.strifeCardMobDrops, Echeladder.get(owner).getRung() / 6);
	}

	@Override
	public int getPrevSelSlot()
	{
		return prevSelSlot;
	}

	@Override
	public void setPrevSelSlot(int slot)
	{
		this.prevSelSlot = slot;
	}

	@Override
	public boolean abstrataSwitcherUnlocked()
	{
		return abstrataSwitcherUnlocked;
	}

	@Override
	public void unlockAbstrataSwitcher(boolean unlocked)
	{
		this.abstrataSwitcherUnlocked = unlocked;
	}

	@Override
	public boolean canStrife()
	{
		return strifeEnabled;
	}

	@Override
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
