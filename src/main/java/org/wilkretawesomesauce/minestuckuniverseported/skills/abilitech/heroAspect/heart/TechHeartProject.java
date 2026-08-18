package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.heart;

import com.mraof.minestuck.entity.DecoyEntity;
import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.AbilitechLoadout;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

import java.util.Deque;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.heart.TechHeartProject}
 * ("Astral Projection") - hold for 3 seconds to leave your body (a real double stands in for you while
 * you're gone) and scout ahead as a spectator, returning automatically after a minute, on your next
 * press, or if anything else knocks you out of spectator mode.
 * <p>
 * The original built its own body double ({@code EntityHeartDecoy}) on Minestuck's 1.12.2-era
 * {@code EntityDecoy}/remote-possession API - that class isn't part of the modern Minestuck 1.21.1
 * dependency, but a real, already-ported equivalent is: {@code com.mraof.minestuck.entity.DecoyEntity},
 * built for Minestuck's own computer/edit-mode feature (leaving a body double behind while a player
 * edits their session remotely). Reused here directly, since it's already exactly "a standalone visual
 * body double you can spawn and later discard", independent of edit-mode's own Sburb-connection
 * machinery - {@code computer.editmode.ServerEditHandler} (which actually drives edit-mode's
 * enter/exit flow) was deliberately <i>not</i> reused, since its entry point requires a live Sburb
 * connection between two identified players and pulls in a lot of unrelated edit-mode behavior
 * (block-break grist costs, deploy lists, cursor entities) that has nothing to do with this ability.
 * <p>
 * The single in-flight projection record (origin position/rotation/gamemode/start tick, plus the decoy
 * itself) is stashed in {@link AbilitechLoadout#getSlotHistory}, the same per-slot scratch state
 * {@code TechTimeRecall} already uses for its own position history - no new attachment needed for one
 * record that never needs to survive a restart.
 */
public class TechHeartProject extends TechHeroAspect
{
	private static final int CHARGE_TICKS = 60;
	private static final int MAX_PROJECTION_TICKS = 1200;
	private static final double LOOK_DISTANCE = 32.0;

	private record AstralOrigin(double x, double y, double z, float yaw, float pitch, GameType gameType, long startGameTime, DecoyEntity decoy)
	{
	}

	public TechHeartProject()
	{
		super(Minestuckuniverseported.id("astral_projection"), EnumAspect.HEART, 1150000, MSUTechType.UTILITY);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel))
			return false;

		AbilitechLoadout loadout = player.getData(MSUAttachments.ABILITECH_LOADOUT);
		Deque<Object> history = loadout.getSlotHistory(techSlot);

		if(!history.isEmpty())
		{
			AstralOrigin origin = (AstralOrigin) history.peek();
			boolean shouldReturn = serverPlayer.gameMode.getGameModeForPlayer() != GameType.SPECTATOR
					|| serverLevel.getGameTime() - origin.startGameTime() >= MAX_PROJECTION_TICKS
					|| state == AbilitechKeyState.PRESS;

			if(shouldReturn)
			{
				history.pop();
				returnFromProjection(serverPlayer, origin);
			}
			return true;
		}

		if(state != AbilitechKeyState.HELD || time > CHARGE_TICKS)
			return false;

		if(!player.isCreative() && player.getFoodData().needsFood())
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(time < CHARGE_TICKS)
		{
			MSUAbilitechParticles.aura(level, player, EnumAspect.HEART, 2);
			return true;
		}

		if(!player.isCreative())
			player.getFoodData().setFoodLevel(0);

		GameType originalGameType = serverPlayer.gameMode.getGameModeForPlayer();

		DecoyEntity decoy = new DecoyEntity(serverLevel, serverPlayer);
		decoy.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
		serverLevel.addFreshEntity(decoy);

		history.push(new AstralOrigin(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(),
				originalGameType, serverLevel.getGameTime(), decoy));

		Vec3 projectTo = player.getEyePosition().add(player.getLookAngle().scale(LOOK_DISTANCE));
		serverPlayer.setGameMode(GameType.SPECTATOR);
		serverPlayer.teleportTo(projectTo.x, projectTo.y, projectTo.z);

		return true;
	}

	private static void returnFromProjection(ServerPlayer player, AstralOrigin origin)
	{
		player.setGameMode(origin.gameType());
		player.moveTo(origin.x(), origin.y(), origin.z(), origin.yaw(), origin.pitch());
		if(origin.decoy().isAlive())
			origin.decoy().remove(Entity.RemovalReason.DISCARDED);
	}

	@Override
	public void onUnequipped(Level level, Player player, int techSlot)
	{
		if(!(player instanceof ServerPlayer serverPlayer))
			return;

		AbilitechLoadout loadout = player.getData(MSUAttachments.ABILITECH_LOADOUT);
		Deque<Object> history = loadout.getSlotHistory(techSlot);
		if(!history.isEmpty() && history.pop() instanceof AstralOrigin origin)
			returnFromProjection(serverPlayer, origin);
	}
}
