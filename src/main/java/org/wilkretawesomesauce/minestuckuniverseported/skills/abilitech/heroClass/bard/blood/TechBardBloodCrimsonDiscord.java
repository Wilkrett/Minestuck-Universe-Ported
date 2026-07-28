package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.bard.blood;

import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.Config;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAspectColors;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.Relationship;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.RelationshipManager;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;

import java.util.List;

/**
 * "Crimson Discord" - new tech, ported from the "Crimson Discord" design document (no 1.12.2 original),
 * Bard of Blood exclusive - "accelerates the natural decay of relationships" rather than directly
 * corrupting or destroying them (that's Schism's job). Mirrors {@code heroClass.prince.blood.TechPrinceBloodSchism}'s
 * own two-mode shape: a single-target press for an immediate burst, and a toggleable passive aura
 * ("Social Decay") for the doc's own radius-based ambient effect - see
 * {@code mechanics.relationship.RelationshipManager}'s own "Instability" doc section for what Instability
 * actually does once it's applied (Blood Bond weakening, Blood Vengeance failure, and Stage 4 automatic
 * collapse with a spreading "Domino Effect") - this tech only ever applies it, the manager owns every
 * consequence.
 */
public class TechBardBloodCrimsonDiscord extends TechHeroClass
{
	public TechBardBloodCrimsonDiscord()
	{
		// new tech, no original cost to port - priced the same as its closest sibling in role and weight,
		// witch.blood.TechBloodWitchCultOfPersonality/prince.blood.TechPrinceBloodSchism (250000 each).
		super(Minestuckuniverseported.id("crimson_discord"), EnumClass.BARD, EnumAspect.BLOOD, 250000, MSUTechType.UTILITY, MSUTechType.PASSIVE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.PRESS)
			return false;

		if(!(player instanceof ServerPlayer bard) || !(level instanceof ServerLevel serverLevel))
			return false;

		LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);
		if(target == null)
			return false;

		RelationshipManager.ensureNaturalRelationship(target, serverLevel.getGameTime());
		List<Relationship> relationships = RelationshipManager.getAllFor(target.getUUID());

		if(relationships.isEmpty())
		{
			player.displayClientMessage(Component.translatable("status.minestuckuniverseported.crimsonDiscordNoRelationships"), true);
			return false;
		}

		long tick = serverLevel.getGameTime();
		for(Relationship rel : relationships)
		{
			RelationshipManager.adjustInstability(rel, (float) Config.crimsonDiscordBurstAmount, tick);
			RelationshipManager.checkForCollapse(serverLevel, rel);
		}

		MSUAbilitechParticles.oneshot(level, target, 15, MSUAspectColors.get(EnumAspect.BLOOD));
		player.displayClientMessage(Component.translatable("status.minestuckuniverseported.crimsonDiscordDestabilized"), true);

		return false;
	}

	@Override
	public boolean onPassiveTick(Level level, Player player, int techSlot)
	{
		if(player instanceof ServerPlayer bard && level instanceof ServerLevel serverLevel)
			RelationshipManager.pulseCrimsonDiscordAura(serverLevel, bard);

		return false;
	}

	@Override
	public void onPassiveToggle(Level level, Player player, boolean active)
	{
		sendToggleMessage(player, active);
	}
}
