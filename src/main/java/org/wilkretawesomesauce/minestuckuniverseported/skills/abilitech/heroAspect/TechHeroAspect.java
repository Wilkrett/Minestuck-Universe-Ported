package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect;

import com.mraof.minestuck.player.ClientPlayerData;
import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import com.mraof.minestuck.player.Title;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAspectColors;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUHeroClass;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.TechBoondollarCost;
import org.wilkretawesomesauce.minestuckuniverseported.skills.Skill;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.TechHeroAspect}. Now
 * extends {@link TechBoondollarCost} for real (matching the original's actual inheritance chain
 * {@code Abilitech -> TechBoondollarCost -> TechHeroAspect}) instead of skipping straight to
 * {@code Abilitech}, now that the unlock-cost economy is real - see {@link TechBoondollarCost}'s own doc
 * comment.
 * <p>
 * The original's {@code canUse} gate - amplifier &ge;1 of a {@code GOD_TIER_LOCK} potion effect - is real
 * (see {@link #canUse}), even though nothing in this project's port currently produces amplifier &ge;1
 * (that was reserved for the God Tier ascension ritual itself, a documented, deliberate simplification -
 * see {@code godtier.GodTierEvents}'s own doc comment). Ready infrastructure, same category as
 * {@code godtier.MediumData}'s Quest Bed position before any structure consumed it.
 * <p>
 * {@link #getColor()}/{@link #canAppearOnList} are real ports now too (using this project's own real
 * {@link MSUAspectColors} table and Minestuck's real {@link Title#isPlayerOfAspect} - both already-proven
 * modern APIs, not guessed). <b>{@code getDisplayTooltip()} is deliberately NOT overridden</b>, unlike the
 * original (which substituted the real secondary-action keybind name into the tooltip): doing so would
 * require referencing the client-only {@code KeyMapping} type from this common-loaded class, which - since
 * every hero-aspect tech across all aspects extends this one class - would give the project's existing,
 * documented "client-only type referenced from common code" dedicated-server crash bug (see
 * {@code AbilitechnosynthBlock}/{@code StrifeCardItem}) a much larger blast radius than its current two
 * known instances. Falls back to {@link Skill}'s
 * generic translation-key tooltip instead.
 * <p>
 * Always reports {@link MSUHeroClass#HERO} - see that enum's own doc comment for why every tech in this
 * {@code heroAspect} package is tagged that way regardless of aspect.
 */
public class TechHeroAspect extends TechBoondollarCost
{
	protected final EnumAspect heroAspect;
	protected final MSUTechType techType;
	private final EnumClass[] flavorClasses;

	public TechHeroAspect(ResourceLocation id, EnumAspect heroAspect, long cost, MSUTechType techType)
	{
		this(id, heroAspect, cost, techType, new EnumClass[0]);
	}

	public TechHeroAspect(ResourceLocation id, EnumAspect heroAspect, long cost, MSUTechType techType, EnumClass... flavorClasses)
	{
		super(id, cost);
		this.heroAspect = heroAspect;
		this.techType = techType;
		this.flavorClasses = flavorClasses;
	}

	public EnumAspect getHeroAspect()
	{
		return heroAspect;
	}

	public MSUTechType getTechType()
	{
		return techType;
	}

	/** Purely descriptive classpect tags - see the flavor-tagging constructor's own doc comment. Empty by default. */
	public EnumClass[] getFlavorClasses()
	{
		return flavorClasses;
	}

	public MSUHeroClass getHeroClass()
	{
		return MSUHeroClass.HERO;
	}

	/** Real port of the original's {@code GOD_TIER_LOCK} amplifier &ge;1 gate - see this class's own doc comment for why it's currently unreachable. */
	@Override
	public boolean canUse(Level level, Player player)
	{
		return !(player.hasEffect(MSUMobEffects.GOD_TIER_LOCK) && player.getEffect(MSUMobEffects.GOD_TIER_LOCK).getAmplifier() >= 1);
	}

	/**
	 * Real port of the original's Title-hero-aspect filter, via Minestuck's own real
	 * {@link Title#isPlayerOfAspect}.
	 * <p>
	 * <b>Real bug fix</b>: this used to read {@code !(player instanceof ServerPlayer serverPlayer) || ...},
	 * which was meant to fail open for some hypothetical non-{@code ServerPlayer} case, but in practice
	 * <i>always</i> evaluates to {@code true} on the client - {@code client.gui.SkillShopScreen} calls this
	 * with {@code Minecraft.getInstance().player}, a {@code LocalPlayer}, never a {@code ServerPlayer} - so
	 * every hero-aspect tech from every aspect silently listed for every player regardless of their own
	 * Title (a real, reported bug, caught from a live screenshot). Real fix: check Minestuck's own real
	 * client-synced {@link ClientPlayerData#getTitle()} on the client instead of defaulting to true.
	 */
	@Override
	public boolean canAppearOnList(Level level, Player player)
	{
		if(!super.canAppearOnList(level, player))
			return false;

		if(player instanceof ServerPlayer serverPlayer)
			return Title.isPlayerOfAspect(serverPlayer, heroAspect);

		Title title = ClientPlayerData.getTitle();
		return title != null && title.heroAspect() == heroAspect;
	}

	/**
	 * Real bug fix: {@code TechBoondollarCost#canUnlock} only ever checked item/boondollar cost, never
	 * aspect membership - since {@code network.SkillShopRequestPackets.Purchase} only calls
	 * {@link #canUnlock}, not {@link #canAppearOnList}, a player could buy any aspect's tech regardless of
	 * their own Title as long as they could afford it (the other half of the same reported bug as
	 * {@link #canAppearOnList}'s own fix above). {@link #canUnlock} only ever runs server-side (see that
	 * packet's own doc comment), so reusing {@link #canAppearOnList} here is safe and not redundant with the
	 * client-side list-filtering use of the same method.
	 */
	@Override
	public boolean canUnlock(Level level, Player player)
	{
		return super.canUnlock(level, player) && canAppearOnList(level, player);
	}

	/** Real port of the original's {@code getColor()} via this project's own real {@link MSUAspectColors} table. */
	@Override
	public int getColor()
	{
		return heroAspect == EnumAspect.SPACE ? 0x202020 : MSUAspectColors.get(heroAspect)[0];
	}
}
