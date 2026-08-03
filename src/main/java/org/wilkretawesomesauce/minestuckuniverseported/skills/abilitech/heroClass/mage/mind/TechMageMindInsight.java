package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.mage.mind;

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
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.mind.DecisionData;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;

/**
 * "Mind Insight" - new tech, no 1.12.2 counterpart, Mage of Mind exclusive - press while aiming at a
 * living entity to read out its current hidden {@code mechanics.mind.DecisionData} (Certainty/Hesitation/
 * Adaptability/Resolve, and its current {@link org.wilkretawesomesauce.minestuckuniverseported.mechanics.mind.DecisionType}
 * + target, if any), the fourth sibling to {@code mage.blood.TechMageBloodInsight}/
 * {@code mage.doom.TechMageDoomInsight}/{@code mage.breath.TechMageBreathInsight} - same "the Mage cannot
 * create/manipulate, only understand" role, purely read-only, no side effects on the target's actual
 * Decision State. This is the source doc's own unbuilt "Predict" operation - see
 * {@code mechanics.mind.DecisionManager}'s own doc comment, which named this exact gap before this tech
 * existed to fill it.
 * <p>
 * Priced the same as all three siblings (100) - a cheap, purely-informational read.
 */
public class TechMageMindInsight extends TechHeroClass
{
	public TechMageMindInsight()
	{
		super(Minestuckuniverseported.id("mind_insight"), EnumClass.MAGE, EnumAspect.MIND, 100, MSUTechType.UTILITY);
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

		DecisionData data = target.getData(MSUAttachments.DECISION_DATA);

		MutableComponent report = Component.literal("Decisions of ")
				.append(Component.literal(target.getName().getString()).withStyle(ChatFormatting.AQUA))
				.append(Component.literal(":"))
				.append(attribute("Certainty", data.getCertainty()))
				.append(attribute("Hesitation", data.getHesitation()))
				.append(attribute("Adaptability", data.getAdaptability()))
				.append(attribute("Resolve", data.getResolve()));

		boolean hasDecision = data.getCurrentDecision() != null;
		boolean hasTarget = data.getCurrentDecisionTarget() != null;

		if(hasDecision || hasTarget)
		{
			report.append(Component.literal("\n - Currently: ").withStyle(ChatFormatting.GRAY))
					.append(Component.literal(hasDecision ? data.getCurrentDecision().toString() : "(untyped)").withStyle(ChatFormatting.LIGHT_PURPLE));

			if(hasTarget)
			{
				String name = serverLevel.getEntity(data.getCurrentDecisionTarget()) instanceof LivingEntity decisionTarget
						? decisionTarget.getName().getString() : "someone no longer present";
				boolean targetsMage = data.getCurrentDecisionTarget().equals(mage.getUUID());

				report.append(Component.literal(" targeting ").withStyle(ChatFormatting.GRAY))
						.append(Component.literal(targetsMage ? "you" : name).withStyle(targetsMage ? ChatFormatting.RED : ChatFormatting.LIGHT_PURPLE));
			}
		}
		else
		{
			report.append(Component.literal("\n - No committed decision right now").withStyle(ChatFormatting.GRAY));
		}

		if(!data.getHistory().isEmpty())
		{
			report.append(Component.literal("\n - Last: ").withStyle(ChatFormatting.GRAY))
					.append(Component.literal(data.getHistory().peekLast().description()).withStyle(ChatFormatting.DARK_AQUA));
		}

		mage.sendSystemMessage(report);
		MSUAbilitechParticles.oneshot(level, target, EnumAspect.MIND, 10);

		return false;
	}

	private static MutableComponent attribute(String label, float value)
	{
		ChatFormatting color = value > 50F ? ChatFormatting.GREEN : value < 50F ? ChatFormatting.GOLD : ChatFormatting.WHITE;
		return Component.literal(" " + label + " ").withStyle(ChatFormatting.GRAY)
				.append(Component.literal(Math.round(value) + "/100").withStyle(color));
	}
}
