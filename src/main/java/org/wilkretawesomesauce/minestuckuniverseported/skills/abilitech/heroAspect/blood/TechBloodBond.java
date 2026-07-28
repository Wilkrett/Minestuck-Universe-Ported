package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.blood;

import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechTetherBond;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;

/**
 * Blood aspect's tether-bond tech: hit a target to bond it, bonus damage on further hits while bonded, and
 * a one-time damage snap (breaking the bond) if the target gets too far away - see {@link TechTetherBond}'s
 * own doc comment for the full, now-generic mechanic this tech just supplies numbers to.
 */
public class TechBloodBond extends TechTetherBond
{
	private static final double BOND_RANGE = 12.0;
	private static final float DAMAGE_MULTIPLIER = 1.1F;
	private static final float FAR_DAMAGE_AMOUNT = 5.0F;

	public TechBloodBond()
	{
		super(Minestuckuniverseported.id("blood_bond"), EnumAspect.BLOOD, 46000, MSUTechType.OFFENSE, EnumClass.KNIGHT); // new tech, no original cost to port - picked to fit this project's own cost spread among sibling Knight techs
	}

	@Override
	protected double getBondRange()
	{
		return BOND_RANGE;
	}

	@Override
	protected float getDamageMultiplier()
	{
		return DAMAGE_MULTIPLIER;
	}

	@Override
	protected float getFarDamageAmount()
	{
		return FAR_DAMAGE_AMOUNT;
	}
}
