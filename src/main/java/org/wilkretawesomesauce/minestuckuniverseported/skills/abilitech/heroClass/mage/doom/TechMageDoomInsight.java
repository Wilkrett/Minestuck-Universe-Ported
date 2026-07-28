package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.mage.doom;

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
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.doom.DoomData;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;

/**
 * "Doom Insight" - new tech, ported from the "Doom Class Abilities Framework" design document (no
 * 1.12.2 original), Mage of Doom exclusive - press while aiming at a living entity to read out its
 * current {@code mechanics.doom.DoomData} (bound Doom, whether it's currently sealed, and any Doom
 * Mark it carries), the exact same "the Mage cannot create/manipulate, only understand" role
 * {@code heroClass.mage.blood.TechMageBloodInsight} already fills for the Relationship system - purely
 * read-only, no side effects on the target's actual Doom.
 * <p>
 * Priced the same as that sibling tech (100) - a cheap, purely-informational read, matching
 * {@code mage_awareness}'s own role as the cheapest tech in this project.
 */
public class TechMageDoomInsight extends TechHeroClass
{
	public TechMageDoomInsight()
	{
		super(Minestuckuniverseported.id("doom_insight"), EnumClass.MAGE, EnumAspect.DOOM, 100, MSUTechType.UTILITY);
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

		DoomData data = target.getData(MSUAttachments.DOOM_DATA);

		MutableComponent report = Component.literal("Doom of ")
				.append(Component.literal(target.getName().getString()).withStyle(ChatFormatting.AQUA))
				.append(Component.literal(": "))
				.append(Component.literal(String.valueOf(Math.round(data.getDoom() * 100) / 100.0)).withStyle(ChatFormatting.DARK_GREEN));

		if(data.isSealed())
			report.append(Component.literal(" (sealed)").withStyle(ChatFormatting.GRAY));

		if(data.isMarked())
		{
			String casterName = data.getMarkCasterId() != null && serverLevel.getEntity(data.getMarkCasterId()) instanceof LivingEntity casterEntity
					? casterEntity.getName().getString()
					: "someone no longer present";

			report.append(Component.literal(" (marked: " + data.getMarkType() + " by ").withStyle(ChatFormatting.DARK_PURPLE))
					.append(Component.literal(casterName).withStyle(ChatFormatting.DARK_PURPLE))
					.append(Component.literal(")").withStyle(ChatFormatting.DARK_PURPLE));
		}

		mage.sendSystemMessage(report);
		MSUAbilitechParticles.oneshot(level, target, EnumAspect.DOOM, 10);

		return false;
	}
}
