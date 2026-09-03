package org.wilkretawesomesauce.minestuckuniverseported.capabilities.badgeEffects;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code capabilities.badgeEffects.IBadgeEffects} - declaring
 * only the currently-implemented subset ({@link BadgeEffects}'s own doc comment has the full accounting of
 * which original fields these are and which ones stayed on separate synced {@code MobEffect}s instead),
 * matching this project's own established convention for its other leftover-capability interfaces
 * ({@code IStrifeData}/{@code IGameData}/{@code IBeamData}/{@code IMediumData}/{@code ISkillKeyStates}).
 */
public interface IBadgeEffects
{
	@Nullable
	Entity getTether(int slot);

	void setTether(int slot, @Nullable Entity entity);

	void clearTether(int slot);

	/**
	 * The original's own dedicated {@code mindflayerEntity} field - deliberately separate from the generic
	 * per-slot {@link #getTether}, matching the original's real {@code IBadgeEffects} (which has both). A
	 * single global value, not per-slot: the original only ever supports one active Mindflayer's Spell
	 * possession per player regardless of which loadout slot it's equipped in.
	 */
	@Nullable
	Entity getMindflayerEntity();

	void setMindflayerEntity(@Nullable Entity entity);

	/** Who is currently possessing this entity via Mindflayer's Spell, if anyone - set on the target's own data, not the controller's. */
	@Nullable
	LivingEntity getMindflayedBy();

	void setMindflayedBy(@Nullable LivingEntity controller);

	boolean isMindflayed();

	@Nullable
	ResourceLocation getExternalTech(int slot);

	void setExternalTech(int slot, @Nullable ResourceLocation id);

	long getLastSeerDodgeTick();

	void setLastSeerDodgeTick(long tick);

	@Nullable
	EntityType<?> getCloakType();

	void setCloakType(@Nullable EntityType<?> type);

	boolean hasWarpPoint();

	@Nullable
	BlockPos getWarpPointPos();

	@Nullable
	ResourceKey<Level> getWarpPointDim();

	void setWarpPoint(BlockPos pos, ResourceKey<Level> dimension);

	void clearWarpPoint();

	@Nullable
	BlockPos getManipulatedPos1();

	@Nullable
	BlockPos getManipulatedPos2();

	@Nullable
	ResourceKey<Level> getManipulatedPos1Dim();

	@Nullable
	ResourceKey<Level> getManipulatedPos2Dim();

	void setManipulatedPos1(@Nullable BlockPos pos, @Nullable ResourceKey<Level> dim);

	void setManipulatedPos2(@Nullable BlockPos pos, @Nullable ResourceKey<Level> dim);

	Set<UUID> getSavingGraceTargets();

	int getCalculating();

	void setCalculating(int calculating);

	boolean isFrenzied();

	void setFrenzied(boolean frenzied);

	boolean isRageShifted();

	void setRageShifted(boolean rageShifted);

	boolean isSavingGraced();

	void setSavingGraced(boolean savingGraced);
}
