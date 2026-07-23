package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.skills.Skill;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.Abilitech}. Properly extends
 * {@link Skill} now (matching the original's actual inheritance), instead of duplicating a chunk of
 * Skill's display-name/id scaffolding inline as an earlier version of this port did.
 * <p>
 * <b>{@code canUse} is not overridden here</b>, matching the original exactly - the original's own
 * {@code Abilitech} doesn't override it either, it just calls the inherited {@link Skill#canUse} from
 * {@link #isUsableExternally}. Real unlock-cost gating now lives on
 * {@link TechBoondollarCost}, matching the original's real inheritance chain
 * ({@code Abilitech -> TechBoondollarCost -> TechHeroAspect}/{@code abilitech.heroClass.TechHeroClass}).
 * <p>
 * Tech-type tagging ({@code EnumTechType} in the original) lives on {@link org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect}
 * as {@link MSUTechType} instead of here, since every concrete tech so far is hero-aspect-tagged anyway;
 * revisit if a tech ever needs a type without an aspect.
 * <p>
 * This class defines zero concrete techs on its own - see {@link MSUAbilitechRegistry} for where they'd
 * be registered once any exist.
 */
public class Abilitech extends Skill
{
	private boolean isSuper = false;
	private String iconOverride = null;

	public Abilitech(ResourceLocation id)
	{
		super(id);
	}

	/**
	 * By default a tech's icon is looked up by its own id (see {@code MSUAbilitechScreen.iconFor}). Use
	 * this for techs that don't have dedicated art yet, to point at a shared fallback icon instead (the
	 * original ships a generic {@code default.png} in the same icons folder for exactly this).
	 */
	public Abilitech setIcon(String iconId)
	{
		this.iconOverride = iconId;
		return this;
	}

	public String getIconId()
	{
		return iconOverride != null ? iconOverride : getId().getPath();
	}

	/** Ported from the original's {@code isSuper}: excludes this tech from normal random/obtainable pools. */
	public boolean isSuper()
	{
		return isSuper;
	}

	public Abilitech setSuper(boolean isSuper)
	{
		this.isSuper = isSuper;
		return this;
	}

	/**
	 * Called every server tick this tech is equipped, with the current activation key's state and how
	 * many ticks it's been in that state. Return true if the tech is actively "doing something" this
	 * tick (used to decide whether to keep any associated particle/status effects running).
	 */
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		return false;
	}

	/** Called every server tick this tech is equipped AND has passive mode toggled on for the owner. */
	public boolean onPassiveTick(Level level, Player player, int techSlot)
	{
		return false;
	}

	public void onEquipped(Level level, Player player, int techSlot)
	{
	}

	public void onUnequipped(Level level, Player player, int techSlot)
	{
	}

	public void onPassiveToggle(Level level, Player player, boolean active)
	{
	}

	/**
	 * Ported from the original's own per-tech {@code status.badgeEnabled}/{@code status.badgeDisabled}
	 * status message, sent whenever a passive-toggleable tech's on/off state actually flips. The
	 * original toggled straight from a key press in each such tech's own {@code onUseTick}; this
	 * project's passive toggle lives entirely in the loadout GUI instead (a shift-click on an equipped
	 * slot, see {@code network.AbilitechRequestPackets.TogglePassive}), so the handful of techs that
	 * actually check {@link AbilitechLoadout#isPassiveEnabled}/{@code isPassiveEnabledFor} call this
	 * from their own {@link #onPassiveToggle} override instead - deliberately opt-in per tech (not a
	 * blanket message for every tech, since {@code TogglePassive} can technically be sent for any
	 * equipped tech regardless of whether toggling it does anything).
	 */
	protected final void sendToggleMessage(Player player, boolean active)
	{
		player.displayClientMessage(Component.translatable(active ? "status.badgeEnabled" : "status.badgeDisabled", getDisplayName()), true);
	}

	/**
	 * Real port of the original's {@code Abilitech#isUsableExternally} default - whether another tech
	 * (e.g. {@code heroClass.bard.TechBardMetronome}, {@code heroClass.mage.TechMageStudy},
	 * {@code heroClass.rogue.TechRogueSteal}) may borrow and drive this one on someone else's behalf.
	 * The original also excluded anything on a config blacklist array
	 * ({@code MSUConfig.abilitechExternalUseBlacklist}) - not ported, this project's {@code Config.java}
	 * has no equivalent list and nothing needs one yet.
	 */
	public boolean isUsableExternally(Level level, Player player)
	{
		return canUse(level, player);
	}
}
