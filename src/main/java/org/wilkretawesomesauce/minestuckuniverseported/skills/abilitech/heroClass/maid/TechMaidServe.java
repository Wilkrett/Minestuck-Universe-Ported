package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.maid;

import com.mraof.minestuck.player.EnumClass;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.events.AbilitechTargetedEvent;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.MSUAspectAmbientEffects;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;

import java.util.List;
import java.util.Map;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroClass.TechMaidServe} ("Irradiant
 * Servitude") - passive: every tick, refreshes the real caster's own {@link MSUAspectAmbientEffects}
 * buffs onto every nearby (3-block) animal and player. Toggled through this project's own real GUI
 * shift-click passive toggle (see {@code abilitech.Abilitech}'s own doc comment), matching the established
 * pattern every other passive {@code heroAspect}/{@code heroClass} tech in this project already uses -
 * {@code onUseTick} isn't overridden at all (defaults to inert), unlike the original's own key-press toggle.
 * <p>
 * {@link #REFRESH_POTIONS} is the original's own real {@code GTEventHandler#REFRESH_POTIONS} allowlist
 * (Absorption/Regeneration/Wither/Poison/{@code GOD_TIER_COMEBACK}) - these five never get topped up
 * while still active (unlike every other effect, which refreshes once its remaining duration/amplifier
 * dips too low), and only get reapplied at all once absent on a real 600-tick cadence rather than
 * instantly the tick they wear off, matching the original's exact real condition.
 */
public class TechMaidServe extends TechHeroClass
{
	private static final double RADIUS = 3;
	private static final int LOW_DURATION_THRESHOLD = 20;
	private static final List<Holder<MobEffect>> REFRESH_POTIONS = List.of(
			MobEffects.ABSORPTION, MobEffects.REGENERATION, MobEffects.WITHER, MobEffects.POISON, MSUMobEffects.GOD_TIER_COMEBACK);

	public TechMaidServe()
	{
		super(Minestuckuniverseported.id("irradiant_servitude"), EnumClass.MAID, 95500, MSUTechType.PASSIVE, MSUTechType.DEFENSE);
	}

	@Override
	public boolean onPassiveTick(Level level, Player player, int techSlot)
	{
		if(!(player instanceof ServerPlayer serverPlayer))
			return false;

		Map<Holder<MobEffect>, MobEffectInstance> buffs = MSUAspectAmbientEffects.getAspectEffects(serverPlayer);
		if(buffs.isEmpty())
			return false;

		for(LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RADIUS), e -> e != player))
		{
			if(!(target instanceof Animal) && !(target instanceof Player))
				continue;
			if(target instanceof Player && NeoForge.EVENT_BUS.post(new AbilitechTargetedEvent(player, target, this, techSlot, true)).isCanceled())
				continue;

			for(Map.Entry<Holder<MobEffect>, MobEffectInstance> entry : buffs.entrySet())
			{
				Holder<MobEffect> effect = entry.getKey();
				MobEffectInstance current = target.getEffect(effect);
				boolean isRefreshPotion = REFRESH_POTIONS.contains(effect);
				int threshold = effect == MobEffects.NIGHT_VISION ? 200 : LOW_DURATION_THRESHOLD;

				boolean shouldApply = current == null
						? (!isRefreshPotion || target.tickCount % 600 == 0)
						: (current.getDuration() <= threshold && current.getAmplifier() <= entry.getValue().getAmplifier() && !isRefreshPotion);

				if(shouldApply)
					target.addEffect(new MobEffectInstance(effect, effect == MobEffects.NIGHT_VISION ? 300 : 100, entry.getValue().getAmplifier(), true, true));
			}
		}

		return true;
	}

	@Override
	public boolean isUsableExternally(Level level, Player player)
	{
		return false;
	}

	@Override
	public void onPassiveToggle(Level level, Player player, boolean active)
	{
		super.onPassiveToggle(level, player, active);
		sendToggleMessage(player, active);
	}
}
