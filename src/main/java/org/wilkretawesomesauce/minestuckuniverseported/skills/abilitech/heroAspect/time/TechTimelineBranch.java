package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time;

import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;
import org.wilkretawesomesauce.minestuckuniverseported.timeline.BranchForker;
import org.wilkretawesomesauce.minestuckuniverseported.timeline.TimelineBranch;
import org.wilkretawesomesauce.minestuckuniverseported.timeline.TimelineBranchRegistry;

import java.util.UUID;

/**
 * The real gameplay-facing way to fork/return between parallel timeline branches (see
 * {@code timeline.TimelineBranch}/{@code timeline.BranchForker}) - as distinct from
 * {@code /msutimeline branch ...}, which is a creative-mode-only debug/testing surface for the same
 * underlying system.
 * <p>
 * Interaction, mirroring {@link TechTimelineRewind}'s charge-then-decide-at-release shape rather than
 * acting on {@code PRESS} itself (acting on both PRESS and a subsequent long-HELD+RELEASE would mean a
 * quick tap forks you AND a following hold-release yanks you back out, which is confusing - deciding
 * everything at release avoids that). Three tiers, all decided by how long the key was held before
 * release:
 * <ul>
 *     <li>Quick tap (release before {@link #PARENT_HOLD_TICKS}, 1 second): forks a new branch from
 *     right here, right now, and moves you into it. No charge dial - unlike a rewind's duration,
 *     there's no "how much" to size for a fork.</li>
 *     <li>Held past {@link #PARENT_HOLD_TICKS} but released before {@link #ALPHA_HOLD_TICKS} (3
 *     seconds): returns you to this branch's parent (or Alpha, if the parent is null) instead.</li>
 *     <li>Held past {@link #ALPHA_HOLD_TICKS} before releasing: jumps straight to Alpha regardless of
 *     how deep in the branch tree you are, skipping every intermediate parent.</li>
 * </ul>
 * A no-op with a status message either way if already in Alpha.
 * <p>
 * <b>Known gap, stated plainly:</b> this three-way hold-duration split is a stand-in for a real menu -
 * a proper in-game branch-browser GUI (a tree view, analogous to {@code MSUAbilitechScreen}'s honeycomb
 * grid or {@code MSUStrifePortfolioScreen}'s fan-of-cards) that lets you jump to any specific branch
 * directly is planned but not built yet. Until then, arbitrary-branch browsing/travel-by-name is
 * command-only ({@code /msutimeline branch travel <name>}, creative-mode-gated); this tech only ever
 * forks, steps to the parent, or jumps all the way to Alpha.
 * <p>
 * Auto-generated branch names ({@link BranchForker#autoName}) are used since there's no text-input GUI
 * to ask the player for one - the same scheme {@code TimelineBranchCommand}'s {@code create} falls back
 * to when no name argument is given.
 */
public class TechTimelineBranch extends TechHeroAspect
{
	private static final int PARENT_HOLD_TICKS = 20;
	private static final int ALPHA_HOLD_TICKS = 60;

	public TechTimelineBranch()
	{
		super(Minestuckuniverseported.id("timeline_branch"), EnumAspect.TIME, 150000, MSUTechType.UTILITY, EnumClass.LORD);
		setIcon("default");
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.PRESS || state == AbilitechKeyState.HELD)
		{
			if(time >= ALPHA_HOLD_TICKS)
				return false;
			MSUAbilitechParticles.burst(level, player, EnumAspect.TIME, 6);
			return true;
		}

		if(state != AbilitechKeyState.RELEASED)
			return false;

		if(!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer))
			return false;

		if(time < PARENT_HOLD_TICKS)
			return forkNewBranch(serverLevel, serverPlayer);
		if(time < ALPHA_HOLD_TICKS)
			return returnToParent(serverLevel, serverPlayer);
		return travelToAlpha(serverLevel, serverPlayer);
	}

	private boolean forkNewBranch(ServerLevel level, ServerPlayer player)
	{
		TimelineBranchRegistry registry = level.getServer().overworld().getData(MSUAttachments.TIMELINE_BRANCHES);
		String name = BranchForker.autoName(player, registry);

		TimelineBranch branch = BranchForker.fork(player, level, name);
		if(branch == null)
		{
			player.displayClientMessage(Component.translatable("status.minestuckuniverseported.timeline.branch_fork_failed"), true);
			return false;
		}

		player.displayClientMessage(Component.translatable("status.minestuckuniverseported.timeline.branch_created", branch.getDisplayName()), true);
		MSUAbilitechParticles.oneshot(level, player, EnumAspect.TIME, 30);
		return true;
	}

	private boolean returnToParent(ServerLevel level, ServerPlayer player)
	{
		MinecraftServer server = level.getServer();
		TimelineBranchRegistry registry = server.overworld().getData(MSUAttachments.TIMELINE_BRANCHES);
		TimelineBranch current = registry.findByDimension(level.dimension());
		if(current == null)
		{
			player.displayClientMessage(Component.translatable("status.minestuckuniverseported.timeline.branch_already_alpha"), true);
			return false;
		}

		UUID parentId = current.getParentBranchId();
		TimelineBranch parent = parentId != null ? registry.get(parentId) : null;
		ServerLevel destination = parent != null ? BranchForker.travelTo(server, parent) : server.overworld();

		player.teleportTo(destination, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
		player.displayClientMessage(Component.translatable("status.minestuckuniverseported.timeline.branch_returned_to_parent"), true);
		MSUAbilitechParticles.oneshot(level, player, EnumAspect.TIME, 30);
		return true;
	}

	/** Skips every intermediate parent and jumps straight to Alpha, regardless of how deep in the branch tree the player is. */
	private boolean travelToAlpha(ServerLevel level, ServerPlayer player)
	{
		if(level.dimension() == Level.OVERWORLD)
		{
			player.displayClientMessage(Component.translatable("status.minestuckuniverseported.timeline.branch_already_alpha"), true);
			return false;
		}

		ServerLevel overworld = level.getServer().overworld();
		player.teleportTo(overworld, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
		player.displayClientMessage(Component.translatable("status.minestuckuniverseported.timeline.branch_returned_to_alpha"), true);
		MSUAbilitechParticles.oneshot(level, player, EnumAspect.TIME, 30);
		return true;
	}
}
