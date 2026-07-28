package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.mage.blood;

import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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
 * "Blood Guidance" - new tech, ported from the "Relationship System" design document (no 1.12.2 original),
 * Mage of Blood exclusive - press while aiming at a living entity to reinforce every {@link Relationship}
 * it's currently a party to: a real strength nudge plus a real, lasting stability increase. That stability
 * increase is a genuine, if indirect, "resist Schism corruption" (the doc's own phrase): higher
 * {@link Relationship#stability} directly shrinks how much {@code heroClass.witch.blood.CultOfPersonalityManager}'s
 * Schism Aura can weaken the relationship (see {@link RelationshipManager#adjustStrength}'s own resistance
 * formula) - a real mechanical interaction between two independently-built systems, not just flavor text.
 * <p>
 * Matches the doc's own "The Mage cannot create relationships" - refuses (and says so) if the target has
 * no relationships at all yet, rather than silently creating one; see the sibling {@code TechMageBloodInsight}
 * for the read-only half of this pair.
 */
public class TechMageBloodGuidance extends TechHeroClass
{
	private static final float STRENGTH_BOOST = 5F;
	private static final float STABILITY_BOOST = 5F;

	public TechMageBloodGuidance()
	{
		// new tech, no original cost to port - priced above the two purely-informational Blood techs
		// since this one has a real, repeatable gameplay effect, not just a readout.
		super(Minestuckuniverseported.id("blood_guidance"), EnumClass.MAGE, EnumAspect.BLOOD, 45000, MSUTechType.UTILITY);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.PRESS)
			return false;

		if(!(player instanceof ServerPlayer mage) || !(level instanceof ServerLevel serverLevel))
			return false;

		LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);
		if(target == null)
			return false;

		RelationshipManager.ensureNaturalRelationship(target, serverLevel.getGameTime());
		List<Relationship> relationships = RelationshipManager.getAllFor(target.getUUID());

		if(relationships.isEmpty())
		{
			player.displayClientMessage(Component.translatable("status.minestuckuniverseported.bloodGuidanceNoRelationships"), true);
			return false;
		}

		long tick = serverLevel.getGameTime();
		for(Relationship rel : relationships)
		{
			RelationshipManager.adjustStrength(rel, STRENGTH_BOOST, tick);
			RelationshipManager.adjustStability(rel, STABILITY_BOOST);
			RelationshipManager.recordEvent(rel, "Reinforced by " + mage.getName().getString(), tick);
		}

		MSUAbilitechParticles.oneshot(level, target, 15, MSUAspectColors.get(EnumAspect.BLOOD));
		player.displayClientMessage(Component.translatable("status.minestuckuniverseported.bloodGuidanceReinforced"), true);

		return false;
	}
}
