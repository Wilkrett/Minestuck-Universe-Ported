package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.rogue.doom;

import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.doom.DoomMarks;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.AbilitechTargetedEvent;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;

/**
 * "Grave Exchange" - new tech, ported from the "Doom Class Abilities Framework" design document (no
 * 1.12.2 original), Rogue of Doom's Offensive ability: "the Rogue forces one entity to inherit the Doom
 * meant for another... force an enemy to inherit the consequences of their actions... someone must pay
 * the price, it does not have to be you." Press while aiming at a target applies
 * {@code mechanics.doom.DoomMarkType#DEAD_SHUFFLE} to them via {@code mechanics.doom.DoomMarks#applyDeadShuffleMark} - the
 * real, first-ever caller of that base-system mark, whose own doc comment already flagged it as a
 * natural fit for exactly this tech. The marked target's Doom accumulates faster and, on death,
 * redirects straight to this tech's caster instead of dispersing into the world's release pool.
 * <p>
 * Priced above this class's own Core tech ({@code TechRogueDoomRedistribution}) - a real, lasting
 * consequence-redirection effect rather than a one-off transfer, matching the design doc's own framing
 * of Grave Exchange as the heavier of the two Rogue-of-Doom techs.
 */
public class TechRogueDoomGraveExchange extends TechHeroClass
{
	public TechRogueDoomGraveExchange()
	{
		super(Minestuckuniverseported.id("grave_exchange"), EnumClass.ROGUE, EnumAspect.DOOM, 300000, MSUTechType.OFFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.PRESS)
			return false;

		LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);
		if(target == null || target == player)
			return false;

		if(NeoForge.EVENT_BUS.post(new AbilitechTargetedEvent(player, target, this, techSlot, false)).isCanceled())
			return false;

		DoomMarks.applyDeadShuffleMark(target, player.getUUID());

		MSUAbilitechParticles.oneshot(level, target, EnumAspect.DOOM, 20);
		player.displayClientMessage(Component.translatable("status.minestuckuniverseported.graveExchangeCast", target.getName()), true);
		return false;
	}
}
