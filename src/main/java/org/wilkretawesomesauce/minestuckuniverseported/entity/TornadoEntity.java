package org.wilkretawesomesauce.minestuckuniverseported.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.MSUEntityTypes;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.WindEngine;

/**
 * Original design for this project, no 1.12.2 counterpart - a small, stationary, purely cosmetic
 * "tornado" (a swirling funnel of wind), visually in the spirit of vanilla's own Breeze wind-charge
 * gust effect. A direct user request, deliberately scoped to visuals only for this pass: no
 * damage/pull/knockback, no movement of its own, and not yet wired to any Tech/Abilitech - see this
 * class's own {@link #tick()} for the one thing it actually does (call
 * {@link WindEngine#tornado}, its real caller, every server tick while alive).
 * <p>
 * Modeled directly on {@link BubbleEntity} - same {@code noPhysics}/not-pickable/synced-size-color-
 * lifespan/{@code create(...)} factory shape - since both are "spawn once at a fixed position, exist
 * for a countdown lifespan, do something every tick" cosmetic entities. Unlike {@link BubbleEntity}
 * this entity draws nothing itself ({@code client.render.TornadoRenderer} is a deliberate no-op) -
 * every visual comes from the server-broadcast {@link org.wilkretawesomesauce.minestuckuniverseported.util.MSUParticles#spawnWindWisp}
 * particles {@link WindEngine#tornado} spawns, the same "particles are the whole visual, no mesh"
 * choice already made for this project's other Breath wind effects.
 */
public class TornadoEntity extends Entity
{
	private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_LIFESPAN = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.INT);

	public TornadoEntity(EntityType<? extends TornadoEntity> type, Level level)
	{
		super(type, level);
		this.noPhysics = true;
	}

	public static TornadoEntity create(Level level, float size, int color, int lifespanTicks)
	{
		TornadoEntity tornado = new TornadoEntity(MSUEntityTypes.TORNADO.get(), level);
		tornado.setTornadoSize(size);
		tornado.setColor(color);
		tornado.setLifespan(lifespanTicks);
		return tornado;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(DATA_SIZE, 1.0F);
		builder.define(DATA_COLOR, 0x47E2FA);
		builder.define(DATA_LIFESPAN, 100);
	}

	@Override
	public void tick()
	{
		super.tick();

		int lifespan = getLifespan();
		if(!level().isClientSide())
		{
			if(lifespan > 0)
				setLifespan(lifespan - 1);
			if(lifespan == 0)
			{
				remove(RemovalReason.DISCARDED);
				return;
			}
		}

		WindEngine.tornado(level(), position(), getTornadoSize(), tickCount, getColor(), 1.0F);
	}

	public float getTornadoSize()
	{
		return entityData.get(DATA_SIZE);
	}

	public void setTornadoSize(float size)
	{
		entityData.set(DATA_SIZE, size);
		refreshDimensions();
	}

	@Override
	public EntityDimensions getDimensions(Pose pose)
	{
		float size = getTornadoSize();
		return EntityDimensions.scalable(2.2F * size, 3.5F * size);
	}

	public int getColor()
	{
		return entityData.get(DATA_COLOR);
	}

	public void setColor(int color)
	{
		entityData.set(DATA_COLOR, color);
	}

	public int getLifespan()
	{
		return entityData.get(DATA_LIFESPAN);
	}

	public void setLifespan(int lifespan)
	{
		entityData.set(DATA_LIFESPAN, lifespan);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag)
	{
		if(tag.contains("Size"))
			setTornadoSize(tag.getFloat("Size"));
		if(tag.contains("Color"))
			setColor(tag.getInt("Color"));
		if(tag.contains("Lifespan"))
			setLifespan(tag.getInt("Lifespan"));
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag)
	{
		tag.putFloat("Size", getTornadoSize());
		tag.putInt("Color", getColor());
		tag.putInt("Lifespan", getLifespan());
	}

	@Override
	public boolean isPickable()
	{
		return false;
	}
}
