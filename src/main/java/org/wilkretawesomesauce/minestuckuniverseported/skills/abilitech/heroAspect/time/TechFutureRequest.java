package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time;

import com.mraof.minestuck.player.Echeladder;
import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.Config;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.MSUItemComponents;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;
import org.wilkretawesomesauce.minestuckuniverseported.network.TimeRequestSyncPacket;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.request.TimeRequest;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.request.TimeRequestCategory;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.request.TimeRequestData;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.request.TimeRequestTierRegistry;

import java.util.Locale;
import java.util.UUID;

/**
 * The "borrow an item from your future self" ability from the Time Request / Doom System design doc
 * (see {@code CLAUDE.md}). Mirrors {@code TechTimelineBranch}'s decide-at-release, hold-duration-tier
 * shape, but with {@link TimeRequestCategory#values()}.length (5) tiers of {@link #TIER_HOLD_TICKS} each
 * instead of 3 - releasing during a tier's window requests that category, resolved to a concrete item via
 * {@link TimeRequestTierRegistry} keyed by the player's {@link Echeladder} rung. An actionbar message each
 * tick during HELD shows which category is currently selected.
 * <p>
 * <b>Known gap, stated plainly</b> (same shape as {@code TechTimelineBranch}'s own doc comment): 5-tier
 * hold-duration selection is a stand-in for a real category-picker GUI, not built here.
 * <p>
 * The given item is stamped with {@code MSUItemComponents.BORROWED_REQUEST_ID} so the Temporal
 * Sendificator can reject it as its own repayment later - see that component's doc comment. Gated by
 * {@link Config#timeRequestCooldownTicks} so this can't be spammed for free progression-appropriate gear.
 */
public class TechFutureRequest extends TechHeroAspect
{
	private static final int TIER_HOLD_TICKS = 20;
	private static final TimeRequestCategory[] CATEGORIES = TimeRequestCategory.values();
	private static final int MAX_HOLD_TICKS = TIER_HOLD_TICKS * CATEGORIES.length;

	public TechFutureRequest()
	{
		super(Minestuckuniverseported.id("future_request"), EnumAspect.TIME, 40000, MSUTechType.UTILITY); // new tech, no original cost to port - picked to fit this project's own cost spread, see class doc comment
		setIcon("default");
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.PRESS || state == AbilitechKeyState.HELD)
		{
			if(time >= MAX_HOLD_TICKS)
				return false;
			player.displayClientMessage(Component.translatable(categoryKey(categoryFor(time))), true);
			MSUAbilitechParticles.aura(level, player, EnumAspect.TIME, 5);
			return true;
		}

		if(state != AbilitechKeyState.RELEASED)
			return false;

		if(!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer))
			return false;

		TimeRequestCategory category = categoryFor(Math.min(time, MAX_HOLD_TICKS - 1));

		TimeRequestData data = serverPlayer.getData(MSUAttachments.TIME_REQUEST_DATA);
		long now = serverLevel.getGameTime();
		if(now - data.getLastRequestGameTime() < Config.timeRequestCooldownTicks)
		{
			serverPlayer.displayClientMessage(Component.translatable("status.minestuckuniverseported.timeRequest.cooldown"), true);
			return false;
		}

		int rung = Echeladder.get(serverPlayer).getRung();
		ResourceLocation itemId = TimeRequestTierRegistry.pickItem(category, rung);
		if(itemId == null)
		{
			serverPlayer.displayClientMessage(Component.translatable("status.minestuckuniverseported.timeRequest.no_tier"), true);
			return false;
		}

		Item item = BuiltInRegistries.ITEM.get(itemId);
		ItemStack stack = new ItemStack(item);

		UUID requestId = UUID.randomUUID();
		stack.set(MSUItemComponents.BORROWED_REQUEST_ID, requestId);

		if(!serverPlayer.getInventory().add(stack))
			serverPlayer.drop(stack, false);

		data.addRequest(new TimeRequest(requestId, category, itemId, now, 0));
		data.setLastRequestGameTime(now);

		PacketDistributor.sendToPlayer(serverPlayer, TimeRequestSyncPacket.create(serverPlayer));
		serverPlayer.displayClientMessage(Component.translatable("status.minestuckuniverseported.timeRequest.borrowed",
				Component.translatable(item.getDescriptionId())), true);
		MSUAbilitechParticles.oneshot(level, player, EnumAspect.TIME, 20);
		return true;
	}

	private static TimeRequestCategory categoryFor(int heldTicks)
	{
		int index = Math.min(CATEGORIES.length - 1, heldTicks / TIER_HOLD_TICKS);
		return CATEGORIES[index];
	}

	private static String categoryKey(TimeRequestCategory category)
	{
		return "timeRequestCategory." + category.name().toLowerCase(Locale.ROOT);
	}
}
