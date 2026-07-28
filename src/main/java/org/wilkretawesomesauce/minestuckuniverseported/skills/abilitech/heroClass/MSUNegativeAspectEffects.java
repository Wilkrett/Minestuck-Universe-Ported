package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code events.handlers.BadgeEventHandler#NEGATIVE_EFFECTS} - a
 * real per-{@link EnumAspect} debuff table several {@code heroClass} techs apply to a target (e.g.
 * {@code bard.TechBard}'s "curse" branch, {@code heir.TechHeir}'s retaliation, {@code lord.TechLord}'s AoE,
 * {@code maid.TechMaid}'s non-player-target branch, {@code witch.TechWitchTrap}). Returns a fresh
 * {@link MobEffectInstance} each call rather than caching one (a {@code MobEffectInstance}'s own duration
 * counts down once applied, so a shared instance would be the wrong thing to hand out repeatedly).
 * <p>
 * Uses this project's own already-real custom effects where the original did ({@link MSUMobEffects#EARTHBOUND}/
 * {@link MSUMobEffects#BLEEDING}/{@link MSUMobEffects#GOD_TIER_LOCK}), vanilla equivalents everywhere else -
 * same "real port, not reinvention" standard as the rest of this project's effects work.
 */
public final class MSUNegativeAspectEffects
{
	private MSUNegativeAspectEffects()
	{
	}

	public static MobEffectInstance get(EnumAspect aspect)
	{
		return switch(aspect)
		{
			case BREATH -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 1200, 2);
			case SPACE -> new MobEffectInstance(MSUMobEffects.EARTHBOUND, 400, 0);
			case BLOOD -> new MobEffectInstance(MSUMobEffects.BLEEDING, 600, 1);
			case RAGE -> new MobEffectInstance(MobEffects.WEAKNESS, 1200, 1);
			case HOPE -> new MobEffectInstance(MSUMobEffects.GOD_TIER_LOCK, 600, 0);
			case DOOM -> new MobEffectInstance(MobEffects.WITHER, 600, 1);
			case HEART -> new MobEffectInstance(MobEffects.HUNGER, 1000, 3);
			case LIFE -> new MobEffectInstance(MobEffects.POISON, 1200, 1);
			case VOID -> new MobEffectInstance(MobEffects.BLINDNESS, 1200, 2);
			case LIGHT -> new MobEffectInstance(MobEffects.UNLUCK, 800, 11);
			case TIME -> new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 1200, 2);
			case MIND -> new MobEffectInstance(MobEffects.CONFUSION, 1200, 0);
		};
	}
}
