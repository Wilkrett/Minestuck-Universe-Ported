package org.wilkretawesomesauce.minestuckuniverseported.entity;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.wilkretawesomesauce.minestuckuniverseported.MSUEntityTypes;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.RelationshipCombatEvents;

import java.util.List;

/**
 * Ported from ModularBosses (1.8)'s {@code entity.projectile.EntityBoulder} - {@link GolemEntity}'s
 * ranged attack (aniID {@link GolemEntity#THROW}), a real sibling of {@link GolemEggEntity} under the
 * shared {@link GolemThrowableEntity} base, matching the original's own {@code EntityBoulder}/
 * {@code EntityCustomEgg} both extending {@code EntityMobThrowable}. Textured the same way the golem
 * itself is (see {@link GolemEntity#getMimicBlock()}), carrying a copy of the mimicked
 * {@link BlockState} in its own synced data rather than reading it off the shooter (the shooter may
 * already be gone/dead by the time this despawns).
 * <p>
 * Three real behaviors ported directly from {@code EntityBoulder}'s own {@code onUpdate}/
 * {@code collideWithEntities}, not approximated via vanilla's generic raytrace-based projectile hit
 * pipeline:
 * <ul>
 * <li>A continuous client-side block-dust trail while in flight (15 particles/tick).</li>
 * <li>Checked via a real per-tick bounding-box overlap ({@link #hurtOverlappingTargets}), not a one-shot
 * movement-path raytrace, so it still catches a target who walks into a boulder that's already come to
 * rest. <b>Real, project-original widening beyond the original</b>: the 1.12.2 original filtered strictly
 * to {@code EntityPlayer} (this was a boss designed to only ever fight players); now that
 * {@link GolemEntity} can be a summoned ally, a boulder it throws needs to be able to actually hurt
 * whatever hostile target it's aimed at, not just other players. Damage is attributed to the shooter
 * ({@code damageSources().thrown(this, getOwner())} - {@code getOwner()} here is vanilla
 * {@code Projectile}'s own shooter accessor, the golem itself), so a golem can never hurt its own real
 * owner this way either - {@code mechanics.relationship.RelationshipCombatEvents} zeroes any damage
 * between two entities with a positive relationship generically, reading that same attribution, not a
 * bespoke owner field on this class.</li>
 * <li>Hitting a block does nothing (the original's own {@code onImpact} was an empty stub) - the boulder
 * just physically stops against it via ordinary collision and can sit there as leftover debris until
 * something eventually touches it, exactly like the original. {@link #canHitEntity} is overridden to
 * <i>never</i> report a hit through vanilla's own raytrace pipeline, so that pipeline never fires
 * {@code onHitEntity}/discards the boulder on a mob or a block - all real collision logic here is the
 * manual per-tick check above instead.</li>
 * </ul>
 */
public class GolemBoulderEntity extends GolemThrowableEntity
{
	private static final EntityDataAccessor<BlockState> DATA_BLOCK = SynchedEntityData.defineId(GolemBoulderEntity.class, EntityDataSerializers.BLOCK_STATE);

	public GolemBoulderEntity(EntityType<? extends GolemBoulderEntity> type, Level level)
	{
		super(type, level);
	}

	/** Ported from {@code EntityBoulder}'s own target-throwing constructor (same param order/meaning). */
	public GolemBoulderEntity(Level level, LivingEntity shooter, LivingEntity target, float velocity, float wobble,
			float frontToBack, float yOffset, float sideToSide, BlockState mimicBlock, float damage)
	{
		super(MSUEntityTypes.GOLEM_BOULDER.get(), level, shooter, target, velocity, wobble, frontToBack, yOffset, sideToSide);
		setDamage(damage);
		setMimicBlock(mimicBlock);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		super.defineSynchedData(builder);
		builder.define(DATA_BLOCK, Blocks.STONE.defaultBlockState());
	}

	public void setMimicBlock(BlockState state)
	{
		entityData.set(DATA_BLOCK, state);
	}

	public BlockState getMimicBlock()
	{
		return entityData.get(DATA_BLOCK);
	}

	@Override
	protected boolean canHitEntity(Entity entity)
	{
		return false;
	}

	@Override
	public void tick()
	{
		super.tick();

		if(level().isClientSide())
		{
			for(int i = 0; i < 15; i++)
			{
				level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, getMimicBlock()), getX(), getY(), getZ(),
						random.nextDouble() / 5.0, random.nextDouble() / 5.0, random.nextDouble() / 5.0);
			}
		}
		else
		{
			hurtOverlappingTargets();
		}
	}

	/**
	 * Ported directly from {@code EntityBoulder#collideWithEntities}, widened from Player-only - see class
	 * doc comment. Filters out a positively-related target here (not just leaving it to
	 * {@code RelationshipCombatEvents}' own passive damage-zeroing hook) so the boulder doesn't even
	 * register as having "hit" its own shooter's owner at all - real fix, caught in a later audit: without
	 * this, {@code hit} would still flip {@code true} off a zeroed-damage "hit" and the boulder would
	 * needlessly explode/discard against someone it was never going to actually hurt, and vanilla's own
	 * {@code hurt()} pipeline can still apply its own small knockback even at 0 final damage.
	 */
	private void hurtOverlappingTargets()
	{
		List<LivingEntity> targets = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox(),
				e -> e != getOwner() && !(getOwner() instanceof LivingEntity shooter && RelationshipCombatEvents.isPositivelyRelated(shooter, e)));
		boolean hit = false;
		for(LivingEntity target : targets)
		{
			if(target.invulnerableTime != 0)
				continue;

			hit = true;
			target.hurt(damageSources().thrown(this, getOwner()), getDamage());
		}

		if(hit)
		{
			net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) level();
			serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, getMimicBlock()),
					getX(), getY(), getZ(), 40, 1.0, 1.0, 1.0, 0.0);
			playSound(SoundEvents.STONE_BREAK, 1.0F, 1.0F);
			discard();
		}
	}
}
