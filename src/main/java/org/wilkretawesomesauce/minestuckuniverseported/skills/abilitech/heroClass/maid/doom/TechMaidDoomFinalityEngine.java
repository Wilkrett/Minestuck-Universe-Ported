package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.maid.doom;

import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.events.AbilitechTargetedEvent;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;

/**
 * "Finality Engine" - new tech, ported from the "Doom Class Abilities Framework" design document (no
 * 1.12.2 original), Maid of Doom's Offensive ability: "the Maid causes attacks... to become increasingly
 * doomed... the longer the Doom remains active, the closer the target moves toward its ending." A literal
 * execute-style payoff for that theme: hold while aiming at a target (same hold-then-trigger shape as
 * {@code heroAspect.doom.TechDoomDecay}'s {@code TRIGGER_TICKS}) to deal direct damage scaled by the
 * <b>target's own current Doom</b> - the more Doom they carry, the harder this hits, capped by
 * {@link #MAX_DAMAGE} so it can never one-shot regardless of how high Doom climbs.
 * <p>
 * Priced above Doomforge (this class's own Core tech) but still moderate, matching Maid's own cheap
 * class-cost tier ({@code TechMaid} at 49550) rather than the far pricier Sylph/Page ultimates.
 */
public class TechMaidDoomFinalityEngine extends TechHeroClass
{
	private static final int MAX_HOLD_TICKS = 26;

	/** How long this must be held before it fires. */
	private static final int CHARGE_TICKS = 25;
	/** Flat damage before scaling by the target's own current Doom. */
	private static final double BASE_DAMAGE = 2.0;
	/** Additional damage per point of the target's own current Doom, before {@link #MAX_DAMAGE}. */
	private static final double DOOM_SCALE = 0.05;
	/** Max damage a single hit can ever deal, regardless of the target's Doom. */
	private static final double MAX_DAMAGE = 40.0;

	public TechMaidDoomFinalityEngine()
	{
		super(Minestuckuniverseported.id("finality_engine"), EnumClass.MAID, EnumAspect.DOOM, 150000, MSUTechType.OFFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE || time >= MAX_HOLD_TICKS)
			return false;

		LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);
		if(target == null)
			return false;

		MSUAbilitechParticles.aura(level, player, EnumAspect.DOOM, 10);

		if(time < CHARGE_TICKS)
			return true;

		if(NeoForge.EVENT_BUS.post(new AbilitechTargetedEvent(player, target, this, techSlot, false)).isCanceled())
			return false;

		double targetDoom = target.getData(MSUAttachments.DOOM_DATA).getDoom();
		double damage = Math.min(MAX_DAMAGE, BASE_DAMAGE + targetDoom * DOOM_SCALE);

		target.hurt(level.damageSources().magic(), (float) damage);
		MSUAbilitechParticles.burst(level, target, EnumAspect.DOOM, 20);

		return false;
	}
}
