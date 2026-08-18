package org.wilkretawesomesauce.minestuckuniverseported.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code MSUUtils#getMouseOver()}.
 */
public final class MSUAbilitechRayTrace
{
	private MSUAbilitechRayTrace()
	{
	}

	public static HitResult getMouseOver(Player player, double reach)
	{
		Vec3 eyePos = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		Vec3 endPos = eyePos.add(look.scale(reach));

		// Equivalent to the old world.rayTraceBlocks(...)
		BlockHitResult blockHit = player.level().clip(new ClipContext(
				eyePos,
				endPos,
				ClipContext.Block.OUTLINE,
				ClipContext.Fluid.NONE,
				player
		));

		double blockDistSq = blockHit.getType() == HitResult.Type.BLOCK
				? eyePos.distanceToSqr(blockHit.getLocation())
				: reach * reach;

		AABB searchBox = player.getBoundingBox()
				.expandTowards(look.scale(reach))
				.inflate(1.0D);

		EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
				player,
				eyePos,
				endPos,
				searchBox,
				entity -> !entity.isSpectator() && entity.isPickable() && entity != player,
				blockDistSq
		);

		return entityHit != null ? entityHit : blockHit;
	}

	@Nullable
	public static LivingEntity getTargetEntity(Player player)
	{
		return getTargetEntity(player, player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE));
	}

	@Nullable
	public static LivingEntity getTargetEntity(Player player, double reach)
	{
		HitResult hit = getMouseOver(player, reach);

		if(hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living)
			return living;

		return null;
	}

	@Nullable
	public static BlockPos getTargetBlock(Player player)
	{
		return getTargetBlock(player, player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE));
	}

	@Nullable
	public static BlockPos getTargetBlock(Player player, double reach)
	{
		HitResult hit = getMouseOver(player, reach);

		if(hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK)
			return blockHit.getBlockPos();

		return null;
	}
}