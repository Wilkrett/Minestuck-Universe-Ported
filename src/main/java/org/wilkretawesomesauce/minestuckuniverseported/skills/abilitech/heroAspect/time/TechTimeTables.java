package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported (simplified) from MinestuckUniverse (1.12.2)'s {@code TechTimeTables}. Hold and aim at something
 * to rewind it: zombies get babied (matches the original - a little joke about zombie aging being
 * backwards), ageable mobs get younger, damaged items get partially repaired, and a dropped raw chicken
 * turns back into a live chicken.
 * <p>
 * Not ported: the requirement to hold a Timetable item (that item doesn't exist yet, and per the
 * sandbox-mode scope decision this isn't gated), the {@code TimetableEffectEvent}/{@code AbilitechTargetedEvent}
 * hooks (nothing posts to them here), and the "deconstruct a damaged unrepairable item back into its
 * crafting ingredients" branch - that leaned on 1.12.2's {@code IRecipe} API in a way that doesn't map
 * cleanly onto the modern recipe system, and it's the least central part of what this tech does.
 */
public class TechTimeTables extends TechHeroAspect
{
	public TechTimeTables()
	{
		super(Minestuckuniverseported.id("rapid_rewind"), EnumAspect.TIME, 1000, MSUTechType.UTILITY);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 1)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(time % 10 != 0)
			return true;

		Entity target = null;
		if(MSUAbilitechRayTrace.getMouseOver(player, player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE)) instanceof EntityHitResult entityHit)
			target = entityHit.getEntity();

		boolean handled = applyEffect(level, target);

		if(handled && !player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);

		MSUAbilitechParticles.aura(level, player, EnumAspect.TIME, target == null ? 1 : 5);

		return true;
	}

	private static boolean applyEffect(Level level, Entity target)
	{
		if(target instanceof Zombie zombie && !zombie.isBaby())
		{
			zombie.setBaby(true);
			return true;
		}
		else if(target instanceof Chicken chicken && chicken.getAge() < -600)
		{
			level.addFreshEntity(new ItemEntity(level, target.getX(), target.getY(), target.getZ(), new ItemStack(Items.EGG)));
			target.discard();
			return true;
		}
		else if(target instanceof AgeableMob ageable)
		{
			ageable.ageUp(-60, false);
			return true;
		}
		else if(target instanceof ItemEntity itemEntity)
		{
			ItemStack stack = itemEntity.getItem();
			if(stack.isDamageableItem() && stack.isDamaged())
			{
				stack.setDamageValue(Math.max(0, stack.getDamageValue() - 3));
				return true;
			}
			else if(stack.getItem() == Items.CHICKEN)
			{
				Chicken chicken = new Chicken(EntityType.CHICKEN, level);
				chicken.setPos(target.getX(), target.getY(), target.getZ());
				level.addFreshEntity(chicken);
				stack.shrink(1);
				return true;
			}
		}
		return false;
	}
}
