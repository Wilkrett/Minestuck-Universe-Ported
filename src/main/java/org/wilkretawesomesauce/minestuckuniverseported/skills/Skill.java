package org.wilkretawesomesauce.minestuckuniverseported.skills;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.Skill} - the base class {@code Abilitech}
 * originally extended (Abilitech itself extends {@code Skill} in the original; {@link Abilitech} extends
 * this directly here for the same reason). Provides the shared display-name/tooltip/unlock/sort scaffolding
 * that used to be duplicated ad hoc across {@link Abilitech}.
 * <p>
 * Not ported: registry-entry plumbing ({@code IForgeRegistryEntry} - replaced by {@link MSUAbilitechRegistry}
 * the same way {@code strife.KindAbstratus} replaced its Forge registry base).
 * <p>
 * <b>{@link #canUse} is real now</b>, not the earlier "no gating" hardcode - {@link MSUMobEffects#GOD_TIER_LOCK}
 * is a real effect (added earlier this session, see that class's own doc comment), and the original's base
 * {@code Skill#canUse} gate is amplifier &ge;2. Individual subclasses (e.g.
 * {@code abilitech.heroAspect.TechHeroAspect}) may still override with their own stricter/different gate -
 * the original's own {@code TechHeroAspect#canUse} doesn't call {@code super.canUse()} either, so this
 * matches that structure exactly rather than layering the two checks.
 */
public abstract class Skill implements Comparable<Skill>
{
	private static int sortCounter = 0;

	private final ResourceLocation id;
	private final int sortIndex;

	protected Skill(ResourceLocation id)
	{
		this.id = id;
		this.sortIndex = sortCounter++;
	}

	public ResourceLocation getId()
	{
		return id;
	}

	public String getTranslationKey()
	{
		return "tech." + id.getNamespace() + "." + id.getPath();
	}

	public Component getDisplayName()
	{
		return Component.translatable(getTranslationKey());
	}

	public Component getDisplayTooltip()
	{
		return Component.translatable(getTranslationKey() + ".tooltip");
	}

	/** Ported as-is: a subtle green-teal, the original's default tint for skill/tech icons and text. */
	public int getColor()
	{
		return 0x77FFEC;
	}

	public int getSortIndex()
	{
		return sortIndex;
	}

	public boolean canAppearOnList(Level level, Player player)
	{
		return true;
	}

	/** Real port of the original's {@code getUnlockRequirements()} - the shop screen's real display text for a skill's cost. */
	public Component getUnlockRequirements()
	{
		return Component.translatable(getTranslationKey() + ".unlock");
	}

	/** Real port of the original's {@code getTags()} - short searchable/displayable tags shown in the shop screen's description pane (e.g. an aspect tag). Empty by default. */
	public List<String> getTags()
	{
		return new ArrayList<>();
	}

	/** Real port of the original base {@code Skill#canUse} - blocked while locked at amplifier &ge;2. See this class's own doc comment. */
	public boolean canUse(Level level, Player player)
	{
		return !(player.hasEffect(MSUMobEffects.GOD_TIER_LOCK) && player.getEffect(MSUMobEffects.GOD_TIER_LOCK).getAmplifier() >= 2);
	}

	public boolean canUnlock(Level level, Player player)
	{
		return true;
	}

	public void onUnlock(Level level, Player player)
	{
	}

	public boolean canDisable()
	{
		return true;
	}

	/** Real port of the original's {@code isReadable}/{@code getReadRequirements} - whether this skill's
	 * description is currently visible at all (distinct from {@link #canUnlock}, which gates actually
	 * purchasing it). Only {@code badges.BadgeLevel} overrides this today. */
	public boolean isReadable(Level level, Player player)
	{
		return true;
	}

	@javax.annotation.Nullable
	public Component getReadRequirements()
	{
		return null;
	}

	public boolean isObtainable()
	{
		return true;
	}

	@Override
	public int compareTo(Skill other)
	{
		return this.sortIndex - other.sortIndex;
	}

	@Override
	public String toString()
	{
		return id.toString();
	}
}
