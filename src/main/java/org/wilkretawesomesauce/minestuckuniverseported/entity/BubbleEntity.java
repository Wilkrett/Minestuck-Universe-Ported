package org.wilkretawesomesauce.minestuckuniverseported.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code entity.EntityBubble} - a stationary, configurable
 * sphere used by three different aspects' abilitechs ({@code blood.TechBloodBubble} "Haemodome",
 * {@code breath.TechBreathBubble} "Entombing Twister", {@code mechanics.doom.TechDoomVoidBubble} "Abyss Cage"),
 * each spawning one with different size/color/lifespan and a different combination of the three
 * behavior flags below. It is spawned once at a fixed position and never moves - lifespan is what a
 * caster continuously refreshes while holding a bubble-tech's key, not a following personal shield.
 * <ul>
 *     <li>{@link #canEnter()} false - nothing new can cross into the bubble from outside (repelled).</li>
 *     <li>{@link #canExit()} false - anything already inside cannot leave (contained).</li>
 *     <li>{@link #getEnsnare()} - only meaningful alongside {@code !canExit()}; see the containment note
 *     below for why this project doesn't need a mechanically distinct implementation for it.</li>
 *     <li>{@link #getSuffocates()} - periodic damage to everything currently inside.</li>
 * </ul>
 * <p>
 * <b>Why containment/repulsion are hand-rolled instead of using collision boxes, unlike the original</b>:
 * the original's {@code !canEnter()} case returned a real solid {@code getCollisionBoundingBox()}. so
 * living entities were blocked by vanilla's own generic entity-collision physics for free, and the
 * {@code !canExit()} case injected extra thin-wall collision boxes for entities already "stuck" inside via
 * Forge 1.12.2's {@code GetCollisionBoxesEvent}. Confirmed via {@code javap} against this project's
 * pinned NeoForge jar: neither a generic "block other entities' movement like a wall" entity-collision
 * hook nor any collision-box-list-modification event exists in modern NeoForge (collision resolution is
 * internal to vanilla's own physics code). Both behaviors are reproduced here instead via direct
 * position/velocity correction every tick - repel by reverting to last tick's position for anything
 * newly crossing in, contain by clamping position to the bubble's bounds for anything already inside -
 * which is why {@link #getEnsnare()} doesn't need separate handling from the plain {@code !canExit()}
 * case here: both are enforced by the same per-tick clamp, since there's no "solid wall" state for
 * ensnare to distinguish itself from.
 */
public class BubbleEntity extends Entity
{
	private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(BubbleEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(BubbleEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_LIFESPAN = SynchedEntityData.defineId(BubbleEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_CAN_ENTER = SynchedEntityData.defineId(BubbleEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_CAN_EXIT = SynchedEntityData.defineId(BubbleEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_ENSNARE = SynchedEntityData.defineId(BubbleEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_SUFFOCATES = SynchedEntityData.defineId(BubbleEntity.class, EntityDataSerializers.BOOLEAN);

	/** Entities currently "let in" - only relevant while {@code !canExit()}; see class doc comment. */
	private final Set<UUID> contained = new HashSet<>();

	public BubbleEntity(EntityType<? extends BubbleEntity> type, Level level)
	{
		super(type, level);
		this.noPhysics = true;
	}

	public static BubbleEntity create(Level level, float size, int color, int lifespanTicks, boolean canEnter, boolean canExit, boolean ensnare)
	{
		BubbleEntity bubble = new BubbleEntity(org.wilkretawesomesauce.minestuckuniverseported.MSUEntityTypes.BUBBLE.get(), level);
		bubble.setBubbleSize(size);
		bubble.setColor(color);
		bubble.setLifespan(lifespanTicks);
		bubble.setCanEnter(canEnter);
		bubble.setCanExit(canExit);
		bubble.setEnsnare(ensnare);
		return bubble;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(DATA_SIZE, 3.0F);
		builder.define(DATA_COLOR, 0xFDB1E8);
		builder.define(DATA_LIFESPAN, 20);
		builder.define(DATA_CAN_ENTER, true);
		builder.define(DATA_CAN_EXIT, true);
		builder.define(DATA_ENSNARE, false);
		builder.define(DATA_SUFFOCATES, false);
	}

	@Override
	public void tick()
	{
		super.tick();

		if(level().isClientSide())
			return;

		int lifespan = getLifespan();
		if(lifespan > 0)
			setLifespan(lifespan - 1);
		if(lifespan == 0)
		{
			remove(RemovalReason.DISCARDED);
			return;
		}

		if(getSuffocates() && tickCount % 20 == 0)
			for(LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox()))
				target.hurt(damageSources().magic(), 2.0F);

		if(!canEnter())
			repelOutsiders();

		if(!canExit())
			containInsiders();
	}

	private void repelOutsiders()
	{
		for(Entity target : level().getEntitiesOfClass(Entity.class, getBoundingBox(), e -> e != this && !(e instanceof BubbleEntity)))
		{
			if(contained.contains(target.getUUID()))
				continue;

			Vec3 delta = new Vec3(target.getX() - target.xo, target.getY() - target.yo, target.getZ() - target.zo);
			if(delta.equals(Vec3.ZERO))
				continue;

			target.setPos(target.xo, target.yo, target.zo);
			target.setDeltaMovement(target.getDeltaMovement().scale(-0.2));
			target.hurtMarked = true;
		}
	}

	private void containInsiders()
	{
		AABB box = getBoundingBox();
		for(Entity target : level().getEntitiesOfClass(Entity.class, box.inflate(2.0), e -> e != this && !(e instanceof BubbleEntity)))
		{
			boolean inside = box.contains(target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ());
			if(inside)
				contained.add(target.getUUID());
			else if(contained.contains(target.getUUID()))
			{
				double x = Mth.clamp(target.getX(), box.minX, box.maxX);
				double y = Mth.clamp(target.getY(), box.minY, box.maxY - target.getBbHeight());
				double z = Mth.clamp(target.getZ(), box.minZ, box.maxZ);
				target.teleportTo(x, y, z);
				target.setDeltaMovement(Vec3.ZERO);
				target.hurtMarked = true;
			}
		}
	}

	public float getBubbleSize()
	{
		return entityData.get(DATA_SIZE);
	}

	public void setBubbleSize(float size)
	{
		entityData.set(DATA_SIZE, size);
		refreshDimensions();
	}

	@Override
	public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose)
	{
		float size = getBubbleSize();
		return net.minecraft.world.entity.EntityDimensions.scalable(size, size);
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

	public boolean canEnter()
	{
		return entityData.get(DATA_CAN_ENTER);
	}

	public void setCanEnter(boolean canEnter)
	{
		entityData.set(DATA_CAN_ENTER, canEnter);
	}

	public boolean canExit()
	{
		return entityData.get(DATA_CAN_EXIT);
	}

	public void setCanExit(boolean canExit)
	{
		entityData.set(DATA_CAN_EXIT, canExit);
	}

	public boolean getEnsnare()
	{
		return entityData.get(DATA_ENSNARE);
	}

	public void setEnsnare(boolean ensnare)
	{
		entityData.set(DATA_ENSNARE, ensnare);
	}

	public boolean getSuffocates()
	{
		return entityData.get(DATA_SUFFOCATES);
	}

	public void setSuffocates(boolean suffocates)
	{
		entityData.set(DATA_SUFFOCATES, suffocates);
	}

	@Override
	protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag)
	{
		if(tag.contains("Size"))
			setBubbleSize(tag.getFloat("Size"));
		if(tag.contains("Color"))
			setColor(tag.getInt("Color"));
		if(tag.contains("Lifespan"))
			setLifespan(tag.getInt("Lifespan"));
		if(tag.contains("CanEnter"))
			setCanEnter(tag.getBoolean("CanEnter"));
		if(tag.contains("CanExit"))
			setCanExit(tag.getBoolean("CanExit"));
		if(tag.contains("Ensnare"))
			setEnsnare(tag.getBoolean("Ensnare"));
		if(tag.contains("Suffocates"))
			setSuffocates(tag.getBoolean("Suffocates"));
	}

	@Override
	protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag)
	{
		tag.putFloat("Size", getBubbleSize());
		tag.putInt("Color", getColor());
		tag.putInt("Lifespan", getLifespan());
		tag.putBoolean("CanEnter", canEnter());
		tag.putBoolean("CanExit", canExit());
		tag.putBoolean("Ensnare", getEnsnare());
		tag.putBoolean("Suffocates", getSuffocates());
	}

	@Override
	public boolean isPickable()
	{
		return false;
	}

	/** Client-only helper for {@code client.render.BubbleRenderer}: how transparent the bubble should look right now, fading in on spawn and out as it nears the end of its life. */
	public float getAlpha()
	{
		return Math.min(1.0F, tickCount / 20.0F) * Math.min(1.0F, getLifespan() / 10.0F);
	}
}
