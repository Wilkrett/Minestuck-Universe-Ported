package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.mage;

import com.mraof.minestuck.player.EnumClass;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.Abilitech;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.AbilitechLoadout;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKey;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.SkillKeyStates;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRegistry;
import org.wilkretawesomesauce.minestuckuniverseported.skills.MSUSkills;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUClassColors;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.AbilitechTargetedEvent;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroClass.TechMageStudy} ("Arcane
 * Study") - a passive that auto-catches whichever real ability just targeted its owner (via
 * {@link AbilitechTargetedEvent}, the same real event {@code bard.TechBard}/{@code knight.TechKnightWard}/
 * etc. all fire) into this slot, then lets the owner drive that borrowed ability on demand exactly like
 * {@code bard.TechBardMetronome} drives its own random pick - see that class's own doc comment for the
 * shared {@link AbilitechLoadout#getExternalTech}/{@code #setExternalTech} mechanism both use.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public class TechMageStudy extends TechHeroClass
{
	public TechMageStudy()
	{
		super(Minestuckuniverseported.id("arcane_study"), EnumClass.MAGE, 625000, MSUTechType.PASSIVE, MSUTechType.HYBRID);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		AbilitechLoadout badgeEffects = player.getData(MSUAttachments.ABILITECH_LOADOUT);

		if(state == AbilitechKeyState.NONE)
			return false;

		var externalId = badgeEffects.getExternalTech(techSlot);
		Abilitech stolenTech = externalId == null ? null : MSUAbilitechRegistry.get(externalId);

		if(state == AbilitechKeyState.PRESS)
		{
			if(stolenTech == null)
			{
				MSUAbilitechParticles.aura(level, player, 3, MSUClassColors.get(EnumClass.MAGE));
				player.displayClientMessage(Component.translatable("status.externalTech.notFound"), true);
			}
			else
			{
				MSUAbilitechParticles.aura(level, player, 10, MSUClassColors.get(EnumClass.MAGE));
				player.displayClientMessage(Component.translatable("status.externalTech.casting", stolenTech.getDisplayName()), true);
			}
		}

		if(stolenTech == null)
			return false;

		boolean active = stolenTech.onUseTick(level, player, techSlot, state, time);

		if(state == AbilitechKeyState.RELEASED)
			stolenTech.onUnequipped(level, player, techSlot);

		return active;
	}

	@Override
	public boolean isUsableExternally(Level level, Player player)
	{
		return false;
	}

	@SubscribeEvent
	private static void onTechTargeted(AbilitechTargetedEvent event)
	{
		if(!(event.getTarget() instanceof Player target))
			return;

		SkillKeyStates keyStates = target.getData(MSUAttachments.SKILL_KEY_STATES);
		GodTierData godTier = target.getData(MSUAttachments.GOD_TIER);
		AbilitechLoadout badgeEffects = target.getData(MSUAttachments.ABILITECH_LOADOUT);
		if(!godTier.isPassiveEnabledFor(MSUSkills.ARCANE_STUDY))
			return;

		for(AbilitechKey key : AbilitechKey.values())
			if(MSUSkills.ARCANE_STUDY.equals(godTier.getTech(key.ordinal())) && keyStates.getKeyState(key) == AbilitechKeyState.NONE)
			{
				badgeEffects.setExternalTech(key.ordinal(), event.getAbilitech().getId());
				break;
			}
	}
}
