package org.wilkretawesomesauce.minestuckuniverseported.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.wilkretawesomesauce.minestuckuniverseported.MSUEntityTypes;

/**
 * Ported from ModularBosses (1.8)'s {@code entity.projectile.EntityCustomEgg} - a real sibling of
 * {@link GolemBoulderEntity} under the shared {@link GolemThrowableEntity} base (matching the original's
 * own {@code EntityCustomEgg extends EntityMobThrowable}), not vanilla's item-stack-carrying
 * {@code ThrowableItemProjectile}/{@code ThrownEgg} - the original egg never carried a real
 * {@code ItemStack} at all, it just always rendered a fixed icon (confirmed against the original's own
 * {@code ModularBossesEntities} registration: {@code RenderSnowball(manager, ModularBossesItems.spawn_egg, itemRender)},
 * a fixed-item billboard, not a per-entity one - see {@link org.wilkretawesomesauce.minestuckuniverseported.client.render.GolemEggRenderer}
 * for the real modern equivalent of that same fixed-icon renderer). Always spawns exactly one
 * {@link GolemEntity} on impact, matching the original's own unconditional {@code spawnCreature} call in
 * {@code onImpact}.
 */
public class GolemEggEntity extends GolemThrowableEntity
{
	public GolemEggEntity(EntityType<? extends GolemEggEntity> type, Level level)
	{
		super(type, level);
	}

	public GolemEggEntity(Level level, LivingEntity shooter)
	{
		super(MSUEntityTypes.GOLEM_EGG.get(), level, shooter);
	}

	public GolemEggEntity(Level level, double x, double y, double z)
	{
		super(MSUEntityTypes.GOLEM_EGG.get(), level, x, y, z);
	}

	@Override
	public void handleEntityEvent(byte id)
	{
		if(id == 3)
		{
			for(int i = 0; i < 8; i++)
			{
				level().addParticle(ParticleTypes.POOF, getX(), getY(), getZ(),
						((double) random.nextFloat() - 0.5) * 0.08, ((double) random.nextFloat() - 0.5) * 0.08, ((double) random.nextFloat() - 0.5) * 0.08);
			}
		}
	}

	@Override
	protected void onHit(HitResult result)
	{
		super.onHit(result);
		if(level().isClientSide())
			return;

		GolemEntity golem = MSUEntityTypes.GOLEM.get().create(level());
		if(golem != null)
		{
			golem.moveTo(result.getLocation().x, result.getLocation().y, result.getLocation().z,
					Mth.wrapDegrees(random.nextFloat() * 360.0F), 0.0F);
			level().addFreshEntity(golem);
		}

		level().broadcastEntityEvent(this, (byte) 3);
		discard();
	}
}
