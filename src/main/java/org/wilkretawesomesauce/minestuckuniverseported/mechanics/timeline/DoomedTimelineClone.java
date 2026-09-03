package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUFakePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The "doomed timeline" ghost, spawned by {@link TimelineManager#rewind}: a {@link MSUFakePlayer} that
 * replays the initiating player's own recorded path forward (oldest state first) at
 * {@code TimelineRewindPlayback#CLONE_REPLAY_SPEED} - real time by
 * default, deliberately decoupled from however fast the world-undo itself is playing back.
 * <p>
 * <b>Now implements the "actions" attribution requested</b>: each {@link Step} carries not just the
 * clone's own movement/pose state but which block positions (if any) the real player changed at that
 * same original tick (see {@link WorldTickSnapshot.BlockChangeRecord#causedBy}). When the clone reaches
 * such a step, it swings before moving on - so the clone visibly appears to be the one breaking/placing
 * those blocks as the world's independent undo restores them nearby, rather than blocks just changing on
 * their own with the clone incidentally standing there. This is a lighter-weight version of what mocap's
 * separate {@code BreakBlock}/{@code PlaceBlock}/{@code Swing} action types do together on one shared
 * tick counter - attributing changes and cueing the swing, without needing to move actual block
 * restoration responsibility onto the clone's own replay (the world-undo still owns that).
 * <p>
 * Despawns (see {@link #despawn}) once its steps run out, playing the same "gears" effect it spawned
 * with - it no longer stands around forever afterward.
 */
public final class DoomedTimelineClone
{
	/** One step of the clone's replay: its own state, plus any block positions it should visibly "act on" here. */
	public record Step(EntitySnapshot state, List<BlockPos> attributedBlocks)
	{
	}

	private final List<Step> steps;
	private final MSUFakePlayer fakePlayer;
	private int index = 0;

	public DoomedTimelineClone(List<Step> steps, MSUFakePlayer fakePlayer)
	{
		this.steps = steps;
		this.fakePlayer = fakePlayer;
	}

	public boolean isDone()
	{
		return index >= steps.size();
	}

	public void advanceOneStep(ServerLevel level)
	{
		if(isDone())
			return;

		Step step = steps.get(index++);
		step.state().applyTo(fakePlayer);
		fakePlayer.broadcastMovement();

		if(!step.attributedBlocks().isEmpty())
			fakePlayer.swing(InteractionHand.MAIN_HAND);
	}

	/**
	 * Rewinds the replay cursor back to the beginning and snaps the clone straight to its first step's
	 * state, without despawning/respawning it - used by {@code timeline.loop.TimeLoopZone}, whose clone
	 * needs to replay the same path over and over for the zone's lifetime rather than once. Not used by
	 * the one-shot rewind clone this class originally supported (a fresh {@link #spawn}/{@link #despawn}
	 * pair per rewind is fine there since it only ever plays once).
	 * <p>
	 * Leaves {@code index} at 1, not 0 - step 0's state is already applied by this call itself, so the
	 * <i>next</i> {@link #advanceOneStep} should show step 1, keeping this in lockstep with a caller
	 * (like {@code TimeLoopPlayback}) that calls this once per pass then {@code advanceOneStep} once per
	 * subsequent tick of that same pass. That lockstep assumption holds whenever the entity this clone was
	 * built from ({@code DoomedTimelineClone#buildSteps}) had a recorded snapshot every tick of the window
	 * - true for the common case (a loaded, living caster is recorded unconditionally every tick), but a
	 * gap would drift the clone out of sync with the rest of that pass, correcting again at the next reset.
	 */
	public void resetToStart()
	{
		if(steps.isEmpty())
		{
			index = 0;
			return;
		}

		steps.get(0).state().applyTo(fakePlayer);
		fakePlayer.broadcastMovement();
		index = 1;
	}

	/**
	 * Spawns the clone into the world - same broadcast-then-add sequence {@code TechTimeZeitgeist}
	 * (and, originally, the mocap mod itself) uses for a {@link MSUFakePlayer}. Plays the "gears rising"
	 * effect (see {@code client.particles.TimeGearsRiseParticle}) at the spawn point.
	 */
	public static MSUFakePlayer spawn(ServerLevel level, com.mojang.authlib.GameProfile profile, EntitySnapshot startState)
	{
		MSUFakePlayer fakePlayer = new MSUFakePlayer(level, profile);
		startState.applyTo(fakePlayer);
		fakePlayer.gameMode.changeGameModeForPlayer(GameType.ADVENTURE);

		PlayerList packetTargets = level.getServer().getPlayerList();
		packetTargets.broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, fakePlayer));
		level.addNewPlayer(fakePlayer);

		playGearsEffect(level, fakePlayer);

		return fakePlayer;
	}

	/** Removes the clone, playing the same effect it spawned with. */
	public void despawn(ServerLevel level)
	{
		playGearsEffect(level, fakePlayer);
		level.getServer().getPlayerList().broadcastAll(new ClientboundPlayerInfoRemovePacket(List.of(fakePlayer.getUUID())));
		fakePlayer.remove(Entity.RemovalReason.KILLED);
	}

	/**
	 * Shared helper since both this and {@code TechTimeZeitgeist} play the same effect on spawn/despawn.
	 * Centered vertically on the entity's bounding box (not its feet) - {@code TimeGearsRiseParticle} is now
	 * sized to cover a whole player-sized double, so spawning it at feet height would mostly grow upward past
	 * the model instead of surrounding it.
	 */
	public static void playGearsEffect(ServerLevel level, Entity entity)
	{
		double centerY = entity.getY() + entity.getBbHeight() / 2.0;
		level.sendParticles(MSUParticles.TIME_GEARS_RISE.get(), entity.getX(), centerY, entity.getZ(), 1, 0, 0, 0, 0);
	}

	/**
	 * Extracts one entity's path (state + any block changes it caused) out of a chronologically-ordered
	 * (oldest first) list of recorded ticks - shared by {@code mechanics.timeline.TimelineManager#spawnDoomedClone}
	 * (which reverses its popped, undo-ordered steps into chronological order first) and
	 * {@code timeline.loop.TimeLoopCaster} (whose captured window is already chronological, being a plain
	 * copy of {@code TimelineData#getHistory()} rather than popped undo steps). Ticks where the entity has
	 * no recorded snapshot are skipped, same as the original inline version of this logic.
	 */
	public static List<Step> buildSteps(UUID entityId, List<WorldTickSnapshot> chronologicalSteps)
	{
		List<Step> path = new ArrayList<>();
		for(WorldTickSnapshot tick : chronologicalSteps)
		{
			EntitySnapshot state = tick.entitySnapshots().get(entityId);
			if(state == null)
				continue;

			List<BlockPos> attributedBlocks = new ArrayList<>();
			for(Map.Entry<BlockPos, WorldTickSnapshot.BlockChangeRecord> entry : tick.blockChanges().entrySet())
				if(entityId.equals(entry.getValue().causedBy()))
					attributedBlocks.add(entry.getKey());

			path.add(new Step(state, attributedBlocks));
		}
		return path;
	}
}
