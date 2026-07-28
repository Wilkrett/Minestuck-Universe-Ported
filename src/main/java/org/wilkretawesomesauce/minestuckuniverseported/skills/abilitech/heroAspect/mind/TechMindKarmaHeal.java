package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.mind;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;

/**
 * Ported 1:1 from MinestuckUniverse (1.12.2)'s
 * {@code skills.abilitech.heroAspect.mind.TechMindKarmaHeal} ("Godhood's Justice") - passive: every
 * 1200 ticks (a minute), nudges Static Karma one step towards zero, in the direction opposite whatever
 * Temp Karma's current sign is. Reads/writes real storage now - see {@link GodTierData}'s own doc
 * comment for what this project's Karma economy does and doesn't cover (the nudge itself is real; what
 * changes Temp Karma in the first place is a separate, much larger, unported feature).
 */
public class TechMindKarmaHeal extends TechHeroAspect
{
	public TechMindKarmaHeal()
	{
		super(Minestuckuniverseported.id("godhoods_justice"), EnumAspect.MIND, 700000, MSUTechType.PASSIVE);
	}

	@Override
	public boolean onPassiveTick(Level level, Player player, int techSlot)
	{
		GodTierData data = player.getData(MSUAttachments.GOD_TIER);
		if(data.getStaticKarma() != 0 && level.getGameTime() % 1200 == 0)
		{
			data.setStaticKarma(data.getStaticKarma() - (int)Math.signum(data.getTempKarma()));
			return true;
		}
		return false;
	}

	@Override
	public void onPassiveToggle(Level level, Player player, boolean active)
	{
		super.onPassiveToggle(level, player, active);
		sendToggleMessage(player, active);
	}
}
