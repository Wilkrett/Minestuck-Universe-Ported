package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.page;

import com.mraof.minestuck.player.EnumClass;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.MSUSkills#PERSEVERANT_AWAKENING} - Page's own
 * class tech, the one entry in the original's real cost table defined as an anonymous
 * {@code TechHeroClass} subclass directly inside {@code MSUSkills} rather than its own file (grepping the
 * original's real {@code heroClass} package for a {@code TechPage.java} turns up nothing - this is the
 * genuine reason). Given a real name/file here instead, matching this project's own standing preference
 * for named classes over anonymous ones. The original itself never gave it an {@code onUseTick}/active
 * mechanic of any kind either - just a stricter unlock gate - so there's nothing missing from this port,
 * it's a faithful empty-body match.
 * <p>
 * The original's real unlock gate also required {@code GodTierData#getSkillLevel(StatType.GENERAL) >= 10}
 * on top of being ascended - real now, via {@code godtier.GodTierData}'s own real (single-stat)
 * {@code getSkillLevel()} - see that field's own doc comment for why it's collapsed to one flat level
 * rather than the original's real per-{@code StatType} map.
 */
public class TechPagePerseverantAwakening extends TechHeroClass
{
	private static final int REQUIRED_SKILL_LEVEL = 10;

	public TechPagePerseverantAwakening()
	{
		super(Minestuckuniverseported.id("perseverant_awakening"), EnumClass.PAGE, 1000000);
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
		return super.canUnlock(level, player) && player.getData(MSUAttachments.GOD_TIER).isAscended()
				&& player.getData(MSUAttachments.GOD_TIER).getSkillLevel() >= REQUIRED_SKILL_LEVEL;
	}
}
