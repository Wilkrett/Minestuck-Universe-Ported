package org.wilkretawesomesauce.minestuckuniverseported.events;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.Abilitech;

import javax.annotation.Nullable;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code events.AbilitechTargetedEvent} - fired just before most
 * {@code heroClass} techs apply their effect to a target, cancellable the same way the original's
 * {@code MinecraftForge.EVENT_BUS.post(...)} calls let a listener veto it. The original's real single
 * consumer, {@code heroClass.mage.TechMageStudy}, listens for exactly this to know when its owner has just
 * been (successfully) targeted by someone else's ability, so it can borrow that ability into its own slot -
 * every other {@code heroClass} tech that fires this only ever fires it, matching the original's own
 * lopsided fire-many/listen-one shape.
 *
 * @param beneficial whether the targeting was meant to help ({@code true}) or harm ({@code false}) the
 *                   target - the original itself sometimes passed a literal {@code null} here (e.g.
 *                   {@code heroClass.rogue.TechRogue}'s own potion-copy, which is neither), so this stays
 *                   a nullable {@link Boolean} rather than a primitive, matching that real ambiguity.
 */
public class AbilitechTargetedEvent extends Event implements ICancellableEvent
{
	private final Player caster;
	private final Entity target;
	private final Abilitech abilitech;
	private final int techSlot;
	@Nullable
	private final Boolean beneficial;

	public AbilitechTargetedEvent(Player caster, Entity target, Abilitech abilitech, int techSlot, @Nullable Boolean beneficial)
	{
		this.caster = caster;
		this.target = target;
		this.abilitech = abilitech;
		this.techSlot = techSlot;
		this.beneficial = beneficial;
	}

	public Player getCaster()
	{
		return caster;
	}

	public Entity getTarget()
	{
		return target;
	}

	public Abilitech getAbilitech()
	{
		return abilitech;
	}

	public int getTechSlot()
	{
		return techSlot;
	}

	@Nullable
	public Boolean getBeneficial()
	{
		return beneficial;
	}
}
