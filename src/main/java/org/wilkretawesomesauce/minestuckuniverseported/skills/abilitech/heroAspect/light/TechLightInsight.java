package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.light;

import com.mraof.minestuck.player.EnumAspect;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported 1:1 from MinestuckUniverse (1.12.2)'s anonymous {@code TechHeroAspect} passive registered
 * under {@code skaian_insight} in {@code MSUSkills} ("Skaian Insight") - "granting the luck and insight
 * needed to find important items such as Jujus and Skaian Scrolls more easily."
 * <p>
 * The original's actual mechanic - a flat 5x drop-chance multiplier for a killer with this passive
 * equipped - is real, wired directly into {@code juju.JujuLootCondition#test} (checked via
 * {@code AbilitechLoadout#isTechEquipped}, which is all this ever needed - the skills/badges level-up
 * economy gated most of the rest of the original's tech tree, but not this one check). No
 * {@code onPassiveTick} body is needed here since the payoff lives entirely in the loot condition.
 */
public class TechLightInsight extends TechHeroAspect
{
	public TechLightInsight()
	{
		super(Minestuckuniverseported.id("skaian_insight"), EnumAspect.LIGHT, 925000, MSUTechType.PASSIVE);
	}
}
