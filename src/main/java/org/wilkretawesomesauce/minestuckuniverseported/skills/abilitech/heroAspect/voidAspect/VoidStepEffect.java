package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.voidAspect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * A marker effect for {@code TechVoidStep} ("Voidstep") - carries no attribute modifiers or tick behavior
 * of its own, it exists purely so "is this player currently voidstepping" is automatically network-synced
 * to every observing client for free (the same way any potion effect already is), which
 * {@code VoidStepClientEvents} needs to set {@link net.minecraft.world.entity.Entity#noPhysics} on the
 * client's own copy of the player - same real shape as {@code breath.WindFormedEffect}/
 * {@code breath.WindVesselClientEvents}, see that pair's own doc comments for the fuller explanation of
 * why a marker effect (not a bespoke sync packet) is the right tool here.
 * <p>
 * <b>Real bug fix, caught from a live report</b>: {@code TechVoidStep} used to set {@code player.noPhysics = true}
 * only on the server's own {@code Player} instance (the whole Abilitech tick framework - see
 * {@code AbilitechEvents#onPlayerTick} - is explicitly server-only). That did nothing for how a real
 * connected player's own client resolves its own collision, because {@code Entity#noPhysics} is a plain,
 * <i>unsynced</i> field - it works for spectator mode only because both sides independently compute
 * {@code noPhysics = isSpectator()} from the same already-synced gamemode inside {@code Player#tick()}
 * (which runs on both logical sides), not because gamemode-driven noPhysics is itself pushed over the
 * network. Void Step never told the client anything at all, so the client's own local collision never
 * changed.
 */
public class VoidStepEffect extends MobEffect
{
	public VoidStepEffect()
	{
		super(MobEffectCategory.BENEFICIAL, 0x104EA2);
	}
}
