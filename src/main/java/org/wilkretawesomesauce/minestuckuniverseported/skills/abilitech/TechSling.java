package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;

import com.mraof.minestuck.inventory.captchalogue.CaptchaDeckHandler;
import com.mraof.minestuck.inventory.captchalogue.Modus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.entity.MSUThrowableEntity;
import org.wilkretawesomesauce.minestuckuniverseported.skills.TechBoondollarCost;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.TechSling} ("Sylladex Sling") - hold
 * to zoom in (real client-side FOV nudge, see {@link SlingChargeEffect}/{@code client.SlingZoomEvents}),
 * release to launch the top item of your Captchalogue Modus forward as a damaging projectile
 * ({@link MSUThrowableEntity} - see that class's own doc comment for the one real scope trim from the
 * original: no per-weapon-property hooks, just the original's always-applicable "plain hit" damage path).
 * <p>
 * {@code com.mraof.minestuck.inventory.captchalogue.CaptchaDeckHandler}/{@code Modus} are the real modern
 * successors of the original's own {@code CaptchaDeckHandler}/{@code Modus} API (same class names,
 * confirmed present via {@code javap} against this project's real Minestuck dependency jar - the
 * captchalogue system itself wasn't renamed, only {@code Modus#getItem} gained a leading
 * {@code ServerPlayer} parameter and a real take-it-or-leave-it {@code boolean} that this port passes
 * {@code true} for, actually consuming the item into the throw - the original passed {@code false} there
 * despite the ability's whole point being to throw the item away, which reads as the original's own bug
 * rather than an intentional non-consuming "peek", so this port doesn't reproduce it).
 */
public class TechSling extends TechBoondollarCost
{
	private static final int MAX_CHARGE_TICKS = 20;
	private static final int FOOD_COST = 2;
	private static final float VELOCITY_PER_CHARGE_TICK = 0.075F;

	public TechSling()
	{
		super(Minestuckuniverseported.id("sylladex_sling"), 235, MSUTechType.OFFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE)
		{
			player.removeEffect(MSUMobEffects.SLING_CHARGE);
			return false;
		}

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < FOOD_COST)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			player.removeEffect(MSUMobEffects.SLING_CHARGE);
			return false;
		}

		int chargeTicks = Math.min(MAX_CHARGE_TICKS, time);
		if(chargeTicks < MAX_CHARGE_TICKS)
			player.addEffect(new MobEffectInstance(MSUMobEffects.SLING_CHARGE, 5, chargeTicks, false, false));

		if(state == AbilitechKeyState.RELEASED)
		{
			player.removeEffect(MSUMobEffects.SLING_CHARGE);

			if(!(player instanceof ServerPlayer serverPlayer))
				return false;

			Modus modus = CaptchaDeckHandler.getModus(serverPlayer);
			if(modus == null || modus.getSize() < 1)
				return false;

			ItemStack stack = modus.getItem(serverPlayer, 0, true);
			if(stack.isEmpty())
				return false;

			MSUThrowableEntity proj = new MSUThrowableEntity(level, player, stack);
			proj.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
			proj.shootFrom(player, chargeTicks * VELOCITY_PER_CHARGE_TICK, 0.0F);
			level.addFreshEntity(proj);

			if(!player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - FOOD_COST);
		}

		return true;
	}

	@Override
	public boolean isUsableExternally(Level level, Player player)
	{
		return player.getFoodData().getFoodLevel() >= FOOD_COST && super.isUsableExternally(level, player);
	}

	/**
	 * Marker effect - carries no attribute modifiers of its own, just lets the caster's own client know
	 * how far into its charge-up the current hold is, the same amplifier-as-charge-percentage idiom
	 * {@code TechTimeAccelerateSelf.AcceleratingEffect} already established (see that class's own doc
	 * comment). Amplifier is the current charge tick count, 0-20 (matching the original's own
	 * {@code IBadgeEffects#getFOV()} nudge, capped the same way - see {@link ClientEvents}, the sole
	 * consumer, and {@link #onUseTick} for where it's refreshed).
	 */
	public static class SlingChargeEffect extends MobEffect
	{
		public SlingChargeEffect()
		{
			super(MobEffectCategory.BENEFICIAL, 0x77FFEC);
		}
	}

	/**
	 * Client-only FOV zoom while charging this tech - real port of the original's own
	 * {@code IBadgeEffects#getFOV()} nudge (1 narrower per charging tick, capped at 20). Driven by
	 * {@link SlingChargeEffect}'s amplifier (see that class's own doc comment for why it carries charge
	 * ticks instead of a real effect strength), only ever meaningful for the local player - same reasoning
	 * as {@code TechTimeAccelerateSelf.ClientEvents}/{@code TechTimeDilation.ClientEvents}.
	 */
	@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
	public static final class ClientEvents
	{
		private static final int MAX_CHARGE_TICKS = 20;
		private static final float MAX_ZOOM_FACTOR = 0.5F;

		private ClientEvents()
		{
		}

		@SubscribeEvent
		private static void onComputeFov(ViewportEvent.ComputeFov event)
		{
			Minecraft mc = Minecraft.getInstance();
			LocalPlayer player = mc.player;
			if(player == null)
				return;

			MobEffectInstance instance = player.getEffect(MSUMobEffects.SLING_CHARGE);
			if(instance == null)
				return;

			float chargeRatio = Mth.clamp(instance.getAmplifier() / (float) MAX_CHARGE_TICKS, 0F, 1F);
			event.setFOV(event.getFOV() * (1.0F - chargeRatio * MAX_ZOOM_FACTOR));
		}
	}
}
