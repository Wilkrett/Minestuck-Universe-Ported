package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * New "basic command" tech from the Time Aspect design discussion - "Steal Time": hold and aim at an
 * entity to afflict them with {@link MSUMobEffects#TIME_DILATION} (slower movement + attack speed - see
 * that class) while simultaneously buffing the caster with vanilla Speed + Haste for as long as held -
 * the target's time, taken and given to the caster. Not ported from anything; mechanically it's
 * {@code TechTimeSlow} and {@code TechTimeAccelerateSelf} combined into a single targeted tech, but the
 * target-facing half uses its own new potion effect (as explicitly asked for) rather than reusing
 * vanilla Slowness/Mining Fatigue the way {@code TechTimeSlow} does.
 * <p>
 * Same simple "must currently be looking at the target" requirement as {@code TechTimeSlow}/
 * {@code TechTimeAccelerateSelf} - no slot-tether "lock on and keep tracking even if you look away"
 * behavior like {@code TechTimeTickUp} has. Costs 2 food per 20 ticks (double {@code TechTimeSlow}'s
 * rate) since this is doing the combined work of both of those techs at once - a judgment call, not
 * derived from the design doc.
 */
public class TechTimeDilation extends TechHeroAspect
{
	public TechTimeDilation()
	{
		super(Minestuckuniverseported.id("time_dilation"), EnumAspect.TIME, 15000, MSUTechType.HYBRID); // new tech, no original cost to port - picked to fit this project's own cost spread, see class doc comment
		setIcon("default");
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.HELD)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 2)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);
		if(target == null)
			return true;

		target.addEffect(new MobEffectInstance(MSUMobEffects.TIME_DILATION, 20, 0, false, false));

		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20, 1, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 20, 1, false, false));

		MSUAbilitechParticles.aura(level, player, EnumAspect.TIME, 5);
		MSUAbilitechParticles.aura(level, target, EnumAspect.TIME, 5);

		if(time % 20 == 0 && !player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 2);

		return true;
	}
}
