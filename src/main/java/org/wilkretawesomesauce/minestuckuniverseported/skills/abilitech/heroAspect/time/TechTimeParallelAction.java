package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time;

import com.mojang.authlib.GameProfile;
import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.AbilitechLoadout;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;
import org.wilkretawesomesauce.minestuckuniverseported.strife.StrifePortfolio;
import org.wilkretawesomesauce.minestuckuniverseported.strife.StrifeSpecibus;
import org.wilkretawesomesauce.minestuckuniverseported.timeline.DoomedTimelineClone;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUFakePlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "Parallel Action" tech, from the Time Aspect design discussion. Press to spawn a double of yourself
 * that guards the spot it was summoned at for 10 seconds (200 ticks); press again to dismiss it early.
 * <p>
 * <b>No longer a pure stub</b> - it used to just stand there ("doesn't act on its own yet", a stated,
 * deliberate gap). It now actually fights: every tick (driven straight out of the existing
 * {@code onUseTick} call in {@code AbilitechEvents#onPlayerTick}, which already runs every player-tick
 * regardless of key state - no separate driver needed), {@link #tickCombat} looks for the nearest
 * {@link Monster} within {@link #GUARD_RADIUS} of the spot the clone spawned at, walks toward it if out
 * of {@link #ATTACK_RANGE} and swings at it (real damage, via {@link Player#attack}) on a fixed cooldown
 * once in range. The guard point is fixed at the clone's spawn position, not wherever it currently is -
 * this is a stationary guardian, not something that chases targets across the map.
 * <p>
 * <b>Weapon is randomized on summon</b>: {@link #pickRandomWeapon} gathers one candidate per non-empty
 * strife specibus in the caster's portfolio (its first assigned item) plus whatever the caster is
 * currently holding, and picks uniformly at random among them - so the double might show up swinging a
 * different weapon than what's in your hand right now. Armor is copied exactly (not randomized - no
 * reason to hand the double worse defense than you actually have).
 * <p>
 * <b>Real bugs found and fixed after the first pass</b>: {@code MSUFakePlayer#tick()} is a no-op (by
 * design - it has no real connection to safely drive normal player-tick logic), which broke two things
 * that normally rely on it running: (1) with no gravity/physics at all, each movement step kept
 * whatever Y it last had, so the double would float or clip the instant it moved over anything but
 * dead-flat ground - fixed by snapping to {@link Heightmap.Types#MOTION_BLOCKING_NO_LEAVES} at its
 * destination column every step it takes (an instant climb/drop, not a real fall/step animation - see
 * known limitations below); (2) {@code Player#attackStrengthTicker} also only advances inside
 * {@code Player#tick()}, so every {@link Player#attack} call was permanently stuck at a near-zero
 * charge scale and dealt effectively no damage - fixed with a
 * {@code MSUFakePlayer#getAttackStrengthScale} override that always reports full charge (see that
 * class for why that's the correct fix for a non-ticking entity, not a balance choice); (3) a separate
 * bug, unrelated to the tick()-dependency pattern above: {@code strife.StrifeRestrictionEvents#onAttack}
 * was silently cancelling the clone's own attacks (in any world with {@code Config.restrictedStrife}
 * enabled) since it only exempted NeoForge's own {@code FakePlayer} class, not {@code MSUFakePlayer} -
 * fixed there directly, see that class's own doc comment; (4) {@code MSUFakePlayer} defaults to
 * invulnerable (right for the passive replay-ghost uses), which silently made this specific clone
 * unkillable in return - fixed by explicitly un-setting that right after spawn, <b>and</b> by no longer
 * requiring {@code isAlive()} to enter the per-tick handling below at all: {@code LivingEntity#isAlive()}
 * goes false the instant health hits 0, but {@code MSUFakePlayer#die()} is a no-op, so a defeated clone
 * would otherwise sit there forever, still physically present but untouchable by either combat-tick or
 * dismiss-on-press logic - "no longer alive" is now treated exactly like "duration expired".
 * <p>
 * <b>Still real known limitations, stated plainly</b>: movement is a straight-line step toward the
 * target with no pathfinding or obstacle avoidance (won't go around walls or actually climb/jump -
 * it'll just instantly snap to whatever height the ground is at its new column, per the heightmap fix
 * above) - fine for guarding open ground, will look wrong on complex terrain. The attack cooldown
 * ({@link #ATTACK_COOLDOWN_TICKS}) is a flat constant, not derived from the weapon's actual attack-speed
 * attribute. Copying the player's real skin onto the double still isn't done (default skin only). None
 * of the design doc's Stability/Timeline Debt costs are ported - this still uses the same food-cost
 * model as the other Time techs.
 */
public class TechTimeParallelAction extends TechHeroAspect
{
	private static final int ENERGY_USE = 10;
	private static final int DURATION_TICKS = 200;

	private static final double GUARD_RADIUS = 10.0;
	private static final double ATTACK_RANGE = 3.0;
	private static final double MOVE_SPEED = 0.25;
	private static final int ATTACK_COOLDOWN_TICKS = 12;

	/** Placeholder balance value - a flat, low HP pool so the clone is a meaningfully killable ally rather than a full-health second player, not derived from anything in the design doc. Revisit once real balancing passes happen. */
	private static final float CLONE_MAX_HEALTH = 5.0F;

	/** Per-clone AI state, keyed by the fake player's UUID - not persisted, cleared on despawn, like every other transient "currently active effect" bit of state elsewhere in this project. */
	private static final Map<UUID, CloneAi> activeClones = new HashMap<>();

	private static final class CloneAi
	{
		final Vec3 home;
		int attackCooldown = 0;

		CloneAi(Vec3 home)
		{
			this.home = home;
		}
	}

	public TechTimeParallelAction()
	{
		super(Minestuckuniverseported.id("parallel_action"), EnumAspect.TIME, 0, MSUTechType.UTILITY); // new tech, no original cost to port - see class doc comment
		setIcon("default");
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		AbilitechLoadout loadout = player.getData(MSUAttachments.ABILITECH_LOADOUT);
		Entity existing = loadout.getSlotTether(techSlot);

		if(state != AbilitechKeyState.PRESS)
		{
			if(existing instanceof MSUFakePlayer fake)
			{
				// LivingEntity#isAlive() goes false the instant health hits 0, but MSUFakePlayer#die() is a
				// no-op (by design - a fake player dying for real would be inappropriate), so a defeated
				// clone would otherwise never actually get removed: it'd sit there permanently, un-dismissable
				// (the PRESS branch below used to require isAlive() too), until manually /killed. Treat "no
				// longer alive" exactly like "duration expired" so a clone that gets killed in a real fight
				// is cleaned up immediately instead of turning into a stuck zombie entity.
				if(!fake.isAlive() || fake.tickCount > DURATION_TICKS)
					despawn(level, loadout, techSlot, fake);
				else if(level instanceof ServerLevel serverLevel)
					tickCombat(serverLevel, fake);
			}
			return existing != null;
		}

		if(existing instanceof MSUFakePlayer fake)
		{
			despawn(level, loadout, techSlot, fake);
			return true;
		}

		if(!(player instanceof ServerPlayer) || !(level instanceof ServerLevel serverLevel))
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < ENERGY_USE)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		GameProfile profile = new GameProfile(UUID.randomUUID(), player.getName().getString() + " (Echo)");
		MSUFakePlayer fakePlayer = new MSUFakePlayer(serverLevel, profile);
		fakePlayer.setPos(player.getX(), player.getY(), player.getZ());
		fakePlayer.setYRot(player.getYRot());
		fakePlayer.gameMode.changeGameModeForPlayer(GameType.ADVENTURE);
		// MSUFakePlayer defaults to invulnerable (right for the passive DoomedTimelineClone/TimeLoop replay
		// ghosts, which have no business taking damage mid-replay) - this one actually fights, so it needs
		// to be a real combatant that can be hit and killed back, not an invincible punching bag.
		fakePlayer.setInvulnerable(false);
		var maxHealthAttribute = fakePlayer.getAttribute(Attributes.MAX_HEALTH);
		if(maxHealthAttribute != null)
			maxHealthAttribute.setBaseValue(CLONE_MAX_HEALTH);
		fakePlayer.setHealth(CLONE_MAX_HEALTH);
		equipClone(player, fakePlayer);

		PlayerList packetTargets = serverLevel.getServer().getPlayerList();
		packetTargets.broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, fakePlayer));
		serverLevel.addNewPlayer(fakePlayer);
		DoomedTimelineClone.playGearsEffect(serverLevel, fakePlayer);

		activeClones.put(fakePlayer.getUUID(), new CloneAi(fakePlayer.position()));

		loadout.setSlotTether(techSlot, fakePlayer);

		if(!player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - ENERGY_USE);

		return true;
	}

	/** Copies the caster's real armor as-is, and arms the clone with a randomly-picked weapon (see {@link #pickRandomWeapon}). */
	private static void equipClone(Player player, MSUFakePlayer fake)
	{
		for(EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET})
			fake.setItemSlot(slot, player.getItemBySlot(slot).copy());

		fake.setItemSlot(EquipmentSlot.MAINHAND, pickRandomWeapon(player));
	}

	/** One candidate per non-empty strife specibus (its first assigned item) plus the caster's actual held item, chosen uniformly at random - "a small bit of randomization on what the clone is using". */
	private static ItemStack pickRandomWeapon(Player player)
	{
		List<ItemStack> candidates = new ArrayList<>();

		StrifePortfolio portfolio = player.getData(MSUAttachments.STRIFE_PORTFOLIO);
		for(StrifeSpecibus specibus : portfolio.getNonEmptyPortfolio())
			if(!specibus.getContents().isEmpty())
				candidates.add(specibus.getContents().getFirst().copy());

		ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
		if(!mainHand.isEmpty())
			candidates.add(mainHand.copy());

		if(candidates.isEmpty())
			return ItemStack.EMPTY;
		return candidates.get(player.level().getRandom().nextInt(candidates.size()));
	}

	/** Guards {@link CloneAi#home}: fights the nearest {@link Monster} within {@link #GUARD_RADIUS} of it, otherwise walks back home. See this class's own doc comment for the movement/attack simplifications. */
	private static void tickCombat(ServerLevel level, MSUFakePlayer fake)
	{
		CloneAi ai = activeClones.get(fake.getUUID());
		if(ai == null)
			return;

		if(ai.attackCooldown > 0)
			ai.attackCooldown--;

		AABB guardBox = new AABB(ai.home.x - GUARD_RADIUS, ai.home.y - GUARD_RADIUS, ai.home.z - GUARD_RADIUS,
				ai.home.x + GUARD_RADIUS, ai.home.y + GUARD_RADIUS, ai.home.z + GUARD_RADIUS);
		List<Monster> nearby = level.getEntitiesOfClass(Monster.class, guardBox, Monster::isAlive);

		Monster target = null;
		double closestSqr = Double.MAX_VALUE;
		for(Monster candidate : nearby)
		{
			double distSqr = candidate.distanceToSqr(fake);
			if(distSqr < closestSqr)
			{
				closestSqr = distSqr;
				target = candidate;
			}
		}

		Vec3 current = fake.position();
		Vec3 destination = target != null ? target.position() : ai.home;
		double distance = current.distanceTo(destination);

		if(target != null && distance <= ATTACK_RANGE)
		{
			fake.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
			fake.broadcastMovement();

			if(ai.attackCooldown <= 0)
			{
				fake.attack(target);
				fake.swing(InteractionHand.MAIN_HAND);
				ai.attackCooldown = ATTACK_COOLDOWN_TICKS;
			}
			return;
		}

		if(distance < 0.5)
			return;

		Vec3 step = new Vec3(destination.x - current.x, 0, destination.z - current.z);
		if(step.lengthSqr() < 1.0E-4)
			return;

		step = step.normalize().scale(Math.min(MOVE_SPEED, distance));
		Vec3 next = current.add(step);

		// MSUFakePlayer#tick() is a no-op, so there's no gravity/physics to keep it settled on the
		// ground as it walks - without this it just keeps whatever Y it spawned with forever, floating
		// or clipping the instant the ground isn't perfectly flat. Snapping straight to the heightmap
		// each step is an instant "climb/drop" rather than a real fall/step animation - a known,
		// accepted simplification (same flavor as this class's lack of real pathfinding), not a physics
		// simulation.
		int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(next.x), Mth.floor(next.z));
		next = new Vec3(next.x, groundY, next.z);

		fake.lookAt(EntityAnchorArgument.Anchor.EYES, destination);
		fake.moveTo(next.x, next.y, next.z, fake.getYRot(), fake.getXRot());
		fake.broadcastMovement();
	}

	private static void despawn(Level level, AbilitechLoadout loadout, int techSlot, ServerPlayer fake)
	{
		if(level instanceof ServerLevel serverLevel)
		{
			serverLevel.getServer().getPlayerList().broadcastAll(new ClientboundPlayerInfoRemovePacket(List.of(fake.getUUID())));
			DoomedTimelineClone.playGearsEffect(serverLevel, fake);
		}
		activeClones.remove(fake.getUUID());
		fake.remove(Entity.RemovalReason.KILLED);
		loadout.setSlotTether(techSlot, null);
	}

	@Override
	public void onUnequipped(Level level, Player player, int techSlot)
	{
		AbilitechLoadout loadout = player.getData(MSUAttachments.ABILITECH_LOADOUT);
		if(loadout.getSlotTether(techSlot) instanceof ServerPlayer fake && fake.isAlive())
			despawn(level, loadout, techSlot, fake);
	}
}
