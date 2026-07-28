package org.wilkretawesomesauce.minestuckuniverseported.mechanics.doom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Config;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Death releases bound Doom - it does not create any (see the design doc's own "Death" section).
 * Original design for this project, no 1.12.2 counterpart.
 * <p>
 * Three outcomes at death, in priority order: (1) sealed Doom is lost with the body entirely (a
 * ward against manipulation while alive, not a death-survival exploit - see {@link DoomData}'s own
 * doc comment on {@code isSealed}); (2) a {@link DoomMarkType#DEAD_SHUFFLE}-marked victim's Doom
 * redirects straight to the caster, if the caster is still alive and resolvable, bypassing the pool
 * entirely; (3) otherwise the Doom becomes an unbound {@link DoomReleaseRecord} in the level's
 * {@link DoomReleasePool}, harvestable for {@link Config#doomHarvestWindowTicks} before it silently
 * dissipates.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class DoomReleaseEvents
{
	private DoomReleaseEvents()
	{
	}

	@SubscribeEvent
	private static void onDeath(LivingDeathEvent event)
	{
		LivingEntity victim = event.getEntity();
		if(!(victim.level() instanceof ServerLevel level))
			return;

		DoomData data = victim.getData(MSUAttachments.DOOM_DATA);
		double amount = data.getDoom();
		if(amount <= 0)
			return;

		if(data.isSealed())
		{
			data.setDoom(0);
			return;
		}

		if(data.getMarkType() == DoomMarkType.DEAD_SHUFFLE && data.getMarkCasterId() != null)
		{
			LivingEntity caster = resolveLivingEntity(level, data.getMarkCasterId());
			if(caster != null && caster.isAlive())
			{
				caster.getData(MSUAttachments.DOOM_DATA).addDoomRaw(amount);
				data.setDoom(0);
				return;
			}
			// Caster not resolvable/not alive - falls through to the normal release-pool path below
			// rather than silently discarding the Doom.
		}

		DoomReleasePool pool = level.getData(MSUAttachments.DOOM_RELEASE_POOL);
		pool.release(victim.blockPosition(), amount, level.getGameTime() + Config.doomHarvestWindowTicks, victim.getUUID());
		data.setDoom(0);
	}

	@SubscribeEvent
	private static void onLevelTick(LevelTickEvent.Post event)
	{
		if(!(event.getLevel() instanceof ServerLevel level))
			return;
		if(level.getGameTime() % Config.doomReleaseTickIntervalTicks != 0)
			return;

		level.getData(MSUAttachments.DOOM_RELEASE_POOL).tick(level.getGameTime());
	}

	@Nullable
	private static LivingEntity resolveLivingEntity(ServerLevel level, UUID id)
	{
		return level.getEntity(id) instanceof LivingEntity living ? living : null;
	}
}
