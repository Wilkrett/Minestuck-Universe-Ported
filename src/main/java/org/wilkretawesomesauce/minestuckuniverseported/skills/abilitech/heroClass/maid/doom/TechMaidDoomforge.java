package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.maid.doom;

import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import org.wilkretawesomesauce.minestuckuniverseported.Config;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.AbilitechTargetedEvent;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;

/**
 * "Doomforge" - new tech, ported from the "Doom Class Abilities Framework" design document (no 1.12.2
 * original), Maid of Doom's Core ability: "the Maid introduces Doom into targets... the created Doom
 * accumulates until an ending becomes increasingly likely." A direct, literal implementation of the base
 * Doom system's own "direct manipulation by Doom abilities" source - press while aiming at a
 * {@code LivingEntity} to inject {@link Config#doomforgeInjectAmount} straight into their
 * {@code mechanics.doom.DoomData} via {@code addDoom} (a no-op if the target is currently sealed, handled inside
 * that method already - Doomforge doesn't need to check for that itself).
 * <p>
 * Priced well below its sibling Offensive tech ({@code TechMaidDoomFinalityEngine}) - matching Maid's own
 * existing generic tech ({@code TechMaid}, 49550) - since this is a simple, repeatable utility action, not
 * an escalating attack.
 */
public class TechMaidDoomforge extends TechHeroClass
{
	public TechMaidDoomforge()
	{
		super(Minestuckuniverseported.id("doomforge"), EnumClass.MAID, EnumAspect.DOOM, 50000, MSUTechType.UTILITY);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.PRESS)
			return false;

		LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);
		if(target == null)
			return false;

		if(NeoForge.EVENT_BUS.post(new AbilitechTargetedEvent(player, target, this, techSlot, null)).isCanceled())
			return false;

		target.getData(MSUAttachments.DOOM_DATA).addDoom(Config.doomforgeInjectAmount);

		MSUAbilitechParticles.oneshot(level, target, EnumAspect.DOOM, 15);
		player.displayClientMessage(Component.translatable("status.minestuckuniverseported.doomforgeCast", target.getName()), true);

		return false;
	}
}
