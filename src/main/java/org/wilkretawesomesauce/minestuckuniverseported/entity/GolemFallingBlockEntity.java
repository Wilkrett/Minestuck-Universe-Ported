package org.wilkretawesomesauce.minestuckuniverseported.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.wilkretawesomesauce.minestuckuniverseported.MSUEntityTypes;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.RelationshipCombatEvents;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Ported from ModularBosses (1.8)'s {@code entity.projectile.EntityCustomFallingBlock} -
 * {@link GolemEntity}'s stomp attack (aniID {@link GolemEntity#STOMP}) shrapnel. Deliberately extends
 * plain {@link Entity} directly rather than reusing vanilla's own {@code FallingBlockEntity} - an
 * earlier version of this port did reuse {@code FallingBlockEntity} + {@code setHurtsEntities} as a
 * "close enough" substitute, but that changes the actual feel of the attack: vanilla's own falling-block
 * damage only triggers once, on landing on top of something, while the original's real mechanic pushes
 * and damages every entity it flies near continuously for its whole ~1-second flight (a radiating
 * "debris burst", not a single falling brick) - {@link #hurtNearbyEntities} below is a direct, faithful
 * port of the original's own {@code collideWithEntities}, not an approximation.
 * <p>
 * Also matches the original in reading whatever real block is actually on the ground at its spawn
 * position ({@link #resolveBlock}) rather than always rendering as the golem's own mimicked material -
 * the golem's stomp kicks up whatever terrain is actually underfoot, which just usually happens to be
 * the same block the golem itself mimics.
 * <p>
 * {@link #shooter} is a real port of the original's own {@code Entity shooter} field (present but never
 * actually consumed for anything in the original) - here it's what damage in {@link #hurtNearbyEntities}
 * is attributed to, real entity-attributed damage rather than the environmental
 * {@code damageSources().fall()} an earlier version of this port used. That attribution is also what lets
 * {@code mechanics.relationship.RelationshipCombatEvents} generically zero this damage against the
 * shooter's own owner (a positive-relationship pair) without this class needing any bespoke owner field
 * of its own - the original 1.12.2 mod never needed this since it had no ally/ownership concept at all.
 */
public class GolemFallingBlockEntity extends Entity
{
	private static final EntityDataAccessor<BlockState> DATA_BLOCK = SynchedEntityData.defineId(GolemFallingBlockEntity.class, EntityDataSerializers.BLOCK_STATE);

	private float damage;

	@Nullable
	private LivingEntity shooter;

	public GolemFallingBlockEntity(EntityType<? extends GolemFallingBlockEntity> type, Level level)
	{
		super(type, level);
	}

	public GolemFallingBlockEntity(Level level, @Nullable LivingEntity shooter, double x, double y, double z, double motionY, float yaw, BlockPos originPos, float damage)
	{
		this(MSUEntityTypes.GOLEM_FALLING_BLOCK.get(), level);
		this.shooter = shooter;
		this.damage = damage;
		this.noPhysics = true;
		setPos(x, y, z);
		setYRot(yaw);
		setDeltaMovement(0.0, motionY, 0.0);
		resolveBlock(level, originPos);
	}

	private void resolveBlock(Level level, BlockPos originPos)
	{
		BlockState state = level.getBlockState(originPos);
		if(state.getBlock() instanceof LiquidBlock || state.isAir())
		{
			discard();
			return;
		}
		entityData.set(DATA_BLOCK, state);
	}

	public BlockState getMimicBlock()
	{
		return entityData.get(DATA_BLOCK);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(DATA_BLOCK, Blocks.STONE.defaultBlockState());
	}

	@Override
	public void tick()
	{
		super.tick();

		setDeltaMovement(getDeltaMovement().subtract(0.0, 0.04, 0.0));
		move(MoverType.SELF, getDeltaMovement());
		setDeltaMovement(getDeltaMovement().scale(0.98));

		if(tickCount > 20)
		{
			discard();
			return;
		}

		if(!level().isClientSide())
			hurtNearbyEntities();
	}

	/** Ported directly from {@code EntityCustomFallingBlock#collideWithEntities} - see class doc comment. */
	private void hurtNearbyEntities()
	{
		double centerX = (getBoundingBox().minX + getBoundingBox().maxX) / 2.0;
		double centerZ = (getBoundingBox().minZ + getBoundingBox().maxZ) / 2.0;

		List<Entity> nearby = level().getEntities(this, getBoundingBox());
		for(Entity entity : nearby)
		{
			if(entity instanceof GolemFallingBlockEntity || entity instanceof GolemEntity)
				continue;
			if(entity.invulnerableTime > 0)
				continue;
			// Real fix, caught in a later audit: RelationshipCombatEvents' own LivingDamageEvent.Pre hook
			// only zeroes damage - it doesn't (and can't, generically) know to also skip this entity's own
			// push()/invulnerableTime side effects, so a positively-related target (the shooter's owner)
			// would still get shoved around by "harmless" debris without this check. Filtering here means
			// they're skipped entirely, not just spared the HP loss.
			if(shooter != null && entity instanceof net.minecraft.world.entity.LivingEntity livingEntity && RelationshipCombatEvents.isPositivelyRelated(shooter, livingEntity))
				continue;

			double dx = entity.getX() - centerX;
			double dz = entity.getZ() - centerZ;
			double distSq = dx * dx + dz * dz;
			if(distSq < 1.0E-4)
				continue;

			entity.push(dx / distSq * 0.2, 1.2, dz / distSq * 0.2);
			entity.hurt(shooter != null ? damageSources().mobAttack(shooter) : damageSources().fall(), damage);
			entity.invulnerableTime = 10;
		}
	}

	@Override
	protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag)
	{
		damage = tag.getFloat("damage");
	}

	@Override
	protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag)
	{
		tag.putFloat("damage", damage);
	}
}
