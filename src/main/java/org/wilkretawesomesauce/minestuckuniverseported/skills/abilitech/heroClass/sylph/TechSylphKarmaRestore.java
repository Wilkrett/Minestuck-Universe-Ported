package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.sylph;

import com.mraof.minestuck.player.EnumClass;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.badgeEffects.BadgeEffects;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.ClasspectColorHandler;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.events.AbilitechTargetedEvent;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroClass.TechSylphKarmaRestore}
 * ("Karmic Restoration") - hold while looking at another player to lock onto them (same real tether
 * mechanism as {@code sylph.TechSylph}) and nudge their Static/Temp Karma back toward zero one step at a
 * time, at a rate scaled by the caster's own Temp Karma, matching the same real "nudge one step toward
 * zero, Static first" idiom {@code mind.TechMindKarmaHeal}'s own passive already established.
 */
public class TechSylphKarmaRestore extends TechHeroClass
{
	public TechSylphKarmaRestore()
	{
		super(Minestuckuniverseported.id("karmic_restoration"), EnumClass.SYLPH, 1500000, MSUTechType.DEFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		BadgeEffects badgeEffects = player.getData(MSUAttachments.BADGE_EFFECTS);

		if(state == AbilitechKeyState.RELEASED)
			badgeEffects.setTether(techSlot, null);

		if(state == AbilitechKeyState.NONE)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 1)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		ServerPlayer target = badgeEffects.getTether(techSlot) instanceof ServerPlayer sp ? sp : null;
		if(target == null && MSUAbilitechRayTrace.getTargetEntity(player) instanceof ServerPlayer raytraced
				&& !NeoForge.EVENT_BUS.post(new AbilitechTargetedEvent(player, raytraced, this, techSlot, true)).isCanceled())
		{
			target = raytraced;
			badgeEffects.setTether(techSlot, target);
		}

		if(target != null)
		{
			GodTierData casterGodTier = player.getData(MSUAttachments.GOD_TIER);
			GodTierData targetGodTier = target.getData(MSUAttachments.GOD_TIER);

			int tickMod = (int) (4 + 0.4F * casterGodTier.getTempKarma());
			if(tickMod > 0 && (targetGodTier.getStaticKarma() != 0 || targetGodTier.getTempKarma() != 0) && time % tickMod == 0)
			{
				if(targetGodTier.getStaticKarma() != 0)
					targetGodTier.setStaticKarma(targetGodTier.getStaticKarma() + (targetGodTier.getStaticKarma() > 0 ? -1 : 1));
				else
					targetGodTier.setTempKarma(targetGodTier.getTempKarma() + (targetGodTier.getTempKarma() > 0 ? -1 : 1));

				if(time % (tickMod * 2) == 0 && !player.isCreative())
					player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);

				MSUAbilitechParticles.burst(target.level(), target, 4, ClasspectColorHandler.get(EnumClass.SYLPH));
			}
		}

		MSUAbilitechParticles.aura(level, player, 2, ClasspectColorHandler.get(EnumClass.SYLPH));

		return true;
	}

	@Override
	public boolean isUsableExternally(Level level, Player player)
	{
		return player.getFoodData().getFoodLevel() >= 1 && super.isUsableExternally(level, player);
	}

	@Override
	public boolean canAppearOnList(Level level, Player player)
	{
		return super.canAppearOnList(level, player) && player.getData(MSUAttachments.GOD_TIER).isAscended();
	}

	@Override
	public boolean canUnlock(Level level, Player player)
	{
		return super.canUnlock(level, player) && player.getData(MSUAttachments.GOD_TIER).isAscended();
	}
}
