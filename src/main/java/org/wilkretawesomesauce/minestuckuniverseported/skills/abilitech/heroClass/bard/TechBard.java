package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.bard;

import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import com.mraof.minestuck.player.Title;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUClassColors;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.AbilitechTargetedEvent;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.MSUAspectAmbientEffects;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.MSUNegativeAspectEffects;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;

import java.util.Optional;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroClass.TechBard} ("Bardic
 * Dissonance") - a tap ability that randomly curses or blesses everyone nearby (10x1x10 around the
 * caster), based on the caster's own Title aspect: 50/50 per target, either a doubled-strength
 * {@link MSUNegativeAspectEffects} debuff, or a copy of the caster's own {@link MSUAspectAmbientEffects}
 * buffs (RAGE-flavored Strength IV as a fallback if the caster has no Title yet, matching the original
 * exactly).
 */
public class TechBard extends TechHeroClass
{
	public TechBard()
	{
		super(Minestuckuniverseported.id("bard_dissonance"), EnumClass.BARD, 4000);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.PRESS)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 6)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		Optional<Title> title = player instanceof ServerPlayer serverPlayer ? Title.getTitle(serverPlayer) : Optional.empty();

		for(LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(10, 1, 10), e -> e != player))
		{
			boolean negative = level.getRandom().nextBoolean();
			if(NeoForge.EVENT_BUS.post(new AbilitechTargetedEvent(player, target, this, techSlot, !negative)).isCanceled())
				continue;

			if(negative)
			{
				EnumAspect aspect = title.map(Title::heroAspect).orElse(EnumAspect.RAGE);
				MobEffectInstance base = MSUNegativeAspectEffects.get(aspect);
				target.addEffect(new MobEffectInstance(base.getEffect(), base.getDuration() * 2, base.getAmplifier() * 2));
			}
			else
			{
				if(title.isEmpty())
					target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 9));
				else if(player instanceof ServerPlayer serverPlayer)
					for(MobEffectInstance effect : MSUAspectAmbientEffects.getAspectEffects(serverPlayer).values())
						target.addEffect(new MobEffectInstance(effect.getEffect(), 1200, 9));

				MSUAbilitechParticles.oneshot(level, target, 10, MSUClassColors.get(EnumClass.MUSE));
			}
		}

		if(!player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 6);

		MSUAbilitechParticles.burst(level, player, 20, MSUClassColors.get(EnumClass.BARD));

		return true;
	}

	@Override
	public boolean isUsableExternally(Level level, Player player)
	{
		return player.getFoodData().getFoodLevel() >= 6 && super.isUsableExternally(level, player);
	}
}
