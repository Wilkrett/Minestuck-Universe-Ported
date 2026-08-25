package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.voidAspect;

import com.mraof.minestuck.entity.DecoyEntity;
import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.voidAspect.TechVoidSnap}
 * ("Return to Dust") - press and aim at a non-player entity to erase it outright
 * ({@link Entity#discard()}, the modern equivalent of the original's {@code setDead()}). Real players
 * are always excluded, matching the original exactly - this doesn't affect players. Also excludes
 * Minestuck's own {@link DecoyEntity} (the same class {@code TechHeartProject} spawns), the modern
 * equivalent of the original excluding its own {@code EntityDecoy}.
 * <p>
 * <b>Dropped:</b> the original's {@code MSUConfig.protectedEntities} registry-name blocklist - this
 * project has no equivalent config list, and nothing in it currently needs protecting beyond decoys.
 */
public class TechVoidSnap extends TechHeroAspect
{
	private static final int ENERGY_USE = 2;

	public TechVoidSnap()
	{
		super(Minestuckuniverseported.id("return_to_dust"), EnumAspect.VOID, 40000, MSUTechType.UTILITY);
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

		MSUAbilitechParticles.aura(level, player, EnumAspect.VOID, 5);

		HitResult hit = MSUAbilitechRayTrace.getMouseOver(player, player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE));
		if(!(hit instanceof EntityHitResult entityHit))
			return true;

		Entity target = entityHit.getEntity();
		if(target instanceof Player || target instanceof DecoyEntity)
			return true;

		target.discard();
		MSUAbilitechParticles.oneshot(level, target, EnumAspect.VOID, 10);

		if(!player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - ENERGY_USE);

		return true;
	}
}
