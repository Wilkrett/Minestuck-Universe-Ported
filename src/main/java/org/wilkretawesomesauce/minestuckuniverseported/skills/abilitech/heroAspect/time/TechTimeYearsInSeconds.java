package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time;

import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * New "Years in Seconds" tech, user-requested, no original 1.12.2 counterpart. Press to instantly apply
 * {@link MSUMobEffects#TIME_DILATION} for {@link #EFFECT_DURATION_TICKS} (10 seconds) to every other
 * living entity within {@link #RADIUS} blocks - allies and hostiles alike, no targeting filter, matching
 * the literal "every entity nearby (besides yourself)" spec - while afflicting the caster with real
 * Weakness II and Slowness V for the same duration, the real cost of an AoE strong enough to hit everyone
 * around unfiltered. Gated by a flat {@link #COOLDOWN_TICKS} (1 minute) cooldown, tracked the same
 * tech-local, per-player {@code Map<UUID, Long>} idiom {@code TechKnightBloodBond#bonds}/
 * {@code TechTimeParallelAction#activeClones} already established for scratch state with exactly one
 * real consumer.
 */
public class TechTimeYearsInSeconds extends TechHeroAspect
{
	private static final double RADIUS = 10.0;
	private static final int EFFECT_DURATION_TICKS = 200;
	private static final int COOLDOWN_TICKS = 1200;
	private static final int ENERGY_USE = 6;

	private static final Map<UUID, Long> lastUseGameTime = new WeakHashMap<>();

	public TechTimeYearsInSeconds()
	{
		super(Minestuckuniverseported.id("years_in_seconds"), EnumAspect.TIME, 120000, MSUTechType.DEFENSE, EnumClass.MAGE); // new tech, no original cost to port - picked to fit this project's own cost spread, see class doc comment
		setIcon("default");
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.PRESS)
			return false;

		if(!(level instanceof ServerLevel serverLevel))
			return false;

		long now = serverLevel.getGameTime();
		Long lastUse = lastUseGameTime.get(player.getUUID());
		if(lastUse != null && now - lastUse < COOLDOWN_TICKS)
		{
			player.displayClientMessage(Component.translatable("status.minestuckuniverseported.yearsInSeconds.cooldown"), true);
			return false;
		}

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < ENERGY_USE)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		for(LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RADIUS), e -> e != player))
			target.addEffect(new MobEffectInstance(MSUMobEffects.TIME_DILATION, EFFECT_DURATION_TICKS, 0, false, false));

		player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EFFECT_DURATION_TICKS, 1, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_DURATION_TICKS, 4, false, false));

		if(!player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - ENERGY_USE);

		lastUseGameTime.put(player.getUUID(), now);
		MSUAbilitechParticles.oneshot(level, player, EnumAspect.TIME, 40);

		return true;
	}
}
