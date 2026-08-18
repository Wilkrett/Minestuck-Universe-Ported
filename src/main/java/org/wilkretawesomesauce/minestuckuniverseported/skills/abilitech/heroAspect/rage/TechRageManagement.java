package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.rage;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.badgeEffects.BadgeEffects;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.entity.ai.EntityAIAttackRageShifted;
import org.wilkretawesomesauce.minestuckuniverseported.util.AspectColorHandler;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

import java.util.List;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.rage.TechRageManagement}
 * ("Anger Management") - quick tap and release (under 2 seconds) toggles a single targeted creature's
 * hostility towards players/Iron Golems on or off; holding for the full 2 seconds instead toggles every
 * creature within {@link #RADIUS} blocks at once, capped by available food. Turning hostility <i>off</i>
 * is kept exactly as drastic as the original - it wipes the creature's entire goal/target selector, not
 * just the goals this tech itself added (see {@link #clearAllGoals}'s own doc comment for why no
 * reflection is needed for that anymore).
 * <p>
 * {@link #enableRageShift}/{@link #onEntityJoinLevel}/{@link #onEntityTick} are ported directly onto this
 * class, matching the original exactly (its own real {@code enableRageShift}/{@code onJoinWorld}/
 * {@code onLivingTick} live on {@code TechRageManagement} itself, not a separate shared helper) - the
 * original's own {@code heroAspect.rage} folder holds nothing but {@code Tech} classes.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public class TechRageManagement extends TechHeroAspect
{
	private static final int RADIUS = 16;
	private static final int SINGLE_TARGET_WINDOW = 40;

	public TechRageManagement()
	{
		super(Minestuckuniverseported.id("anger_management"), EnumAspect.RAGE, 1240, MSUTechType.UTILITY);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 3)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(time < SINGLE_TARGET_WINDOW)
		{
			if(state == AbilitechKeyState.RELEASED)
			{
				LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);
				if(!(target instanceof Mob mob))
					return false;

				toggleRageShift(mob);
				MSUAbilitechParticles.oneshot(level, mob, EnumAspect.RAGE, 10);
				if(!player.isCreative())
					player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 3);
			}

			MSUAbilitechParticles.aura(level, player, EnumAspect.RAGE, 5);
		}
		else if(time == SINGLE_TARGET_WINDOW)
		{
			List<Mob> nearby = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(RADIUS));
			int count = 0;

			for(Mob target : nearby)
			{
				if(!player.isCreative() && player.getFoodData().getFoodLevel() < 3)
					break;

				toggleRageShift(target);
				MSUAbilitechParticles.oneshot(level, target, EnumAspect.RAGE, 10);
				count++;

				if(!player.isCreative())
					player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 3);
			}

			if(count == 0 && !nearby.isEmpty())
			{
				player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
				return false;
			}

			MSUAbilitechParticles.burst(level, player, EnumAspect.RAGE, nearby.isEmpty() ? 1 : 4);
		}

		return true;
	}

	private static void toggleRageShift(Mob mob)
	{
		BadgeEffects badgeEffects = mob.getData(MSUAttachments.BADGE_EFFECTS);

		if(!badgeEffects.isRageShifted() && !badgeEffects.isFrenzied())
			enableRageShift(mob);
		else
		{
			clearAllGoals(mob);
			badgeEffects.setRageShifted(false);
			badgeEffects.setFrenzied(false);
		}
	}

	/** Ported from the original's own {@code TechRageManagement#enableRageShift}. */
	static void enableRageShift(Mob mob)
	{
		mob.getData(MSUAttachments.BADGE_EFFECTS).setRageShifted(true);
		mob.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(mob, Player.class, true));
		mob.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(mob, IronGolem.class, true));
		ensureAttackGoal(mob);
	}

	/** Matches the original's own drastic {@code disableRageShift} - wipes every goal outright, not just the ones this class added. */
	static void clearAllGoals(Mob mob)
	{
		for(WrappedGoal goal : List.copyOf(mob.targetSelector.getAvailableGoals()))
			mob.targetSelector.removeGoal(goal.getGoal());
		for(WrappedGoal goal : List.copyOf(mob.goalSelector.getAvailableGoals()))
			mob.goalSelector.removeGoal(goal.getGoal());
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
	 * Re-injects the hostile-targeting goals whenever a previously rage-shifted creature (re)loads - goals
	 * themselves aren't part of vanilla's own NBT persistence, only the
	 * {@code capabilities.badgeEffects.BadgeEffects} flag is (see that class's own doc comment for why
	 * it's persisted) - the modern equivalent of the original's own {@code onJoinWorld} handler.
	 */
	@SubscribeEvent
	private static void onEntityJoinLevel(EntityJoinLevelEvent event)
	{
		if(event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob))
			return;

		if(mob.getData(MSUAttachments.BADGE_EFFECTS).isRageShifted())
			enableRageShift(mob);
	}

	/**
	 * Ports the original's own {@code onLivingTick} ambient particle handler 1:1 - any frenzied
	 * <i>or</i> rage-shifted creature has a flat 5% chance per tick to emit a single particle in one of
	 * {@link EnumAspect#RAGE}'s two colors, picked at random each time (matching the original's own
	 * {@code rand.nextInt(colors.length)} pick, not always the same color). The original placed this
	 * single combined handler on this class specifically (not {@code TechRageFrenzy}, and not split into
	 * two), so this does the same rather than duplicating it.
	 */
	@SubscribeEvent
	private static void onEntityTick(EntityTickEvent.Post event)
	{
		if(event.getEntity().level().isClientSide() || !(event.getEntity() instanceof Mob mob))
			return;

		BadgeEffects badgeEffects = mob.getData(MSUAttachments.BADGE_EFFECTS);
		if((badgeEffects.isFrenzied() || badgeEffects.isRageShifted()) && mob.getRandom().nextFloat() < 0.05f)
		{
			int[] colors = AspectColorHandler.get(EnumAspect.RAGE);
			MSUAbilitechParticles.oneshot(mob.level(), mob, 1, colors[mob.getRandom().nextInt(colors.length)]);
		}
	}
}
