package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;

import com.mraof.minestuck.inventory.captchalogue.CaptchaDeckHandler;
import com.mraof.minestuck.inventory.captchalogue.Modus;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
}
