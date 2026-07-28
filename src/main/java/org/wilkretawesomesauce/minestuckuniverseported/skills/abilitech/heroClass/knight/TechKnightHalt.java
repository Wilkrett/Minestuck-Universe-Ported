package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.knight;

import com.mraof.minestuck.player.EnumClass;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUClassColors;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.AbilitechTargetedEvent;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroClass.TechKnightHalt} ("Guardian's
 * Halt") - holding freezes every non-living entity (dropped items, arrows, thrown potions, minecarts, etc)
 * within a 5x1x5 box around the caster in place, for up to 60 ticks. The original also re-aimed any
 * {@code IProjectile} with a zeroed {@code shoot(0,0,0,0,0)} call on top of zeroing its motion - modern
 * {@code Projectile} has no matching single-call equivalent, and zeroing {@link Entity#setDeltaMovement}
 * alone already halts it the same way every other frozen entity here is halted, so that extra call isn't
 * reproduced (a real, stated simplification of the implementation, not the mechanic).
 */
public class TechKnightHalt extends TechHeroClass
{
	public TechKnightHalt()
	{
		super(Minestuckuniverseported.id("guardian_halt"), EnumClass.KNIGHT, 39000, MSUTechType.DEFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 3)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(state == AbilitechKeyState.NONE || time >= 60)
			return false;

		if(time % 20 == 0 && !player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 3);

		for(Entity target : level.getEntitiesOfClass(Entity.class, player.getBoundingBox().inflate(5, 1, 5), e -> !(e instanceof LivingEntity)))
		{
			if(NeoForge.EVENT_BUS.post(new AbilitechTargetedEvent(player, target, this, techSlot, false)).isCanceled())
				continue;

			target.setDeltaMovement(Vec3.ZERO);
			target.hurtMarked = true;
		}

		MSUAbilitechParticles.oneshot(level, player, 20, MSUClassColors.get(EnumClass.KNIGHT));

		return true;
	}

	@Override
	public boolean isUsableExternally(Level level, Player player)
	{
		return player.getFoodData().getFoodLevel() >= 3 && super.isUsableExternally(level, player);
	}
}
