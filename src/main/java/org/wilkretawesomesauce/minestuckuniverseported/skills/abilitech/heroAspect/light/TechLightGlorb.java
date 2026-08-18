package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.light;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.light.TechLightGlorb}
 * ("Orb of Light") - press to place a small Glowing Orb ("Glorb") at your feet, if the space is air.
 * <p>
 * The original hand-built its own invisible light-emitting {@code MinestuckUniverseBlocks.glorb} block -
 * 1.12.2 vanilla had nothing like that. Modern vanilla's own {@link Blocks#LIGHT} (added in 1.14, well
 * after this project's source material) is a direct, real equivalent - an invisible, walk-through,
 * max-brightness block - reused here instead of duplicating it.
 */
public class TechLightGlorb extends TechHeroAspect
{
	public TechLightGlorb()
	{
		this(Minestuckuniverseported.id("orb_of_light"), 185, MSUTechType.UTILITY);
	}

	protected TechLightGlorb(net.minecraft.resources.ResourceLocation id, long cost, MSUTechType techType)
	{
		super(id, EnumAspect.LIGHT, cost, techType);
	}

	@Override
	public boolean canUse(Level level, Player player)
	{
		return super.canUse(level, player) && player.getAbilities().mayBuild;
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.PRESS)
			return false;

		return placeGlorb(level, player);
	}

	protected static boolean placeGlorb(Level level, Player player)
	{
		if(!level.getBlockState(player.blockPosition()).isAir())
			return false;

		MSUAbilitechParticles.aura(level, player, EnumAspect.LIGHT, 4);
		level.setBlockAndUpdate(player.blockPosition(), Blocks.LIGHT.defaultBlockState());
		return true;
	}
}
