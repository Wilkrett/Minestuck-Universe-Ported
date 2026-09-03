package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The actual "does time travel work" plumbing: records enough of the world's recent history that both
 * {@link TimelineManager} (destructive rewind) and {@code PastObserver} (non-destructive spectating -
 * "watch the last N minutes unfold again without touching the present") can work from it.
 * <p>
 * <b>Now genuinely always-on, not gated to Time-tech users.</b> An earlier version only recorded changes
 * near a player who currently had a Time-aspect tech equipped, on the reasoning that nothing else needed
 * the history. That assumption doesn't hold anymore: watching the past unfold requires having actually
 * recorded it *before* anyone decided they wanted to look, for every player and every change, not just
 * the ones a Time user happened to be standing next to. So recording now runs continuously for every
 * loaded level, unconditionally - real added baseline cost, bounded by {@link #HISTORY_TICKS}
 * (old ticks still get dropped off the rolling window the same as before) but not by proximity to anyone
 * in particular anymore.
 * <p>
 * <b>Coverage, and its remaining limits</b> - this is the important part to understand before relying on it:
 * <ul>
 *     <li><b>Blocks:</b> only changes NeoForge fires a discrete event for - player break/place
 *     ({@link BlockEvent.BreakEvent}/{@link BlockEvent.EntityPlaceEvent}) and fluid placement
 *     ({@link BlockEvent.FluidPlaceBlockEvent}). "Natural" changes with no such event - crop growth, leaf
 *     decay, redstone-driven updates, fire spread - are <b>not</b> captured or reverted/replayed. Catching
 *     literally every block write would need a mixin into {@code Level#setBlock}; that's a real option
 *     for later but adds build-system risk (Mixin isn't currently part of this project).</li>
 *     <li><b>Entities:</b> every loaded living entity in the level, every tick, unconditionally now (see
 *     {@link ServerLevel#getAllEntities()}). This is the most expensive part of "always recording" -
 *     untested at real scale, and the first thing to revisit if it turns out to be too costly on a busy
 *     server.</li>
 * </ul>
 * <p>
 * <b>Category-gated, per {@link TimelineRecordCategory}</b> - {@link #RECORDED_CATEGORIES}
 * decides both whether block changes are recorded at all ({@link #onBlockBreak}/{@link #onBlockPlace}/
 * {@link #onFluidPlace} all skip outright if {@link TimelineRecordCategory#BLOCKS} is off) and which parts
 * of each entity's state {@link EntitySnapshot#of(LivingEntity, Set)} actually reads. See that enum's own
 * doc comment for why this is real, working scaffolding rather than an actually-used restriction - every
 * category is on by default and nothing currently asks for less.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class TimelineRecorder
{
	/** Max ticks of world history each dimension keeps. 6000 = 5 minutes. */
	private static final int HISTORY_TICKS = 6000;
	/** Which categories of world history get captured each tick - see {@link TimelineRecordCategory} for the full list. */
	private static final Set<TimelineRecordCategory> RECORDED_CATEGORIES = Set.copyOf(Arrays.asList(TimelineRecordCategory.values()));

	private TimelineRecorder()
	{
	}

	private static TimelineData dataOf(ServerLevel level)
	{
		return level.getData(MSUAttachments.TIMELINE);
	}

	@SubscribeEvent
	private static void onBlockBreak(BlockEvent.BreakEvent event)
	{
		if(!RECORDED_CATEGORIES.contains(TimelineRecordCategory.BLOCKS))
			return;
		if(event.getLevel() instanceof ServerLevel level)
			dataOf(level).recordBlockChange(event.getPos(), level.getBlockState(event.getPos()),
					net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), event.getPlayer().getUUID());
	}

	@SubscribeEvent
	private static void onBlockPlace(BlockEvent.EntityPlaceEvent event)
	{
		if(!RECORDED_CATEGORIES.contains(TimelineRecordCategory.BLOCKS))
			return;
		if(event.getLevel() instanceof ServerLevel level)
		{
			UUID causedBy = event.getEntity() instanceof net.minecraft.world.entity.player.Player player ? player.getUUID() : null;
			dataOf(level).recordBlockChange(event.getPos(), event.getBlockSnapshot().getState(), event.getPlacedBlock(), causedBy);
		}
	}

	@SubscribeEvent
	private static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent event)
	{
		if(!RECORDED_CATEGORIES.contains(TimelineRecordCategory.BLOCKS))
			return;
		if(event.getLevel() instanceof ServerLevel level)
			dataOf(level).recordBlockChange(event.getPos(), event.getOriginalState(), event.getNewState(), null);
	}

	@SubscribeEvent
	private static void onLevelTick(LevelTickEvent.Post event)
	{
		if(!(event.getLevel() instanceof ServerLevel level))
			return;

		Map<UUID, EntitySnapshot> snapshots = new HashMap<>();
		for(Entity entity : level.getAllEntities())
			if(entity instanceof LivingEntity living)
				snapshots.put(living.getUUID(), EntitySnapshot.of(living, RECORDED_CATEGORIES));

		dataOf(level).pushTick(snapshots, HISTORY_TICKS);
	}
}
