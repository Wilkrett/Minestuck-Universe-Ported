package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.rogue;

import com.mraof.minestuck.player.EnumClass;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.Abilitech;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKey;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.badgeEffects.BadgeEffects;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechRegistry;
import org.wilkretawesomesauce.minestuckuniverseported.util.ClasspectColorHandler;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.events.AbilitechTargetedEvent;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroClass.TechRogueSteal} ("Roguelike
 * Adaptability") - tap while looking at another player to borrow the first ability on <i>their</i> loadout
 * (that isn't this tech itself and reports {@link Abilitech#isUsableExternally}) into this slot, then drive
 * it exactly like {@code bard.TechBardMetronome}/{@code mage.TechMageStudy} do - see those classes' own doc
 * comments for the shared external-tech mechanism.
 */
public class TechRogueSteal extends TechHeroClass
{
	public TechRogueSteal()
	{
		super(Minestuckuniverseported.id("roguelike_adaptability"), EnumClass.ROGUE, 88950, MSUTechType.HYBRID);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		BadgeEffects badgeEffects = player.getData(MSUAttachments.BADGE_EFFECTS);

		if(state == AbilitechKeyState.NONE)
		{
			var externalId = badgeEffects.getExternalTech(techSlot);
			Abilitech stolenTech = externalId == null ? null : MSUAbilitechRegistry.get(externalId);
			if(stolenTech != null)
				stolenTech.onUnequipped(level, player, techSlot);
			badgeEffects.setExternalTech(techSlot, null);
			return false;
		}

		LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);

		if(state == AbilitechKeyState.PRESS && target instanceof Player targetPlayer)
		{
			if(!NeoForge.EVENT_BUS.post(new AbilitechTargetedEvent(player, target, this, techSlot, null)).isCanceled())
			{
				GodTierData targetGodTier = targetPlayer.getData(MSUAttachments.GOD_TIER);
				Abilitech found = null;
				for(AbilitechKey key : AbilitechKey.values())
				{
					Abilitech tech = targetGodTier.getTech(key.ordinal());
					if(tech != null && tech.getClass() != getClass() && tech.isUsableExternally(level, player))
					{
						found = tech;
						break;
					}
				}

				if(found != null)
				{
					badgeEffects.setExternalTech(techSlot, found.getId());
					MSUAbilitechParticles.oneshot(level, player, 10, ClasspectColorHandler.get(EnumClass.ROGUE));
					player.displayClientMessage(Component.translatable("status.externalTech.casting", found.getDisplayName()), true);
				}
				else
				{
					player.displayClientMessage(Component.translatable("status.externalTech.notFound"), true);
					MSUAbilitechParticles.oneshot(level, player, 3, ClasspectColorHandler.get(EnumClass.ROGUE));
				}
			}
		}

		var externalId = badgeEffects.getExternalTech(techSlot);
		Abilitech stolenTech = externalId == null ? null : MSUAbilitechRegistry.get(externalId);
		if(stolenTech == null)
			return false;

		return stolenTech.onUseTick(level, player, techSlot, state, time);
	}
}
