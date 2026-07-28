package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.space;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.AbilitechLoadout;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.space.TechSpaceGrab}
 * ("Kinetic Grab") - press and aim to tether a creature "light enough" (max health capped at
 * {@link #MAX_SOUL_VALUE}, matching the original's "soul value" cap exactly), then drag it towards
 * wherever you're looking every tick while held; release to instead fling it towards that point with
 * real velocity. Real players can never be grabbed at all - the original excluded them outright
 * (rather than relying on the health cap, which a player's own max health could accidentally clear).
 * <p>
 * <b>Simplified out:</b> the original's Underling-specific 0.5x soul-value discount and its
 * passenger-stacking soul cost (a mounted player instantly failed the cap via a flat 1e8 soul value;
 * mounted non-players added their own max health) - this project has no equivalent concept to weigh
 * passengers against, so only the target's own max health is checked.
 */
public class TechSpaceGrab extends TechHeroAspect
{
	private static final double MAX_SOUL_VALUE = 20;

	public TechSpaceGrab()
	{
		super(Minestuckuniverseported.id("kinetic_grab"), EnumAspect.SPACE, 515000, MSUTechType.UTILITY);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE)
			return false;

		AbilitechLoadout badgeEffects = player.getData(MSUAttachments.ABILITECH_LOADOUT);

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 1)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			badgeEffects.setTether(techSlot, null);
			return false;
		}

		Entity target;
		if(state == AbilitechKeyState.PRESS)
		{
			LivingEntity hit = MSUAbilitechRayTrace.getTargetEntity(player);
			if(hit == null || hit instanceof Player || hit.getMaxHealth() > MAX_SOUL_VALUE)
				target = null;
			else
				target = hit;
			badgeEffects.setTether(techSlot, target);
			MSUAbilitechParticles.oneshot(level, player, EnumAspect.SPACE, 1);
		}
		else
			target = badgeEffects.getTether(techSlot);

		if(target == null)
			return false;

		HitResult blockHit = MSUAbilitechRayTrace.getMouseOver(player, player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE));
		Vec3 hitPos = blockHit.getLocation();

		target.setDeltaMovement(Vec3.ZERO);
		target.hasImpulse = true;
		target.setOnGround(false);
		target.resetFallDistance();

		if(state == AbilitechKeyState.RELEASED)
		{
			target.setDeltaMovement(hitPos.subtract(target.position()));
			badgeEffects.setTether(techSlot, null);
		}
		else
			target.move(MoverType.PLAYER, hitPos.subtract(target.position()));

		if(!player.isCreative() && time % 50 == 0)
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);

		MSUAbilitechParticles.aura(level, player, EnumAspect.SPACE, 1);

		return true;
	}
}
