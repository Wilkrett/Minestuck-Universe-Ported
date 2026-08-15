package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.heart;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.client.gui.SoulShockScreen;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.AbilitechLoadout;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.heart.TechSoulStun}
 * ("Soul Shock") - hold and aim at a target to lock onto it (tether persists even if you look away,
 * same shape as {@code TechTimeTickUp}). A {@link Mob} has its AI disabled outright
 * ({@code setNoAi(true)}); a {@link Player} gets {@link SoulShockedEffect}, which
 * {@code client.gui.SoulShockScreen}/{@code client.SoulShockClientEvents} force into a real,
 * inescapable-except-to-the-pause-menu screen takeover on their own client - the original's actual
 * centerpiece, now built for real instead of the vanilla-debuff stand-in used before this pass.
 */
public class TechSoulStun extends TechHeroAspect
{
	public TechSoulStun()
	{
		super(Minestuckuniverseported.id("soul_shock"), EnumAspect.HEART, 960000, MSUTechType.OFFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		AbilitechLoadout badgeEffects = player.getData(MSUAttachments.ABILITECH_LOADOUT);
		Entity tether = badgeEffects.getTether(techSlot);
		LivingEntity current = tether instanceof LivingEntity livingTether && livingTether.isAlive() ? livingTether : null;

		LivingEntity target = state == AbilitechKeyState.NONE ? null : current;
		if(target == null && state != AbilitechKeyState.NONE)
			target = MSUAbilitechRayTrace.getTargetEntity(player);

		if(current != target)
		{
			if(current instanceof Mob mob)
				mob.setNoAi(false);
			if(target instanceof Mob mob)
				mob.setNoAi(true);
			badgeEffects.setTether(techSlot, target);
		}

		if(state == AbilitechKeyState.NONE)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 1)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(target instanceof Player)
		{
			target.addEffect(new MobEffectInstance(MSUMobEffects.SOUL_SHOCKED, 20, 0, false, false));
			MSUAbilitechParticles.oneshot(level, target, 3, 0xFFB745, 0xFF7929);
		}
		else if(target != null)
			MSUAbilitechParticles.oneshot(level, target, EnumAspect.HEART, 3);

		if(!player.isCreative() && time % 10 == 0)
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);

		if(target instanceof Player)
			MSUAbilitechParticles.aura(level, player, 2, 0xFFB745, 0xFF7929);
		else if(target != null)
			MSUAbilitechParticles.aura(level, player, EnumAspect.HEART, 2);

		return true;
	}

	@Override
	public void onUnequipped(Level level, Player player, int techSlot)
	{
		AbilitechLoadout badgeEffects = player.getData(MSUAttachments.ABILITECH_LOADOUT);
		if(badgeEffects.getTether(techSlot) instanceof Mob mob)
			mob.setNoAi(false);
		badgeEffects.setTether(techSlot, null);
	}

	/**
	 * Marker effect - no attribute modifiers or tick behavior of its own. It exists purely so "is this
	 * player currently soul-shocked" is network-synced to their own client for free, which
	 * {@link SoulShockScreen}/{@link ClientEvents} needs to know without a bespoke synced flag.
	 */
	public static class SoulShockedEffect extends MobEffect
	{
		public SoulShockedEffect()
		{
			super(MobEffectCategory.HARMFUL, 0xFFB745);
		}
	}

	/**
	 * Client-side half of this tech - forces {@link SoulShockScreen} open on the local player for as long
	 * as they're soul-shocked, and closes it again the instant they're not (whether the effect wore off,
	 * or this tech was released on the server, both sync the same way any potion effect already does).
	 */
	@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
	public static final class ClientEvents
	{
		private ClientEvents()
		{
		}

		@SubscribeEvent
		private static void onClientTick(ClientTickEvent.Post event)
		{
			Minecraft mc = Minecraft.getInstance();
			LocalPlayer player = mc.player;
			if(player == null)
				return;

			boolean shocked = player.hasEffect(MSUMobEffects.SOUL_SHOCKED);

			// PauseScreen is deliberately exempt - SoulShockScreen#onClose() sends the player there on
			// Escape specifically so it stays reachable; re-forcing the stun screen open over it here would
			// defeat that entirely.
			if(shocked && !(mc.screen instanceof SoulShockScreen) && !(mc.screen instanceof PauseScreen))
				mc.setScreen(new SoulShockScreen());
			else if(!shocked && mc.screen instanceof SoulShockScreen)
				mc.setScreen(null);
		}
	}
}
