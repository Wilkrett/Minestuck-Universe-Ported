package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.hope;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.Input;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.hope.TechHopeyShit}
 * ("Hopeful Outburst") - hold to become a chaotic force: everything nearby (in a radius that grows the
 * longer you hold, up to {@link #MAX_RADIUS}) takes 1 chip damage and gets pushed away from you every 5
 * ticks, with an occasional goofy on-screen message for yourself and anyone else close enough to see it,
 * while your own movement input gets dampened and you're continuously nudged upward (a "giddy, floating,
 * can't quite control yourself" self-effect - see {@link HopingEffect}/{@code client.HopefulOutburstClientEvents}).
 * <p>
 * One thing is still downgraded rather than fully ported: the original's full-screen {@code SPacketTitle}
 * popups become action-bar messages instead - this project already has an established
 * {@code displayClientMessage(..., true)} pattern for exactly that everywhere else, whereas building a
 * dedicated title-packet helper for one tech's flavor text wasn't worth it.
 */
public class TechHopeyShit extends TechHeroAspect
{
	private static final String[] STATUS_OPTIONS = {"tallyHo", "gadzooks", "boyHowdy", "holyToledo", "landSakesAlive",
			"helloNurse", "byGum", "ayChihuahua", "bobUncle", "sockItToMe", "shiverMeTimbers", "winOneForTheGipper",
			"jumpinJehosaPhat", "shuckyDarn", "fiddleFaddle"};

	private static final double MAX_RADIUS = 12.0;
	private static final float TITLE_CHANCE = 0.005F;

	public TechHopeyShit()
	{
		super(Minestuckuniverseported.id("hopeful_outburst"), EnumAspect.HOPE, 2000000, MSUTechType.OFFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE)
			return false;

		player.addEffect(new MobEffectInstance(MSUMobEffects.HOPING, 5, 0, false, false));

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 1)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(time % 5 == 0 && level instanceof ServerLevel serverLevel)
		{
			double range = Math.min(time / 30.0 + 2.0, MAX_RADIUS);
			boolean announce = serverLevel.getRandom().nextFloat() < TITLE_CHANCE;
			Component message = announce ? statusMessage(serverLevel) : null;

			if(message != null)
				player.displayClientMessage(message, true);

			for(LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range), e -> e != player))
			{
				if(target.distanceToSqr(player) >= range * range)
					continue;

				if(message != null && target instanceof Player targetPlayer)
					targetPlayer.displayClientMessage(message, true);

				target.invulnerableTime = 0;
				target.hurt(serverLevel.damageSources().magic(), 1.0F);

				Vec3 direction = new Vec3(player.getX() - target.getX(), 0, player.getZ() - target.getZ()).normalize();
				Vec3 current = target.getDeltaMovement();
				double pushY = target.onGround() ? Math.min(current.y / 2.0 + 0.3, 0.4) : current.y;
				target.setDeltaMovement(current.x / 2.0 - direction.x * 0.3, pushY, current.z / 2.0 - direction.z * 0.3);
				target.hurtMarked = true;
			}

			if(!player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);

			MSUAbilitechParticles.burst(level, player, EnumAspect.HOPE, 10);
		}

		return true;
	}

	private static Component statusMessage(ServerLevel level)
	{
		String key = STATUS_OPTIONS[level.getRandom().nextInt(STATUS_OPTIONS.length)];
		return Component.translatable("status.hopey." + key).withStyle(ChatFormatting.GOLD);
	}

	/**
	 * Marker effect - no attribute modifiers or tick behavior of its own, just lets {@link ClientEvents}
	 * know locally that the caster's own client should be dampening/nudging their movement input right
	 * now, the same synced-marker-effect pattern already used for Wind Vessel and Soul Shock.
	 */
	public static class HopingEffect extends MobEffect
	{
		public HopingEffect()
		{
			super(MobEffectCategory.BENEFICIAL, 0xF3296F);
		}
	}

	/**
	 * Client-side half of this tech - while {@link MSUMobEffects#HOPING} is active, movement input is
	 * dampened to 10% and the caster is continuously nudged upward, same as the original including its
	 * per-tick upward push constant (0.5). Reads the same synced-marker-effect pattern
	 * {@code TechBreathWindVessel.ClientEvents}/{@code TechSoulStun.ClientEvents} already use, applied
	 * every tick this tech is held.
	 * <p>
	 * <b>Real bug fix, not a faithfulness call</b>: the original's own {@code motionY += 0.5f} has no cap
	 * at all - a real, confirmed-via-source oversight in the original itself, not a deliberate design
	 * choice: the exact same method's nearby-enemy knockback a few lines below explicitly clamps vertical
	 * velocity to 0.4 (`if (target.motionY > 0.4) target.motionY = 0.4`), so the original's own author
	 * clearly intended vertical speed to be bounded here too, just never applied that same clamp to the
	 * self-effect. Left uncapped, holding the key compounds +0.5 blocks/tick of upward velocity with
	 * literally no ceiling - confirmed via a live playtest report to launch the caster into the
	 * stratosphere within a couple of seconds. This is the "preserve the original's own quirks" policy
	 * meeting its actual limit: a quirk that makes the tech unusable isn't a quirk worth preserving
	 * (unlike e.g. {@code AbilitechnosynthBlock}'s harmless {@code 5/15d} typo).
	 * <p>
	 * <b>Second real bug, same report</b>: a first attempt at this fix set {@link #MAX_UPWARD_VELOCITY}
	 * equal to {@link #UPWARD_PUSH_PER_TICK} (both 0.5) - since {@code min(motion.y + push, cap)} reaches
	 * that cap on the very first held tick from rest and never exceeds it, this technically capped the
	 * <i>acceleration</i> but not the actual sustained ascent rate, which is what the player experiences:
	 * 0.5 blocks/tick (10 blocks/second) held for the tech's whole duration still reaches build height in
	 * well under a minute - still effectively "the stratosphere," just no longer accelerating further. The
	 * cap value itself, not merely the presence of a cap, was the bug. Lowered to a sustained ~3
	 * blocks/second instead - still an immediate, noticeable "giddy float" (still reached in one tick,
	 * since {@link #UPWARD_PUSH_PER_TICK} is left at 0.5 so the pop-off feel is unchanged), just not a
	 * launch.
	 */
	@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
	public static final class ClientEvents
	{
		private static final double UPWARD_PUSH_PER_TICK = 0.5;
		private static final double MAX_UPWARD_VELOCITY = 0.15;

		private ClientEvents()
		{
		}

		@SubscribeEvent
		private static void onMovementInput(MovementInputUpdateEvent event)
		{
			if(!event.getEntity().hasEffect(MSUMobEffects.HOPING))
				return;

			Input input = event.getInput();
			input.forwardImpulse *= 0.1F;
			input.leftImpulse *= 0.1F;

			var motion = event.getEntity().getDeltaMovement();
			double newY = Math.min(motion.y + UPWARD_PUSH_PER_TICK, MAX_UPWARD_VELOCITY);
			event.getEntity().setDeltaMovement(motion.x, newY, motion.z);
		}
	}
}
