package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKey;

import com.mraof.minestuck.computer.editmode.ServerEditHandler;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.SkillKeyStates;
import org.wilkretawesomesauce.minestuckuniverseported.network.MSUAbilitechPackets;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code SkillKeyStates.onPlayerTick}/{@code onWorldJoin}.
 * <p>
 * Simplification from the original: the {@code IBadgeEffects}-based checks (skip ticking while
 * time-stopped or soul-shocked) aren't ported - those specific fields were deliberately not among the
 * handful pulled into this project's own real {@code capabilities.badgeEffects.BadgeEffects} (see that
 * class's own doc comment), since time-stop/soul-shock are already real, dedicated marker
 * {@code MobEffect}s elsewhere and re-checking them here would be redundant with how those effects
 * already gate behavior at their own call sites. The "not in edit mode" and "not spectator/dead" guards
 * are kept - those don't depend on anything out of scope.
 * <p>
 * Also handles the login/respawn sync - same reasoning as {@code StrifePortfolioEvents}: attachments
 * aren't automatically synced to the client, so without this the loadout screen would show stale/empty
 * data until some other mutation happened to trigger a sync.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class AbilitechEvents
{
	private AbilitechEvents()
	{
	}

	@SubscribeEvent
	private static void onEntityJoinLevel(EntityJoinLevelEvent event)
	{
		if(!event.getLevel().isClientSide() && event.getEntity() instanceof ServerPlayer player)
			player.getData(MSUAttachments.SKILL_KEY_STATES).resetKeyStates();
	}

	@SubscribeEvent
	private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
	{
		if(event.getEntity() instanceof ServerPlayer player)
			MSUAbilitechPackets.sendLoadoutSync(player);
	}

	@SubscribeEvent
	private static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event)
	{
		if(event.getEntity() instanceof ServerPlayer player)
			MSUAbilitechPackets.sendLoadoutSync(player);
	}

	@SubscribeEvent
	private static void onPlayerTick(PlayerTickEvent.Post event)
	{
		if(event.getEntity().level().isClientSide())
			return;
		if(!(event.getEntity() instanceof ServerPlayer player))
			return;

		SkillKeyStates keyStates = player.getData(MSUAttachments.SKILL_KEY_STATES);
		GodTierData godTier = player.getData(MSUAttachments.GOD_TIER);

		boolean canAct = !player.isSpectator() && player.isAlive() && ServerEditHandler.getData(player) == null;

		for(AbilitechKey key : AbilitechKey.values())
		{
			Abilitech tech = godTier.getTech(key.ordinal());
			if(tech == null)
				continue;

			if(canAct && tech.canUse(player.level(), player))
			{
				tech.onUseTick(player.level(), player, key.ordinal(), keyStates.getKeyState(key), keyStates.getKeyTime(key));

				if(godTier.isPassiveEnabled(key.ordinal()))
					tech.onPassiveTick(player.level(), player, key.ordinal());
			}
		}

		keyStates.tickKeyStates();
	}
}
