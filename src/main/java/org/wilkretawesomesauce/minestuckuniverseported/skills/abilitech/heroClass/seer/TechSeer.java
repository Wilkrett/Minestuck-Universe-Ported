package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.seer;

import com.mraof.minestuck.player.EnumClass;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.skills.MSUSkills;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.events.AbilitechTargetedEvent;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroClass.TechSeer} ("Seer's
 * Prediction") - hold while looking at another player to slowly reveal their Karma alignment (a
 * color-shifting particle aura on the target, gold/purple/green same as {@code mage.TechMage}'s own
 * self-reading), with a confirmed status message once held past 100 ticks. The reveal threshold rises
 * from &plusmn;20 to &plusmn;40 Karma if the <i>target</i> (not the caster) has {@link MSUSkills#KARMA}
 * unlocked, matching the original exactly.
 */
public class TechSeer extends TechHeroClass
{
	private static final int MIN_KARMA = 20;
	private static final int MIN_KARMA_WITH_BADGE = 40;

	public TechSeer()
	{
		super(Minestuckuniverseported.id("seer_prediction"), EnumClass.SEER, 1850, MSUTechType.UTILITY);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.HELD)
			return false;

		LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);
		if(!(target instanceof ServerPlayer targetPlayer))
			return false;

		if(NeoForge.EVENT_BUS.post(new AbilitechTargetedEvent(player, target, this, techSlot, null)).isCanceled())
			return false;

		if(!player.isCreative())
		{
			if(player.getFoodData().getFoodLevel() <= 0)
			{
				player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
				return false;
			}
			if(time % 20 == 0)
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);
		}

		GodTierData targetGodTier = targetPlayer.getData(MSUAttachments.GOD_TIER);
		int karma = targetGodTier.getStaticKarma() + targetGodTier.getTempKarma();
		int minKarma = targetGodTier.isBadgeActive(MSUSkills.KARMA, level, targetPlayer) ? MIN_KARMA_WITH_BADGE : MIN_KARMA;

		int alignmentColor = 0x00FF15;
		if(karma >= minKarma)
			alignmentColor = 0xFFD800;
		else if(karma <= -minKarma)
			alignmentColor = 0xB200FF;

		MSUAbilitechParticles.aura(level, target, 5, time > 15 ? alignmentColor : 0xD670FF);

		if(time > 100)
		{
			ChatFormatting color = karma >= minKarma ? ChatFormatting.GOLD : karma <= -minKarma ? ChatFormatting.DARK_PURPLE : ChatFormatting.GREEN;
			player.displayClientMessage(Component.translatable("status.karma", targetPlayer.getName(), karma).setStyle(Style.EMPTY.withColor(color)), true);
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
		return super.canAppearOnList(level, player) && player.getData(MSUAttachments.GOD_TIER).isAscended();
	}

	@Override
	public boolean canUnlock(Level level, Player player)
	{
		return super.canUnlock(level, player) && player.getData(MSUAttachments.GOD_TIER).isAscended();
	}
}
