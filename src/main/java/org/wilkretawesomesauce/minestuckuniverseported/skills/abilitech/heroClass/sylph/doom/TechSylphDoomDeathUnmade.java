package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.sylph.doom;

import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import org.wilkretawesomesauce.minestuckuniverseported.Config;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.doom.DoomData;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.events.AbilitechTargetedEvent;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;

/**
 * "Death Unmade" - new tech, ported from the "Doom Class Abilities Framework" design document (no
 * 1.12.2 original), Sylph of Doom's Offensive ability: "the Sylph destroys the Doom sustaining an
 * enemy's current state... removing the fatal aspect of an attack... causing an enemy's destructive
 * ability to collapse." Press while aiming at a target instantly removes
 * {@link Config#deathUnmadeRemoveAmount} from their {@code mechanics.doom.DoomData} and clears any Doom Mark they
 * carry ({@code IDoomData#clearMark}) - a real defensive counter to a high-Doom enemy currently
 * benefiting from {@code mechanics.doom.DoomDamageEvents}' damage-amplification curve, and a real way to strip a
 * hostile {@code mechanics.doom.DoomMarkType#DEAD_SHUFFLE} mark off an ally before it can redirect their death-Doom
 * to an enemy caster.
 * <p>
 * Priced high, matching Sylph's own existing tier (its generic tech, {@code TechSylph}, costs 995000) -
 * a strong defensive/counter-play tool, not a cheap repeatable action like this class's own Core tech.
 */
public class TechSylphDoomDeathUnmade extends TechHeroClass
{
	public TechSylphDoomDeathUnmade()
	{
		super(Minestuckuniverseported.id("death_unmade"), EnumClass.SYLPH, EnumAspect.DOOM, 750000, MSUTechType.DEFENSE, MSUTechType.OFFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.PRESS)
			return false;

		LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);
		if(target == null)
			return false;

		if(NeoForge.EVENT_BUS.post(new AbilitechTargetedEvent(player, target, this, techSlot, true)).isCanceled())
			return false;

		DoomData data = target.getData(MSUAttachments.DOOM_DATA);
		data.removeDoom(Config.deathUnmadeRemoveAmount);
		data.clearMark();

		MSUAbilitechParticles.oneshot(level, target, EnumAspect.DOOM, 20);
		return false;
	}
}
