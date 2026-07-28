package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.heart;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code util.SoulData} - a full snapshot of one entity's
 * position/rotation/velocity, health, active potion effects, and (for a player) selected hotbar slot,
 * that can later be stamped onto a <i>different</i> entity. Used by {@code TechHeartSoulSwitcher}
 * ("Soul Switcher") to make two entities genuinely trade everything about their current state, not
 * just where they're standing.
 * <p>
 * The original also captured/restored {@code decayTime} - a counter tracked on {@code IBadgeEffects},
 * the badges/skills capability this whole project has never ported (every {@code TechHeroAspect}
 * subclass's own scope notes already call this out as a standing, deliberate omission). Dropped for
 * that reason, not a new gap specific to this class. Inventory contents themselves were never part of
 * the swap either (only which hotbar slot was selected) - matches the original exactly.
 */
public final class SoulData
{
	private final Vec3 position;
	private final float yaw;
	private final float pitch;
	private final Vec3 motion;
	private final float health;
	private final List<MobEffectInstance> effects;
	private final int selectedSlot;

	public SoulData(LivingEntity entity)
	{
		this.position = entity.position();
		this.yaw = entity.getYRot();
		this.pitch = entity.getXRot();
		this.motion = entity.getDeltaMovement();
		this.health = entity.getHealth();

		this.effects = new ArrayList<>();
		for(MobEffectInstance effect : entity.getActiveEffects())
			effects.add(new MobEffectInstance(effect));

		this.selectedSlot = entity instanceof Player player ? player.getInventory().selected : -1;
	}

	public void apply(LivingEntity entity)
	{
		entity.moveTo(position.x, position.y, position.z, yaw, pitch);
		entity.setYHeadRot(yaw);
		entity.setDeltaMovement(motion);
		if(entity.isAlive())
			entity.setHealth(Math.min(health, entity.getMaxHealth()));

		entity.removeAllEffects();
		for(MobEffectInstance effect : effects)
			entity.addEffect(new MobEffectInstance(effect));

		if(selectedSlot >= 0 && entity instanceof Player player)
			player.getInventory().selected = selectedSlot;
	}
}
