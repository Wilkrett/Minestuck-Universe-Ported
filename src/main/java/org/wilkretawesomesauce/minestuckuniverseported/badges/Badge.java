package org.wilkretawesomesauce.minestuckuniverseported.badges;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.skills.Skill;
import org.wilkretawesomesauce.minestuckuniverseported.skills.TechBoondollarCost;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.badges.Badge} - real, non-Abilitech
 * {@link Skill}s, built specifically to unblock the `heroClass` techs that read
 * {@code godtier.GodTierData#isBadgeActive} directly ({@code KARMA}, {@code EFFECT_BUFF},
 * {@code BADGE_PAGE}, {@code BADGE_OVERLORD}, {@code BUILDER_BADGE} - see each concrete class's own doc
 * comment). The rest of the original's real Badge hierarchy ({@code MasterBadge}/{@code BadgeConsort} +
 * the ~6 remaining bespoke sacrifice-gated badges no `heroClass` tech reads) is still real, ready future
 * work, not built here - this class's own shape already matches the original closely enough to extend
 * later.
 * <p>
 * <b>Known gap shared by every concrete badge here</b>: {@code client.gui.SkillShopScreen} only lists
 * {@code Abilitech}s, not badges (see that class's own doc comment) - the only currently-reachable unlock
 * path for any of them is {@code /msu godtier badge <id>} (real permission-level-2 debug command, still
 * spends the real cost) or a bespoke in-game trigger like {@code BadgeOverlord}'s own. Not a regression
 * from adding {@code BadgeBuilder} - every badge before it had exactly this same reachability already.
 * <p>
 * Unlike {@link TechBoondollarCost}, badges
 * have no shared boondollar-cost concept in the original - each concrete badge's own {@code canUnlock}
 * spends whatever bespoke real cost it wants (grist, items, XP levels) directly.
 */
public abstract class Badge extends Skill
{
	public static final List<Badge> BADGES = new ArrayList<>();

	protected Badge(ResourceLocation id)
	{
		super(id);
		BADGES.add(this);
	}

	/** Real port of the original's own {@code "badge." + unlocalizedName} prefix - distinct from
	 * {@link Skill#getTranslationKey}'s default {@code "tech."} prefix, matching the original's real
	 * naming split between Abilitechs and Badges. */
	@Override
	public String getTranslationKey()
	{
		return "badge." + getId().getNamespace() + "." + getId().getPath();
	}

	/** Real port of the original's {@code getTextureLocation()} - the real imported
	 * {@code textures/gui/badges/<id>.png} icon set, already present for every badge in this project's
	 * resources (used by a future Badge-aware Skill Shop screen). */
	public ResourceLocation getTextureLocation()
	{
		return ResourceLocation.fromNamespaceAndPath(Minestuckuniverseported.MODID, "textures/gui/badges/" + getId().getPath() + ".png");
	}

	/** Real port of {@code Badge#findItem} - identical to
	 * {@link TechBoondollarCost#findItem}, kept
	 * as its own copy here since the original itself duplicated this exact method onto both classes rather
	 * than sharing one, and this project doesn't override that structural choice. */
	public static boolean findItem(Player player, ItemStack stack, boolean decrement)
	{
		return TechBoondollarCost.findItem(player, stack, decrement);
	}

	/** Real port of {@code Badge#onBadgeUnlocked} - called once, right after a successful
	 * {@link #canUnlock} spends its cost. */
	public void onBadgeUnlocked(Level level, Player player)
	{
	}
}
