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
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.Relationship;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.RelationshipManager;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * "Blood Memory" - new tech, ported from the "Relationship System" design document (no 1.12.2 original),
 * Mage of Blood exclusive - press while aiming at a living entity to read out the most recent events
 * across every {@link Relationship} it's a party to (the doc's own "who helped/harmed an entity, how a
 * relationship changed, why a bond weakened"), merged and sorted newest-first, capped at
 * {@link #MAX_SHOWN} total regardless of how many relationships that spans. History itself is populated
 * by {@link RelationshipManager#recordEvent} (called from that class's own damage/death listeners, and
 * from {@code heroClass.mage.blood.TechMageBloodGuidance}'s own reinforcement) - this tech only ever reads it.
 */
public class TechMageBloodMemory extends TechHeroClass
{
	private static final int MAX_SHOWN = 10;

	public TechMageBloodMemory()
	{
		// new tech, no original cost to port - same weight/role as its sibling Blood Insight.
		super(Minestuckuniverseported.id("blood_memory"), EnumClass.MAGE, EnumAspect.BLOOD, 100, MSUTechType.UTILITY);
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

		List<Relationship.RelationshipEvent> merged = new ArrayList<>();
		for(Relationship rel : relationships)
			merged.addAll(rel.history);
		merged.sort(Comparator.comparingLong(Relationship.RelationshipEvent::tick).reversed());

		MutableComponent report = Component.literal("Memory of ")
				.append(Component.literal(target.getName().getString()).withStyle(ChatFormatting.AQUA))
				.append(Component.literal(":"));

		if(merged.isEmpty())
		{
			report.append(Component.literal(" nothing remembered").withStyle(ChatFormatting.GRAY));
		}
		else
		{
			long now = serverLevel.getGameTime();
			for(int i = 0; i < Math.min(MAX_SHOWN, merged.size()); i++)
			{
				Relationship.RelationshipEvent event = merged.get(i);
				long secondsAgo = (now - event.tick()) / 20L;
				report.append(Component.literal("\n - "))
						.append(Component.literal(event.description()))
						.append(Component.literal(" (" + secondsAgo + "s ago)").withStyle(ChatFormatting.GRAY));
			}
		}

		mage.sendSystemMessage(report);
		MSUAbilitechParticles.oneshot(level, target, 10, AspectColorHandler.get(EnumAspect.BLOOD));

		return false;
	}
}
