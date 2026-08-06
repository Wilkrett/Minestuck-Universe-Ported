package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.maid.mind;

import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.entity.GolemEntity;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.RelationshipManager;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.RelationshipType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;

/**
 * "Construct Golem" - new tech, no 1.12.2 counterpart, Maid of Mind exclusive
 * ({@code [Maid] [Mind] [Offense]}). Hold to charge (matching {@code heroAspect.hope.TechHopeGolem}'s own
 * real summon pattern) - on release/completion, wakes a real {@link GolemEntity} near the caster and forms
 * a real {@link RelationshipType#OWNERSHIP} relationship between them, per direct design intent instantly
 * rather than one the two have to organically earn over time - a Maid's own "shape decisions and
 * constructs into being" framing read literally: this isn't a bond that forms through the usual
 * {@code RelationshipManager} lazy-detection path (see that class's own doc comment for why this is a
 * deliberate, direct exception to that), it exists the moment the construct does.
 * <p>
 * {@link GolemEntity} itself carries no owner field or owner-specific behavior of any kind - this tech
 * doesn't set one. The entire "never targets/damages its owner, defends and assists them like a real
 * vanilla wolf" behavior that Ownership implies is generic, relationship-driven enforcement
 * ({@code mechanics.relationship.RelationshipCombatEvents}, see that class's own doc comment) that applies
 * to any entity with a real Ownership (or other positive) relationship, not something coded per summon
 * tech or per entity class.
 * <p>
 * The moment of summon itself is marked with a real, generally-reusable visual -
 * {@code skills.abilitech.MSUAbilitechParticles#focusFlash}, a fading {@code textures/foci/mind.png} icon
 * at the spawn point (the same real technique {@code heroAspect.TechTetherBond}'s own impact flash uses,
 * generalized into an "easy util" any tech can call - see that method's own doc comment).
 */
public class TechMaidMindConstructGolem extends TechHeroClass
{
	private static final int SUMMON_CHARGE_TICKS = 80;
	private static final float STARTING_STRENGTH = 70F;
	private static final float STARTING_STABILITY = 70F;

	public TechMaidMindConstructGolem()
	{
		super(Minestuckuniverseported.id("construct_golem"), EnumClass.MAID, EnumAspect.MIND, 120000, MSUTechType.OFFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.HELD)
			return false;

		if(!(level instanceof ServerLevel serverLevel))
			return false;

		if(time > SUMMON_CHARGE_TICKS)
			return false;

		if(time == SUMMON_CHARGE_TICKS)
		{
			GolemEntity golem = new GolemEntity(org.wilkretawesomesauce.minestuckuniverseported.MSUEntityTypes.GOLEM.get(), serverLevel);
			golem.setPos(player.getX() + serverLevel.getRandom().nextDouble() * 6 - 3,
					player.getY(), player.getZ() + serverLevel.getRandom().nextDouble() * 6 - 3);
			serverLevel.addFreshEntity(golem);

			RelationshipManager.getOrCreate(golem.getUUID(), player.getUUID(), RelationshipType.OWNERSHIP,
					serverLevel.getGameTime(), STARTING_STRENGTH, STARTING_STABILITY);

			MSUAbilitechParticles.oneshot(level, golem, EnumAspect.MIND, 20);
			MSUAbilitechParticles.focusFlash(level, golem.position(), EnumAspect.MIND);
		}

		MSUAbilitechParticles.aura(level, player, EnumAspect.MIND, (int) ((float) time / SUMMON_CHARGE_TICKS * 20));

		return true;
	}
}
