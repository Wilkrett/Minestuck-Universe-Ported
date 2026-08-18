package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.witch.blood;

import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.AspectColorHandler;
import org.wilkretawesomesauce.minestuckuniverseported.events.AbilitechTargetedEvent;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.Relationship;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.relationship.RelationshipManager;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;

/**
 * "Cult of Personality" - new tech, ported from a design document (no 1.12.2 original), Witch of Blood
 * exclusive - the <i>first</i> tech in this project gated on a full Title (both a specific
 * {@link EnumClass} and a specific {@link EnumAspect}), which is why it lives in its own {@code blood}
 * subfolder under {@code heroClass.witch} rather than directly alongside {@code TechWitch}/
 * {@code TechWitchTrap}: this class extends {@link TechHeroClass} (matching every other file under
 * {@code heroClass}'s own folder-mirrors-superclass convention), passing {@link EnumAspect#BLOOD} as its
 * real {@code requiredAspect} - see that constructor's own doc comment for why this is a real gate
 * ({@link TechHeroClass#canAppearOnList}/{@code canUnlock} enforce it directly), not a cosmetic tag like
 * {@code TechHeroAspect}'s {@code flavorClasses}.
 * <p>
 * Press while aiming at a living entity to link it into a Blood Bond cult - see
 * {@link CultOfPersonalityManager}'s own doc comment for the real linking/effect logic this tech just
 * drives. The Witch doesn't need to be, and by default isn't, a member of the bond they create.
 * <p>
 * Pressing while aiming at an <i>already-bonded</i> member of the Witch's own cult doesn't attempt (and
 * silently fail) to re-link it - it selects that {@link Mob} as a command target instead (see
 * {@code CultOfPersonalityManager#selectCommandMob}). The Witch's next press then either orders it to
 * attack (aiming at a different living entity) or to walk to a location (aiming at a block) - see
 * {@link CultOfPersonalityManager}'s own doc comment for the real vanilla-AI caveat on commanding a
 * strictly passive mob to attack.
 * <p>
 * Pressing on a member of a bond corrupted by {@code heroClass.prince.blood.TechPrinceBloodSchism} instead
 * restores it - any Witch of Blood can do this, not just the bond's original creator - see
 * {@link CultOfPersonalityManager}'s own "Corruption" doc section.
 * <p>
 * Selecting a command target (see above) also has a small side effect straight from the "Crimson Discord"
 * design document's own "Interaction With Other Blood Classes": "Witch of Blood: Can reinforce
 * relationships and reduce Instability" - see {@link #calmRelationships}.
 */
public class TechBloodWitchCultOfPersonality extends TechHeroClass
{
	public TechBloodWitchCultOfPersonality()
	{
		super(Minestuckuniverseported.id("cult_of_personality"), EnumClass.WITCH, EnumAspect.BLOOD, 250000, MSUTechType.UTILITY);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.PRESS)
			return false;

		if(!(player instanceof ServerPlayer witch) || !(level instanceof ServerLevel serverLevel))
			return false;

		if(player.isShiftKeyDown())
		{
			CultOfPersonalityManager.resetPending(witch);
			player.displayClientMessage(Component.translatable("status.minestuckuniverseported.cultOfPersonalityReset"), true);
			return false;
		}

		Mob selectedMob = CultOfPersonalityManager.takeSelectedCommandMob(serverLevel, witch);
		if(selectedMob != null)
		{
			issueCommand(level, player, selectedMob);
			return false;
		}

		LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);
		if(target == null || NeoForge.EVENT_BUS.post(new AbilitechTargetedEvent(player, target, this, techSlot, null)).isCanceled())
			return false;

		// "Restored by a Witch of Blood" - Schism's own cleansing path (heroClass.prince.blood.TechPrinceBloodSchism),
		// any Witch of Blood, not just the bond's own creator. Checked first since a corrupted bond member
		// would otherwise just fall through to the (pointless) already-bonded relink attempt below.
		if(CultOfPersonalityManager.isCorrupted(target) && CultOfPersonalityManager.cleanseByWitch(serverLevel, target))
		{
			MSUAbilitechParticles.oneshot(level, target, 15, AspectColorHandler.get(EnumAspect.BLOOD));
			player.displayClientMessage(Component.translatable("status.minestuckuniverseported.cultOfPersonalityRestored"), true);
			return false;
		}

		if(target instanceof Mob mob && CultOfPersonalityManager.isOwnCultMember(witch, mob))
		{
			CultOfPersonalityManager.selectCommandMob(serverLevel, witch, mob);
			calmRelationships(serverLevel, mob);
			player.displayClientMessage(Component.translatable("status.minestuckuniverseported.cultOfPersonalitySelected"), true);
			return false;
		}

		boolean linked = CultOfPersonalityManager.tryLink(serverLevel, witch, target);

		MSUAbilitechParticles.oneshot(level, target, 15, AspectColorHandler.get(EnumAspect.BLOOD));
		player.displayClientMessage(Component.translatable(linked
				? "status.minestuckuniverseported.cultOfPersonalityLinked"
				: "status.minestuckuniverseported.cultOfPersonalityMarked"), true);

		return false;
	}

	/** Finishes a command started by selecting one of the Witch's own bonded {@link Mob}s - see this class's own doc comment. */
	private void issueCommand(Level level, Player player, Mob selectedMob)
	{
		LivingEntity rayEntity = MSUAbilitechRayTrace.getTargetEntity(player);
		if(rayEntity != null && rayEntity != selectedMob)
		{
			CultOfPersonalityManager.commandAttack(selectedMob, rayEntity);
			player.displayClientMessage(Component.translatable("status.minestuckuniverseported.cultOfPersonalityCommandAttack"), true);
		}
		else
		{
			BlockPos pos = MSUAbilitechRayTrace.getTargetBlock(player);
			if(pos != null)
			{
				CultOfPersonalityManager.commandMoveTo(selectedMob, pos);
				player.displayClientMessage(Component.translatable("status.minestuckuniverseported.cultOfPersonalityCommandMove"), true);
			}
		}

		MSUAbilitechParticles.oneshot(level, selectedMob, 15, AspectColorHandler.get(EnumAspect.BLOOD));
	}

	/** A small, real "Can reduce Instability" side effect of a Witch's own attention - see this class's own doc comment. Not a full Blood Guidance-strength reinforcement (that's {@code heroClass.mage.blood.TechMageBloodGuidance}'s own, larger job). */
	private static void calmRelationships(ServerLevel level, LivingEntity entity)
	{
		long tick = level.getGameTime();
		for(Relationship rel : RelationshipManager.getAllFor(entity.getUUID()))
			RelationshipManager.adjustInstability(rel, -5F, tick);
	}
}
