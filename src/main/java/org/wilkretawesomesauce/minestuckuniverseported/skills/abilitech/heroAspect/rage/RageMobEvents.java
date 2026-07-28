package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.rage;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAspectColors;

/**
 * Re-injects {@link FrenzyTargetGoal}/the hostile-targeting goals whenever a previously frenzied or
 * rage-shifted creature (re)loads - goals themselves aren't part of vanilla's own NBT persistence, only
 * the marker effect is, so this is the modern equivalent of the original's two separate
 * {@code onJoinWorld} handlers in {@code TechRageFrenzy}/{@code TechRageManagement}.
 * <p>
 * {@link #onEntityTick} also ports the original's {@code TechRageManagement#onLivingTick} ambient
 * particle handler 1:1 - any frenzied or rage-shifted creature has a flat 5% chance per tick to emit a
 * single particle in one of {@link EnumAspect#RAGE}'s two colors, picked at random each time (matching
 * the original's own {@code rand.nextInt(colors.length)} pick, not always the same color).
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class RageMobEvents
{
	private RageMobEvents()
	{
	}

	@SubscribeEvent
	private static void onEntityJoinLevel(EntityJoinLevelEvent event)
	{
		if(event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob))
			return;

		if(mob.hasEffect(MSUMobEffects.FRENZIED))
			RageAI.enableFrenzy(mob);
		else if(mob.hasEffect(MSUMobEffects.RAGE_SHIFTED))
			RageAI.enableRageShift(mob);
	}

	@SubscribeEvent
	private static void onEntityTick(EntityTickEvent.Post event)
	{
		if(event.getEntity().level().isClientSide() || !(event.getEntity() instanceof Mob mob))
			return;

		if((mob.hasEffect(MSUMobEffects.FRENZIED) || mob.hasEffect(MSUMobEffects.RAGE_SHIFTED)) && mob.getRandom().nextFloat() < 0.05f)
		{
			int[] colors = MSUAspectColors.get(EnumAspect.RAGE);
			MSUAbilitechParticles.oneshot(mob.level(), mob, 1, colors[mob.getRandom().nextInt(colors.length)]);
		}
	}
}
