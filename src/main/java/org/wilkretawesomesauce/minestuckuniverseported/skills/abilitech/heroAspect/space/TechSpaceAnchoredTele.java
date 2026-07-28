package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.space;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.AbilitechLoadout;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.space.TechSpaceAnchoredTele}
 * ("Anchored Wormhole"). Crouch and press to set a warp point at your feet, or clear it if one is
 * already set - the same shared warp point {@link org.wilkretawesomesauce.minestuckuniverseported.capabilities.badgeEffects.AbilitechLoadout} now stores for
 * {@link TechSpaceTargetTele} ("Spatial Warp") too, matching the original's shared
 * {@code IBadgeEffects} field. Press without crouching: if no warp point is set, blink to a random
 * nearby safe spot (real {@code LivingEntity#randomTeleport}, the same Enderman/chorus-fruit
 * mechanic); if one is set, teleport straight to it - across dimensions if needed, since the warp
 * point remembers which one it was set in.
 */
public class TechSpaceAnchoredTele extends TechHeroAspect
{
	private static final int RANGE = 20;

	public TechSpaceAnchoredTele()
	{
		super(Minestuckuniverseported.id("anchored_wormhole"), EnumAspect.SPACE, 100000, MSUTechType.UTILITY);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.PRESS)
			return false;
		if(!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer))
			return false;

		AbilitechLoadout badgeEffects = player.getData(MSUAttachments.ABILITECH_LOADOUT);

		if(player.isCrouching())
		{
			if(!badgeEffects.hasWarpPoint())
			{
				badgeEffects.setWarpPoint(player.blockPosition(), level.dimension());
				player.displayClientMessage(Component.translatable("status.spatialWarp.setPoint",
						player.getBlockX(), player.getBlockY(), player.getBlockZ()), true);
			}
			else
			{
				badgeEffects.clearWarpPoint();
				player.displayClientMessage(Component.translatable("status.spatialWarp.clearPoint"), true);
			}
			MSUAbilitechParticles.burst(level, player, EnumAspect.SPACE, badgeEffects.hasWarpPoint() ? 10 : 3);
			return true;
		}

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 6)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(!badgeEffects.hasWarpPoint())
		{
			serverPlayer.randomTeleport(
					player.getX() + (player.getRandom().nextDouble() - 0.5) * RANGE * 2,
					net.minecraft.util.Mth.clamp(player.getY() + (player.getRandom().nextDouble() - 0.5) * RANGE * 2, serverLevel.getMinBuildHeight(), serverLevel.getMaxBuildHeight() - 1),
					player.getZ() + (player.getRandom().nextDouble() - 0.5) * RANGE * 2,
					true);
		}
		else
		{
			BlockPos warpPos = badgeEffects.getWarpPointPos();
			ResourceKey<Level> warpDim = badgeEffects.getWarpPointDim();
			MinecraftServer server = serverLevel.getServer();
			ServerLevel destination = server.getLevel(warpDim);
			if(destination == null)
				return false;

			serverPlayer.teleportTo(destination, warpPos.getX() + 0.5, warpPos.getY(), warpPos.getZ() + 0.5, serverPlayer.getYRot(), serverPlayer.getXRot());
		}

		MSUAbilitechParticles.oneshot(level, player, EnumAspect.SPACE, 5);

		if(!player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 6);

		MSUAbilitechParticles.burst(level, player, EnumAspect.SPACE, 10);

		return true;
	}
}
