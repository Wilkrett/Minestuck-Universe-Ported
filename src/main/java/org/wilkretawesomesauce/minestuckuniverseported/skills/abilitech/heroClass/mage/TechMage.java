package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.mage;

import com.mraof.minestuck.player.EnumClass;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.skills.MSUSkills;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUClassColors;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroClass.TechMage} ("Mage's
 * Awareness") - holding continuously reveals the caster's own Karma alignment via a color-shifting
 * particle aura (gold = heroic, purple = just, green = neutral, thresholded at &plusmn;20 Karma, real-rising
 * to &plusmn;40 with {@link MSUSkills#KARMA} unlocked), with a status-bar confirmation once held past 50 ticks.
 */
public class TechMage extends TechHeroClass
{
	private static final int MIN_KARMA = 20;
	private static final int MIN_KARMA_WITH_BADGE = 40;

	public TechMage()
	{
		super(Minestuckuniverseported.id("mage_awareness"), EnumClass.MAGE, 100, MSUTechType.UTILITY);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE)
			return false;

		if(!player.isCreative())
		{
			if(player.getFoodData().getFoodLevel() <= 0)
			{
				player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
				return false;
			}
			if(time % 30 == 0)
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);
		}

		GodTierData godTier = player.getData(MSUAttachments.GOD_TIER);
		int karma = godTier.getStaticKarma() + godTier.getTempKarma();
		int minKarma = godTier.isBadgeActive(MSUSkills.KARMA, level, player) ? MIN_KARMA_WITH_BADGE : MIN_KARMA;

		int alignmentColor = 0x00FF15;
		if(karma >= minKarma)
			alignmentColor = 0xFFD800;
		else if(karma <= -minKarma)
			alignmentColor = 0xB200FF;

		MSUAbilitechParticles.aura(level, player, 5, alignmentColor);
		MSUAbilitechParticles.aura(level, player, 2, MSUClassColors.get(EnumClass.MAGE));

		if(time > 50)
		{
			String key = karma >= minKarma ? "status.alignmentPrediction.heroicSelf"
					: karma <= -minKarma ? "status.alignmentPrediction.justSelf" : "status.alignmentPrediction.neutralSelf";
			ChatFormatting color = karma >= minKarma ? ChatFormatting.GOLD : karma <= -minKarma ? ChatFormatting.DARK_PURPLE : ChatFormatting.GREEN;
			player.displayClientMessage(Component.translatable(key).setStyle(Style.EMPTY.withColor(color)), true);
		}

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
		return player.getData(MSUAttachments.GOD_TIER).isAscended() && super.canAppearOnList(level, player);
	}

	@Override
	public boolean canUnlock(Level level, Player player)
	{
		return player.getData(MSUAttachments.GOD_TIER).isAscended() && super.canUnlock(level, player);
	}
}
