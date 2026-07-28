package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.sylph;

import com.mraof.minestuck.player.EnumClass;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.AbilitechLoadout;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUClassColors;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.AbilitechTargetedEvent;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroClass.TechSylph} ("Sylph's Mend") -
 * hold while looking at a hurt (or hungry) target to lock onto them (the same real per-slot tether
 * {@code time.TechTimeTickUp} already established, via {@link AbilitechLoadout#getTether}), healing 2
 * HP and 1 hunger point every second at 1 food/second cost to the caster.
 */
public class TechSylph extends TechHeroClass
{
	public TechSylph()
	{
		super(Minestuckuniverseported.id("sylph_mend"), EnumClass.SYLPH, 995000, MSUTechType.DEFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		AbilitechLoadout badgeEffects = player.getData(MSUAttachments.ABILITECH_LOADOUT);

		if(state == AbilitechKeyState.RELEASED)
			badgeEffects.setTether(techSlot, null);

		if(state != AbilitechKeyState.HELD)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 1)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		LivingEntity target = badgeEffects.getTether(techSlot) instanceof LivingEntity living ? living : null;
		if(target == null)
		{
			target = MSUAbilitechRayTrace.getTargetEntity(player);
			badgeEffects.setTether(techSlot, target);
		}

		boolean needsMending = target != null && (target.getHealth() < target.getMaxHealth() || (target instanceof Player targetPlayer && targetPlayer.getFoodData().needsFood()));
		if(!needsMending)
			return false;

		if(NeoForge.EVENT_BUS.post(new AbilitechTargetedEvent(player, target, this, techSlot, true)).isCanceled())
			return false;

		MSUAbilitechParticles.aura(level, player, 5, MSUClassColors.get(EnumClass.SYLPH));

		if(time % 20 == 0)
		{
			target.heal(2);
			if(target instanceof Player targetPlayer && targetPlayer.getFoodData().needsFood())
				targetPlayer.getFoodData().eat(1, 1);

			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);
			if(level instanceof ServerLevel serverLevel)
				serverLevel.sendParticles(ParticleTypes.HEART, target.getX() + (Math.random() - 0.5) / 2, target.getY() + 1.5, target.getZ() + (Math.random() - 0.5) / 2, 1, 1, 0, 0.5, 0);
		}

		return true;
	}

	@Override
	public boolean isUsableExternally(Level level, Player player)
	{
		return player.getFoodData().getFoodLevel() >= 1 && super.isUsableExternally(level, player);
	}
}
