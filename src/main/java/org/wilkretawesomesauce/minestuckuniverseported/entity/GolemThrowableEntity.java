package org.wilkretawesomesauce.minestuckuniverseported.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;

/**
 * Ported from ModularBosses (1.8)'s {@code entity.projectile.EntityMobThrowable} - a shared abstract
 * base ({@link GolemBoulderEntity} and {@link GolemEggEntity} both extend it, exactly matching the
 * original's own {@code EntityBoulder}/{@code EntityCustomEgg} hierarchy) providing the "aim from a
 * shooter at a target, with an arc and a fixed spawn offset" constructor plus a generic {@code damage}
 * field with NBT persistence. Extends vanilla {@link ThrowableProjectile} rather than {@code Projectile}
 * directly - {@code Projectile}'s own constructor is package-private in modern Minecraft (a real
 * accessibility difference from 1.8's public {@code EntityThrowable}), so {@code ThrowableProjectile}
 * (protected constructors) is the closest modern equivalent to what the original built on.
 * <p>
 * The original's target-aim math (spawn a fixed offset from the shooter, aim roughly at the target with
 * a 2-block-low arc bias, subject to a wobble/inaccuracy factor) is preserved as-is; only the actual
 * "apply this heading" step changed, from 1.8's own {@code EntityThrowable#setThrowableHeading} to modern
 * {@link net.minecraft.world.entity.projectile.Projectile#shoot}, which does the same normalize-then-add-
 * inaccuracy job.
 */
public abstract class GolemThrowableEntity extends ThrowableProjectile
{
	private float damage;

	protected GolemThrowableEntity(EntityType<? extends GolemThrowableEntity> type, Level level)
	{
		super(type, level);
	}

	protected GolemThrowableEntity(EntityType<? extends GolemThrowableEntity> type, Level level, double x, double y, double z)
	{
		super(type, x, y, z, level);
	}

	protected GolemThrowableEntity(EntityType<? extends GolemThrowableEntity> type, Level level, LivingEntity shooter)
	{
		super(type, shooter, level);
	}

	/**
	 * Ported from {@code EntityMobThrowable}'s own target-aiming constructor - spawns offset from the
	 * shooter by {@code frontToBack}/{@code sideToSide}/{@code yOffset}, then throws roughly at the
	 * target's position (biased 2 blocks low, for a believable arc) at {@code velocity} with
	 * {@code wobble} inaccuracy.
	 */
	protected GolemThrowableEntity(EntityType<? extends GolemThrowableEntity> type, Level level, LivingEntity shooter, LivingEntity target,
			float velocity, float wobble, float frontToBack, float yOffset, float sideToSide)
	{
		this(type, level, shooter);

		float yawRad = shooter.getYRot() * Mth.DEG_TO_RAD;
		float cos = Mth.cos(yawRad);
		float sin = Mth.sin(yawRad);
		double xOff = (cos * -frontToBack) + (sin * sideToSide);
		double zOff = (sin * frontToBack) + (cos * sideToSide);

		double eyeY = shooter.getY() + shooter.getEyeHeight() - 0.2;
		double dx = target.getX() - shooter.getX();
		double dy = target.getY() - eyeY - 2.0;
		double dz = target.getZ() - shooter.getZ();
		double horizDist = Math.sqrt(dx * dx + dz * dz);

		if(horizDist >= 1.0E-7)
		{
			setPos(shooter.getX() + xOff, eyeY - yOffset, shooter.getZ() + zOff);
			shoot(dx - xOff, dy, dz - zOff, velocity, wobble);
		}
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
	}

	public float getDamage()
	{
		return damage;
	}

	public GolemThrowableEntity setDamage(float amount)
	{
		this.damage = amount;
		return this;
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag)
	{
		super.addAdditionalSaveData(tag);
		tag.putFloat("damage", damage);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag)
	{
		super.readAdditionalSaveData(tag);
		damage = tag.getFloat("damage");
	}
}
