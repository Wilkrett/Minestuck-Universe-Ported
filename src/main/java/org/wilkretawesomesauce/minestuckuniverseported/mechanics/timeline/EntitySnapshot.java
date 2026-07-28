package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * A single tracked entity's recorded state at one tick. Expanded from the original position/health-only
 * version to cover what was actually asked for, matching the field list the referenced mocap mod records
 * (see its {@code Movement}/{@code ChangePose}/{@code SetEntityFlags}/{@code SetLivingEntityFlags}/
 * {@code ChangeItem}/{@code Swing} action classes) - movement, sprint/swim/sneak state, swinging, item
 * use, held/worn items, fire/invisible/glowing, and what the entity is riding.
 * <p>
 * <b>Structural difference from the mocap mod:</b> mocap records each of those as a separate per-type
 * "action" that only gets written when it changes (a proper diff-per-field system, serialized to a
 * recording file for later playback). This instead bundles everything into one combined snapshot per
 * entity per tick, matching this project's existing tick-snapshot architecture rather than adopting
 * mocap's own action-log format - same data coverage, simpler unified structure, at the cost of not
 * distinguishing "nothing changed" from "changed back to the same value" (irrelevant for a short rolling
 * rewind buffer, since every tick's full state is cheap to keep for the ~30 second window this needs).
 * <p>
 * <b>Not captured:</b> potion effects (a real gap - an entity's buffs/debuffs aren't restored), and
 * anything needing non-public field access.
 */
public record EntitySnapshot(
		Vec3 pos, float yaw, float pitch, boolean onGround,
		float health,
		Pose pose,
		boolean onFire, int remainingFireTicks, boolean sprinting, boolean swimming, boolean shiftKeyDown, boolean invisible, boolean glowing, boolean fallFlying,
		boolean usingItem, InteractionHand usedItemHand,
		boolean swinging, int swingTime, InteractionHand swingingArm,
		Map<EquipmentSlot, ItemStack> equipment,
		@Nullable UUID vehicleId
)
{
	public static EntitySnapshot of(LivingEntity entity)
	{
		Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
		for(EquipmentSlot slot : EquipmentSlot.values())
			equipment.put(slot, entity.getItemBySlot(slot).copy());

		return new EntitySnapshot(
				entity.position(), entity.getYRot(), entity.getXRot(), entity.onGround(),
				entity.getHealth(),
				entity.getPose(),
				entity.isOnFire(), entity.getRemainingFireTicks(), entity.isSprinting(), entity.isSwimming(), entity.isShiftKeyDown(), entity.isInvisible(), entity.isCurrentlyGlowing(), entity.isFallFlying(),
				entity.isUsingItem(), entity.getUsedItemHand(),
				entity.swinging, entity.swingTime, entity.swingingArm,
				equipment,
				entity.getVehicle() != null ? entity.getVehicle().getUUID() : null
		);
	}

	public void applyTo(LivingEntity entity)
	{
		// ServerPlayer#teleportTo(x,y,z) is entirely routed through this.connection.teleport(...) - no
		// fallback to the base Entity behavior at all. For a normal Mob that's irrelevant (it doesn't
		// override teleportTo), but for a ServerPlayer-type entity with a dummy connection (see
		// util.MSUFakePlayer, used by DoomedTimelineClone) that call was silently a no-op: the position
		// never actually changed. moveTo(x,y,z,yaw,pitch) is NOT overridden by ServerPlayer at all (only
		// its 3-arg sibling is), so it directly sets position/rotation fields the same way for every
		// entity type - this is also exactly what the referenced mocap mod uses for the same reason.
		entity.moveTo(pos.x, pos.y, pos.z, yaw, pitch);
		entity.setYHeadRot(yaw);
		entity.setDeltaMovement(Vec3.ZERO);
		entity.setOnGround(onGround);
		if(entity.isAlive())
			entity.setHealth(health);

		entity.setPose(pose);
		entity.setRemainingFireTicks(remainingFireTicks);
		entity.setSprinting(sprinting);
		entity.setSwimming(swimming);
		entity.setShiftKeyDown(shiftKeyDown);
		entity.setInvisible(invisible);
		entity.setGlowingTag(glowing);

		entity.swinging = swinging;
		entity.swingTime = swingTime;
		entity.swingingArm = swingingArm;

		for(Map.Entry<EquipmentSlot, ItemStack> entry : equipment.entrySet())
			entity.setItemSlot(entry.getKey(), entry.getValue().copy());

		if(vehicleId != null && entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel
				&& serverLevel.getEntity(vehicleId) instanceof net.minecraft.world.entity.Entity vehicle)
		{
			entity.startRiding(vehicle, true);
		}
		else if(entity.getVehicle() != null)
		{
			entity.stopRiding();
		}
	}
}
