package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.mind;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.MSUSkills;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.badgeEffects.BadgeEffects;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported 1:1 from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.mind.TechMindStrike}
 * ("Calculated Strike") - hold to charge, release to lock in a damage multiplier that spends itself on the
 * first {@code LivingIncomingDamageEvent} this player causes afterwards. The charge-time formula, the
 * sine-wave power curve, and the low/med/high status thresholds are all kept exactly as sourced, including
 * the original's own raw {@code IBadgeEffects#getCalculating()} tick counter - now
 * {@link BadgeEffects#getCalculating()}/{@link BadgeEffects#setCalculating(int)}, matching every
 * other real ported {@code IBadgeEffects} field this project consolidated onto that class (see its own doc
 * comment) - rather than a synced {@code MobEffect}, since both this method and {@link #onIncomingDamage}
 * are entirely server-side and never needed the client to know this value at all. This also restores the
 * original's real decay rule, which a prior potion-duration stand-in didn't faithfully reproduce (a
 * {@code MobEffectInstance}'s duration ticks down every tick unconditionally): the counter only decays by 1
 * per tick while idle ({@link AbilitechKeyState#NONE}), not while actively charging.
 */
public class TechMindStrike extends TechHeroAspect
{
	public TechMindStrike()
	{
		super(Minestuckuniverseported.id("calculated_strike"), EnumAspect.MIND, 62330, MSUTechType.OFFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		BadgeEffects badgeEffects = player.getData(MSUAttachments.BADGE_EFFECTS);

		if(state == AbilitechKeyState.NONE)
		{
			if(badgeEffects.getCalculating() > 0)
				badgeEffects.setCalculating(badgeEffects.getCalculating() - 1);
			return false;
		}

		if(!player.isCreative())
		{
			if(player.getFoodData().getFoodLevel() < 1)
			{
				player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
				return false;
			}
			if(time % 20 == 0)
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);
		}

		if(state == AbilitechKeyState.RELEASED)
		{
			badgeEffects.setCalculating(Math.max(time + badgeEffects.getCalculating(), 100));
			int calculating = badgeEffects.getCalculating();

			double power = Math.sin(calculating * 1.1 + Math.PI * 1.5) / 2 + calculating * 0.017 + 0.5;
			if(power < 0.8)
				player.displayClientMessage(Component.translatable("status.calculatedStrike.low"), true);
			else if(power < 1.2)
				player.displayClientMessage(Component.translatable("status.calculatedStrike.med"), true);
			else
				player.displayClientMessage(Component.translatable("status.calculatedStrike.high"), true);
			MSUAbilitechParticles.oneshot(level, player, EnumAspect.MIND, 2);
			return true;
		}

		MSUAbilitechParticles.aura(level, player, EnumAspect.MIND, 2);
		return true;
	}

	@SubscribeEvent
	private static void onIncomingDamage(LivingIncomingDamageEvent event)
	{
		if(!(event.getSource().getEntity() instanceof Player attacker))
			return;

		GodTierData godTier = attacker.getData(MSUAttachments.GOD_TIER);
		if(!godTier.isTechEquipped(MSUSkills.MIND_STRIKE))
			return;

		BadgeEffects badgeEffects = attacker.getData(MSUAttachments.BADGE_EFFECTS);
		int calculating = badgeEffects.getCalculating();
		if(calculating <= 0)
			return;

		double power = Math.sin(calculating * 1.1 + Math.PI * 1.5) / 2 + calculating * 0.017 + 0.5;

		event.setAmount((float)(event.getAmount() * Math.max(2, power)));
		badgeEffects.setCalculating(0);
	}
}
