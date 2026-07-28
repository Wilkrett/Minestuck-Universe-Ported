package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.page.doom;

import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.Config;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.doom.DoomReleasePool;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;

/**
 * "Doom Reservoir" - new tech, ported from the "Doom Class Abilities Framework" design document (no
 * 1.12.2 original), Page of Doom's Core ability: "the Page absorbs and stores Doom within
 * themselves... holding Doom that would normally disperse." A direct, literal consumer of the base Doom
 * system's own harvest API - a passive toggle (same shape as
 * {@code heroClass.prince.blood.TechPrinceBloodSchism}'s Schism Aura forwarding) that, every
 * {@link Config#doomReservoirHarvestIntervalTicks}, auto-harvests from {@code mechanics.doom.DoomReleasePool}
 * within {@link Config#doomReservoirHarvestRadius} of the Page and adds whatever was actually available
 * (up to {@link Config#doomReservoirHarvestAmountPerPulse}) straight into their own Doom - exactly the
 * base spec's own "Doom Harvest" section, now with a real, standing consumer.
 * <p>
 * Priced in the low-hundred-thousands - a real, passive economy engine, but far below Page's own
 * existing ultimate ({@code TechPagePerseverantAwakening}, 1,000,000) since it has no direct combat
 * payoff on its own.
 */
public class TechPageDoomReservoir extends TechHeroClass
{
	public TechPageDoomReservoir()
	{
		super(Minestuckuniverseported.id("doom_reservoir"), EnumClass.PAGE, EnumAspect.DOOM, 300000, MSUTechType.PASSIVE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		return false;
	}

	@Override
	public boolean onPassiveTick(Level level, Player player, int techSlot)
	{
		if(!(player instanceof ServerPlayer) || !(level instanceof ServerLevel serverLevel))
			return false;

		if(serverLevel.getGameTime() % Config.doomReservoirHarvestIntervalTicks != 0)
			return false;

		DoomReleasePool pool = serverLevel.getData(MSUAttachments.DOOM_RELEASE_POOL);
		double harvested = pool.harvest(player.blockPosition(), Config.doomReservoirHarvestRadius, Config.doomReservoirHarvestAmountPerPulse);
		if(harvested > 0)
		{
			player.getData(MSUAttachments.DOOM_DATA).addDoom(harvested);
			MSUAbilitechParticles.aura(level, player, EnumAspect.DOOM, 5);
		}

		return false;
	}

	@Override
	public void onPassiveToggle(Level level, Player player, boolean active)
	{
		sendToggleMessage(player, active);
	}
}
