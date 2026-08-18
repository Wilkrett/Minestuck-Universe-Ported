package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.mage.blood;

import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.AspectColorHandler;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.InstabilityStage;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.Relationship;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.RelationshipManager;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;

import java.util.List;

/**
 * "Blood Insight" - new tech, ported from the "Relationship System" design document (no 1.12.2 original),
 * Mage of Blood exclusive - press while aiming at a living entity to read out every
 * {@link Relationship} it's currently a party to (the doc's own worked example: partner, type,
 * strength%, stability%). Purely read-only, matching the doc's own "The Mage cannot create relationships,
 * they can only understand and reinforce existing ones" (Blood Guidance, a sibling tech, is the
 * "reinforce" half). Calls {@link RelationshipManager#ensureNaturalRelationship} first so a tamed pet or
 * {@code entity.HopeGolemEntity} ally shows its real Ownership relationship even if nothing has touched it
 * since taming/summoning - see that method's own doc comment.
 * <p>
 * Also reports each relationship's Instability/{@link InstabilityStage} - the later "Crimson Discord"
 * design document's own "Mage of Blood: Can detect Instability levels and identify relationships at risk
 * of collapse".
 */
public class TechMageBloodInsight extends TechHeroClass
{
	public TechMageBloodInsight()
	{
		// new tech, no original cost to port - priced as a cheap, purely-informational read like
		// mage_awareness (100), the closest existing sibling in both weight and role.
		super(Minestuckuniverseported.id("blood_insight"), EnumClass.MAGE, EnumAspect.BLOOD, 100, MSUTechType.UTILITY);
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

		MutableComponent report = Component.literal("Relationships of ")
				.append(Component.literal(target.getName().getString()).withStyle(ChatFormatting.AQUA))
				.append(Component.literal(":"));

		if(relationships.isEmpty())
		{
			report.append(Component.literal(" none known").withStyle(ChatFormatting.GRAY));
		}
		else
		{
			for(Relationship rel : relationships)
			{
				String otherName = serverLevel.getEntity(rel.other(target.getUUID())) instanceof LivingEntity otherEntity
						? otherEntity.getName().getString()
						: "someone no longer present";

				report.append(Component.literal("\n - "))
						.append(Component.literal(otherName).withStyle(ChatFormatting.AQUA))
						.append(Component.literal(": " + rel.type + " "))
						.append(Component.literal(Math.round(rel.strength) + "% strength").withStyle(ChatFormatting.GOLD))
						.append(Component.literal(", "))
						.append(Component.literal(Math.round(rel.stability) + "% stability").withStyle(ChatFormatting.GREEN))
						.append(rel.instability > 0F
								? Component.literal(", " + Math.round(rel.instability) + "% instability (" + RelationshipManager.stageOf(rel) + ")").withStyle(ChatFormatting.RED)
								: Component.empty())
						.append(rel.corrupted ? Component.literal(" (corrupted)").withStyle(ChatFormatting.DARK_PURPLE) : Component.empty());
			}
		}

		mage.sendSystemMessage(report);
		MSUAbilitechParticles.oneshot(level, target, 10, AspectColorHandler.get(EnumAspect.BLOOD));

		return false;
	}
}
