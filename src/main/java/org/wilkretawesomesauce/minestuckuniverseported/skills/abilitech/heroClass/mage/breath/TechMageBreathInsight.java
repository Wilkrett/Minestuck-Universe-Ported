package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.mage.breath;

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
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.freedom.FreedomData;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.freedom.FreedomLevel;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;

/**
 * "Breath Insight" - new tech, no 1.12.2 counterpart, Mage of Breath exclusive - press while aiming at a
 * living entity to read out its current hidden {@code mechanics.freedom.FreedomData} (raw 0-100 value,
 * its {@link FreedomLevel} bracket, who last raised it via Liberate, and who it's currently willingly
 * following, if anyone), the exact same "the Mage cannot create/manipulate, only understand" role
 * {@code mage.doom.TechMageDoomInsight}/{@code mage.blood.TechMageBloodInsight} already fill for their
 * own aspects - purely read-only, no side effects on the target's actual Freedom.
 * <p>
 * Priced the same as both those siblings (100) - a cheap, purely-informational read, matching
 * {@code mage_awareness}'s own role as the cheapest tech in this project.
 */
public class TechMageBreathInsight extends TechHeroClass
{
	public TechMageBreathInsight()
	{
		super(Minestuckuniverseported.id("breath_insight"), EnumClass.MAGE, EnumAspect.BREATH, 100, MSUTechType.UTILITY);
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

		FreedomData data = target.getData(MSUAttachments.FREEDOM_DATA);
		FreedomLevel freedomLevel = data.getLevel();

		MutableComponent report = Component.literal("Freedom of ")
				.append(Component.literal(target.getName().getString()).withStyle(ChatFormatting.AQUA))
				.append(Component.literal(": "))
				.append(Component.literal(Math.round(data.getFreedom()) + "/100").withStyle(colorFor(freedomLevel)))
				.append(Component.literal(" (" + levelName(freedomLevel) + ")").withStyle(colorFor(freedomLevel)));

		if(data.getLastLiberatedBy() != null)
		{
			String name = serverLevel.getEntity(data.getLastLiberatedBy()) instanceof LivingEntity liberator
					? liberator.getName().getString() : "someone no longer present";
			report.append(Component.literal(" (last raised by ").withStyle(ChatFormatting.GRAY))
					.append(Component.literal(name).withStyle(ChatFormatting.GRAY))
					.append(Component.literal(")").withStyle(ChatFormatting.GRAY));
		}

		if(data.getFollowing() != null)
		{
			String name = serverLevel.getEntity(data.getFollowing()) instanceof LivingEntity leader
					? leader.getName().getString() : "someone no longer present";
			report.append(Component.literal(" - willingly following ").withStyle(ChatFormatting.LIGHT_PURPLE))
					.append(Component.literal(name).withStyle(ChatFormatting.LIGHT_PURPLE));
		}

		mage.sendSystemMessage(report);
		MSUAbilitechParticles.oneshot(level, target, EnumAspect.BREATH, 10);

		return false;
	}

	private static ChatFormatting colorFor(FreedomLevel level)
	{
		return switch(level)
		{
			case HIGH -> ChatFormatting.GREEN;
			case NEUTRAL -> ChatFormatting.WHITE;
			case LOW -> ChatFormatting.GOLD;
			case EXTREME_LOW -> ChatFormatting.RED;
		};
	}

	private static String levelName(FreedomLevel level)
	{
		return switch(level)
		{
			case HIGH -> "High";
			case NEUTRAL -> "Neutral";
			case LOW -> "Low";
			case EXTREME_LOW -> "Extremely Low";
		};
	}
}
