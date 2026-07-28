package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.heart;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.AbilitechLoadout;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported (simplified) from MinestuckUniverse (1.12.2)'s
 * {@code skills.abilitech.heroAspect.heart.TechHeartBond} ("Spiritual Bond") - hold and aim at a target
 * to link your souls: for as long as the link holds, both your health pools are continuously averaged
 * together, every tick - hurt them and you feel it, heal them and so do you.
 * <p>
 * The original required 20 ticks of continuous aim before the link actually locked on (tracked via a
 * custom "soul link intent" counter on {@code IBadgeEffects}), enforced mutual exclusivity (a target
 * already linked to someone else, or already linking someone else, couldn't be re-linked), and had an
 * entire death-prevention mechanic for linked entities - which was already fully commented out in the
 * original source itself, dead code, not something this port is removing. This version locks on
 * immediately on press (reusing {@code AbilitechLoadout}'s existing slot-tether, the same mechanism
 * {@code TechTimeTickUp} already uses) and drops the exclusivity bookkeeping - nothing else in this
 * project's Heart aspect creates a second, competing link, so it wasn't worth the added state.
 */
public class TechHeartBond extends TechHeroAspect
{
	public TechHeartBond()
	{
		super(Minestuckuniverseported.id("spiritual_bond"), EnumAspect.HEART, 34600, MSUTechType.HYBRID);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		AbilitechLoadout badgeEffects = player.getData(MSUAttachments.ABILITECH_LOADOUT);

		if(state == AbilitechKeyState.NONE || state == AbilitechKeyState.RELEASED)
		{
			badgeEffects.setTether(techSlot, null);
			return false;
		}

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 1)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			badgeEffects.setTether(techSlot, null);
			return false;
		}

		Entity tether = badgeEffects.getTether(techSlot);
		LivingEntity target = tether instanceof LivingEntity livingTether && livingTether.isAlive() ? livingTether : null;

		if(target == null && state == AbilitechKeyState.PRESS)
		{
			LivingEntity raytraced = MSUAbilitechRayTrace.getTargetEntity(player);
			if(raytraced != null && raytraced != player)
			{
				badgeEffects.setTether(techSlot, raytraced);
				target = raytraced;
			}
		}

		if(target == null || !player.isAlive())
			return false;

		float linkedPercent = (player.getHealth() / player.getMaxHealth() + target.getHealth() / target.getMaxHealth()) / 2.0F;
		player.setHealth(player.getMaxHealth() * linkedPercent);
		target.setHealth(target.getMaxHealth() * linkedPercent);

		if(time % 20 == 0 && !player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);

		MSUAbilitechParticles.aura(level, player, EnumAspect.HEART, 3);
		MSUAbilitechParticles.oneshot(level, target, EnumAspect.HEART, 3);

		return true;
	}

	@Override
	public void onUnequipped(Level level, Player player, int techSlot)
	{
		player.getData(MSUAttachments.ABILITECH_LOADOUT).setTether(techSlot, null);
	}
}
