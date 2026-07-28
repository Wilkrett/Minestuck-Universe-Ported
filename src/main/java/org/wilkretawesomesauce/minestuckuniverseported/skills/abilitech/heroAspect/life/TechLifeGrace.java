package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.life;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.AbilitechLoadout;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported 1:1 from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.life.TechLifeGrace}
 * ("Saving Grace") - hold for 3 seconds, release while aiming at a target to cast a protective ward on
 * them: the next time they'd die, {@link SavingGraceEvents} cancels it, fully heals them, and grants a
 * stacking Absorption buff instead. A target already warded (checked via the marker effect) can't be
 * warded again, and the caster can't re-grant to a target they've already granted to and hasn't yet had
 * their grant consumed (tracked via {@link AbilitechLoadout#getSavingGraceTargets()}) - both real,
 * matching the original's two-layer check.
 * <p>
 * Also records the caster in {@link SavingGraceEvents#recordCaster} (real, project-original addition,
 * not part of the original tech) purely so a real Rescue relationship (see that class's own doc comment)
 * can be credited at the moment the ward actually triggers, which may be long after this cast.
 */
public class TechLifeGrace extends TechHeroAspect
{
	private static final int CHARGE_TICKS = 60;
	private static final int WARD_DURATION_TICKS = 1200000;

	public TechLifeGrace()
	{
		super(Minestuckuniverseported.id("saving_grace"), EnumAspect.LIFE, 890000, MSUTechType.DEFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() <= 0)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(time >= CHARGE_TICKS && state == AbilitechKeyState.RELEASED)
		{
			LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);
			AbilitechLoadout badgeEffects = player.getData(MSUAttachments.ABILITECH_LOADOUT);

			if(target == null || badgeEffects.getSavingGraceTargets().contains(target.getUUID()) || target.hasEffect(MSUMobEffects.SAVING_GRACED))
				return false;

			target.addEffect(new MobEffectInstance(MSUMobEffects.SAVING_GRACED, WARD_DURATION_TICKS, 0, true, false));
			badgeEffects.getSavingGraceTargets().add(target.getUUID());
			SavingGraceEvents.recordCaster(target.getUUID(), player.getUUID());
			MSUAbilitechParticles.oneshot(level, target, EnumAspect.LIFE, 20);
		}

		MSUAbilitechParticles.aura(level, player, EnumAspect.LIFE, time < CHARGE_TICKS ? 2 : 10);

		return true;
	}
}
