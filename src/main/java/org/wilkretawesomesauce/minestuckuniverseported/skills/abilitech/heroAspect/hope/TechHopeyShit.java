package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.hope;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
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
}
