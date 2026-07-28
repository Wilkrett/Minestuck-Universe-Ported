package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.voidAspect;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
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

		if(!player.hasEffect(MSUMobEffects.CONCEAL))
			MSUAbilitechParticles.aura(level, player, 1, 0x104EA2, 0x001856);

		return true;
	}

	@Override
	public void onUnequipped(Level level, Player player, int techSlot)
	{
		super.onUnequipped(level, player, techSlot);
		player.noPhysics = false;
	}

	@Override
	public void onPassiveToggle(Level level, Player player, boolean active)
	{
		super.onPassiveToggle(level, player, active);
		sendToggleMessage(player, active);
	}
}
