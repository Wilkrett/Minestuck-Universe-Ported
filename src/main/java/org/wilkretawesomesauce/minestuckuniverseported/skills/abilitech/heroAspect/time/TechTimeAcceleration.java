package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code TechTimeAcceleration}. Hold and aim at a block to force
 * its scheduled tick (crop growth, redstone, water/lava flow, etc.) to run early. Costs 1 food per 20 ticks.
 * <p>
 * Not ported: forcing extra block *entity* ticks (the original called {@code ITickable#update()} extra
 * times on the target's tile entity). Modern block entities don't have that interface anymore - ticking
 * is done through a registered {@code BlockEntityTicker} rather than an arbitrary callable method, so
 * there's no equivalent "just tick it again" hook to call safely. The block-level scheduled tick (the
 * more common case - crops, redstone, etc.) is still fully ported.
 */
public class TechTimeAcceleration extends TechHeroAspect
{
	public TechTimeAcceleration()
	{
		super(Minestuckuniverseported.id("flow_accelerator"), EnumAspect.TIME, 1520, MSUTechType.UTILITY);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.HELD)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 1)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		BlockPos target = MSUAbilitechRayTrace.getTargetBlock(player);
		if(target == null)
			return true;

		if(level instanceof ServerLevel serverLevel)
		{
			BlockState state1 = serverLevel.getBlockState(target);
			state1.tick(serverLevel, target, serverLevel.getRandom());
		}

		if(time % 20 == 0 && !player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);

		MSUAbilitechParticles.aura(level, player, EnumAspect.TIME, 2);

		return true;
	}
}
