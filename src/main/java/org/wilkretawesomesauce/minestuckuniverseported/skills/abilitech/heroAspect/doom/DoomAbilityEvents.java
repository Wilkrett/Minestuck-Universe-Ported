package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.doom;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Restores a player's flight/building abilities once {@link EarthboundEffect}/{@link BuildInhibitEffect}
 * actually wears off. Neither effect's own {@code applyEffectTick} can do this itself - real
 * {@link net.minecraft.world.effect.MobEffect#removeAttributeModifiers(net.minecraft.world.entity.ai.attributes.AttributeMap)}
 * (confirmed real via this project's pinned NeoForge source, the same hook {@code BerserkEffect} already
 * uses for its own cleanup) only ever receives an {@code AttributeMap}, which has no back-reference to
 * the owning entity - there's no way to reach a {@code Player} from it to flip an ability field back.
 * Tracking "was suppressed last tick, isn't now" here instead sidesteps that gap entirely. Both original
 * potions (see {@code potions.PotionFlight}/{@code PotionBuildInhibit}) never restored these fields at
 * all once applied - a real bug in the original this port doesn't reproduce, since it would otherwise
 * permanently strip a creative player's flight (or anyone's building) after a single, brief debuff.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class DoomAbilityEvents
{
	private static final Set<UUID> earthboundSuppressed = new HashSet<>();
	private static final Set<UUID> buildInhibitSuppressed = new HashSet<>();

	private DoomAbilityEvents()
	{
	}

	@SubscribeEvent
	private static void onPlayerTick(PlayerTickEvent.Post event)
	{
		if(!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide())
			return;

		UUID id = player.getUUID();

		if(player.hasEffect(MSUMobEffects.EARTHBOUND))
			earthboundSuppressed.add(id);
		else if(earthboundSuppressed.remove(id))
		{
			player.getAbilities().mayfly = player.isCreative() || player.isSpectator();
			player.onUpdateAbilities();
		}

		if(player.hasEffect(MSUMobEffects.BUILD_INHIBIT))
			buildInhibitSuppressed.add(id);
		else if(buildInhibitSuppressed.remove(id))
		{
			player.getAbilities().mayBuild = true;
			player.onUpdateAbilities();
		}
	}
}
