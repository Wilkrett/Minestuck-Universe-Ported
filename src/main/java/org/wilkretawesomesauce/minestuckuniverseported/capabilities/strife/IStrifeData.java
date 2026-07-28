package org.wilkretawesomesauce.minestuckuniverseported.capabilities.strife;

import net.minecraft.server.level.ServerPlayer;
import org.wilkretawesomesauce.minestuckuniverseported.strife.KindAbstratus;
import org.wilkretawesomesauce.minestuckuniverseported.strife.StrifeSpecibus;

import javax.annotation.Nullable;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code capabilities.strife.IStrifeData} - the original also
 * extended {@code capabilities.IMSUCapabilityBase<EntityLivingBase>} for
 * {@code writeToNBT}/{@code readFromNBT}/{@code setOwner}; not repeated here since {@link StrifeData}
 * already implements NeoForge's own {@code INBTSerializable} directly (the same convention every real
 * capability class in this project's {@code capabilities} package uses now that Forge's
 * capability-interface plumbing has no NeoForge equivalent to mirror). The original's own
 * {@code writeSelectedIndexes}/{@code writePortfolio}/{@code writeDroppedCards}/{@code writeConfig}
 * partial-NBT helpers aren't declared here either - internal decomposition of the original's own
 * {@code writeToNBT()}, not real interface-level API surface (this port's {@link StrifeData#serializeNBT}
 * writes everything in one method instead, and this project's own sync packets serialize whatever subset
 * they need directly rather than calling back into capability-side partial-write helpers).
 */
public interface IStrifeData
{
	StrifeSpecibus[] getPortfolio();

	boolean isPortfolioFull();

	boolean isPortfolioEmpty();

	boolean portfolioHasAbstratus(@Nullable KindAbstratus abstratus);

	boolean addSpecibus(StrifeSpecibus specibus);

	@Nullable
	StrifeSpecibus removeSpecibus(int index);

	void setSpecibus(StrifeSpecibus specibus, int index);

	void clearPortfolio();

	boolean canStrife();

	void setStrifeEnabled(boolean canStrife);

	boolean abstrataSwitcherUnlocked();

	void unlockAbstrataSwitcher(boolean unlocked);

	int getDroppedCards();

	void setDroppedCards(int v);

	boolean canDropCards(ServerPlayer owner);

	int getSelectedSpecibusIndex();

	int getSelectedWeaponIndex();

	void setSelectedSpecibusIndex(int index);

	void setSelectedWeaponIndex(int index);

	boolean isArmed();

	void setArmed(boolean armed);

	int getPrevSelSlot();

	void setPrevSelSlot(int slot);

	StrifeSpecibus[] getNonEmptyPortfolio();

	int getSpecibusIndex(StrifeSpecibus specibus);
}
