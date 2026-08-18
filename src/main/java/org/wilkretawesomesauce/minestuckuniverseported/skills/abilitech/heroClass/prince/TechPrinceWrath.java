package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.prince;

import com.mraof.minestuck.player.EnumClass;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.ClasspectColorHandler;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.events.AbilitechTargetedEvent;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroClass.TechPrinceWrath} ("Prince's
 * Wrath") - hold to charge (ambient particles at an accelerating rate), release to strike the current
 * raytraced target for {@code 10 * clamp(chargeTicks/20, 1, 3)} damage.
 * <p>
 * The original's real crit flag ({@code EntityCritDamageSource#setCrit()}) exists purely to trigger
 * vanilla's own client-visible "critical hit" feedback (star particles + the crit sound) - modern
 * {@link net.minecraft.world.damagesource.DamageSource} carries no matching per-instance crit flag at all
 * (real crits are detected internally by {@code Player#attack()}'s own fall-speed/sprint check, not read
 * back off the source), so this reproduces that same real visible/audible feedback directly instead:
 * real {@link ParticleTypes#CRIT} + {@link SoundEvents#PLAYER_ATTACK_CRIT} at the moment of impact, the
 * same real cue a genuine critical hit produces.
 */
public class TechPrinceWrath extends TechHeroClass
{
	public TechPrinceWrath()
	{
		super(Minestuckuniverseported.id("prince_wrath"), EnumClass.PRINCE, 57300, MSUTechType.OFFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 9)
		{
			if(state == AbilitechKeyState.HELD)
				player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(state == AbilitechKeyState.RELEASED)
		{
			LivingEntity target = MSUAbilitechRayTrace.getTargetEntity(player);
			float dmg = 10 * Math.min(3.0F, Math.max(1.0F, time / 20F));
			if(target != null && !NeoForge.EVENT_BUS.post(new AbilitechTargetedEvent(player, target, this, techSlot, false)).isCanceled())
			{
				MSUAbilitechParticles.oneshot(level, target, 20, ClasspectColorHandler.get(EnumClass.PRINCE));
				target.hurt(player.damageSources().playerAttack(player), dmg);
				if(level instanceof ServerLevel serverLevel)
					serverLevel.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 15, 0.3, 0.3, 0.3, 0.2);
				level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, target.getSoundSource(), 1.0F, 1.0F);
				if(!player.isCreative())
					player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 9);
			}
		}
		else if((int) (time % (120F / Math.max(time, 1F))) == 0)
			MSUAbilitechParticles.aura(level, player, 2, ClasspectColorHandler.get(EnumClass.PRINCE));

		return true;
	}

	@Override
	public boolean isUsableExternally(Level level, Player player)
	{
		return player.getFoodData().getFoodLevel() >= 9 && super.isUsableExternally(level, player);
	}
}
