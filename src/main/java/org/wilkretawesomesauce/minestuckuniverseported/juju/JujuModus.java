package org.wilkretawesomesauce.minestuckuniverseported.juju;

import com.mraof.minestuck.inventory.captchalogue.ArrayModus;
import com.mraof.minestuck.inventory.captchalogue.CaptchaDeckHandler;
import com.mraof.minestuck.inventory.captchalogue.Modus;
import com.mraof.minestuck.inventory.captchalogue.ModusType;
import com.mraof.minestuck.player.IdentifierHandler;
import com.mraof.minestuck.player.PlayerIdentifier;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.LogicalSide;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code captchalogue.JujuModus} (+ the list-storage half of its
 * parent {@code captchalogue.BaseModus}, which this port doesn't duplicate as a separate base class -
 * {@link ArrayModus}, Minestuck's own real, modern equivalent, already provides the same plain-list
 * storage {@code BaseModus} hand-rolled, confirmed via {@code javap} to be a real, non-final, extensible
 * class an addon can build on). Two players link their Juju modus decks: whatever you put in stays in your
 * own {@link #list} ({@link #putItemStack}, inherited unchanged from {@link ArrayModus}), but taking
 * something <i>out</i> ({@link #getItem}) always pulls from your <b>partner's</b> stash instead
 * ({@link #rawGetItem} on the partner instance - the modern equivalent of the original's
 * {@code getItemSuper}, bypassing this class's own override so the redirect doesn't loop). Both players see
 * the same real, modern array-shaped sylladex screen Minestuck itself provides
 * ({@code client.gui.captchalouge.ArraySylladexScreen}, which accepts any {@link Modus} and only needs
 * array-shaped storage underneath - not rebuilt here) rather than a new custom GUI.
 * <p>
 * {@link #link}/{@link #unlink}/{@link #getLinkPlayer} are ported closely, using the same real, still-present
 * {@link IdentifierHandler}/{@link PlayerIdentifier} API the original used (confirmed present in the modern
 * dependency jar, package moved from {@code com.mraof.minestuck.util} to {@code com.mraof.minestuck.player}) -
 * not replaced with a simpler raw-UUID scheme, since the real thing still exists and works. Exposed via
 * {@code command.JujuCommand} ({@code /msujuju link}/{@code unlink}) rather than the original's in-GUI
 * button - see that command's own doc comment for why.
 * <p>
 * {@link #cardTexIndex} is kept (read/written, matching the original's NBT shape) for state fidelity, but
 * nothing currently renders a different card texture off it - the original's own visual tweak lived inside
 * its custom {@code JujuGuiHandler}, which isn't ported now that this reuses Minestuck's real
 * {@code ArraySylladexScreen} instead (see this class's own top note).
 * <p>
 * <b>Known gap, confirmed via the real modern {@link Modus} signatures, not guessed</b>: the original
 * auto-detected and cleaned up a stale link from inside {@code readFromNBT} using its own stored
 * {@code this.player} field. Modern {@code Modus#readFromNBT}/{@code #canSwitchFrom} carry no player
 * reference at all (every method that needs one now takes it as an explicit parameter instead - a real,
 * deliberate modern API design difference, confirmed via {@code javap}), so that specific auto-unlink path
 * isn't reachable here. {@link #getItem} still defensively no-ops instead of crashing if the partner's modus
 * ever isn't a {@code JujuModus} anymore; {@code /msujuju unlink} is the always-available manual fix.
 */
public class JujuModus extends ArrayModus
{
	public int partnerID = -1;
	public int cardTexIndex = 0;

	public JujuModus(ModusType<? extends ArrayModus> type, LogicalSide side)
	{
		super(type, side);
	}

	public static boolean link(ServerPlayer self)
	{
		if(!(CaptchaDeckHandler.getModus(self) instanceof JujuModus selfModus))
			return false;

		ServerPlayer partner = findLinkPartner(self);
		if(partner == null)
		{
			self.displayClientMessage(Component.translatable("status.minestuckuniverseported.jujuModusLinkFail"), false);
			return false;
		}

		JujuModus partnerModus = (JujuModus) CaptchaDeckHandler.getModus(partner);

		selfModus.partnerID = IdentifierHandler.encode(partner).getId();
		selfModus.cardTexIndex = 1;
		partnerModus.partnerID = IdentifierHandler.encode(self).getId();
		partnerModus.cardTexIndex = 2;

		self.displayClientMessage(Component.translatable("status.minestuckuniverseported.jujuModusLink", partner.getName()), false);
		partner.displayClientMessage(Component.translatable("status.minestuckuniverseported.jujuModusLink", self.getName()), false);

		selfModus.markDirty();
		partnerModus.markDirty();
		selfModus.checkAndResend(self);
		partnerModus.checkAndResend(partner);

		return true;
	}

	public static boolean unlink(ServerPlayer self)
	{
		if(!(CaptchaDeckHandler.getModus(self) instanceof JujuModus selfModus) || selfModus.partnerID == -1)
			return false;

		PlayerIdentifier partnerId = IdentifierHandler.getById(selfModus.partnerID);
		ServerPlayer partner = partnerId == null ? null : partnerId.getPlayer(self.server);

		if(partner != null && CaptchaDeckHandler.getModus(partner) instanceof JujuModus partnerModus)
		{
			partnerModus.partnerID = -1;
			partnerModus.cardTexIndex = 0;
			partnerModus.markDirty();
			partnerModus.checkAndResend(partner);
			partner.displayClientMessage(Component.translatable("status.minestuckuniverseported.jujuModusUnlink", self.getName()), false);
		}

		selfModus.partnerID = -1;
		selfModus.cardTexIndex = 0;
		self.displayClientMessage(Component.translatable("status.minestuckuniverseported.jujuModusUnlink",
				partnerId == null ? "?" : partnerId.getUsername()), false);

		selfModus.markDirty();
		selfModus.checkAndResend(self);

		return true;
	}

	/** Ported from the original's {@code getLinkPlayer} - a random nearby player with an unlinked Juju modus. */
	private static ServerPlayer findLinkPartner(ServerPlayer self)
	{
		List<ServerPlayer> candidates = self.server.getPlayerList().getPlayers().stream()
				.filter(p -> p != self && CaptchaDeckHandler.getModus(p) instanceof JujuModus juju && juju.partnerID == -1)
				.toList();

		if(candidates.isEmpty())
			return null;

		return candidates.get(self.getRandom().nextInt(candidates.size()));
	}

	/** Used by {@code juju.JujuMenu} to display what this modus's partner currently has stashed. */
	public NonNullList<ItemStack> getPartnerItems(net.minecraft.server.MinecraftServer server)
	{
		PlayerIdentifier partnerId = IdentifierHandler.getById(partnerID);
		if(partnerId == null)
			return NonNullList.create();

		ServerPlayer partner = partnerId.getPlayer(server);
		Modus partnerModus = partner == null ? null : CaptchaDeckHandler.getModus(partner);
		return partnerModus == null ? NonNullList.create() : partnerModus.getItems();
	}

	@Nonnull
	@Override
	public ItemStack getItem(ServerPlayer player, int id, boolean asCard)
	{
		PlayerIdentifier partnerId = IdentifierHandler.getById(partnerID);
		if(partnerId == null)
			return ItemStack.EMPTY;

		ServerPlayer partner = partnerId.getPlayer(player.server);
		if(partner == null || !(CaptchaDeckHandler.getModus(partner) instanceof JujuModus partnerModus))
			return ItemStack.EMPTY;

		if(id == CaptchaDeckHandler.EMPTY_SYLLADEX)
		{
			ejectPartnerSylladex(partner, partnerModus);
			markDirty();
			return ItemStack.EMPTY;
		}

		ItemStack result = partnerModus.rawGetItem(partner, id, asCard);
		markDirty();
		partnerModus.markDirty();
		partnerModus.checkAndResend(partner);
		return result;
	}

	/** Modern equivalent of the original's {@code getItemSuper} - {@code ArrayModus}'s own, non-redirected retrieval. */
	protected ItemStack rawGetItem(ServerPlayer player, int id, boolean asCard)
	{
		return super.getItem(player, id, asCard);
	}

	private static void ejectPartnerSylladex(ServerPlayer partner, JujuModus partnerModus)
	{
		for(ItemStack item : partnerModus.list)
			CaptchaDeckHandler.launchAnyItem(partner, item);
		partnerModus.list.clear();
	}

	@Override
	public boolean putItemStack(ServerPlayer player, ItemStack stack)
	{
		boolean result = super.putItemStack(player, stack);
		if(result)
			markDirty();
		return result;
	}

	@Override
	public boolean increaseSize(ServerPlayer player)
	{
		boolean result = super.increaseSize(player);
		if(result)
			markDirty();
		return result;
	}

	@Override
	public void readFromNBT(CompoundTag nbt, HolderLookup.Provider provider)
	{
		super.readFromNBT(nbt, provider);
		partnerID = nbt.getInt("PartnerID");
		cardTexIndex = nbt.getInt("CardTexture");
	}

	@Override
	public CompoundTag writeToNBT(CompoundTag nbt, HolderLookup.Provider provider)
	{
		CompoundTag result = super.writeToNBT(nbt, provider);
		result.putInt("PartnerID", partnerID);
		result.putInt("CardTexture", cardTexIndex);
		return result;
	}
}
