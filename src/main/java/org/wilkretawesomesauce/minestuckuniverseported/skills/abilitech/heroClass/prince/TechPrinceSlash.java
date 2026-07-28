package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.prince;

import com.mraof.minestuck.player.EnumClass;
import com.mraof.minestuck.player.Title;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;
import org.wilkretawesomesauce.minestuckuniverseported.skills.MSUSkills;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroClass.TechPrinceSlash} ("Ruling
 * Slash") - pure passive: whenever a player with this tech's passive toggled on attacks another player of
 * the exact same Title aspect, the hit deals triple damage. Ported onto {@link LivingIncomingDamageEvent}
 * (a pre-final-damage hook, matching the original's own {@code LivingHurtEvent}) rather than a post-damage
 * event, since the original scales the damage amount itself, not just reacts to it afterward.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public class TechPrinceSlash extends TechHeroClass
{
	public TechPrinceSlash()
	{
		super(Minestuckuniverseported.id("ruling_slash"), EnumClass.PRINCE, 64660, MSUTechType.PASSIVE, MSUTechType.OFFENSE);
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

	@SubscribeEvent
	private static void onIncomingDamage(LivingIncomingDamageEvent event)
	{
		if(!(event.getEntity() instanceof ServerPlayer target) || !(event.getSource().getDirectEntity() instanceof ServerPlayer source))
			return;

		var targetTitle = Title.getTitle(target);
		var sourceTitle = Title.getTitle(source);
		if(targetTitle.isEmpty() || sourceTitle.isEmpty() || targetTitle.get().heroAspect() != sourceTitle.get().heroAspect())
			return;

		GodTierData godTier = source.getData(MSUAttachments.GOD_TIER);
		if(!godTier.isPassiveEnabledFor(MSUSkills.RULING_SLASH))
			return;

		event.setAmount(event.getAmount() * 3);
	}
}
