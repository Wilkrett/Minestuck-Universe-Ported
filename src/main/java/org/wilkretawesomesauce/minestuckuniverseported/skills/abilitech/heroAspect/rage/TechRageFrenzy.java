package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.rage;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.entity.ai.AIRageFrenzyTarget;
import org.wilkretawesomesauce.minestuckuniverseported.entity.ai.EntityAIAttackRageShifted;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

import java.util.List;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.rage.TechRageFrenzy}
 * ("Frenzied Mayhem") - hold for 1 second, release to send every creature within
 * {@link #RADIUS} blocks berserk against anything nearby, itself included in each other's targeting
 * pool - real mutual chaos via {@link AIRageFrenzyTarget}, not just a status flag.
 * <p>
 * {@link #enableRageFrenzy}/{@link #onEntityJoinLevel} are ported directly onto this class, matching the
 * original exactly (its own real {@code enableRageFrenzy}/{@code onJoinWorld} live on
 * {@code TechRageFrenzy} itself, not a separate shared helper) - the original's own {@code heroAspect.rage}
 * folder holds nothing but {@code Tech} classes.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public class TechRageFrenzy extends TechHeroAspect
{
	private static final int RADIUS = 16;
	private static final int CHARGE_TICKS = 20;

	public TechRageFrenzy()
	{
		super(Minestuckuniverseported.id("frenzied_mayhem"), EnumAspect.RAGE, 1390, MSUTechType.UTILITY);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE || time > CHARGE_TICKS)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 5)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		MSUAbilitechParticles.aura(level, player, EnumAspect.RAGE, 2);

		if(time == CHARGE_TICKS)
		{
			List<Mob> nearby = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(RADIUS));

			for(Mob target : nearby)
			{
				if(!target.getData(MSUAttachments.BADGE_EFFECTS).isFrenzied())
					enableRageFrenzy(target);
				MSUAbilitechParticles.oneshot(level, target, EnumAspect.RAGE, 10);
			}

			if(!player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 5);

			MSUAbilitechParticles.burst(level, player, EnumAspect.RAGE, nearby.isEmpty() ? 1 : 4);
		}

		return true;
	}

	/** Ported from the original's own {@code TechRageFrenzy#enableRageFrenzy}. */
	static void enableRageFrenzy(Mob mob)
	{
		mob.getData(MSUAttachments.BADGE_EFFECTS).setFrenzied(true);
		mob.targetSelector.addGoal(1, new AIRageFrenzyTarget(mob));
		ensureAttackGoal(mob);
	}

	/** Adds this tech's own real melee attack goal ({@link EntityAIAttackRageShifted}) if the creature doesn't already have one - a modern addition, replacing the original's reflection-based {@code resetAI} pass, which no longer has an equivalent to port (modern {@code GoalSelector} already exposes plain public add/remove). */
	private static void ensureAttackGoal(Mob mob)
	{
		boolean hasAttackGoal = mob.goalSelector.getAvailableGoals().stream()
				.anyMatch(goal -> goal.getGoal() instanceof MeleeAttackGoal);

		if(!hasAttackGoal && mob instanceof PathfinderMob pathfinder)
			mob.goalSelector.addGoal(2, new EntityAIAttackRageShifted(pathfinder, 1.5, false));
	}

	/**
	 * Re-injects {@link AIRageFrenzyTarget} whenever a previously frenzied creature (re)loads - goals
	 * themselves aren't part of vanilla's own NBT persistence, only the
	 * {@code capabilities.badgeEffects.BadgeEffects} flag is (see that class's own doc comment for why
	 * it's persisted) - the modern equivalent of the original's own {@code onJoinWorld} handler.
	 */
	@SubscribeEvent
	private static void onEntityJoinLevel(EntityJoinLevelEvent event)
	{
		if(event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob))
			return;

		if(mob.getData(MSUAttachments.BADGE_EFFECTS).isFrenzied())
			enableRageFrenzy(mob);
	}
}
