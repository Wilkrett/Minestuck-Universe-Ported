package org.wilkretawesomesauce.minestuckuniverseported.entity;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.wilkretawesomesauce.minestuckuniverseported.MSUEntityTypes;

/**
 * Ported (partially - see below) from MinestuckUniverse (1.12.2)'s {@code entity.EntityMSUThrowable} - a
 * generic "throw an arbitrary {@link ItemStack} as a damaging projectile" entity, real infrastructure
 * currently only consumed by {@code skills.abilitech.TechSling} ("Sylladex Sling").
 * <p>
 * <b>Scope note - what's NOT ported</b>: the original hooked every lifecycle event (impact, gravity,
 * dropped items, status updates) through {@code items.properties.throwkind.IPropertyThrowable}, a whole
 * per-weapon-property customization layer belonging to this project's still-only-partially-ported Strife
 * weapon-property system (see {@code strife} package's own "Known gap" note on 3D icon rendering/the
 * older overrides-predicate system, and {@code beam.BeamWeaponItem}'s own note on only one of several
 * original beam weapons being ported so far). None of {@code TechSling}'s own actual behavior depends on
 * that layer - the original's own code only ever consulted it when the thrown item happened to be an
 * {@code MSUThrowableBase} weapon, which the top item of an arbitrary player's Sylladex essentially never
 * is - so this class only ports the original's real, always-applicable "plain hit" path: fixed gravity,
 * generic velocity/stack-size-scaled damage on any hit, and dropping the thrown item (or crumbs, if it
 * broke) on impact. Reintroduce {@code IPropertyThrowable} hooks here if a future weapon-throwing tech
 * ever needs them.
 * <p>
 * Extends vanilla's {@link ThrowableItemProjectile} (the same base snowballs/eggs/ender pearls use)
 * rather than reproducing the original's own bespoke {@code EntityThrowable} subclass and
 * {@code RenderThrowable} - the modern equivalent already provides synced item-stack tracking and (via
 * {@link net.minecraft.client.renderer.entity.ThrownItemRenderer}, registered in
 * {@code client.MSUClientSetup}) a real in-flight render of whatever item is actually being thrown, for
 * free.
 */
public class MSUThrowableEntity extends ThrowableItemProjectile
{
	private static final float DAMAGE_BASE = 1.0F;
	private static final float DAMAGE_STACK_CAP = 5.0F;
	private static final float DAMAGE_VELOCITY_CAP = 2.5F;

	public MSUThrowableEntity(EntityType<? extends MSUThrowableEntity> type, Level level)
	{
		super(type, level);
	}

	public MSUThrowableEntity(Level level, LivingEntity shooter, ItemStack stack)
	{
		super(MSUEntityTypes.MSU_THROWABLE.get(), shooter, level);
		setItem(stack);
	}

	/** Real port of the original's own {@code shoot(thrower, pitch, yaw, 0, velocity, inaccuracy)} call site. */
	public void shootFrom(LivingEntity shooter, float velocity, float inaccuracy)
	{
		shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), 0.0F, velocity, inaccuracy);
	}

	@Override
	protected Item getDefaultItem()
	{
		return Items.STICK;
	}

	@Override
	protected void onHitEntity(EntityHitResult result)
	{
		super.onHitEntity(result);

		if(level().isClientSide() || tickCount <= 4 && result.getEntity() == getOwner())
			return;

		ItemStack stack = getItem();
		float damage = DAMAGE_BASE * Math.min(DAMAGE_STACK_CAP,
				((float) stack.getCount() / stack.getMaxStackSize() + 1) * Math.min(DAMAGE_VELOCITY_CAP, 1.0F + (float) getDeltaMovement().lengthSqr()));

		DamageSource source = level().damageSources().thrown(this, getOwner());
		result.getEntity().hurt(source, damage);
	}

	@Override
	protected void onHit(HitResult result)
	{
		super.onHit(result);

		if(!(level() instanceof ServerLevel serverLevel))
			return;

		ItemStack stack = getItem();
		boolean ownerIsCreative = getOwner() instanceof Player owner && owner.isCreative();

		if(stack.isDamageableItem())
			stack.setDamageValue(Math.min(stack.getMaxDamage() - 1, stack.getDamageValue() + 1));

		if(!ownerIsCreative && !stack.isEmpty())
		{
			ItemEntity item = new ItemEntity(serverLevel, getX(), getY(), getZ(), stack);
			item.setDeltaMovement(0, 0.2, 0);
			item.setPickUpDelay(5);
			serverLevel.addFreshEntity(item);
		}
		else if(!stack.isEmpty())
		{
			serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack), getX(), getY(), getZ(), 8, 0.05, 0.05, 0.05, 0.0);
		}

		discard();
	}
}
