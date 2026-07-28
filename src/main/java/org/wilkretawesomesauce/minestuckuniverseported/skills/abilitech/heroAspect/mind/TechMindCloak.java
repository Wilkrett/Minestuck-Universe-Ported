package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.mind;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.AbilitechLoadout;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;
import org.wilkretawesomesauce.minestuckuniverseported.network.CloakSyncPacket;

/**
 * Ported 1:1 from MinestuckUniverse (1.12.2)'s
 * {@code skills.abilitech.heroAspect.mind.TechMindCloak} ("Illusory Cloak"). Only the branch the
 * original actually shipped is ported - its own source has the player-disguise and block-disguise
 * branches entirely {@code /* TODO *}{@code /}'d out, so "press and aim at a non-player entity" (disguise
 * as its type) and "press and aim at an already-cloaked player" (chain-copy their disguise) are the only
 * two live cases here too, matching what the original actually did, not what it left unfinished.
 * <p>
 * The original's actual centerpiece - a full client-side render swap - is real now:
 * {@link CloakSyncPacket} broadcasts "this player is disguised as entity type Y" to every observer
 * (via {@link PacketDistributor#sendToPlayersTrackingEntityAndSelf}), and
 * {@code client.CloakRenderEvents} cancels the real render and draws a throwaway entity of that type in
 * its place - see that class's own doc comment for the render-offset math (confirmed via
 * {@code EntityRenderDispatcher} bytecode, not guessed) and its one stated simplification (animation/
 * equipment aren't mirrored onto the throwaway entity). {@link #onVisibilityCheck} reproduces the
 * original's own "harder to detect while cloaked" side effect for real via
 * {@link LivingEvent.LivingVisibilityEvent}, the same hook {@code blood.TechBloodReformer} already uses
 * for its own visibility reduction - no vanilla Invisibility stand-in needed anymore.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public class TechMindCloak extends TechHeroAspect
{
	public TechMindCloak()
	{
		super(Minestuckuniverseported.id("illusory_cloak"), EnumAspect.MIND, 1790, MSUTechType.UTILITY);
	}

	@Override
	public void onUnequipped(Level level, Player player, int techSlot)
	{
		super.onUnequipped(level, player, techSlot);
		uncloak(player);
		player.displayClientMessage(Component.translatable("status.tech.illusoryCloak.reset"), true);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		AbilitechLoadout badgeEffects = player.getData(MSUAttachments.ABILITECH_LOADOUT);
		boolean wasCloaked = badgeEffects.getCloakType() != null;

		if(wasCloaked)
		{
			if(!player.isCreative() && player.getFoodData().getFoodLevel() < 1)
			{
				player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
				uncloak(player);
				return false;
			}
			if(!player.isCreative() && time % 60 == 0)
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);
		}

		if(state != AbilitechKeyState.PRESS)
			return false;

		boolean nowCloaked = false;
		HitResult hit = MSUAbilitechRayTrace.getMouseOver(player, player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE));

		if(hit instanceof EntityHitResult entityHit)
		{
			Entity target = entityHit.getEntity();

			if(target instanceof Player targetPlayer)
			{
				EntityType<?> theirCloak = targetPlayer.getData(MSUAttachments.ABILITECH_LOADOUT).getCloakType();
				if(theirCloak != null)
				{
					cloakAs(player, theirCloak);
					player.displayClientMessage(Component.translatable("status.tech.illusoryCloak.disguise", targetPlayer.getDisplayName()), true);
					nowCloaked = true;
				}
			}
			else
			{
				cloakAs(player, target.getType());
				player.displayClientMessage(Component.translatable("status.tech.illusoryCloak.disguise", target.getDisplayName()), true);
				nowCloaked = true;
			}
		}

		if(wasCloaked && !nowCloaked)
		{
			uncloak(player);
			player.displayClientMessage(Component.translatable("status.tech.illusoryCloak.reset"), true);
		}

		return true;
	}

	private static void cloakAs(Player player, EntityType<?> type)
	{
		player.getData(MSUAttachments.ABILITECH_LOADOUT).setCloakType(type);
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
				new CloakSyncPacket(player.getId(), true, BuiltInRegistries.ENTITY_TYPE.getKey(type)));
		MSUAbilitechParticles.aura(player.level(), player, EnumAspect.MIND, 5);
	}

	private static void uncloak(Player player)
	{
		player.getData(MSUAttachments.ABILITECH_LOADOUT).setCloakType(null);
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
				new CloakSyncPacket(player.getId(), false, BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.PIG)));
		MSUAbilitechParticles.aura(player.level(), player, EnumAspect.MIND, 5);
	}

	@SubscribeEvent
	private static void onVisibilityCheck(LivingEvent.LivingVisibilityEvent event)
	{
		if(event.getEntity() instanceof Player player && player.getData(MSUAttachments.ABILITECH_LOADOUT).getCloakType() != null)
			event.modifyVisibility(0.0);
	}
}
