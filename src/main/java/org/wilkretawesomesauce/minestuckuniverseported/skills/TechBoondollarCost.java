package org.wilkretawesomesauce.minestuckuniverseported.skills;

import com.mraof.minestuck.player.PlayerBoondollars;
import com.mraof.minestuck.player.PlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.Abilitech;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;

/**
 * Real port of MinestuckUniverse (1.12.2)'s {@code skills.TechBoondollarCost} - the real per-tech
 * unlock-cost layer, sitting between {@link Abilitech} and {@code abilitech.heroAspect.TechHeroAspect}/
 * the still-unported {@code TechHeroClass}, matching the original's actual inheritance chain exactly
 * (this project previously skipped this class entirely, going straight
 * {@code TechHeroAspect extends Abilitech}).
 * <p>
 * Real currency wiring, confirmed via {@code javap} against this project's real Minestuck dependency
 * jar (not guessed): {@link PlayerData#get(ServerPlayer)} returns an {@code Optional}, and
 * {@link PlayerBoondollars#getBoondollars}/{@link PlayerBoondollars#tryTakeBoondollars} are the real
 * balance-read/spend methods - the same currency this project's own {@code Echeladder.get(player)
 * .getRung()} calls elsewhere already prove is real and reachable. Client-side calls (e.g. from a future
 * shop screen's "can afford" preview) fall back to {@code com.mraof.minestuck.player.ClientPlayerData
 * #getBoondollars()}, the real client-cached balance Minestuck itself already syncs for its own HUD -
 * server-side is still the only place a purchase is ever actually authorized (see
 * {@code network.SkillShopRequestPackets}, the real enforcement point once the shop exists).
 * <p>
 * {@link #requiredStacks} holds {@code Supplier<ItemStack>}, not a resolved {@code ItemStack}, deliberately -
 * every concrete tech's own constructor runs during {@code skills.MSUSkills}'s static init, which NeoForge
 * triggers during mod construction (see {@code Minestuckuniverseported}'s own constructor), well before
 * any {@code DeferredItem}'s underlying registry entry is actually bound. Resolving a required item's real
 * {@code Item}/{@code ItemStack} eagerly in a tech constructor throws a real
 * "trying to access unbound value" crash at mod-load time - confirmed the hard way (a real crash report,
 * not a guess) when {@code abilitech.TechDragonAura}/{@code TechReturn} first called {@code MSUItems.X.get()}
 * directly in their own constructors. Every call site below resolves the supplier lazily instead, by which
 * point (any real gameplay method call, always well after {@code RegisterEvent}) the registry is safely
 * populated.
 */
public class TechBoondollarCost extends Abilitech
{
	public long cost;
	public final List<Supplier<ItemStack>> requiredStacks = new ArrayList<>();
	public boolean needsReqStacks = true;
	protected MSUTechType techType;

	public TechBoondollarCost(ResourceLocation id, long cost)
	{
		super(id);
		this.cost = cost;
	}

	public TechBoondollarCost(ResourceLocation id, long cost, MSUTechType techType)
	{
		this(id, cost);
		this.techType = techType;
	}



	@Override
	public Component getUnlockRequirements()
	{
		if(requiredStacks.isEmpty())
			return cost == 0
					? Component.translatable("tech.unlock.free")
					: Component.translatable("tech.unlock.boondollar", cost);

		MutableComponent text = Component.empty();
		for(Supplier<ItemStack> stackSupplier : requiredStacks)
		{
			ItemStack stack = stackSupplier.get();
			text.append(Component.translatable("tech.unlock.list.entry", stack.getCount(), stack.getHoverName()));
		}
		if(cost > 0)
			text.append(Component.translatable("tech.unlock.boondollar", cost));
		return text;
	}

	@Override
	public boolean canUnlock(Level level, Player player)
	{
		boolean hasItems = true;
		for(Supplier<ItemStack> stackSupplier : requiredStacks)
			hasItems = hasItems && findItem(player, stackSupplier.get(), false);

		if(!hasItems)
			return false;

		if(player instanceof ServerPlayer serverPlayer)
			return PlayerData.get(serverPlayer).map(data -> PlayerBoondollars.getBoondollars(data) >= cost).orElse(false);

		return com.mraof.minestuck.player.ClientPlayerData.getBoondollars() >= cost;
	}

	@Override
	public void onUnlock(Level level, Player player)
	{
		if(player instanceof ServerPlayer serverPlayer)
			PlayerData.get(serverPlayer).ifPresent(data -> PlayerBoondollars.tryTakeBoondollars(data, cost, true));

		for(Supplier<ItemStack> stackSupplier : requiredStacks)
			findItem(player, stackSupplier.get(), true);
	}

	@Override
	public boolean canAppearOnList(Level level, Player player)
	{
		if(super.canAppearOnList(level, player) && (!needsReqStacks || requiredStacks.isEmpty()))
			return true;

		boolean hasItems = true;
		for(Supplier<ItemStack> stackSupplier : requiredStacks)
		{
			ItemStack single = stackSupplier.get().copy();
			single.setCount(1);
			hasItems = hasItems && findItem(player, single, false);
		}
		return hasItems;
	}

	/**
	 * Real port of the original's {@code Badge#findItem} - kept here too (not only on the still-to-be-built
	 * {@code Badge} base class) since {@link TechBoondollarCost#requiredStacks} needs the exact same
	 * inventory-stack lookup/consumption the original's own {@code TechBoondollarCost} reused from
	 * {@code Badge} directly.
	 */
	public static boolean findItem(Player player, ItemStack stack, boolean decrement)
	{
		ItemStack remaining = stack.copy();
		for(int i = 0; i < player.getInventory().getContainerSize(); i++)
		{
			ItemStack invStack = player.getInventory().getItem(i);
			if(invStack.isEmpty() || !ItemStack.isSameItem(invStack, remaining))
				continue;

			if(remaining.getCount() > invStack.getCount())
			{
				remaining.shrink(invStack.getCount());
				if(decrement)
					invStack.setCount(0);
			}
			else
			{
				if(decrement)
					invStack.shrink(remaining.getCount());
				return true;
			}
		}
		return false;
	}

	public MSUTechType getTechType()
	{
		return techType;
	}
}
