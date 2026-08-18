package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.bard;

import com.mraof.minestuck.player.EnumClass;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.Abilitech;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.badgeEffects.BadgeEffects;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechRegistry;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroClass.TechBardMetronome} ("Magic
 * Metronome") - holding it picks a uniformly-random other registered {@link Abilitech} (that's currently
 * {@link Abilitech#isUsableExternally usable externally}) and drives it on the caster's behalf for as long
 * as the key is held, matching the original's own {@code IBadgeEffects#getExternalTech}-backed "borrow a
 * random ability" idiom - now real via {@link BadgeEffects#getExternalTech}/{@code #setExternalTech}
 * and the real {@link MSUAbilitechRegistry}. Only appears/unlocks for an ascended God Tier player, matching
 * the original's own real gate.
 */
public class TechBardMetronome extends TechHeroClass
{
	public TechBardMetronome()
	{
		super(Minestuckuniverseported.id("magic_metronome"), EnumClass.BARD, 115000, MSUTechType.HYBRID);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		BadgeEffects badgeEffects = player.getData(MSUAttachments.BADGE_EFFECTS);

		if(state == AbilitechKeyState.NONE)
		{
			badgeEffects.setExternalTech(techSlot, null);
			return false;
		}

		if(!player.isCreative() && player.getFoodData().getFoodLevel() <= 0)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		var externalId = badgeEffects.getExternalTech(techSlot);
		Abilitech externalTech = externalId == null ? null : MSUAbilitechRegistry.get(externalId);

		if(externalTech == null)
		{
			List<Abilitech> pool = new ArrayList<>(MSUAbilitechRegistry.getAll());
			pool.removeIf(tech -> tech == this || !tech.isUsableExternally(level, player));
			if(pool.isEmpty())
			{
				player.displayClientMessage(Component.translatable("status.externalTech.notFound"), true);
				return false;
			}
			externalTech = pool.get(level.getRandom().nextInt(pool.size()));
			player.displayClientMessage(Component.translatable("status.externalTech.casting", externalTech.getDisplayName()), true);
			badgeEffects.setExternalTech(techSlot, externalTech.getId());
		}

		boolean active = externalTech.onUseTick(level, player, techSlot, state, time);

		if(state == AbilitechKeyState.RELEASED)
		{
			externalTech.onUnequipped(level, player, techSlot);
			if(!player.isCreative())
				player.getFoodData().setFoodLevel(0);
		}

		return active;
	}

	@Override
	public boolean isUsableExternally(Level level, Player player)
	{
		return false;
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
