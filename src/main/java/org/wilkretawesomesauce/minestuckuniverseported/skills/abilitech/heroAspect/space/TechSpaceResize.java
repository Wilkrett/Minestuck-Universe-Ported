package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.space;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported 1:1 from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.space.TechSpaceResize}
 * ("Spatial Manipulator"). Press and aim at a living creature to grow it, or crouch and press to
 * shrink it, one step at a time (shrinking it down to nothing makes it disappear); press and aim at one
 * of Minestuck's own giant SBURB machine multiblocks instead to relocate it - see
 * {@link SpaceSaltUtils}'s own doc comment for that half.
 * <p>
 * The original stored size on a raw NBT {@code "Size"} float tag that only some entities (Minestuck's
 * own Frogs, specifically) actually read. This uses the real generic
 * {@code minecraft:generic.scale} attribute instead ({@link Attributes#SCALE}, added well after 1.12.2)
 * - it exists on every {@link LivingEntity}, actually resizes the hitbox and render size the same way
 * for anything, and is a strictly more general real equivalent rather than a workaround.
 */
public class TechSpaceResize extends TechHeroAspect
{
	private static final float STEP = 0.2F;
	private static final float MAX_SCALE = 10.0F;
	private static final int ENERGY_USE = 4;

	public TechSpaceResize()
	{
		super(Minestuckuniverseported.id("spatial_manipulator"), EnumAspect.SPACE, 1000, MSUTechType.UTILITY);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.PRESS)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < ENERGY_USE)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		HitResult hit = MSUAbilitechRayTrace.getMouseOver(player, player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE));

		if(hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK)
		{
			if(!SpaceSaltUtils.onSpaceSaltUse(level, blockHit.getBlockPos(), blockHit.getDirection()))
				return false;

			MSUAbilitechParticles.aura(level, player, EnumAspect.SPACE, 4);
		}
		else if(hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target)
		{
			AttributeInstance scaleAttribute = target.getAttribute(Attributes.SCALE);
			if(scaleAttribute == null)
				return false;

			float newScale = (float)scaleAttribute.getBaseValue() + STEP * (player.isCrouching() ? -1 : 1);
			newScale = Math.min(MAX_SCALE, newScale);

			if(newScale <= 0)
				target.discard();
			else
				scaleAttribute.setBaseValue(newScale);

			MSUAbilitechParticles.aura(level, player, EnumAspect.SPACE, 4);
			MSUAbilitechParticles.oneshot(level, target, EnumAspect.SPACE, 10);
		}
		else
			return false;

		if(!player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - ENERGY_USE);

		return true;
	}
}
