package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.mind;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.mind.DecisionManager;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.AbilitechLoadout;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;
import org.wilkretawesomesauce.minestuckuniverseported.network.MindControlSyncPacket;

/**
 * Ported 1:1 from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.mind.TechMindControl}
 * ("Mindflayer's Spell") - press and aim to possess a target, press again (or run out of food, or
 * stray more than a reasonable range) to release it.
 * <p>
 * <b>Non-player target - real, full puppeting.</b> A real {@link MindflayerGoal} is added to the
 * target {@link Mob}'s own goal selector - see that class's own doc comment for how it drives movement/
 * attacks off the controller's aim using genuine pathfinding, actually more capable than this project's
 * other puppeted-entity movement.
 * <p>
 * <b>Player target - real look-forcing AND real movement puppeting.</b> Every tick, the possessed
 * player's camera is forced to match the controller's own look direction via the real
 * {@code ServerPlayer#lookAt(Anchor, Vec3)} packet (aimed at a point projected far along the
 * controller's view vector, not just "look at the controller's position" - the same apparent direction
 * the original's client-side {@code player.turn(...)} nudge produced). Movement puppeting is real too
 * now: this tech applies {@link MindControllingEffect} to the controller (a marker their own client
 * checks), whose {@code client.MindControlClientEvents} forwards the controller's own WASD to the
 * server every tick as a world-relative vector ({@code network.MindControlInputPacket}), which the
 * server relays down to the target's own client ({@code network.MindControlSyncPacket}) to apply as
 * its own local input override - the same three-hop client-forwards-to-server-relays-to-client design
 * the original used, since a live connected player's own movement is otherwise client-authoritative.
 * <p>
 * Also real now: a target carrying {@link MSUMobEffects#MIND_FORTITUDE} can't be newly possessed, and
 * an already-possessed target that gains it is immediately released - matching the original's own two
 * real checks. The original's third check (auto-release once the target strays more than 20 blocks away)
 * isn't ported - a separate distance-limit mechanic, not part of "port the real potion effects."
 * <p>
 * <b>Real Resolve resistance</b>, from the later "Mind Aspect System Design" document (no 1.12.2
 * counterpart): {@code mechanics.mind.DecisionData#getResolve()} - "harder to redirect... less
 * vulnerable to Mind abilities" at high Resolve - gets a real chance to reject the possession attempt
 * outright, above-neutral Resolve only (same "only resists above the 50 baseline" shape
 * {@code mechanics.freedom.FreedomEvents} already established for its own resistance checks). This is
 * the single most literal "redirect this entity's decisions" ability in the whole project, so it's the
 * natural first real consumer of Resolve - checked once, on the initial possession attempt only (not
 * every held tick), matching how the original tech itself only ever rolls its own gates once per press.
 */
public class TechMindControl extends TechHeroAspect
{
	private static final double PROJECT_DISTANCE = 64;

	public TechMindControl()
	{
		super(Minestuckuniverseported.id("mindflayers_spell"), EnumAspect.MIND, 1800000, MSUTechType.OFFENSE);
	}

	@Override
	public void onUnequipped(Level level, Player player, int techSlot)
	{
		super.onUnequipped(level, player, techSlot);
		release(player, techSlot);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		AbilitechLoadout badgeEffects = player.getData(MSUAttachments.ABILITECH_LOADOUT);
		Entity target = badgeEffects.getTether(techSlot);

		if(state == AbilitechKeyState.PRESS)
		{
			if(target != null)
			{
				release(player, techSlot);
				return true;
			}

			LivingEntity newTarget = MSUAbilitechRayTrace.getTargetEntity(player);
				if(newTarget != null && newTarget.hasEffect(MSUMobEffects.MIND_FORTITUDE))
					newTarget = null;
				if(newTarget != null && DecisionManager.resistsInfluence(newTarget))
				{
					player.displayClientMessage(Component.translatable("status.mindResisted"), true);
					newTarget = null;
				}

			if(newTarget != null && newTarget != player)
			{
				badgeEffects.setTether(techSlot, newTarget);
				target = newTarget;

				if(newTarget instanceof Mob mob)
					mob.goalSelector.addGoal(0, new MindflayerGoal(mob, player));
				else if(newTarget instanceof ServerPlayer)
					player.addEffect(new MobEffectInstance(MSUMobEffects.MIND_CONTROLLING, -1, 0, true, false));
			}

			MSUAbilitechParticles.oneshot(level, player, EnumAspect.MIND, target != null ? 5 : 2);
		}

		if(target == null)
			return false;

		if(target instanceof LivingEntity livingTarget && livingTarget.hasEffect(MSUMobEffects.MIND_FORTITUDE))
		{
			release(player, techSlot);
			return false;
		}

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 1)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			release(player, techSlot);
			return false;
		}

		if(!player.isCreative() && time % 40 == 0)
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);

		if(target instanceof ServerPlayer possessed)
		{
			Vec3 projected = player.getEyePosition(1.0F).add(player.getViewVector(1.0F).scale(PROJECT_DISTANCE));
			possessed.lookAt(EntityAnchorArgument.Anchor.EYES, projected);
		}

		MSUAbilitechParticles.aura(level, player, EnumAspect.MIND, 2);

		return true;
	}

	private static void release(Player player, int techSlot)
	{
		AbilitechLoadout badgeEffects = player.getData(MSUAttachments.ABILITECH_LOADOUT);
		Entity target = badgeEffects.getTether(techSlot);

		if(target instanceof Mob mob)
		{
			var toRemove = mob.goalSelector.getAvailableGoals().stream()
					.map(net.minecraft.world.entity.ai.goal.WrappedGoal::getGoal)
					.filter(goal -> goal instanceof MindflayerGoal flayer && flayer.getController() == player)
					.toList();
			toRemove.forEach(mob.goalSelector::removeGoal);
		}
		else if(target instanceof ServerPlayer possessed)
		{
			PacketDistributor.sendToPlayer(possessed, new MindControlSyncPacket(false, 0, 0, false, false));
		}

		badgeEffects.setTether(techSlot, null);
		GodTierData godTier = player.getData(MSUAttachments.GOD_TIER);

		boolean stillControllingAPlayer = false;
		for(int slot = 0; slot < AbilitechLoadout.SLOTS; slot++)
			if(slot != techSlot && godTier.getTech(slot) instanceof TechMindControl && badgeEffects.getTether(slot) instanceof ServerPlayer)
				stillControllingAPlayer = true;

		if(!stillControllingAPlayer)
			player.removeEffect(MSUMobEffects.MIND_CONTROLLING);
	}
}
