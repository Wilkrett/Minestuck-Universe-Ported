package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.blood;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.blood.TechBloodTransfusion}.
 * Hold and aim at a target: every {@link #DAMAGE_INTERVAL_TICKS}, the caster takes {@link #SELF_DAMAGE}
 * (their own lifeforce, paid directly rather than gated behind food like most other Time/Blood techs -
 * matches the original, which never charged food for this one either) and the target is healed
 * {@link #TARGET_HEAL} - or, if already at full health, granted/extended a stacking Health Boost and
 * topped off, exactly like the original.
 * <p>
 * The original's self-damage used a bespoke {@code DamageSource} with {@code setDamageBypassesArmor()} -
 * 1.21.1's damage sources are data-driven per {@code DamageType} (a real registry entry with its own
 * datapack-defined tags, not a builder flag), so standing up a whole new one just for this single flat
 * armor-bypass would be new infrastructure for a one-line behavior. {@code damageSources().magic()} is
 * already tagged {@code bypasses_armor} in vanilla's own data and reads thematically fine for "blood
 * magic self-harm" - reused here instead, same call this project's {@code TimeDilationLagEvents} already
 * uses for its own flat chip damage, rather than inventing a second custom damage type.
 * <p>
 * Its particle effect is tinted {@link EnumAspect#HEART}, not {@code BLOOD} - matches the original's own
 * source exactly (an apparent original quirk, not a transcription error here), so kept as-is rather than
 * "corrected" to the tech's own Blood tagging.
 */
public class TechBloodTransfusion extends TechHeroAspect
{
	private static final int DAMAGE_INTERVAL_TICKS = 10;
	private static final float SELF_DAMAGE = 8.0F;
	private static final float TARGET_HEAL = 4.0F;

	public TechBloodTransfusion()
	{
		super(Minestuckuniverseported.id("lifeforce_transfusion"), EnumAspect.BLOOD, 450000, MSUTechType.DEFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.HELD)
			return false;
		if(time % DAMAGE_INTERVAL_TICKS != 0)
			return false;
		if(!(level instanceof ServerLevel serverLevel))
			return false;

		LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);
		if(target == null)
		{
			MSUAbilitechParticles.aura(level, player, EnumAspect.HEART, 2);
			return true;
		}

		player.hurt(serverLevel.damageSources().magic(), SELF_DAMAGE);

		if(target.getHealth() >= target.getMaxHealth())
		{
			MobEffectInstance existing = target.getEffect(MobEffects.HEALTH_BOOST);
			int amplifier = existing != null ? existing.getAmplifier() + 1 : 0;
			target.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 12000, amplifier));
			target.heal(target.getMaxHealth());
		}
		else
			target.heal(TARGET_HEAL);

		MSUAbilitechParticles.aura(level, player, EnumAspect.HEART, 4);
		MSUAbilitechParticles.oneshot(level, target, EnumAspect.HEART, 2);

		return true;
	}
}
