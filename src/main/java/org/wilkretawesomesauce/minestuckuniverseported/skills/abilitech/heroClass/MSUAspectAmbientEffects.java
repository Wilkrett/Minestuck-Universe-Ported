package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass;

import com.mraof.minestuck.player.Echeladder;
import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.Title;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.skills.MSUSkills;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;

import java.util.HashMap;
import java.util.Map;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code events.handlers.GTEventHandler#aspectEffects}/
 * {@code aspectStrength}/{@code getAspectEffects} - the real "ambient Title-Aspect buff, scaled by
 * Echeladder rung" table, previously flagged in this project as exclusively reachable through the
 * never-ported skills/badges economy (see the real potion/status-effects pass's own doc comment) - now
 * reachable for real, since several {@code heroClass} techs consume it directly
 * ({@code bard.TechBard}/{@code bard.TechBardMetronome}'s own borrowed instance/{@code maid.TechMaid}/
 * {@code muse.TechMuse}/{@code mage.TechMage} all read either the raw arrays or the computed map).
 * <p>
 * <b>Both original badge-gated branches are real now</b>: {@link MSUSkills#BADGE_PAGE} grants a flat +2
 * potion-level bonus; {@link MSUSkills#EFFECT_BUFF} swaps in a per-aspect special case for DOOM/HOPE/
 * MIND/VOID (real Absorption/{@link MSUMobEffects#DECAYPROOF}/{@link MSUMobEffects#MIND_FORTITUDE}/
 * {@link MSUMobEffects#CONCEAL}) and otherwise just doubles the default formula, matching the original
 * exactly.
 */
public final class MSUAspectAmbientEffects
{
	/** {@code Holder<MobEffect>} per aspect, in {@link EnumAspect} declaration order - matches the
	 * original's own array, just keyed by a real {@link Map} instead of trusting ordinal alignment. */
	private static final Map<EnumAspect, Holder<MobEffect>> EFFECTS = new HashMap<>();
	private static final Map<EnumAspect, Float> STRENGTH = new HashMap<>();

	static
	{
		EFFECTS.put(EnumAspect.BLOOD, MobEffects.ABSORPTION);
		EFFECTS.put(EnumAspect.BREATH, MobEffects.MOVEMENT_SPEED);
		EFFECTS.put(EnumAspect.DOOM, MobEffects.DAMAGE_RESISTANCE);
		EFFECTS.put(EnumAspect.HEART, MobEffects.ABSORPTION);
		EFFECTS.put(EnumAspect.HOPE, MobEffects.FIRE_RESISTANCE);
		EFFECTS.put(EnumAspect.LIFE, MobEffects.REGENERATION);
		EFFECTS.put(EnumAspect.LIGHT, MobEffects.LUCK);
		EFFECTS.put(EnumAspect.MIND, MobEffects.NIGHT_VISION);
		EFFECTS.put(EnumAspect.RAGE, MobEffects.DAMAGE_BOOST);
		EFFECTS.put(EnumAspect.SPACE, MobEffects.JUMP);
		EFFECTS.put(EnumAspect.TIME, MobEffects.DIG_SPEED);
		EFFECTS.put(EnumAspect.VOID, MobEffects.INVISIBILITY);

		STRENGTH.put(EnumAspect.BLOOD, 1.0F / 12);
		STRENGTH.put(EnumAspect.BREATH, 1.0F / 15);
		STRENGTH.put(EnumAspect.DOOM, 1.0F / 28);
		STRENGTH.put(EnumAspect.HEART, 1.0F / 25);
		STRENGTH.put(EnumAspect.HOPE, 1.0F / 18);
		STRENGTH.put(EnumAspect.LIFE, 1.0F / 20);
		STRENGTH.put(EnumAspect.LIGHT, 1.0F / 10);
		STRENGTH.put(EnumAspect.MIND, 1.0F / 12);
		STRENGTH.put(EnumAspect.RAGE, 1.0F / 25);
		STRENGTH.put(EnumAspect.SPACE, 1.0F / 10);
		STRENGTH.put(EnumAspect.TIME, 1.0F / 13);
		STRENGTH.put(EnumAspect.VOID, 1.0F / 12);
	}

	private MSUAspectAmbientEffects()
	{
	}

	public static Holder<MobEffect> effectFor(EnumAspect aspect)
	{
		return EFFECTS.get(aspect);
	}

	public static float strengthFor(EnumAspect aspect)
	{
		return STRENGTH.get(aspect);
	}

	/**
	 * Real port of {@code GTEventHandler#getAspectEffects(EntityPlayer)}, including both original
	 * badge-gated branches now - see this class's own doc comment. Empty if {@code player} has no Title
	 * yet. Rung and God Tier status are read directly from the real {@link Echeladder}/
	 * {@code godtier.GodTierData} attachment rather than pushed onto every caller.
	 */
	public static Map<Holder<MobEffect>, MobEffectInstance> getAspectEffects(ServerPlayer player)
	{
		Map<Holder<MobEffect>, MobEffectInstance> result = new HashMap<>();

		var title = Title.getTitle(player);
		if(title.isEmpty())
			return result;

		GodTierData godTier = player.getData(MSUAttachments.GOD_TIER);
		int rung = Echeladder.get(player).getRung();
		boolean isGodTier = godTier.isAscended();

		EnumAspect aspect = title.get().heroAspect();
		int potionLevel = (int) (strengthFor(aspect) * (isGodTier ? 60 : rung)) + (godTier.isBadgeActive(MSUSkills.BADGE_PAGE, player.level(), player) ? 2 : 0);

		if(godTier.isBadgeActive(MSUSkills.EFFECT_BUFF, player.level(), player))
		{
			switch(aspect)
			{
				case DOOM -> result.put(MobEffects.ABSORPTION, new MobEffectInstance(MobEffects.ABSORPTION, 600, 2, true, false));
				case HOPE -> result.put(MSUMobEffects.DECAYPROOF, new MobEffectInstance(MSUMobEffects.DECAYPROOF, 600, 0, true, false));
				case MIND -> result.put(MSUMobEffects.MIND_FORTITUDE, new MobEffectInstance(MSUMobEffects.MIND_FORTITUDE, 600, 0, true, false));
				case VOID -> {
					if(!(player.hasEffect(MobEffects.GLOWING) && player.getEffect(MobEffects.GLOWING).getAmplifier() >= 2))
						result.put(MSUMobEffects.CONCEAL, new MobEffectInstance(MSUMobEffects.CONCEAL, 600, 0, true, false));
				}
				default -> potionLevel *= 2;
			}
		}

		Holder<MobEffect> effect = effectFor(aspect);
		if(potionLevel > 0)
			result.put(effect, new MobEffectInstance(effect, 600, potionLevel - 1, true, false));

		if((isGodTier || rung > 18) && aspect == EnumAspect.HOPE)
			result.put(MobEffects.WATER_BREATHING, new MobEffectInstance(MobEffects.WATER_BREATHING, 600, 0, true, false));

		return result;
	}
}
