package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code util.MSUUtils#getMouseOver}/{@code getTargetEntity}/
 * {@code getTargetBlock} - several abilitechs raytrace to find what the player's looking at. The original
 * hand-rolled the look-vector trig itself; this uses the equivalent vanilla helpers instead
 * ({@code Item.getPlayerPOVHitResult} for blocks, {@code ProjectileUtil.getEntityHitResult} for entities),
 * which do the same job and are what modern Minecraft/NeoForge code normally reaches for.
 */
public final class MSUAbilitechRayTrace
{
	private MSUAbilitechRayTrace()
	{
	}

	public static HitResult getMouseOver(Player player, double reach)
	{
		BlockHitResult blockHit = Item.getPlayerPOVHitResult(player.level(), player, ClipContext.Fluid.NONE);

		Vec3 eyePos = player.getEyePosition(1.0F);
		double blockDistSq = blockHit.getType() != HitResult.Type.MISS
				? blockHit.getLocation().distanceToSqr(eyePos)
				: reach * reach;

		Vec3 look = player.getViewVector(1.0F);
		Vec3 endPos = eyePos.add(look.scale(reach));
		AABB searchBox = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0);

		EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eyePos, endPos, searchBox,
				e -> !e.isSpectator() && e.isPickable() && e != player, blockDistSq);

		if(entityHit != null)
			return entityHit;
		return blockHit;
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
