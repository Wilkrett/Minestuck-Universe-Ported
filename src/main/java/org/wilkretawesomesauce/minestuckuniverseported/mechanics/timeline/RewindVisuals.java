package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.network.RewindGhostPacket;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.loop.TimeLoopZone;

import java.util.ArrayList;
import java.util.List;

/**
 * User-requested "generic rewind function" - a reusable trigger for the gray doppelganger-comet effect any
 * future or existing rewind pathway can call once an entity's state has actually been snapped backward, not
 * a one-off built only for {@code skills.abilitech.heroAspect.time.TechTimeLoopBeta}.
 * <p>
 * <b>Correction (1/2), from a direct user rejection of the first version</b>: that version sent a single
 * "previous position" point and faded a lone static ghost there - correctly described back as "that's not
 * rewinding, that's simply resetting". This version sends the entity's own actual recorded path across the
 * rewound window ({@link PathPoint}, one per tick, taken straight from the same {@code EntitySnapshot}s the
 * rewind itself already used) and the client ({@code client.rewind.RewindGhostPlayback}/
 * {@code client.render.RewindGhostRenderer}) animates a short comet of doppelgangers sweeping backward
 * through that path.
 * <p>
 * <b>Correction (2/2), from a second direct user rejection</b>: even with a full path, the rewound entity
 * was still snapping straight to the window's start - only the cosmetic comet moved through the path, not
 * the entity itself. {@link #sampleReversePath} is the shared "where should this entity actually be,
 * {@code reverseTick} ticks into a {@code durationTicks}-long reverse walk" formula now used by both
 * per-tick movers - {@code timeline.loop.TimeLoopReplay#reverseStep} (puppeted zone entities) and
 * {@code skills.abilitech.heroAspect.time.TechTimeLoopBeta}'s own player-specific tick driver - so the
 * cosmetic comet and the actual entity move through the identical path on the identical schedule
 * ({@link TimeLoopZone#REVERSE_TICKS_EFFECTIVE}, sent to the client via {@code RewindGhostPacket} instead of
 * a separately-guessed constant, so the two can never drift out of sync).
 * <p>
 * <b>Deliberately wired only into the actual "reset to a window's start" moments</b> -
 * {@code timeline.loop.TimeLoopReplay} (puppeted entities) and {@code TechTimeLoopBeta} (the dying player) -
 * not into every per-tick forward-replay step ({@code TimeLoopReplay#replayStepForward} also calls
 * {@link EntitySnapshot#applyTo}, but that's the loop naturally playing forward, not "a rewind happening").
 * Not yet wired into {@code TimelineManager}'s own destructive rewind/travel - a low-risk future extension
 * of this same generic function, just not done here since nothing asked for it there yet.
 */
public final class RewindVisuals
{
	/**
	 * One recorded position/rotation sample along a rewound entity's own real path - the compact,
	 * network-friendly shape {@link EntitySnapshot} gets flattened down to for this purpose alone (that
	 * record carries far more than a visual effect needs - equipment, pose flags, vehicle id - none of
	 * which belongs on the wire here).
	 */
	public record PathPoint(float x, float y, float z, float yaw, float pitch)
	{
		public static final StreamCodec<RegistryFriendlyByteBuf, PathPoint> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.FLOAT, PathPoint::x,
				ByteBufCodecs.FLOAT, PathPoint::y,
				ByteBufCodecs.FLOAT, PathPoint::z,
				ByteBufCodecs.FLOAT, PathPoint::yaw,
				ByteBufCodecs.FLOAT, PathPoint::pitch,
				PathPoint::new
		);
	}

	private RewindVisuals()
	{
	}

	/**
	 * @param path the entity's own real recorded states across the rewound window, chronological
	 *              (oldest first, matching {@code TimeLoopZone#getWindow()}'s own order) - every tick this
	 *              specific entity actually had a snapshot for, not necessarily one entry per window tick
	 *              (an entity that only started being tracked partway through the window has a shorter path)
	 */
	public static void showRewindGhost(LivingEntity entity, List<EntitySnapshot> path)
	{
		if(!(entity.level() instanceof ServerLevel) || entity.isRemoved() || path.size() < 2)
			return;

		List<PathPoint> points = new ArrayList<>(path.size());
		for(EntitySnapshot snapshot : path)
			points.add(new PathPoint((float) snapshot.pos().x, (float) snapshot.pos().y, (float) snapshot.pos().z,
					snapshot.yaw(), snapshot.pitch()));

		RewindGhostPacket packet = new RewindGhostPacket(entity.getId(), points, TimeLoopZone.REVERSE_TICKS_EFFECTIVE);
		if(entity instanceof ServerPlayer serverPlayer)
			PacketDistributor.sendToPlayersTrackingEntityAndSelf(serverPlayer, packet);
		else
			PacketDistributor.sendToPlayersTrackingEntity(entity, packet);
	}

	/**
	 * The shared reverse-walk sampling math - see this class's own "Correction (2/2)" doc note for why both
	 * per-tick movers and the cosmetic comet need to agree on this exact formula. {@code reverseTick=0} is
	 * the very first step of the walk (still near {@code path}'s newest end); {@code reverseTick=durationTicks-1}
	 * (the last step) lands exactly on {@code path}'s oldest entry - the walk's destination.
	 */
	public static EntitySnapshot sampleReversePath(List<EntitySnapshot> path, int reverseTick, int durationTicks)
	{
		float progress = (reverseTick + 1) / (float) durationTicks;
		float exactIndex = (1F - progress) * (path.size() - 1);
		return path.get(Math.round(Mth.clamp(exactIndex, 0, path.size() - 1)));
	}
}
