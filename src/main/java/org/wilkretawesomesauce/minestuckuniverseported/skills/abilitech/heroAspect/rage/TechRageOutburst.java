package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.rage;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;

/**
 * Ported 1:1 from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.rage.TechRageOutburst}
 * ("Vengeful Outburst") - hold for half a second, release to hit every non-allied player/hostile mob
 * within 16 blocks with real armor-bypassing damage (reusing {@code damageSources().magic()}, the same
 * reuse {@code TechBloodTransfusion}/{@code TimeDilationLagEvents} already established for exactly this
 * need rather than a new custom {@code DamageType}), scaled off the caster's real
 * {@link GodTierData#getTempKarma()} exactly like the original ({@code max(20, abs(tempKarma)/2)}) -
 * see that class's own doc comment for what this project's Karma economy does and doesn't cover.
 */
public class TechRageOutburst extends TechHeroAspect
{
	private static final int CHARGE_TICKS = 10;
	private static final double RADIUS = 16;

	public TechRageOutburst()
	{
		super(Minestuckuniverseported.id("vengeful_outburst"), EnumAspect.RAGE, 25740, MSUTechType.OFFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE)
			return false;
		if(!(level instanceof ServerLevel serverLevel))
			return false;

		float damage = Math.max(20, Math.abs(player.getData(MSUAttachments.GOD_TIER).getTempKarma()) / 2F);

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < damage / 2)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(time < CHARGE_TICKS)
		{
			MSUAbilitechParticles.burst(level, player, EnumAspect.RAGE, 5);
			return true;
		}

		if(state != AbilitechKeyState.RELEASED)
		{
			MSUAbilitechParticles.aura(level, player, EnumAspect.RAGE, 10);
			return true;
		}

		for(LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RADIUS),
				e -> e != player && (e instanceof Player || e instanceof Enemy)))
		{
			if(!player.isAlliedTo(target))
				target.hurt(serverLevel.damageSources().magic(), damage);
		}

		if(!player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - (int)(damage * 0.75F));

		return true;
	}
}
