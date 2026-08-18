package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.space;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.space.TechSpaceTele}
 * ("Wormhole Trotter") - press to teleport forwards to wherever you're looking (a block face, or just
 * behind whatever entity you're aiming at, facing it). Range and food cost both scale off current
 * hunger, exactly like the original ({@code reach = foodLevel * 2}, cost = {@code distance / 6}).
 * <p>
 * Uses {@link Player#teleportTo(double, double, double)} directly rather than the 5-arg
 * {@code moveTo} this project reaches for on fake/dummy-connection entities elsewhere (see
 * CLAUDE.md's recurring-bug-pattern #1) - the caster here is always a real connected
 * {@code ServerPlayer}, for which {@code teleportTo} is the correct, normal call.
 */
public class TechSpaceTele extends TechHeroAspect
{
	public TechSpaceTele()
	{
		super(Minestuckuniverseported.id("wormhole_trotter"), EnumAspect.SPACE, 2500, MSUTechType.UTILITY);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.PRESS || time != 0)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 1)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		double reach = Math.max(player.getFoodData().getFoodLevel() * 2, 1);
		HitResult hit = MSUAbilitechRayTrace.getMouseOver(player, reach);

		double destX, destY, destZ;
		float destYaw = player.getYRot();

		if(hit instanceof EntityHitResult entityHit)
		{
			Entity target = entityHit.getEntity();
			destYaw = target.getYHeadRot();
			Direction facing = Direction.fromYRot(destYaw).getOpposite();
			int offset = (int)Math.ceil(target.getBbWidth() / 2.0);
			BlockPos pos = BlockPos.containing(target.position()).relative(facing, offset);
			destX = pos.getX() + 0.5;
			destY = pos.getY();
			destZ = pos.getZ() + 0.5;
		}
		else if(hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK)
		{
			BlockPos pos = blockHit.getBlockPos().relative(blockHit.getDirection(), blockHit.getDirection() == Direction.DOWN ? 2 : 1);
			destX = pos.getX() + 0.5;
			destY = pos.getY();
			destZ = pos.getZ() + 0.5;
		}
		else
		{
			// MISS - nothing in reach (e.g. teleporting through open air). Original 1.12.2 behavior:
			// the raytrace's "last uncollidable" endpoint is used directly as the destination, unlike
			// a real block hit which offsets off the hit face.
			Vec3 miss = hit.getLocation();
			destX = miss.x;
			destY = miss.y;
			destZ = miss.z;
		}

		double distance = player.position().distanceTo(new Vec3(destX, destY, destZ));

		if(!player.isCreative())
			player.getFoodData().setFoodLevel((int)Math.max(0, player.getFoodData().getFoodLevel() - Math.floor(distance) / 6));

		player.teleportTo(destX, destY, destZ);
		player.setYRot(destYaw);
		player.setYHeadRot(destYaw);

		MSUAbilitechParticles.aura(level, player, EnumAspect.SPACE, 10);

		player.teleportTo(destX, destY, destZ);
		player.setYRot(destYaw);
		player.setYHeadRot(destYaw);
		player.setYBodyRot(destYaw);

		return true;
	}
}
