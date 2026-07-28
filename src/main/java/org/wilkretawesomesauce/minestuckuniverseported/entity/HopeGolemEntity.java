package org.wilkretawesomesauce.minestuckuniverseported.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code entity.EntityHopeGolem} ("Willed Alliance"'s real
 * ally, replacing the self-buff stand-in {@code hope.TechHopeGolem} used before this pass) - a
 * personal combat ally summoned by holding the tech, extending vanilla's own {@link IronGolem} exactly
 * like the original did (reusing vanilla's model/attributes/attack-animation infrastructure rather than
 * building a golem from scratch).
 * <p>
 * {@link #getHopeTicks()} is both a power level and a lifespan: it decays every tick (faster the lower
 * the owner's own health is, or fast-decays entirely once the owner is dead/gone), scaled up further by
 * how many other Hope Golem allies the same owner already has out (discourages spamming a golem army for
 * free) - once it runs out, the golem despawns. It also directly caps how much damage the golem can take
 * per hit ({@link #hurt}): near-full hope ticks makes it nearly untouchable, while a nearly-spent golem
 * takes much more per hit - matches the original's exact formula.
 * <p>
 * The three owner-relationship goals ({@link FollowOwnerGoal}, {@link OwnerHurtByTargetGoal},
 * {@link OwnerHurtTargetGoal}) are custom rather than reusing vanilla's own {@code FollowOwnerGoal}/
 * {@code OwnerHurtByTargetGoal}/{@code OwnerHurtTargetGoal}, because those vanilla classes are written
 * specifically for {@code TamableAnimal} (this golem isn't tameable, it extends {@code IronGolem}
 * directly, matching the original) - same idea, different owner-lookup plumbing. The follow-goal's
 * teleport-when-stuck-far-away fallback is simplified to a direct teleport next to the owner rather than
 * the original's 5x5 safe-landing-spot block scan - a minor fidelity trade for a lot less code, not a
 * core mechanic.
 */
public class HopeGolemEntity extends IronGolem
{
	public static final int MAX_HOPE_TICKS = 7200;
	public static final int MAX_EFFECTIVE_TICKS = 6000;

	private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID = SynchedEntityData.defineId(HopeGolemEntity.class, EntityDataSerializers.OPTIONAL_UUID);
	private static final EntityDataAccessor<Integer> DATA_HOPE_TICKS = SynchedEntityData.defineId(HopeGolemEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_ANGRY = SynchedEntityData.defineId(HopeGolemEntity.class, EntityDataSerializers.BOOLEAN);

	public HopeGolemEntity(EntityType<? extends IronGolem> type, Level level)
	{
		super(type, level);
		setPlayerCreated(true);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		super.defineSynchedData(builder);
		builder.define(DATA_OWNER_UUID, Optional.empty());
		builder.define(DATA_HOPE_TICKS, MAX_EFFECTIVE_TICKS);
		builder.define(DATA_ANGRY, false);
	}

	@Override
	protected void registerGoals()
	{
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
		this.goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 0.9, 32.0F));
		this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
		this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.0, 10.0F, 2.0F));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

		this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
		this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, false, true,
				target -> target != null && !(target instanceof Creeper) && shouldAttackEntity(target, getOwner())));
	}

	@Override
	public void tick()
	{
		super.tick();

		if(level().isClientSide())
			return;

		entityData.set(DATA_ANGRY, getTarget() != null);

		int hopeDecay = 1;
		LivingEntity owner = getOwner();
		if(owner != null)
			hopeDecay = !owner.isAlive() ? MAX_EFFECTIVE_TICKS : Math.max(10 - (int) (owner.getHealth() / owner.getMaxHealth() * 10), 1);

		long allyCount = level().getEntitiesOfClass(HopeGolemEntity.class, getBoundingBox().inflate(128),
				other -> other != this && other.getOwner() == getOwner()).size();
		hopeDecay *= allyCount * 2 + 1;

		setHopeTicks(getHopeTicks() - hopeDecay);
		if(getHopeTicks() < 0)
			remove(RemovalReason.DISCARDED);
	}

	@Override
	public boolean hurt(DamageSource source, float amount)
	{
		return super.hurt(source, Math.min(amount, 10.0F * (1.0F - (float) getHopeTicks() / MAX_EFFECTIVE_TICKS)));
	}

	/**
	 * Real bug fix: this used to skip {@code super.doHurtTarget} entirely to swap in a custom damage
	 * source/roll, which also silently skipped vanilla {@code IronGolem#doHurtTarget}'s own real
	 * animation-triggering side effect - confirmed via {@code javap} that vanilla's implementation does
	 * {@code this.attackAnimationTick = 10; this.level().broadcastEntityEvent(this, (byte) 4);} before
     * applying damage (that same broadcast is also what plays the iron golem attack sound, via
	 * {@code handleEntityEvent}, inherited unchanged). Without it, the golem never played its attack swing
	 * animation or sound - a real, reported bug ("hope golem has no animations"), not a hypothetical one.
	 * Replicated here directly rather than calling {@code super.doHurtTarget} (which would also apply
	 * vanilla's own damage source/amount, which this method deliberately overrides).
	 */
	@Override
	public boolean doHurtTarget(Entity target)
	{
		level().broadcastEntityEvent(this, (byte) 4);

		DamageSource source = getHopeTicks() >= MAX_EFFECTIVE_TICKS * 0.8 ? damageSources().magic() : damageSources().mobAttack(this);
		return target.hurt(source, 7 + random.nextInt(15));
	}

	public int getHopeTicks()
	{
		return entityData.get(DATA_HOPE_TICKS);
	}

	public void setHopeTicks(int ticks)
	{
		entityData.set(DATA_HOPE_TICKS, Math.min(MAX_HOPE_TICKS, ticks));
	}

	/**
	 * Real bug fix, not the original's own shape: this used to derive straight from {@link #getTarget()}
	 * (matching the original's own {@code isAngry()}, driven by {@code setAttackTarget}) - but {@code Mob}'s
	 * own {@code target} field (confirmed via {@code javap}) is plain server-side state, never included in
	 * {@link #defineSynchedData}, so {@link #getTarget()} is always {@code null} on the client. Since
	 * {@code client.render.HopeGolemRenderer} (the only real caller of this method) runs client-side, the
	 * enraged texture could never actually show - a real bug, not a hypothetical one. {@link #DATA_ANGRY}
	 * mirrors the real server-side target state into real synced entity data every server tick instead.
	 */
	public boolean isAngry()
	{
		return entityData.get(DATA_ANGRY);
	}

	@Nullable
	public UUID getOwnerId()
	{
		return entityData.get(DATA_OWNER_UUID).orElse(null);
	}

	public void setOwnerId(@Nullable UUID id)
	{
		entityData.set(DATA_OWNER_UUID, Optional.ofNullable(id));
	}

	public void setCreatedBy(Player player)
	{
		setOwnerId(player.getUUID());
	}

	@Nullable
	public LivingEntity getOwner()
	{
		UUID id = getOwnerId();
		if(id == null)
			return null;
		return level() instanceof ServerLevel serverLevel ? serverLevel.getServer().getPlayerList().getPlayer(id) : null;
	}

	public boolean hasOwner()
	{
		return getOwner() != null;
	}

	public boolean shouldAttackEntity(LivingEntity target, @Nullable LivingEntity owner)
	{
		return target != owner && !(target instanceof Creeper);
	}

	@Override
	public boolean isAlliedTo(Entity entity)
	{
		if(hasOwner())
		{
			LivingEntity owner = getOwner();
			if(entity == owner)
				return true;
			if(owner != null)
				return owner.isAlliedTo(entity);
		}
		return super.isAlliedTo(entity);
	}

	@Override
	public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag)
	{
		super.addAdditionalSaveData(tag);
		tag.putInt("HopeTicks", getHopeTicks());
		if(hasOwner())
			tag.putUUID("OwnerUUID", getOwnerId());
	}

	@Override
	public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag)
	{
		super.readAdditionalSaveData(tag);
		if(tag.contains("HopeTicks"))
			setHopeTicks(tag.getInt("HopeTicks"));
		if(tag.hasUUID("OwnerUUID"))
			setOwnerId(tag.getUUID("OwnerUUID"));
	}

	/** Custom equivalent of vanilla's {@code TamableAnimal}-only {@code FollowOwnerGoal} - see class doc comment for why this can't just reuse the vanilla one. */
	private static class FollowOwnerGoal extends Goal
	{
		private final HopeGolemEntity golem;
		private final double speed;
		private final PathNavigation navigation;
		private final float minDist;
		private final float maxDist;
		private LivingEntity owner;
		private int timeToRecalcPath;

		FollowOwnerGoal(HopeGolemEntity golem, double speed, float minDist, float maxDist)
		{
			this.golem = golem;
			this.speed = speed;
			this.navigation = golem.getNavigation();
			this.minDist = minDist;
			this.maxDist = maxDist;
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse()
		{
			LivingEntity owner = golem.getOwner();
			if(owner == null || (owner instanceof Player p && p.isSpectator()))
				return false;
			if(golem.distanceToSqr(owner) < minDist * minDist)
				return false;
			this.owner = owner;
			return true;
		}

		@Override
		public boolean canContinueToUse()
		{
			return !navigation.isDone() && golem.distanceToSqr(owner) > maxDist * maxDist;
		}

		@Override
		public void start()
		{
			timeToRecalcPath = 0;
		}

		@Override
		public void stop()
		{
			owner = null;
			navigation.stop();
		}

		@Override
		public void tick()
		{
			LookControl lookControl = golem.getLookControl();
			lookControl.setLookAt(owner, 10.0F, golem.getMaxHeadXRot());

			if(--timeToRecalcPath <= 0)
			{
				timeToRecalcPath = 10;
				if(golem.getLeashData() == null && !golem.isPassenger())
				{
					if(golem.distanceToSqr(owner) >= 144.0)
						golem.teleportTo(owner.getX(), owner.getY(), owner.getZ());
					else
						navigation.moveTo(owner, speed);
				}
			}
		}
	}

	/** Custom equivalent of vanilla's {@code OwnerHurtByTargetGoal} (also {@code TamableAnimal}-only) - retaliate against whoever last hurt the owner. */
	private static class OwnerHurtByTargetGoal extends Goal
	{
		private final HopeGolemEntity golem;
		private LivingEntity attacker;
		private int timestamp;

		OwnerHurtByTargetGoal(HopeGolemEntity golem)
		{
			this.golem = golem;
			setFlags(EnumSet.of(Flag.TARGET));
		}

		@Override
		public boolean canUse()
		{
			LivingEntity owner = golem.getOwner();
			if(owner == null)
				return false;

			attacker = owner.getLastHurtByMob();
			int time = owner.getLastHurtByMobTimestamp();
			return time != timestamp && attacker != null && golem.shouldAttackEntity(attacker, owner);
		}

		@Override
		public void start()
		{
			golem.setTarget(attacker);
			LivingEntity owner = golem.getOwner();
			if(owner != null)
				timestamp = owner.getLastHurtByMobTimestamp();
		}
	}

	/** Custom equivalent of vanilla's {@code OwnerHurtTargetGoal} - pile onto whatever the owner is currently attacking. */
	private static class OwnerHurtTargetGoal extends Goal
	{
		private final HopeGolemEntity golem;
		private LivingEntity attacker;
		private int timestamp;

		OwnerHurtTargetGoal(HopeGolemEntity golem)
		{
			this.golem = golem;
			setFlags(EnumSet.of(Flag.TARGET));
		}

		@Override
		public boolean canUse()
		{
			LivingEntity owner = golem.getOwner();
			if(owner == null)
				return false;

			attacker = owner.getLastHurtMob();
			int time = owner.getLastHurtMobTimestamp();
			return time != timestamp && attacker != null && golem.shouldAttackEntity(attacker, owner);
		}

		@Override
		public void start()
		{
			golem.setTarget(attacker);
			LivingEntity owner = golem.getOwner();
			if(owner != null)
				timestamp = owner.getLastHurtMobTimestamp();
		}
	}
}
