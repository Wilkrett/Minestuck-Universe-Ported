package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.doom;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.doom.TechDoomChain}
 * ("Chains of Despair") - two-stage AoE debuff. Pressing immediately applies a first debuff to everyone
 * within {@link #RADIUS}; holding to ~1 second applies a stronger second one on top.
 * <p>
 * Now using the original's own two real custom effects instead of this project's earlier vanilla
 * Slowness/Weakness stand-in: {@link MSUMobEffects#EARTHBOUND} (disables flight) on press, then
 * {@link MSUMobEffects#BUILD_INHIBIT} (disables building) added on top of that if held past
 * {@link #HOLD_THRESHOLD_TICKS} - see {@code potions.EarthboundEffect}/{@code potions.BuildInhibitEffect}'s
 * own doc comments for what each really does. See {@link AbilityRestoreEvents}'s own doc comment for how
 * this tech's two effects get their suppressed flight/build abilities back once they wear off.
 */
public class TechDoomChain extends TechHeroAspect
{
	private static final double RADIUS = 20.0;
	private static final int HOLD_THRESHOLD_TICKS = 18;

	public TechDoomChain()
	{
		super(Minestuckuniverseported.id("chains_of_despair"), EnumAspect.DOOM, 8780, MSUTechType.UTILITY);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.PRESS)
		{
			if(!player.isCreative() && player.getFoodData().getFoodLevel() < 4)
			{
				player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
				return false;
			}

			for(LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RADIUS)))
			{
				target.addEffect(new MobEffectInstance(MSUMobEffects.EARTHBOUND, 60, 0, false, false));
				MSUAbilitechParticles.oneshot(level, target, EnumAspect.DOOM, 10);
			}

			if(!player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 4);

			MSUAbilitechParticles.burst(level, player, EnumAspect.DOOM, 10);
		}

		if(state == AbilitechKeyState.NONE || time >= 19)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 4)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(time >= HOLD_THRESHOLD_TICKS)
		{
			for(LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RADIUS)))
			{
				target.addEffect(new MobEffectInstance(MSUMobEffects.EARTHBOUND, 60, 0, false, false));
				target.addEffect(new MobEffectInstance(MSUMobEffects.BUILD_INHIBIT, 60, 0, false, false));
				MSUAbilitechParticles.oneshot(level, target, EnumAspect.DOOM, 10);
			}

			if(!player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 4);

			MSUAbilitechParticles.burst(level, player, EnumAspect.DOOM, 20);
		}

		MSUAbilitechParticles.aura(level, player, EnumAspect.DOOM, 10);

		return true;
	}

	/**
	 * Restores a player's flight/building abilities once {@code MSUMobEffects#EARTHBOUND}/
	 * {@code MSUMobEffects#BUILD_INHIBIT} (this tech's own two stages, the only place either effect is
	 * ever granted) actually wear off. Neither effect's own {@code applyEffectTick} can do this itself -
	 * {@link net.minecraft.world.effect.MobEffect#removeAttributeModifiers(net.minecraft.world.entity.ai.attributes.AttributeMap)}
	 * (confirmed real via this project's pinned NeoForge source, the same hook {@code potions.BerserkEffect}
	 * already uses for its own cleanup) only ever receives an {@code AttributeMap}, which has no
	 * back-reference to the owning entity - there's no way to reach a {@code Player} from it to flip an
	 * ability field back. Tracking "was suppressed last tick, isn't now" here instead sidesteps that gap
	 * entirely. Both original potions (see {@code potions.PotionFlight}/{@code PotionBuildInhibit}) never
	 * restored these fields at all once applied - a real bug in the original this port doesn't reproduce,
	 * since it would otherwise permanently strip a creative player's flight (or anyone's building) after a
	 * single, brief debuff.
	 */
	@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
	public static final class AbilityRestoreEvents
	{
		private static final Set<UUID> earthboundSuppressed = new HashSet<>();
		private static final Set<UUID> buildInhibitSuppressed = new HashSet<>();

		private AbilityRestoreEvents()
		{
		}

		@SubscribeEvent
		private static void onPlayerTick(PlayerTickEvent.Post event)
		{
			if(!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide())
				return;

			UUID id = player.getUUID();

			if(player.hasEffect(MSUMobEffects.EARTHBOUND))
				earthboundSuppressed.add(id);
			else if(earthboundSuppressed.remove(id))
			{
				player.getAbilities().mayfly = player.isCreative() || player.isSpectator();
				player.onUpdateAbilities();
			}

			if(player.hasEffect(MSUMobEffects.BUILD_INHIBIT))
				buildInhibitSuppressed.add(id);
			else if(buildInhibitSuppressed.remove(id))
			{
				player.getAbilities().mayBuild = true;
				player.onUpdateAbilities();
			}
		}
	}
}
