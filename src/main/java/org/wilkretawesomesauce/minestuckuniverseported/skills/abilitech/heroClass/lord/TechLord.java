package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.lord;

import com.mraof.minestuck.player.EnumClass;
import com.mraof.minestuck.player.Title;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.skills.MSUSkills;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.ClasspectColorHandler;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.events.AbilitechTargetedEvent;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.MSUNegativeAspectEffects;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroClass.TechLord} ("Lord's Decree") -
 * an 18-tick charge-up (halved to 9 for a real {@link MSUSkills#BADGE_OVERLORD}-holding "World Ender")
 * unleashing a doubled-strength {@link MSUNegativeAspectEffects} debuff across a real 48-block radius, plus
 * (real now, World-Ender-only) a unique per-aspect instant-effect switch. Faithfully excludes nearby
 * (&lt;6 blocks) real players from the blast, and further filters distant players by Karma alignment
 * (skipped if same-signed, nonzero Karma as the caster) - non-player targets only ever get hit if they're
 * a real {@link Monster}.
 */
public class TechLord extends TechHeroClass
{
	private static final double RADIUS = 48;
	private static final double PLAYER_EXCLUSION_RADIUS = 6;

	public TechLord()
	{
		super(Minestuckuniverseported.id("lord_decree"), EnumClass.LORD, 1850000, MSUTechType.OFFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(!(player instanceof ServerPlayer serverPlayer))
			return false;

		var title = Title.getTitle(serverPlayer);

		boolean isOverlord = player.getData(MSUAttachments.GOD_TIER).isBadgeActive(MSUSkills.BADGE_OVERLORD, level, player);
		int chargeTime = isOverlord ? 9 : 18;
		int energy = isOverlord ? 6 : 12;

		if(title.isEmpty() || state == AbilitechKeyState.NONE || time > chargeTime)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < energy)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(time > chargeTime - 3)
			MSUAbilitechParticles.burst(level, player, 20, ClasspectColorHandler.get(EnumClass.LORD));
		else
			MSUAbilitechParticles.aura(level, player, 20, ClasspectColorHandler.get(EnumClass.LORD));

		if(time >= chargeTime)
		{
			GodTierData casterGodTier = player.getData(MSUAttachments.GOD_TIER);
			int casterKarma = casterGodTier.getStaticKarma() + casterGodTier.getTempKarma();

			for(LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RADIUS), e -> e != player))
			{
				if(target instanceof ServerPlayer targetPlayer && target.distanceTo(player) >= PLAYER_EXCLUSION_RADIUS)
				{
					GodTierData targetGodTier = targetPlayer.getData(MSUAttachments.GOD_TIER);
					int targetKarma = targetGodTier.getStaticKarma() + targetGodTier.getTempKarma();
					if(targetKarma != 0 && Math.signum(targetKarma) == Math.signum(casterKarma))
						continue;
				}
				else if(!(target instanceof Monster))
					continue;

				if(NeoForge.EVENT_BUS.post(new AbilitechTargetedEvent(player, target, this, techSlot, false)).isCanceled())
					continue;

				if(isOverlord)
				{
					switch(title.get().heroAspect())
					{
						case SPACE -> target.addEffect(new MobEffectInstance(MobEffects.JUMP, 800, 200));
						case LIGHT -> target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 1200, 2));
						case MIND -> target.addEffect(new MobEffectInstance(MSUMobEffects.MIND_CONFUSION, 300, 0));
						case TIME -> target.addEffect(new MobEffectInstance(MSUMobEffects.TIME_STOP, 600, 0));
						case DOOM -> target.addEffect(new MobEffectInstance(MSUMobEffects.DECAY, 115, 2));
						case RAGE -> target.addEffect(new MobEffectInstance(MSUMobEffects.RAGE_BERSERK, 300, 10));
						case VOID -> target.hurt(target.level().damageSources().fellOutOfWorld(), 18);
						case HOPE -> target.hurt(target.level().damageSources().onFire(), 30);
						case BREATH -> target.hurt(target.level().damageSources().drown(), 18);
						case LIFE, HEART, BLOOD -> target.hurt(target.level().damageSources().magic(), 40);
					}
				}

				MobEffectInstance base = MSUNegativeAspectEffects.get(title.get().heroAspect());
				target.addEffect(new MobEffectInstance(base.getEffect(), base.getDuration() * 2 * (isOverlord ? 2 : 1),
						(int) ((base.getAmplifier() + 1) * 1.5F * (isOverlord ? 2 : 1)) - 1));
				MSUAbilitechParticles.oneshot(level, target, 10, ClasspectColorHandler.get(EnumClass.LORD));
			}
			if(!player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - energy);
		}

		return true;
	}

	@Override
	public boolean isUsableExternally(Level level, Player player)
	{
		boolean isOverlord = player.getData(MSUAttachments.GOD_TIER).isBadgeActive(MSUSkills.BADGE_OVERLORD, level, player);
		return player.getFoodData().getFoodLevel() >= (isOverlord ? 6 : 12) && super.isUsableExternally(level, player);
	}
}
