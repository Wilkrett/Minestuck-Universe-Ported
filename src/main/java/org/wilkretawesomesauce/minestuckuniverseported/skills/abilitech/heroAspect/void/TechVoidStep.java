package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.voidAspect;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.voidAspect.TechVoidStep}
 * ("Voidstep") - passive: while enabled, phase through every block. Costs 1 food every 40 ticks,
 * matching the original exactly, and turns itself back off once out of food.
 * <p>
 * The original faked this three separate ways - forcing {@code noClip} via a living-update hook,
 * clearing the player's own collision box list on a Forge-1.12.2-only {@code GetCollisionBoxesEvent},
 * and cancelling a client-only push-out-of-blocks correction - because 1.12.2 had no single field that
 * did all of it at once for a non-spectator player. Modern {@link Player#noPhysics} (confirmed via
 * {@code javap}) is the real, direct equivalent of all three at once: setting it true is already enough
 * to let a survival-mode player pass through blocks exactly like a spectator does, so there's nothing
 * left needing separate event hooks. The original's flying/wind-formed gate was already commented out
 * in the shipped source (grep confirms it, not just in this reading) - preserved as commented-out here
 * too, not re-added: Void Step just always phases through blocks while toggled on, gravity included, so
 * standing still on solid ground while it's active really does mean falling straight through the floor.
 * That's the original's actual, intentional risk, not a bug this port introduced.
 * <p>
 * Now also emits the original's own ambient aura particles (alternating between two literal colors,
 * matching the original's real one-off call exactly rather than this aspect's own registered table
 * entry) - skipped while {@link MSUMobEffects#CONCEAL} is active, matching the original's own
 * "don't show particles while concealed" check.
 * <p>
 * <b>Real bug fix, from a live report ("void step doesn't work")</b>: setting {@code player.noPhysics}
 * only ever touched the <i>server's</i> own {@code Player} instance - the whole Abilitech tick framework
 * ({@code SkillKeyStates#onPlayerTick}) is explicitly server-only, and {@code Entity#noPhysics} is a
 * plain, unsynced field, so the real connected client never found out Void Step was active and kept
 * resolving its own local collision normally. See {@link VoidStepEffect}'s own doc comment for the full
 * explanation. Fixed the same way {@code breath.TechBreathWindVessel} already had to solve this exact
 * problem: {@link MSUMobEffects#VOID_STEP} is now applied every held tick as a plain marker potion effect
 * (auto-synced to the client for free), which {@link VoidStepClientEvents} reads to set the client's own
 * copy of {@code noPhysics} too.
 */
public class TechVoidStep extends TechHeroAspect
{
	public TechVoidStep()
	{
		super(Minestuckuniverseported.id("voidstep"), EnumAspect.VOID, 190000, MSUTechType.PASSIVE);
	}

	@Override
	public boolean onPassiveTick(Level level, Player player, int techSlot)
	{
		if(!player.isCreative() && player.tickCount % 40 == 1)
		{
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);
			if(player.getFoodData().getFoodLevel() < 1)
			{
				player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
				return false;
			}
		}

		player.noPhysics = true;
		player.addEffect(new MobEffectInstance(MSUMobEffects.VOID_STEP, 20, 0, true, false));

		if(!player.hasEffect(MSUMobEffects.CONCEAL))
			MSUAbilitechParticles.aura(level, player, 1, 0x104EA2, 0x001856);

		return true;
	}

	@Override
	public void onUnequipped(Level level, Player player, int techSlot)
	{
		super.onUnequipped(level, player, techSlot);
		player.noPhysics = false;
		player.removeEffect(MSUMobEffects.VOID_STEP);
	}

	@Override
	public void onPassiveToggle(Level level, Player player, boolean active)
	{
		super.onPassiveToggle(level, player, active);
		sendToggleMessage(player, active);
	}

	/**
	 * Marker effect - carries no attribute modifiers or tick behavior of its own, it exists purely so
	 * "is this player currently voidstepping" is automatically network-synced to every observing client
	 * for free (the same way any potion effect already is), which {@link ClientEvents} needs to set
	 * {@link Player#noPhysics} on the client's own copy of the player - same real shape as
	 * {@code breath.TechBreathWindVessel}'s own marker/client-events pair.
	 */
	public static class VoidStepEffect extends MobEffect
	{
		public VoidStepEffect()
		{
			super(MobEffectCategory.BENEFICIAL, 0x104EA2);
		}
	}

	/**
	 * Client-side half of this tech - sets the client's own copy of {@link Player#noPhysics} when the
	 * synced {@link MSUMobEffects#VOID_STEP} marker is present, since that field isn't otherwise exposed
	 * through any event - hooked at {@link PlayerTickEvent.Post} (after {@code Player#tick()}'s own
	 * unconditional {@code this.noPhysics = this.isSpectator()} reset for that same tick, so this actually
	 * sticks instead of being immediately overwritten) on the client only. Fires for every client-visible
	 * {@code Player} entity that ticks locally (the real local player, and any other nearby real players
	 * also voidstepping), not just the local one - correct either way, since the marker effect is only
	 * ever actually applied to whoever equipped the tech.
	 */
	@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
	public static final class ClientEvents
	{
		private ClientEvents()
		{
		}

		@SubscribeEvent
		private static void onPlayerTick(PlayerTickEvent.Post event)
		{
			Player player = event.getEntity();
			if(player.hasEffect(MSUMobEffects.VOID_STEP))
				player.noPhysics = true;
		}
	}
}
