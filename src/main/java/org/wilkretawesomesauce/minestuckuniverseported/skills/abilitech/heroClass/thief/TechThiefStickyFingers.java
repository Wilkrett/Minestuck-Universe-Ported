package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.thief;

import com.mraof.minestuck.player.EnumClass;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.ClasspectColorHandler;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;

/**
 * New "Sticky Fingers" Thief tech, user-requested, no original 1.12.2 counterpart. Press to steal from
 * the item in your main hand and give to the item in your off-hand, across all three attribute
 * categories the user asked for, each independently gated on whether both stacks are actually eligible:
 * <ul>
 *     <li><b>Durability</b> - moves as much remaining durability as it can from main-hand to off-hand in
 *     one go (capped by what main-hand can give without breaking outright, and by what off-hand actually
 *     needs), for any pair of damageable items.</li>
 *     <li><b>Enchantments</b> - every enchantment on main-hand is upgraded onto off-hand (per-enchantment
 *     max of the two, via {@link ItemEnchantments.Mutable#upgrade}, not a flat overwrite) and then
 *     cleared from main-hand entirely - a full transfer, not a partial drain, since enchant levels don't
 *     have a clean "steal part of it" reading the way durability does. Gated on
 *     {@link EnchantmentHelper#canStoreEnchantments} so this only ever touches items that can actually
 *     carry enchantments (tools/armor/books).</li>
 *     <li><b>Potion effects</b> - moves the real {@link DataComponents#POTION_CONTENTS} data component
 *     (which potion, custom effects, color) from main-hand to off-hand, leaving main-hand's own contents
 *     empty - only when both stacks are potion-family items to begin with (checked via
 *     {@link ItemStack#has}, which is only ever true by default for Potion/Splash/Lingering/Tipped-Arrow
 *     items).</li>
 * </ul>
 * Each category is independent - a pair of plain undamaged, unenchanted swords with no potion contents
 * simply does nothing and reports as much, rather than three separate no-op messages.
 */
public class TechThiefStickyFingers extends TechHeroClass
{
	private static final int DURABILITY_RESERVE = 1;
	private static final int ENERGY_USE = 4;

	public TechThiefStickyFingers()
	{
		super(Minestuckuniverseported.id("sticky_fingers"), EnumClass.THIEF, 8000, MSUTechType.UTILITY); // new tech, no original cost to port - picked to fit this project's own cost spread, see class doc comment
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.PRESS)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < ENERGY_USE)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		ItemStack main = player.getMainHandItem();
		ItemStack off = player.getOffhandItem();
		if(main.isEmpty() || off.isEmpty())
		{
			player.displayClientMessage(Component.translatable("status.minestuckuniverseported.stickyFingers.needBothHands"), true);
			return false;
		}

		boolean stole = stealDurability(main, off);
		stole |= stealEnchantments(main, off);
		stole |= stealPotionContents(main, off);

		if(!stole)
		{
			player.displayClientMessage(Component.translatable("status.minestuckuniverseported.stickyFingers.nothingToSteal"), true);
			return false;
		}

		if(!player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - ENERGY_USE);

		MSUAbilitechParticles.oneshot(level, player, 20, ClasspectColorHandler.get(EnumClass.THIEF));

		return true;
	}

	private static boolean stealDurability(ItemStack main, ItemStack off)
	{
		if(!main.isDamageableItem() || !off.isDamageableItem())
			return false;

		int available = main.getMaxDamage() - main.getDamageValue() - DURABILITY_RESERVE;
		int needed = off.getDamageValue();
		int amount = Math.min(available, needed);
		if(amount <= 0)
			return false;

		main.setDamageValue(main.getDamageValue() + amount);
		off.setDamageValue(off.getDamageValue() - amount);
		return true;
	}

	private static boolean stealEnchantments(ItemStack main, ItemStack off)
	{
		ItemEnchantments mainEnchants = EnchantmentHelper.getEnchantmentsForCrafting(main);
		if(mainEnchants.isEmpty() || !EnchantmentHelper.canStoreEnchantments(off))
			return false;

		EnchantmentHelper.updateEnchantments(off, mutable ->
		{
			for(var entry : mainEnchants.entrySet())
				mutable.upgrade(entry.getKey(), entry.getIntValue());
		});
		EnchantmentHelper.updateEnchantments(main, mutable -> mutable.removeIf(holder -> true));
		return true;
	}

	private static boolean stealPotionContents(ItemStack main, ItemStack off)
	{
		if(!main.has(DataComponents.POTION_CONTENTS) || !off.has(DataComponents.POTION_CONTENTS))
			return false;

		PotionContents mainPotion = main.get(DataComponents.POTION_CONTENTS);
		if(mainPotion == null || mainPotion.equals(PotionContents.EMPTY))
			return false;

		off.set(DataComponents.POTION_CONTENTS, mainPotion);
		main.set(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
		return true;
	}

	@Override
	public boolean isUsableExternally(Level level, Player player)
	{
		return player.getFoodData().getFoodLevel() >= ENERGY_USE && super.isUsableExternally(level, player);
	}
}
