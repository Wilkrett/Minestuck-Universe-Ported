package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.heir;

import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import com.mraof.minestuck.player.Title;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.MSUSkills;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUClassColors;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.MSUNegativeAspectEffects;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroClass.TechHeir} - a pure-passive
 * retaliation tech, real-registered <b>twice</b> under two different ids/costs/{@link MSUTechType} tags
 * (matching the original's own {@code MSUSkills} table exactly): {@code heir_will} ("Heir's Will",
 * PASSIVE+DEFENSE) retaliates whenever its owner takes damage or dies; {@code universal_reverse}
 * ("Universal Reverse", PASSIVE+OFFENSE) retaliates whenever its owner's own attack lands. Both branches
 * live in this one class's static event handlers (matching the original's own single-class, two-instance
 * shape) - {@link MSUSkills#HEIR_WILL}/{@link MSUSkills#UNIVERSAL_REVERSE} are referenced by name
 * from inside those handlers, resolved at call time (not during static init), so the ordering is safe.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public class TechHeir extends TechHeroClass
{
	public TechHeir(ResourceLocation id, long cost, MSUTechType... techTypes)
	{
		super(id, EnumClass.HEIR, cost, techTypes);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		return false;
	}

	@Override
	public boolean isUsableExternally(Level level, Player player)
	{
		return false;
	}

	private static void doHeirThings(ServerPlayer target, LivingEntity trueSource, float amount)
	{
		GodTierData godTier = target.getData(MSUAttachments.GOD_TIER);
		if(!godTier.isPassiveEnabledFor(MSUSkills.HEIR_WILL))
			return;

		boolean triggers = amount == -1
				|| target.level().getRandom().nextFloat() < Math.max(0.1F, amount / target.getHealth() * 0.8F
						* ((float) target.getAttributeValue(Attributes.LUCK) / 4F - 0.2F));
		if(!triggers)
			return;

		var title = Title.getTitle(target);
		if(title.isEmpty())
			return;

		EnumAspect aspect = title.get().heroAspect();
		if(aspect == EnumAspect.HOPE)
		{
			trueSource.setRemainingFireTicks(100);
			trueSource.setAirSupply(0);
		}

		MobEffectInstance base = MSUNegativeAspectEffects.get(aspect);
		trueSource.addEffect(new MobEffectInstance(base.getEffect(), base.getDuration(), base.getAmplifier()));
		MSUAbilitechParticles.oneshot(trueSource.level(), trueSource, 5, MSUClassColors.get(EnumClass.HEIR));
	}

	@SubscribeEvent
	private static void onLivingDamage(LivingDamageEvent.Post event)
	{
		if(event.getEntity().level().isClientSide() || !(event.getEntity() instanceof ServerPlayer target) || !(event.getSource().getEntity() instanceof LivingEntity trueSource))
			return;

		doHeirThings(target, trueSource, event.getNewDamage());

		if(event.getSource().getDirectEntity() instanceof ServerPlayer source)
		{
			var sourceTitle = Title.getTitle(source);
			var targetTitle = Title.getTitle(target);
			if(sourceTitle.isEmpty() && targetTitle.isEmpty())
				return;

			GodTierData sourceGodTier = source.getData(MSUAttachments.GOD_TIER);
			if(!sourceGodTier.isPassiveEnabledFor(MSUSkills.UNIVERSAL_REVERSE))
				return;

			float luck = (float) source.getAttributeValue(Attributes.LUCK);
			if(source.level().getRandom().nextFloat() >= Math.max(0.1F, (source.getHealth() - source.getMaxHealth()) * 0.65F * (luck / 4F - 0.2F)))
				return;

			EnumAspect aspect = targetTitle.isPresent() ? targetTitle.get().heroAspect() : sourceTitle.get().heroAspect();
			MobEffectInstance base = MSUNegativeAspectEffects.get(aspect);
			target.addEffect(new MobEffectInstance(base.getEffect(), base.getDuration(), base.getAmplifier()));
			MSUAbilitechParticles.oneshot(target.level(), target, 3, MSUClassColors.get(EnumClass.HEIR));
		}
	}

	@SubscribeEvent
	private static void onLivingDeath(LivingDeathEvent event)
	{
		if(event.getEntity().level().isClientSide() || !(event.getEntity() instanceof ServerPlayer target) || !(event.getSource().getEntity() instanceof LivingEntity trueSource))
			return;

		doHeirThings(target, trueSource, -1);
	}
}
