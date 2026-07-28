package org.wilkretawesomesauce.minestuckuniverseported.capabilities.beam;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code capabilities.beam.Beam} - not an {@link Entity}, a plain
 * object tracked in {@link BeamData}. Every tick it grows outward from its source, hand-rolling its own
 * raytrace against both blocks and entities (real vanilla entity collision doesn't apply here since nothing
 * about this is a real entity), applying damage/knockback along the way and calling into {@link IPropertyBeam}
 * hooks (see that interface's own doc comment for how this port's property system differs from the
 * original's much larger, mostly-unrelated generic {@code WeaponProperty} framework).
 * <p>
 * Ticked identically on both logical sides (see {@code beam.BeamEvents}) from the same synced starting
 * state, exactly like the original's {@code onWorldTick}/{@code onClientTick} pair - the growing-beam
 * animation depends on the client replaying the same deterministic tick logic locally between full-state
 * resyncs, not on interpolating between server-sent snapshots every tick.
 */
public class Beam
{
	private static final ResourceKey<DamageType> BEAM_DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, Minestuckuniverseported.id("beam"));
	private static final ResourceLocation DEFAULT_TEXTURE = Minestuckuniverseported.id("textures/entity/projectiles/beam.png");

	public Level level;
	public ItemStack sourceStack = ItemStack.EMPTY;
	public Entity source;
	public UUID sourceUuid;
	private final UUID uuid;
	private boolean dead = false;
	private int ageInTicks = 0;
	public int color = 0xFFFFFF;

	public float damage = 10;
	protected int duration = 10;
	public int decayTime = duration;
	protected float length = 0;
	protected boolean released = false;
	protected boolean anchorToSource = false;
	protected int damageCooldown = 0;

	public double posX, posY, posZ;
	public double motionX, motionY, motionZ;
	public double prevPosX, prevPosY, prevPosZ;
	public double prevMotionX, prevMotionY, prevMotionZ;

	public Beam(Level level, UUID uuid)
	{
		this.level = level;
		this.uuid = uuid;
	}

	public Beam(LivingEntity source, ItemStack stack, float speed)
	{
		this(source.level(), UUID.randomUUID());

		motionX *= speed;
		motionY *= speed;
		motionZ *= speed;
		setPositionToEntity(source);

		sourceStack = stack;
		this.source = source;
		anchorToSource = true;
	}

	public void setPositionToEntity(Entity source)
	{
		posX = source.getX();
		posY = source.getY() + source.getEyeHeight() * 0.8;
		posZ = source.getZ();

		double beamSpeed = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);

		motionX = -Mth.sin(source.getYRot() * 0.017453292F) * Mth.cos(source.getXRot() * 0.017453292F) * beamSpeed + source.getDeltaMovement().x;
		motionY = -Mth.sin(source.getXRot() * 0.017453292F);
		motionZ = Mth.cos(source.getYRot() * 0.017453292F) * Mth.cos(source.getXRot() * 0.017453292F) * beamSpeed + source.getDeltaMovement().z;
	}

	public void onUpdate()
	{
		ageInTicks++;
		if(ageInTicks > 12000)
		{
			setDead();
			return;
		}

		if(source == null && sourceUuid != null && level instanceof net.minecraft.server.level.ServerLevel serverLevel)
			source = serverLevel.getEntity(sourceUuid);

		prevPosX = posX;
		prevPosY = posY;
		prevPosZ = posZ;
		prevMotionX = motionX;
		prevMotionY = motionY;
		prevMotionZ = motionZ;

		damageCooldown = Math.max(0, damageCooldown - 1);

		if(sourceStack.getItem() instanceof IPropertyBeam property)
			property.onBeamTick(sourceStack, this);

		if(!released)
		{
			if(anchorToSource && !sourceStack.isEmpty() && source instanceof LivingEntity livingSource
					&& !ItemStack.matches(livingSource.getUseItem(), sourceStack))
			{
				if(source instanceof Player player)
					player.getCooldowns().addCooldown(sourceStack.getItem(), decayTime);
				released = true;
			}
			else if(length > 512)
				released = true;
		}

		if(source != null && anchorToSource)
			setPositionToEntity(source);

		if(released)
		{
			decayTime--;
			if(decayTime < 0)
				setDead();
		}

		Vec3 startVec = getStartPoint(1);
		Vec3 endVec = getEndPoint(1);

		Entity target = findEntityOnPath(startVec, endVec);
		if(target != null)
		{
			length = (float) Math.sqrt((posX - target.getX()) * (posX - target.getX()) + (posY - target.getY()) * (posY - target.getY()) + (posZ - target.getZ()) * (posZ - target.getZ()));
			endVec = new Vec3(motionX, motionY, motionZ).normalize().scale(length).add(startVec);
		}
		else
		{
			BlockHitResult hit = level.clip(new ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
			if(hit.getType() != HitResult.Type.MISS)
			{
				Vec3 hitVec = hit.getLocation();
				length = (float) Math.sqrt((posX - hitVec.x) * (posX - hitVec.x) + (posY - hitVec.y) * (posY - hitVec.y) + (posZ - hitVec.z) * (posZ - hitVec.z));
				endVec = new Vec3(motionX, motionY, motionZ).normalize().scale(length).add(startVec);
			}
		}

		startVec = endVec;
		endVec = endVec.add(motionX, motionY, motionZ);

		target = findEntityOnPath(startVec, endVec);
		if(target != null)
		{
			DamageSource damageSource = new DamageSource(level.registryAccess().holderOrThrow(BEAM_DAMAGE_TYPE), source);

			if(sourceStack.getItem() instanceof IPropertyBeam property)
				damageSource = property.onEntityImpact(sourceStack, this, target, damageSource);

			if(damageSource != null && damageCooldown <= 0 && damage != 0 && target.invulnerableTime <= 0)
			{
				if(damage < 0)
				{
					if(target instanceof LivingEntity livingTarget)
						livingTarget.heal(-damage);
				}
				else
					target.hurt(damageSource, damage);

				target.invulnerableTime = sourceStack.getItem() instanceof IBeamStats beamStats ? beamStats.getBeamHurtTime(sourceStack) : 15;
			}
		}
		else
		{
			BlockHitResult blockHit = level.clip(new ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
			if(blockHit.getType() == HitResult.Type.MISS)
			{
				length += (float) Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
			}
			else if(sourceStack.getItem() instanceof IPropertyBeam property)
			{
				property.onBlockImpact(sourceStack, this, blockHit.getBlockPos());
			}
		}
	}

	public boolean isDead()
	{
		return dead;
	}

	public void setDead()
	{
		dead = true;
	}

	public static void fireBeam(Beam beam)
	{
		beam.level.getData(org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments.BEAM_DATA).addBeam(beam.level, beam);
		BeamEvents.broadcast(beam.level);
	}

	public void readFromNBT(CompoundTag nbt)
	{
		posX = nbt.getDouble("PosX");
		posY = nbt.getDouble("PosY");
		posZ = nbt.getDouble("PosZ");

		motionX = nbt.getDouble("MotionX");
		motionY = nbt.getDouble("MotionY");
		motionZ = nbt.getDouble("MotionZ");

		color = nbt.getInt("Color");
		ageInTicks = nbt.getInt("Age");
		decayTime = nbt.getInt("DecayTime");
		duration = nbt.getInt("Duration");
		damageCooldown = nbt.getInt("DamageCooldown");
		damage = nbt.getFloat("Damage");
		length = nbt.getFloat("Length");
		anchorToSource = nbt.getBoolean("Anchored");
		released = nbt.getBoolean("BeamReleased");

		sourceStack = ItemStack.parseOptional(level.registryAccess(), nbt.getCompound("Item"));
		if(nbt.hasUUID("Source"))
			sourceUuid = nbt.getUUID("Source");
	}

	public CompoundTag writeToNBT()
	{
		CompoundTag nbt = new CompoundTag();
		nbt.putUUID("UUID", uuid);

		nbt.putDouble("PosX", posX);
		nbt.putDouble("PosY", posY);
		nbt.putDouble("PosZ", posZ);

		nbt.putDouble("MotionX", motionX);
		nbt.putDouble("MotionY", motionY);
		nbt.putDouble("MotionZ", motionZ);

		nbt.putInt("Color", color);
		nbt.putInt("Age", ageInTicks);
		nbt.putInt("DecayTime", decayTime);
		nbt.putInt("Duration", duration);
		nbt.putInt("DamageCooldown", damageCooldown);
		nbt.putFloat("Damage", damage);
		nbt.putFloat("Length", length);
		nbt.putBoolean("Anchored", anchorToSource);
		nbt.putBoolean("BeamReleased", released);

		nbt.put("Item", sourceStack.save(level.registryAccess()));
		if(source != null)
			nbt.putUUID("Source", source.getUUID());

		return nbt;
	}

	public void releaseBeam()
	{
		released = true;
	}

	public boolean isBeamReleased()
	{
		return released;
	}

	public UUID getUniqueID()
	{
		return uuid;
	}

	public Vec3 getStartPoint(float partialTicks)
	{
		return new Vec3(lerp(prevPosX, posX, partialTicks), lerp(prevPosY, posY, partialTicks), lerp(prevPosZ, posZ, partialTicks));
	}

	public Vec3 getEndPoint(float partialTicks)
	{
		double x = lerp(prevMotionX, motionX, partialTicks), y = lerp(prevMotionY, motionY, partialTicks), z = lerp(prevMotionZ, motionZ, partialTicks);
		return new Vec3(x, y, z).normalize().scale(length)
				.add(lerp(prevPosX, posX, partialTicks) + x, lerp(prevPosY, posY, partialTicks) + y, lerp(prevPosZ, posZ, partialTicks) + z);
	}

	public static double lerp(double a, double b, double amount)
	{
		return a + (b - a) * amount;
	}

	@Nullable
	protected Entity findEntityOnPath(Vec3 start, Vec3 end)
	{
		Entity found = null;
		Vec3 diff = end.subtract(start);

		AABB searchBox = new AABB(start.x, start.y, start.z, start.x, start.y, start.z).expandTowards(diff).inflate(1.0D);
		List<Entity> list = level.getEntities(source, searchBox, e -> !e.isSpectator() && e.isAlive() && e.isPickable());

		double closestDistSqr = 0.0D;
		for(Entity candidate : list)
		{
			AABB box = candidate.getBoundingBox().inflate(0.30000001192092896D);
			var intercept = box.clip(start, end);
			if(intercept.isPresent())
			{
				double distSqr = start.distanceToSqr(intercept.get());
				if(distSqr < closestDistSqr || closestDistSqr == 0.0D)
				{
					found = candidate;
					closestDistSqr = distSqr;
				}
			}
		}

		return found;
	}

	public float getLength()
	{
		return length;
	}

	public float getAlpha()
	{
		return (float) decayTime / (float) duration;
	}

	public int getDuration()
	{
		return duration;
	}

	public void setDuration(int duration)
	{
		decayTime = (int) ((float) duration / (float) this.duration * decayTime);
		this.duration = duration;
	}

	public float getRadius()
	{
		return sourceStack.getItem() instanceof IBeamStats beamStats ? beamStats.getBeamRadius(sourceStack) : 0.05f;
	}

	public ResourceLocation getTexture()
	{
		return sourceStack.getItem() instanceof IBeamStats beamStats ? beamStats.getBeamTexture() : DEFAULT_TEXTURE;
	}
}
