package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.light;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.light.TechLightAutoGlorb}
 * ("Eternal Glow") - passive: automatically places a Glorb at your feet whenever you're standing on an
 * air block darker than light level 8, same threshold and behavior as the original.
 */
public class TechLightAutoGlorb extends TechLightGlorb
{
	public TechLightAutoGlorb()
	{
		super(Minestuckuniverseported.id("eternal_glow"), 685, MSUTechType.PASSIVE);
	}

	@Override
	public boolean onPassiveTick(Level level, Player player, int techSlot)
	{
		if(level.getBrightness(LightLayer.BLOCK, player.blockPosition()) >= 8)
			return false;

		return placeGlorb(level, player);
	}

	@Override
	public void onPassiveToggle(Level level, Player player, boolean active)
	{
		super.onPassiveToggle(level, player, active);
		sendToggleMessage(player, active);
	}
}
