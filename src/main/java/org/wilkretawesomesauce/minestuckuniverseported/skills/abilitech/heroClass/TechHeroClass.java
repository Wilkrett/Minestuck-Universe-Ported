package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass;

import com.mraof.minestuck.player.ClientPlayerData;
import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import com.mraof.minestuck.player.Title;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.util.ClasspectColorHandler;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUHeroClass;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.TechBoondollarCost;

import javax.annotation.Nullable;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroClass.TechHeroClass} - the base
 * every real, Title-Class-specific tech in this new {@code heroClass} package extends, sibling to
 * {@code abilitech.heroAspect.TechHeroAspect} (both extend {@link TechBoondollarCost} directly, matching
 * the original's own real inheritance shape). Unlike {@code TechHeroAspect} (a single {@link MSUTechType}
 * field), this keeps the original's real {@code EnumTechType... techTypes} varargs shape, since several
 * real {@code heroClass} techs are tagged with more than one type (e.g. {@code heir.TechHeir} is both
 * {@code PASSIVE} and {@code DEFENSE}/{@code OFFENSE} depending on which of its two registered instances).
 * <p>
 * {@link #canUse}/{@link #getColor} mirror {@code TechHeroAspect}'s own real ports exactly (same
 * {@code GOD_TIER_LOCK} amplifier &ge;1 gate, same real color-table lookup - just {@link ClasspectColorHandler}
 * instead of {@code MSUAspectColors}). {@link #canAppearOnList} checks the real {@link Title#heroClass()}
 * accessor (this project doesn't have a matching {@code isPlayerOfClass} helper the way
 * {@link Title#isPlayerOfAspect} exists for aspects, so this reads {@link Title#getTitle(ServerPlayer)}
 * directly, same pattern {@code godtier.GodTierEvents} already uses elsewhere).
 */
public class TechHeroClass extends TechBoondollarCost
{
	protected final EnumClass heroClass;
	@Nullable
	protected final EnumAspect requiredAspect;
	protected final MSUTechType[] techTypes;

	public TechHeroClass(ResourceLocation id, EnumClass heroClass, long cost, MSUTechType... techTypes)
	{
		this(id, heroClass, null, cost, techTypes);
	}

	/**
	 * Real, load-bearing exclusivity - not the same thing as {@code TechHeroAspect}'s own
	 * {@code flavorClasses} (those are purely cosmetic tags, never gate anything). {@code requiredAspect}
	 * actually restricts {@link #canAppearOnList}/{@link #canUnlock} to players whose own Title is that
	 * exact aspect, on top of the class check every {@code heroClass} tech already has - for a tech gated
	 * on a full Title (both halves), like {@code witch.blood.TechBloodWitchCultOfPersonality} (Witch of
	 * Blood specifically, not any Witch).
	 */
	public TechHeroClass(ResourceLocation id, EnumClass heroClass, @Nullable EnumAspect requiredAspect, long cost, MSUTechType... techTypes)
	{
		super(id, cost);
		this.heroClass = heroClass;
		this.requiredAspect = requiredAspect;
		this.techTypes = techTypes;
	}

	public EnumClass getRealHeroClass()
	{
		return heroClass;
	}

	/** {@code null} for every plain class-only tech - see this class's own constructor doc comment. */
	@Nullable
	public EnumAspect getRequiredAspect()
	{
		return requiredAspect;
	}

	public MSUTechType[] getTechTypes()
	{
		return techTypes;
	}

	public MSUHeroClass getHeroClass()
	{
		return MSUHeroClass.from(heroClass);
	}

	/** Real port of the original's identical {@code GOD_TIER_LOCK} amplifier &ge;1 gate - see
	 * {@code heroAspect.TechHeroAspect#canUse}'s own doc comment, unchanged here. */
	@Override
	public boolean canUse(Level level, Player player)
	{
		return !(player.hasEffect(MSUMobEffects.GOD_TIER_LOCK) && player.getEffect(MSUMobEffects.GOD_TIER_LOCK).getAmplifier() >= 1);
	}

	/**
	 * Real port of the original's Title-hero-class filter.
	 * <p>
	 * <b>Real bug fix</b>: this used to read {@code !(player instanceof ServerPlayer serverPlayer) || ...},
	 * always evaluating {@code true} on the client - see {@code heroAspect.TechHeroAspect#canAppearOnList}'s
	 * own doc comment for the full explanation (same bug, same fix, this class's own real Title-class
	 * equivalent via {@link ClientPlayerData#getTitle()} instead of {@code EnumAspect}).
	 * <p>
	 * Also enforces {@link #requiredAspect} when a subclass set one - see that field's own constructor doc
	 * comment. {@code null} (the overwhelming majority of {@code heroClass} techs) skips this half entirely.
	 */
	@Override
	public boolean canAppearOnList(Level level, Player player)
	{
		if(!super.canAppearOnList(level, player))
			return false;

		if(player instanceof ServerPlayer serverPlayer)
		{
			if(!Title.getTitle(serverPlayer).map(t -> t.heroClass() == heroClass).orElse(false))
				return false;
			return requiredAspect == null || Title.isPlayerOfAspect(serverPlayer, requiredAspect);
		}

		Title title = ClientPlayerData.getTitle();
		if(title == null || title.heroClass() != heroClass)
			return false;
		return requiredAspect == null || title.heroAspect() == requiredAspect;
	}

	/**
	 * Real bug fix: see {@code heroAspect.TechHeroAspect#canUnlock}'s own doc comment - the same purchase-
	 * bypass bug (a player could buy any class's tech regardless of their own Title as long as they could
	 * afford it, since {@code TechBoondollarCost#canUnlock} never checked class membership) applied here too.
	 */
	@Override
	public boolean canUnlock(Level level, Player player)
	{
		return super.canUnlock(level, player) && canAppearOnList(level, player);
	}

	/** Real port of the original's {@code getColor()} via this project's own real {@link ClasspectColorHandler} table. */
	@Override
	public int getColor()
	{
		return ClasspectColorHandler.get(heroClass)[0];
	}

}
