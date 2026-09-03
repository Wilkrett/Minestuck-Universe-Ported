package org.wilkretawesomesauce.minestuckuniverseported.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Ported from ModularBosses (1.8)'s {@code entity.EntityGolem} - a hostile golem that "wakes up" out of
 * whatever solid block it spawns on top of, mimicking that block's texture and scaling its own max
 * health/attack damage off that block's real hardness ({@link #resolveMimicBlock()}, matching the
 * original's own {@code getTexture()}). Combat is driven by a hand-authored keyframe animation state
 * machine (see {@link org.wilkretawesomesauce.minestuckuniverseported.client.model.golem.GolemModel})
 * rather than vanilla attack animations - {@link #aniID} (one of {@link #BUILD}/{@link #STAND}/
 * {@link #THROW}/{@link #ROLL}/{@link #STOMP}/{@link #DIE}, synced) picks the active animation,
 * {@link #aniFrame} is a plain tick counter (both logical sides, unsynced - deterministically
 * re-derived from {@link #aniID} transitions exactly like the original's own {@code aniFrame}) that the
 * model reads directly every frame.
 * <p>
 * Three real attacks, matching the original's own {@code attackPicker}: {@link #THROW} lobs a
 * {@link GolemBoulderEntity} textured like the mimicked block; {@link #STOMP} erupts real
 * {@link GolemFallingBlockEntity} chunks of whatever's actually underfoot around the golem; {@link #ROLL}
 * is a body-slam charge that knocks back and damages anyone in its path.
 * <p>
 * <b>Known simplifications</b> (stated plainly, not oversights): targeting uses vanilla's own
 * {@link NearestAttackableTargetGoal} (nearest visible player) rather than the original's own
 * random-visible-player pick; the original's separately-configurable loot-string-list is replaced with
 * a flat drop of the mimicked block itself (see {@link #dropCustomDeathLoot}) - simpler, and thematically
 * consistent ("the golem is that block"), rather than porting the original's whole
 * {@code "chance|qty|itemName"} config parser for one mob.
 * <p>
 * <b>Ownership is enforced generically, not tracked on this class at all</b> (real, project-original
 * addition, no 1.12.2 counterpart - see {@code heroClass.maid.mind.TechMaidMindConstructGolem}, this
 * golem's own Maid-of-Mind summon tech): this entity carries no owner field, no owner-aware
 * {@code setTarget} override, and no owner-aware goals of its own. The moment that tech creates a real
 * {@code mechanics.relationship.RelationshipType#OWNERSHIP} relationship between a summoned golem and its
 * caster, {@code mechanics.relationship.RelationshipCombatEvents} is what makes that relationship mean
 * anything behaviorally - never targeting or damaging the other side of an Ownership (or any other
 * positive) relationship, and (a direct user report: "when I summon the golem + attack something else, it
 * doesn't attack that target too - it should behave as if it were a wolf") defending/assisting the owner
 * against whatever's attacking or being attacked. That class's own doc comment covers the mechanism in
 * full - this is deliberate: the same generic enforcement applies to any future summon with an Ownership
 * relationship, not just this one golem, without each needing its own bespoke copy of this logic.
 * {@link #kickEntities} (ROLL) and {@link GolemBoulderEntity}/{@link GolemFallingBlockEntity}'s own
 * nearby-target damage were real-widened from the original's own Player-only filter to general
 * {@code LivingEntity} so an assist target (usually a hostile mob) can actually be hurt at all - the
 * generic relationship check is what then keeps the owner themselves safe from that same widened damage,
 * not any per-entity owner field.
 */
public class GolemEntity extends Monster
{
	public static final int BUILD = 0;
	public static final int STAND = 1;
	public static final int THROW = 2;
	public static final int ROLL = 3;
	public static final int STOMP = 4;
	public static final int DIE = 5;

	/** The golem's max health is its mimicked spawn block's own hardness multiplied by this. */
	private static final double MAX_HEALTH_MULTIPLIER = 20.0;
	/** The golem's attack damage is its mimicked spawn block's own hardness multiplied by this. */
	private static final double DAMAGE_MULTIPLIER = 1.0;
	/** Minimum ticks between the golem picking a new attack (Throw/Stomp/Roll) while it has a target. */
	private static final int ATTACK_COOLDOWN_TICKS = 40;
	/** Experience dropped on killing a golem. */
	private static final int EXP_DROP = 100;

	private static final EntityDataAccessor<Integer> DATA_ANI_ID = SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<BlockState> DATA_MIMIC_BLOCK = SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.BLOCK_STATE);
	private static final EntityDataAccessor<Boolean> DATA_MIMIC_RESOLVED = SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.BOOLEAN);

	/** Client- and server-side both, matches {@link #aniID} every tick - see class doc comment. */
	public int aniID = BUILD;
	private int prevAniID = BUILD;
	public int aniFrame;

	private int attackCooldown;
	private int rollCount;
	private BlockPos rollTargetPos;
	private float mimicHardness = 1.0F;

	public GolemEntity(EntityType<? extends GolemEntity> type, Level level)
	{
		super(type, level);
		xpReward = EXP_DROP;
	}

	public static AttributeSupplier.Builder createAttributes()
	{
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 10.0)
				.add(Attributes.FOLLOW_RANGE, 20.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.MOVEMENT_SPEED, 0.699)
				.add(Attributes.ATTACK_DAMAGE, 2.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		super.defineSynchedData(builder);
		builder.define(DATA_ANI_ID, BUILD);
		builder.define(DATA_MIMIC_BLOCK, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
		builder.define(DATA_MIMIC_RESOLVED, false);
	}

	@Override
	protected void registerGoals()
	{
		this.goalSelector.addGoal(1, new GolemCombatGoal(this));
		this.goalSelector.addGoal(2, new GolemWanderGoal(this, 0.25));
		this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 20, true, false, null));
	}

	// ================================================================================================
	// Block mimicry - ported from EntityGolem#getTexture
	// ================================================================================================

	public BlockState getMimicBlock()
	{
		return entityData.get(DATA_MIMIC_BLOCK);
	}

	private void resolveMimicBlock()
	{
		if(entityData.get(DATA_MIMIC_RESOLVED))
			return;

		BlockPos pos = blockPosition();
		int guard = 0;
		while(!level().getBlockState(pos).blocksMotion() && pos.getY() > level().getMinBuildHeight() && guard++ < 256)
			pos = pos.below();

		BlockState state = level().getBlockState(pos);
		mimicHardness = Math.max(0.1F, state.getDestroySpeed(level(), pos));

		entityData.set(DATA_MIMIC_BLOCK, state);
		entityData.set(DATA_MIMIC_RESOLVED, true);

		double maxHealth = MAX_HEALTH_MULTIPLIER * mimicHardness;
		getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.max(1.0, maxHealth));
		setHealth(getMaxHealth());
	}

	private float attackDamage()
	{
		return (float) (mimicHardness * DAMAGE_MULTIPLIER);
	}

	// ================================================================================================
	// Tick / animation state machine - ported from EntityGolem#onLivingUpdate
	// ================================================================================================

	@Override
	public void tick()
	{
		super.tick();

		if(!level().isClientSide())
			resolveMimicBlock();

		int syncedAniID = entityData.get(DATA_ANI_ID);
		this.aniID = syncedAniID;
		this.aniFrame = (this.aniID != this.prevAniID) ? 0 : this.aniFrame + 1;

		if(!level().isClientSide())
			serverAnimationTick();

		this.prevAniID = this.aniID;
	}

	private void setAniID(int id)
	{
		entityData.set(DATA_ANI_ID, id);
	}

	private void serverAnimationTick()
	{
		if(aniID == BUILD && aniFrame == 1)
		{
			playSound(SoundEvents.STONE_PLACE, 4.0F, 0.6F);
		}
		else if(aniID == BUILD && aniFrame > 90)
		{
			setAniID(STAND);
		}
		else if(aniID == THROW && aniFrame == 15)
		{
			throwBoulder();
		}
		else if(aniID == THROW && aniFrame > 29)
		{
			setTarget(null);
			setAniID(STAND);
		}
		else if(aniID == ROLL && aniFrame > 0 && aniFrame < 9)
		{
			LivingEntity target = getTarget();
			if(target != null)
			{
				getLookControl().setLookAt(target, 30.0F, 30.0F);
				getMoveControl().setWantedPosition(target.getX(), getY(), target.getZ(), 0.3);
			}
		}
		else if(aniID == ROLL && aniFrame == 9)
		{
			playSound(SoundEvents.STONE_BREAK, 4.0F, 0.6F);
			rollCount = 0;
			Vec3 look = getLookAngle();
			double dx = getX() + look.x * 20.0;
			double dz = getZ() + look.z * 20.0;
			rollTargetPos = BlockPos.containing(dx, getY() - 1, dz);
		}
		else if(aniID == ROLL && aniFrame > 9 && aniFrame < 20)
		{
			// Real fix, caught in a later audit: filtered here (not just left to RelationshipCombatEvents'
			// own passive damage-zeroing hook) so a positively-related target is skipped entirely, not just
			// spared the HP loss after already being pushed and having its invulnerability timer stomped -
			// see GolemFallingBlockEntity#hurtNearbyEntities's own identical fix.
			List<LivingEntity> nearby = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(1.0, 0.0, 1.0),
					e -> e != this && !org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.RelationshipCombatEvents.isPositivelyRelated(this, e));
			kickEntities(nearby);
			if(rollTargetPos != null)
				getMoveControl().setWantedPosition(rollTargetPos.getX(), getY(), rollTargetPos.getZ(), 1.0);
		}
		else if(aniID == ROLL && aniFrame == 20 && rollCount < 2)
		{
			this.aniFrame = 10;
			rollCount++;
		}
		else if(aniID == ROLL && aniFrame > 23)
		{
			setTarget(null);
			setAniID(STAND);
		}
		else if(aniID == STOMP && aniFrame > 8 && aniFrame < 16)
		{
			stompAttack();
		}
		else if(aniID == STOMP && aniFrame > 17)
		{
			setAniID(STAND);
			setTarget(null);
		}
		else if(aniID == DIE && aniFrame == 1)
		{
			playSound(SoundEvents.STONE_BREAK, 4.0F, 0.6F);
		}
		else if(aniID == DIE && aniFrame > 54)
		{
			finishDying();
		}
	}

	private void kickEntities(List<LivingEntity> targets)
	{
		double centerX = (getBoundingBox().minX + getBoundingBox().maxX) / 2.0;
		double centerZ = (getBoundingBox().minZ + getBoundingBox().maxZ) / 2.0;
		for(LivingEntity target : targets)
		{
			double dx = target.getX() - centerX;
			double dz = target.getZ() - centerZ;
			double distSq = dx * dx + dz * dz;
			if(distSq < 1.0E-4)
				continue;
			target.hurt(damageSources().mobAttack(this), attackDamage());
			target.push(dx / distSq * 3.0, 1.0, dz / distSq * 3.0);
		}
	}

	private void throwBoulder()
	{
		LivingEntity target = getTarget();
		if(target == null)
			return;

		// Ported from EntityGolem#throwRock's own EntityBoulder construction (same param order/meaning).
		GolemBoulderEntity boulder = new GolemBoulderEntity(level(), this, target, 1.5F, 0.0F, 2.4F, -1.6F, 0.0F, getMimicBlock(), attackDamage());
		level().addFreshEntity(boulder);
		playSound(SoundEvents.STONE_BREAK, 2.0F, 0.7F);
	}

	/** Ported directly from {@code EntityGolem#stompAttack} - see {@link GolemFallingBlockEntity}'s own doc comment for why the shrapnel is a real dedicated entity again, not reused vanilla {@code FallingBlockEntity}. */
	private void stompAttack()
	{
		if(level().isClientSide())
			return;

		for(int side = -1; side <= 1; side += 2)
		{
			for(float i = 0.5F; i < 3.0F; i++)
			{
				double angle = Math.toRadians((i * 6.0 * side) + getYRot() + 90.0);
				double x = getX() + (aniFrame - 6) * Math.cos(angle);
				double z = getZ() + (aniFrame - 6) * Math.sin(angle);
				BlockPos pos = BlockPos.containing(x, getY() - 1, z);
				float yaw = (float) ((i * 6.0 * side) + getYRot() + 90.0);

				GolemFallingBlockEntity falling = new GolemFallingBlockEntity(level(), this, x, getY() - 1, z, 0.4, yaw, pos, attackDamage());
				level().addFreshEntity(falling);
			}
		}
		playSound(SoundEvents.GENERIC_EXPLODE.value(), 4.0F, 1.4F);
	}

	// ================================================================================================
	// Death - the DIE animation runs to completion before the golem actually disappears, matching the
	// original's own EntityGolem#onDeathUpdate. Real removal/loot/exp happen in finishDying(), driven
	// off aniFrame from serverAnimationTick() above, not vanilla's own tickDeath/deathTime pipeline.
	// ================================================================================================

	@Override
	public void die(DamageSource damageSource)
	{
		if(aniID == DIE)
			return;

		this.lastHurtByPlayerTime = this.tickCount;
		setHealth(1.0F);
		setTarget(null);
		setAniID(DIE);
	}

	private void finishDying()
	{
		if(level() instanceof ServerLevel serverLevel)
		{
			for(int i = 0; i < 40; i++)
			{
				float f = (random.nextFloat() - 0.5F) * 3.0F;
				float f1 = (random.nextFloat() - 0.5F) * 3.0F;
				float f2 = (random.nextFloat() - 0.5F) * 3.0F;
				serverLevel.sendParticles(ParticleTypes.EXPLOSION, getX() + f, getY() + 1.0 + f1, getZ() + f2, 1, 0.0, 0.0, 0.0, 0.0);
			}

			dropCustomDeathLoot(serverLevel, damageSources().generic(), true);
			dropExperience(null);
		}

		discard();
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit)
	{
		spawnAtLocation(getMimicBlock().getBlock().asItem(), 1 + random.nextInt(3));
	}

	@Override
	public boolean isInvulnerableTo(DamageSource source)
	{
		return aniID == BUILD || super.isInvulnerableTo(source);
	}

	@Override
	protected boolean shouldDespawnInPeaceful()
	{
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distanceSq)
	{
		return false;
	}

	@Override
	public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag)
	{
		super.addAdditionalSaveData(tag);
		if(entityData.get(DATA_MIMIC_RESOLVED))
		{
			tag.put("MimicBlock", net.minecraft.nbt.NbtUtils.writeBlockState(getMimicBlock()));
			tag.putBoolean("MimicResolved", true);
		}
	}

	@Override
	public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag)
	{
		super.readAdditionalSaveData(tag);
		if(tag.contains("MimicResolved") && tag.getBoolean("MimicResolved"))
		{
			BlockState state = net.minecraft.nbt.NbtUtils.readBlockState(level().holderLookup(net.minecraft.core.registries.Registries.BLOCK), tag.getCompound("MimicBlock"));
			mimicHardness = Math.max(0.1F, state.getDestroySpeed(level(), blockPosition()));
			entityData.set(DATA_MIMIC_BLOCK, state);
			entityData.set(DATA_MIMIC_RESOLVED, true);
		}
		if(aniID == BUILD && tag.contains("MimicResolved"))
			setAniID(STAND);
	}

	/** Ported from {@code EntityGolem#onLivingUpdate}'s own conditional {@code EntityAIWander} add/remove - only wanders while {@link #STAND} with no target. */
	private static class GolemWanderGoal extends WaterAvoidingRandomStrollGoal
	{
		private final GolemEntity golem;

		GolemWanderGoal(GolemEntity golem, double speed)
		{
			super(golem, speed);
			this.golem = golem;
		}

		@Override
		public boolean canUse()
		{
			return golem.aniID == STAND && golem.getTarget() == null && super.canUse();
		}
	}

	/**
	 * Ported from {@code EntityGolem#attackPicker} - only ever runs while {@link #STAND} and a target is
	 * present, matching the original's own gating.
	 */
	private static class GolemCombatGoal extends Goal
	{
		private final GolemEntity golem;

		GolemCombatGoal(GolemEntity golem)
		{
			this.golem = golem;
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse()
		{
			return golem.aniID == STAND && golem.getTarget() != null && golem.getTarget().isAlive();
		}

		@Override
		public boolean canContinueToUse()
		{
			return canUse();
		}

		@Override
		public void tick()
		{
			LivingEntity target = golem.getTarget();
			if(target == null)
				return;

			golem.getLookControl().setLookAt(target, 30.0F, 30.0F);
			golem.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 0.35);

			if(golem.attackCooldown <= 0)
			{
				int pick = golem.random.nextInt(11);
				if(pick < 5)
					golem.setAniID(THROW);
				else if(pick <= 7)
					golem.setAniID(STOMP);
				else
					golem.setAniID(ROLL);
				golem.attackCooldown = ATTACK_COOLDOWN_TICKS;
			}
			else
			{
				golem.attackCooldown--;
			}
		}
	}
}
