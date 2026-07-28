package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.hope;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.hope.TechHopePrayers}
 * ("Answered Prayers") - hold for 1 second to bless every player within {@link #RADIUS} (not yourself)
 * with a random beneficial potion effect; keep holding to 2 seconds and bless yourself too.
 * <p>
 * The original built its random-beneficial-effect pool by iterating the entire potion registry and
 * filtering {@code !potion.isBadEffect()}. {@link MobEffect#isBeneficial()} is the same check on the
 * modern registry ({@link BuiltInRegistries#MOB_EFFECT}), so this ports directly with no need to
 * hand-curate a replacement list.
 */
public class TechHopePrayers extends TechHeroAspect
{
	private static final double RADIUS = 20.0;
	private static final int AOE_TRIGGER_TICKS = 20;
	private static final int SELF_TRIGGER_TICKS = 40;

	public TechHopePrayers()
	{
		super(Minestuckuniverseported.id("answered_prayers"), EnumAspect.HOPE, 65555, MSUTechType.DEFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE || time > SELF_TRIGGER_TICKS)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 6)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(time == AOE_TRIGGER_TICKS)
		{
			MobEffect effect = randomBeneficialEffect(player);
			for(Player target : level.getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(RADIUS), e -> e != player))
			{
				target.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), effect.isInstantenous() ? 0 : 300, 2, false, false));
				MSUAbilitechParticles.oneshot(level, target, EnumAspect.HOPE, 10);
			}

			if(!player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 8);

			MSUAbilitechParticles.burst(level, player, EnumAspect.HOPE, 10);
		}
		else if(time == SELF_TRIGGER_TICKS)
		{
			MobEffect effect = randomBeneficialEffect(player);
			player.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), effect.isInstantenous() ? 0 : 300, 2, false, false));

			if(!player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 6);
		}

		MSUAbilitechParticles.aura(level, player, EnumAspect.HOPE, time < 20 ? 2 : 6);

		return true;
	}

	private static MobEffect randomBeneficialEffect(Player player)
	{
		List<MobEffect> beneficial = new ArrayList<>();
		for(MobEffect effect : BuiltInRegistries.MOB_EFFECT)
			if(effect.isBeneficial())
				beneficial.add(effect);

		return beneficial.get(player.level().getRandom().nextInt(beneficial.size()));
	}
}
