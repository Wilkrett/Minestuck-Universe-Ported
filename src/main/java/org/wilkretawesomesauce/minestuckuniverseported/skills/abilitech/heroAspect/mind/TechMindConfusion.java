package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.mind;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.client.player.Input;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.mind.DecisionManager;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.mind.TechMindConfusion}
 * ("Sensory Break") - press and aim at a target to scramble their mind for 20 seconds
 * ({@link #DURATION_TICKS}, matching the original's 400-tick duration exactly), reversing their controls
 * for as long as it lasts. See {@link MindConfusionEffect}'s own doc comment for how the reversal itself
 * is implemented client-side.
 * <p>
 * <b>Real Resolve resistance</b>, from the later "Mind Aspect System Design" document (no 1.12.2
 * counterpart): {@code mechanics.mind.DecisionManager#resistsInfluence} - "harder to confuse" at high
 * Resolve - gets a real chance to reject the effect outright before it's ever applied, same shape and
 * same shared formula {@code heroAspect.mind.TechMindControl}'s own possession-attempt check uses.
 */
public class TechMindConfusion extends TechHeroAspect
{
	private static final int ENERGY_USE = 9;
	private static final int DURATION_TICKS = 400;

	public TechMindConfusion()
	{
		super(Minestuckuniverseported.id("sensory_break"), EnumAspect.MIND, 38780, MSUTechType.OFFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.PRESS)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < ENERGY_USE)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		MSUAbilitechParticles.aura(level, player, EnumAspect.MIND, 5);

		LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);
		if(target == null)
			return true;

		if(DecisionManager.resistsInfluence(target))
		{
			player.displayClientMessage(Component.translatable("status.mindResisted"), true);
			return true;
		}

		target.addEffect(new MobEffectInstance(MSUMobEffects.MIND_CONFUSION, DURATION_TICKS, 0));
		MSUAbilitechParticles.oneshot(level, target, EnumAspect.MIND, 10);

		if(!player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - ENERGY_USE);

		return true;
	}

	/**
	 * Marker effect - carries no attribute modifiers, real modern equivalent of the original's custom
	 * {@code MSUPotions.MIND_CONFUSION}. Whether a player has it is what {@link ClientEvents} checks to
	 * reverse their movement input, the same "marker effect + MovementInputUpdateEvent" pattern already
	 * used by Wind Vessel and Hopeful Outburst.
	 */
	public static class MindConfusionEffect extends MobEffect
	{
		public MindConfusionEffect()
		{
			super(MobEffectCategory.HARMFUL, 0x7B2FBE);
		}
	}

	/**
	 * Client-side half of this tech - reverses the local player's own movement input for as long as
	 * {@link MSUMobEffects#MIND_CONFUSION} is active, the same marker-effect-plus-
	 * {@code MovementInputUpdateEvent} pattern already used by Wind Vessel and Hopeful Outburst.
	 */
	@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
	public static final class ClientEvents
	{
		private ClientEvents()
		{
		}

		@SubscribeEvent
		private static void onMovementInput(MovementInputUpdateEvent event)
		{
			if(!event.getEntity().hasEffect(MSUMobEffects.MIND_CONFUSION))
				return;

			Input input = event.getInput();
			input.forwardImpulse *= -1F;
			input.leftImpulse *= -1F;
		}
	}
}
