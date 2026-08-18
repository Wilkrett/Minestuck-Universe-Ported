package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.blood;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import org.wilkretawesomesauce.minestuckuniverseported.entity.ai.EntityAIFollowReformer;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.MSUSkills;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public class TechBloodReformer extends TechHeroAspect
{
	private static final double REACH_RADIUS = 32.0;

	public TechBloodReformer()
	{
		super(Minestuckuniverseported.id("reformers_reach"), EnumAspect.BLOOD, 510, MSUTechType.PASSIVE);
	}

	@Override
	public boolean onPassiveTick(Level level, Player player, int techSlot)
	{
		if(!(level instanceof ServerLevel serverLevel))
			return false;

		for(Mob mob : serverLevel.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(REACH_RADIUS), m -> m.getTarget() == player))
			mob.setTarget(null);

		MSUAbilitechParticles.aura(level, player, EnumAspect.BLOOD, 6);

		return true;
	}

	@Override
	public void onPassiveToggle(Level level, Player player, boolean active)
	{
		super.onPassiveToggle(level, player, active);
		sendToggleMessage(player, active);
	}

	public static boolean hasReformerActive(Player player)
	{
		return player.getData(MSUAttachments.GOD_TIER).isPassiveEnabledFor(MSUSkills.BLOOD_REFORMER);
	}

	@SubscribeEvent
	private static void onVisibilityCheck(LivingEvent.LivingVisibilityEvent event)
	{
		if(event.getEntity() instanceof Player player && hasReformerActive(player))
			event.modifyVisibility(0.0);
	}

	@SubscribeEvent
	private static void onChangeTarget(LivingChangeTargetEvent event)
	{
		if(event.getNewAboutToBeSetTarget() instanceof Player player && hasReformerActive(player))
			event.setCanceled(true);
	}

	@SubscribeEvent
	private static void onEntityJoinLevel(EntityJoinLevelEvent event)
	{
		if(event.getEntity() instanceof Animal animal && animal.getNavigation() instanceof GroundPathNavigation)
			animal.goalSelector.addGoal(3, new EntityAIFollowReformer(animal, 1.1));
	}
}
