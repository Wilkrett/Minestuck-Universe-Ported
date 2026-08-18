package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;

import com.mraof.minestuck.event.OnEntryEvent;
import com.mraof.minestuck.player.IdentifierHandler;
import com.mraof.minestuck.player.PlayerIdentifier;
import com.mraof.minestuck.skaianet.SburbPlayerData;
import com.mraof.minestuck.util.ColorHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.MSUItems;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.skills.TechBoondollarCost;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.TechReturn} ("Return Jump") - hold for
 * 10 ticks to teleport back to your own Land, real-registered as {@code MSUSkills#RETURN_JUMP} (2000
 * boondollars, real-gated on holding a {@link MSUItems#RETURN_MEDALLION}).
 * <p>
 * The original located the caster's Land via {@code SkaianetHandler#getMainConnection}/
 * {@code SburbConnection#getClientDimension}, a whole API surface that no longer exists in the 1.21.1
 * Minestuck port (Skaianet was rebuilt around {@code skaianet.SburbPlayerData} - confirmed via
 * {@code javap} against this project's real Minestuck dependency jar, not guessed). This project's real
 * substitute: {@link SburbPlayerData#get(ServerPlayer)}{@code .getLandDimensionIfEntered()}.
 * <p>
 * <b>Destination position, corrected</b>: an earlier version of this class used the target Land's own
 * {@link ServerLevel#getSharedSpawnPos()}, on the assumption that a Land dimension's shared spawn was set
 * to something meaningful at world-gen time, the same "sensible default landing spot" concept the
 * original's own {@code WorldProvider#getRandomizedSpawnPoint()} stood in for. That assumption was wrong -
 * confirmed the hard way (a real player report: Return Jump was landing everyone at the Land's raw
 * dimension origin, not anywhere near their actual base) and then confirmed <i>why</i> via {@code javap}
 * against this project's real Minestuck dependency jar: {@code com.mraof.minestuck.entry.EntryProcess}
 * (the real code that runs when a player enters their Land) copies their Overworld structure into the Land
 * at a computed offset and teleports them there directly - it never calls
 * {@code ServerLevel#setDefaultSpawnPos} or anything like it, so the Land's own shared spawn position is
 * simply never set, always defaulting to raw dimension origin. There's also no query anywhere in Minestuck
 * for "where did entry actually put this player" after the fact - {@link OnEntryEvent} itself only carries
 * the player, not a position. So {@link #onEntry} below records it directly: the instant entry finishes,
 * this listens for that event and captures the player's real live position into
 * {@link AbilitechLoadout#setLandEntryPoint}, real and persisted - see that class's own doc comment for
 * the full reasoning. {@code onUseTick} below reads it back; {@code getSharedSpawnPos()} is kept only as a
 * last-resort fallback for the edge case of a player who somehow has {@code hasEntered() == true} but no
 * recorded entry point (e.g. they entered before this fix ever ran once).
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public class TechReturn extends TechBoondollarCost
{
	private static final int CHARGE_TICKS = 10;
	private static final int FOOD_COST = 4;

	public TechReturn()
	{
		super(Minestuckuniverseported.id("return_jump"), 2000, MSUTechType.UTILITY);
		requiredStacks.add(() -> new ItemStack(MSUItems.RETURN_MEDALLION.get()));
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE || time > CHARGE_TICKS)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < FOOD_COST)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(!(player instanceof ServerPlayer serverPlayer))
			return false;

		PlayerIdentifier identifier = IdentifierHandler.encode(player);
		int color = ColorHandler.getColorForPlayer(identifier, level);
		MSUAbilitechParticles.aura(level, player, 2, color);

		if(time < CHARGE_TICKS)
			return true;

		SburbPlayerData data = SburbPlayerData.get(serverPlayer);
		ResourceKey<Level> landDim = data.getLandDimensionIfEntered();
		if(landDim == null)
			return false;

		MinecraftServer server = serverPlayer.getServer();
		ServerLevel destination = server == null ? null : server.getLevel(landDim);
		if(destination == null)
			return false;

		AbilitechLoadout loadout = player.getData(MSUAttachments.ABILITECH_LOADOUT);
		BlockPos entryPos = loadout.getLandEntryPos();
		ResourceKey<Level> entryDim = loadout.getLandEntryDim();
		BlockPos spawn = (entryPos != null && entryDim == landDim) ? entryPos : destination.getSharedSpawnPos();

		serverPlayer.teleportTo(destination, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, serverPlayer.getYRot(), serverPlayer.getXRot());

		if(!player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - FOOD_COST);

		return true;
	}

	@Override
	public boolean isUsableExternally(Level level, Player player)
	{
		return player.getFoodData().getFoodLevel() >= FOOD_COST && super.isUsableExternally(level, player);
	}

	/** Real port's actual data source for {@code onUseTick}'s destination above - see this class's own
	 * doc comment for why the Land's own shared spawn position can't be trusted instead. */
	@SubscribeEvent
	private static void onEntry(OnEntryEvent event)
	{
		ServerPlayer player = event.getPlayer().getPlayer(event.getMcServer());
		if(player == null)
			return;

		player.getData(MSUAttachments.ABILITECH_LOADOUT).setLandEntryPoint(player.blockPosition(), player.level().dimension());
	}
}
