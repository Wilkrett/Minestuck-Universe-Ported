package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.space;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.badgeEffects.BadgeEffects;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

import java.util.Set;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.space.TechSpaceTargetTele}
 * ("Spatial Warp") - the targeted sibling of {@link TechSpaceAnchoredTele}. Crouch and press to
 * set/clear the same shared warp point (see that class's own doc comment). Press without crouching:
 * blinks whatever entity you're aiming at to a random nearby safe spot, or straight to the warp point
 * if one is set - across dimensions if needed.
 */
public class TechSpaceTargetTele extends TechHeroAspect
{
	private static final int RANGE = 20;

	public TechSpaceTargetTele()
	{
		super(Minestuckuniverseported.id("spatial_warp"), EnumAspect.SPACE, 695000, MSUTechType.HYBRID);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.PRESS)
			return false;
		if(!(level instanceof ServerLevel serverLevel))
			return false;

		BadgeEffects badgeEffects = player.getData(MSUAttachments.BADGE_EFFECTS);

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

		LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);

		if(target != null)
		{
			if(!badgeEffects.hasWarpPoint())
			{
				target.randomTeleport(
						target.getX() + (target.getRandom().nextDouble() - 0.5) * RANGE * 2,
						Mth.clamp(target.getY() + (target.getRandom().nextDouble() - 0.5) * RANGE * 2, serverLevel.getMinBuildHeight(), serverLevel.getMaxBuildHeight() - 1),
						target.getZ() + (target.getRandom().nextDouble() - 0.5) * RANGE * 2,
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

				if(target instanceof ServerPlayer targetPlayer)
					targetPlayer.teleportTo(destination, warpPos.getX() + 0.5, warpPos.getY(), warpPos.getZ() + 0.5, target.getYRot(), target.getXRot());
				else
					target.teleportTo(destination, warpPos.getX() + 0.5, warpPos.getY(), warpPos.getZ() + 0.5, Set.<RelativeMovement>of(), target.getYRot(), target.getXRot());
			}

			MSUAbilitechParticles.oneshot(level, target, EnumAspect.SPACE, 5);

			if(!player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 6);
		}

		MSUAbilitechParticles.burst(level, player, EnumAspect.SPACE, target != null ? 10 : 3);

		return true;
	}
}
