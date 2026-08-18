package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.prince.blood;

import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.AspectColorHandler;
import org.wilkretawesomesauce.minestuckuniverseported.events.AbilitechTargetedEvent;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.InstabilityStage;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.Relationship;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.RelationshipManager;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.witch.blood.CultOfPersonalityManager;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;

/**
 * "Schism" - new tech, ported from a design document (no 1.12.2 original), Prince of Blood exclusive -
 * corrupts an existing Blood Bond (formed by {@code heroClass.witch.blood.TechBloodWitchCultOfPersonality}'s
 * Cult of Personality) rather than destroying it: "Relationships remain intact, but every benefit becomes
 * a liability" (the design doc's own words). Reuses {@link TechHeroClass}'s {@code requiredAspect} the same
 * way Cult of Personality does (Prince of Blood specifically, not any Prince), which is why this lives in
 * its own {@code blood} subfolder under {@code heroClass.prince}, mirroring that sibling tech's own
 * {@code heroClass.witch.blood} placement.
 * <p>
 * All the actual corruption logic - Shared Pain (amplified damage transfer), Fractured Loyalty (survivors
 * blaming each other instead of avenging), Corrupted Awareness (the tether recoloring), cleansing, and
 * auto-expiry - lives on the bond itself in {@code CultOfPersonalityManager}, per the design doc's own
 * "reference the original Blood Bond whenever possible rather than creating duplicate networks"
 * requirement; this tech is just the activation: press while aiming at a living entity that currently
 * belongs to an active Blood Bond to corrupt it, or press again on an already-corrupted one to remove that
 * corruption (see {@link CultOfPersonalityManager#corrupt} - this doubles as "prevent corrupting an
 * already Corrupted Bond" and the design doc's own "Removed by the Prince" cleansing path). If the target
 * isn't part of a real Cult of Personality bond, {@link #corruptUnstableRelationships} falls back to the
 * "Crimson Discord" design document's own "Prince of Blood: Can immediately corrupt unstable relationships
 * into Corrupted Relationships" - any of the target's plain {@code mechanics.relationship.Relationship}s already
 * at {@link InstabilityStage#NOTICEABLE} or worse get marked {@link Relationship#corrupted} outright
 * (skipping the gradual Instability climb entirely) rather than left to fail on their own. Aiming at an
 * entity with neither a real bond nor any sufficiently-unstable relationship does nothing but report as much.
 * <p>
 * Also a passive, toggleable aura (ported from a second, later design document, "Schism - Anti-Blood
 * Design Philosophy") - {@link #onPassiveTick} just forwards to {@link CultOfPersonalityManager#pulseSchismAura}
 * every tick while toggled on (the generic project-wide passive-toggle mechanism already used by e.g.
 * {@code heroAspect.blood.TechBloodBleeding}, not anything specific to this tech), which owns the actual
 * radius-effect logic - see that method's own doc comment for what it does and doesn't attempt.
 */
public class TechPrinceBloodSchism extends TechHeroClass
{
	public TechPrinceBloodSchism()
	{
		// new tech, no original cost to port - priced the same as Cult of Personality (250000), the tech
		// it directly acts on and its closest sibling in both weight and theme.
		super(Minestuckuniverseported.id("schism"), EnumClass.PRINCE, EnumAspect.BLOOD, 250000, MSUTechType.UTILITY, MSUTechType.PASSIVE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.PRESS)
			return false;

		if(!(player instanceof ServerPlayer prince) || !(level instanceof ServerLevel serverLevel))
			return false;

		LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);
		if(target == null || NeoForge.EVENT_BUS.post(new AbilitechTargetedEvent(player, target, this, techSlot, false)).isCanceled())
			return false;

		CultOfPersonalityManager.CorruptResult result = CultOfPersonalityManager.corrupt(serverLevel, prince, target);

		if(result == CultOfPersonalityManager.CorruptResult.NOT_BONDED && corruptUnstableRelationships(serverLevel, target))
		{
			MSUAbilitechParticles.oneshot(level, target, 15, 0x4B0082);
			player.displayClientMessage(Component.translatable("status.minestuckuniverseported.schismRelationshipCorrupted"), true);
			return false;
		}

		switch(result)
		{
			case NOT_BONDED -> player.displayClientMessage(Component.translatable("status.minestuckuniverseported.schismNotBonded"), true);
			case CORRUPTED ->
			{
				// Matches TetherBondRenderer's own CORRUPTED_COLOR literal - that constant lives in a
				// client-only class this server-side tech can't reference directly.
				MSUAbilitechParticles.oneshot(level, target, 15, 0x4B0082);
				player.displayClientMessage(Component.translatable("status.minestuckuniverseported.schismCorrupted"), true);
			}
			case CLEANSED ->
			{
				MSUAbilitechParticles.oneshot(level, target, 15, AspectColorHandler.get(EnumAspect.BLOOD));
				player.displayClientMessage(Component.translatable("status.minestuckuniverseported.schismRemoved"), true);
			}
		}

		return false;
	}

	/** The design doc's own "immediately corrupt unstable relationships" fallback - see this class's own doc comment. Returns whether anything was actually corrupted. */
	private static boolean corruptUnstableRelationships(ServerLevel level, LivingEntity target)
	{
		RelationshipManager.ensureNaturalRelationship(target, level.getGameTime());

		boolean any = false;
		for(Relationship rel : RelationshipManager.getAllFor(target.getUUID()))
		{
			if(rel.corrupted || RelationshipManager.stageOf(rel) == InstabilityStage.MINOR)
				continue;

			rel.corrupted = true;
			rel.instability = 0F;
			any = true;
		}

		return any;
	}

	@Override
	public boolean onPassiveTick(Level level, Player player, int techSlot)
	{
		if(player instanceof ServerPlayer prince && level instanceof ServerLevel serverLevel)
			CultOfPersonalityManager.pulseSchismAura(serverLevel, prince);

		return false;
	}

	@Override
	public void onPassiveToggle(Level level, Player player, boolean active)
	{
		sendToggleMessage(player, active);
	}
}
