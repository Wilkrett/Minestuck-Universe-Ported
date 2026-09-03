package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.rogue.doom;

import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.doom.IDoomData;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.ClasspectColorHandler;
import org.wilkretawesomesauce.minestuckuniverseported.events.AbilitechTargetedEvent;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;

/**
 * "Doom Redistribution" - new tech, ported from the "Doom Class Abilities Framework" design document
 * (no 1.12.2 original), Rogue of Doom's Core ability: "the Rogue moves Doom between entities... taking
 * Doom from an ally... placing Doom onto an enemy." Press while aiming at a target - sneaking
 * ({@link Player#isShiftKeyDown()}) transfers {@link #TRANSFER_AMOUNT} from the caster's
 * own Doom onto the target, otherwise the same amount moves the other way, from the target onto the
 * caster - both directions of the same tech, gated by a held modifier key, matching this project's
 * existing sneak-to-change-mode convention (e.g.
 * {@code heroClass.witch.blood.CultOfPersonalityManager#resetPending}).
 * <p>
 * Priced close to Rogue's own existing generic tech ({@code TechRogueSteal}, 88950) - a repeatable
 * utility action, not an ultimate.
 */
public class TechRogueDoomRedistribution extends TechHeroClass
{
	/** Doom moved per press. */
	private static final double TRANSFER_AMOUNT = 10.0;

	public TechRogueDoomRedistribution()
	{
		super(Minestuckuniverseported.id("doom_redistribution"), EnumClass.ROGUE, EnumAspect.DOOM, 90000, MSUTechType.UTILITY);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.PRESS)
			return false;

		LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);
		if(target == null || target == player)
			return false;

		if(NeoForge.EVENT_BUS.post(new AbilitechTargetedEvent(player, target, this, techSlot, null)).isCanceled())
			return false;

		IDoomData casterData = player.getData(MSUAttachments.DOOM_DATA);
		IDoomData targetData = target.getData(MSUAttachments.DOOM_DATA);

		if(player.isShiftKeyDown())
			casterData.transferTo(targetData, TRANSFER_AMOUNT);
		else
			targetData.transferTo(casterData, TRANSFER_AMOUNT);

		MSUAbilitechParticles.oneshot(level, target, EnumAspect.DOOM, 10);
		MSUAbilitechParticles.oneshot(level, player, 10, ClasspectColorHandler.get(EnumClass.ROGUE));
		return false;
	}
}
