package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.wilkretawesomesauce.minestuckuniverseported.MSUItemComponents;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
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
 * <p>
 * <b>Category-gated, per {@link TimelineRecordCategory}</b> - user-requested scaffolding: {@link #of(LivingEntity, Set)}
 * only actually reads an entity's real state for whichever categories are passed in, and {@link #categories}
 * remembers exactly which ones that was so {@link #applyTo} only ever touches those - a category that was
 * never captured is never restored (leaving whatever the target already had), rather than being restored to
 * a meaningless default value. See that enum's own doc comment for why this is currently a single global
 * toggle (via {@link TimelineRecorder}), not something any individual caller varies yet.
 */
public record EntitySnapshot(
		Set<TimelineRecordCategory> categories,
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
	/** Captures every category - equivalent to how this record always behaved before {@link TimelineRecordCategory} existed. */
	public static EntitySnapshot of(LivingEntity entity)
	{
		return of(entity, TimelineRecordCategory.ALL);
	}

	public static EntitySnapshot of(LivingEntity entity, Set<TimelineRecordCategory> categories)
	{
		boolean position = categories.contains(TimelineRecordCategory.ENTITY_POSITION);
		boolean health = categories.contains(TimelineRecordCategory.ENTITY_HEALTH);
		boolean status = categories.contains(TimelineRecordCategory.ENTITY_STATUS);
		boolean actions = categories.contains(TimelineRecordCategory.ENTITY_ACTIONS);
		boolean equipmentOn = categories.contains(TimelineRecordCategory.ENTITY_EQUIPMENT);
		boolean vehicle = categories.contains(TimelineRecordCategory.ENTITY_VEHICLE);

		Map<EquipmentSlot, ItemStack> equipment = Map.of();
		if(equipmentOn)
		{
			equipment = new EnumMap<>(EquipmentSlot.class);
			for(EquipmentSlot slot : EquipmentSlot.values())
				equipment.put(slot, entity.getItemBySlot(slot).copy());
		}

		return new EntitySnapshot(
				Set.copyOf(categories),
				position ? entity.position() : Vec3.ZERO, position ? entity.getYRot() : 0F, position ? entity.getXRot() : 0F, position && entity.onGround(),
				health ? entity.getHealth() : 0F,
				position ? entity.getPose() : Pose.STANDING,
				status && entity.isOnFire(), status ? entity.getRemainingFireTicks() : 0, status && entity.isSprinting(), status && entity.isSwimming(), status && entity.isShiftKeyDown(), status && entity.isInvisible(), status && entity.isCurrentlyGlowing(), status && entity.isFallFlying(),
				actions && entity.isUsingItem(), actions ? entity.getUsedItemHand() : InteractionHand.MAIN_HAND,
				actions && entity.swinging, actions ? entity.swingTime : 0, actions ? entity.swingingArm : InteractionHand.MAIN_HAND,
				equipment,
				vehicle && entity.getVehicle() != null ? entity.getVehicle().getUUID() : null
		);
	}

	public void applyTo(LivingEntity entity)
	{
		if(categories.contains(TimelineRecordCategory.ENTITY_POSITION))
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
			entity.setPose(pose);
		}

		if(categories.contains(TimelineRecordCategory.ENTITY_HEALTH) && entity.isAlive())
			entity.setHealth(health);

		if(categories.contains(TimelineRecordCategory.ENTITY_STATUS))
		{
			entity.setRemainingFireTicks(remainingFireTicks);
			entity.setSprinting(sprinting);
			entity.setSwimming(swimming);
			entity.setShiftKeyDown(shiftKeyDown);
			entity.setInvisible(invisible);
			entity.setGlowingTag(glowing);
		}

		if(categories.contains(TimelineRecordCategory.ENTITY_ACTIONS))
		{
			entity.swinging = swinging;
			entity.swingTime = swingTime;
			entity.swingingArm = swingingArm;
		}

		if(categories.contains(TimelineRecordCategory.ENTITY_EQUIPMENT))
		{
			// Braid-style "this doesn't rewind" idea, item-tag half - see mechanics.timeline.TimelineTags'
			// own doc comment. Checked against what's *currently* worn/held (not the recorded past item): a
			// marked item should stay in its slot through a rewind, not get swapped back to whatever used
			// to be there.
			for(Map.Entry<EquipmentSlot, ItemStack> entry : equipment.entrySet())
			{
				if(!isTimelineImmune(entity.getItemBySlot(entry.getKey())))
					entity.setItemSlot(entry.getKey(), entry.getValue().copy());
			}
		}

		if(categories.contains(TimelineRecordCategory.ENTITY_VEHICLE))
		{
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

	private static boolean isTimelineImmune(ItemStack stack)
	{
		return stack.is(TimelineTags.IMMUNE_ITEMS) || Boolean.TRUE.equals(stack.get(MSUItemComponents.TIMELINE_IMMUNE.get()));
	}
}
