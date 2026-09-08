package org.wilkretawesomesauce.minestuckuniverseported.mechanics.soul;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.entity.UninhabitedBodyEntity;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;

/**
 * The real hook-wiring half of the Soul system - {@code mechanics.mind}'s own established split between a
 * pure-data Manager ({@link SoulManager}, which alone mutates {@link PlayerSoul}/{@link BodyRegistryData})
 * and an Events class that alone touches live gameplay hooks. Original design for this project, no 1.12.2
 * counterpart.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class SoulProjectionEvents
{
	private SoulProjectionEvents()
	{
	}

	/**
	 * The per-tick anchor upkeep half of {@link SoulManager}'s own "hidden anchor" design, relevant only
	 * while occupying a host (see that class's own doc comment - a free Spirit is just the real player,
	 * needing none of this): rotation flows anchor -&gt; host (real mouse-look, unmodified, already drives
	 * the anchor normally), position flows host -&gt; anchor (the host is what's actually being simulated
	 * by gravity/collision/the relay controller) - this is what keeps the anchor's own unmodified
	 * attack/use-item/interact raycasts landing in the right place with zero custom relay code needed for
	 * those actions.
	 */
	@SubscribeEvent
	private static void onEntityTick(EntityTickEvent.Post event)
	{
		if(!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide())
			return;

		Entity target = SoulManager.resolveProjectionTarget(player);
		if(target == null)
			return;

		target.setYRot(player.getYRot());
		target.setXRot(player.getXRot());
		target.setYHeadRot(player.getYHeadRot());

		player.moveTo(target.getX(), target.getY(), target.getZ(), player.getYRot(), player.getXRot());
	}

	/** Design doc §17 ("Generic Host Entry") - a free Spirit right-clicking a valid, uninhabited {@link SoulHost} enters it, routed through {@link SoulManager#inhabitBody} rather than any per-entity-type interaction logic. */
	@SubscribeEvent
	private static void onEntityInteract(PlayerInteractEvent.EntityInteract event)
	{
		if(event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player))
			return;
		if(!player.getData(MSUAttachments.PLAYER_SOUL).isFreeSpirit())
			return; // not currently a free Spirit - nothing to enter from here

		if(!(event.getTarget() instanceof UninhabitedBodyEntity body) || !body.canBeInhabited())
			return;

		event.setCanceled(true);
		SoulManager.inhabitBody(player, body);
	}

	/**
	 * Design doc §5 ("not normally attack entities... not normally interact with blocks... unable to
	 * break or place blocks") - a real, direct user request after a live test found a free Spirit could
	 * still deal damage and break blocks. Scoped to {@link ISoulData#isFreeSpirit()} specifically, not
	 * host occupation - an occupied {@code UninhabitedBodyEntity} genuinely does support attack/block
	 * interaction ({@code HostCapabilities#FULL}), matching design doc §14's own capability table.
	 */
	@SubscribeEvent
	private static void onAttack(AttackEntityEvent event)
	{
		if(event.getEntity() instanceof ServerPlayer player && player.getData(MSUAttachments.PLAYER_SOUL).isFreeSpirit())
			event.setCanceled(true);
	}

	/** See {@link #onAttack}'s own doc comment. */
	@SubscribeEvent
	private static void onBlockBreak(BlockEvent.BreakEvent event)
	{
		if(event.getPlayer() instanceof ServerPlayer player && player.getData(MSUAttachments.PLAYER_SOUL).isFreeSpirit())
			event.setCanceled(true);
	}

	/** See {@link #onAttack}'s own doc comment - covers both placing a block and any other right-click-on-a-block interaction (opening a container, pressing a button, etc.), matching the design doc's own blanket "not normally interact with blocks" wording rather than just placement specifically. */
	@SubscribeEvent
	private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event)
	{
		if(event.getEntity() instanceof ServerPlayer player && player.getData(MSUAttachments.PLAYER_SOUL).isFreeSpirit())
			event.setCanceled(true);
	}

	/** Design doc §19/§20 ("Host Death/Destruction" / "Uninhabited Body Death"). */
	@SubscribeEvent
	private static void onBodyDeath(LivingDeathEvent event)
	{
		if(event.getEntity() instanceof UninhabitedBodyEntity body && !body.level().isClientSide())
			BodyManager.onBodyDeath(body);
	}

	/** Real caller: {@link SoulManager#reapplyProjectionState}'s own doc comment - potion effects/invulnerable/camera don't reliably survive a relog for a fresh {@link ServerPlayer} instance. */
	@SubscribeEvent
	private static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
	{
		if(event.getEntity() instanceof ServerPlayer player)
			SoulManager.reapplyProjectionState(player);
	}

	/** Same reasoning as {@link #onLoggedIn} - a respawn also produces a fresh {@link ServerPlayer} instance. In practice the anchor is always invulnerable while projecting, so this should never actually have anything to do, but costs nothing to keep as a defensive safeguard. */
	@SubscribeEvent
	private static void onRespawn(PlayerEvent.PlayerRespawnEvent event)
	{
		if(event.getEntity() instanceof ServerPlayer player)
			SoulManager.reapplyProjectionState(player);
	}

	@SubscribeEvent
	private static void onServerTick(ServerTickEvent.Post event)
	{
		BodyManager.tickReplacements(event.getServer());
	}
}
