package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.mind;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.AbilitechLoadout;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;
import org.wilkretawesomesauce.minestuckuniverseported.network.CloakSyncPacket;

import java.util.HashMap;
import java.util.Map;

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

	/**
	 * Client-only registry of "which real entity id is currently disguised as which {@link EntityType}",
	 * kept in sync by {@link CloakSyncPacket}. Read by {@link ClientEvents} every frame it renders a
	 * player.
	 */
	public static final class CloakClientState
	{
		private static final Map<Integer, EntityType<?>> cloaked = new HashMap<>();

		private CloakClientState()
		{
		}

		public static void setCloaked(int entityId, ResourceLocation entityType)
		{
			cloaked.put(entityId, BuiltInRegistries.ENTITY_TYPE.get(entityType));
		}

		public static void clearCloaked(int entityId)
		{
			cloaked.remove(entityId);
		}

		public static EntityType<?> getCloakType(int entityId)
		{
			return cloaked.get(entityId);
		}
	}

	/**
	 * Real client-side render substitution for this tech - cancels the real render of a cloaked player
	 * and draws a throwaway entity of the disguise type in its place instead, the same "spawn a real
	 * {@code Entity} instance purely to feed it to a renderer, never add it to the level" idiom
	 * {@code timeline.vision.GhostEntity} already validates for past-vision ghosts, applied here to
	 * {@link net.minecraft.client.renderer.entity.EntityRenderDispatcher#render} directly rather than a
	 * raw entity-add packet (there's no server-side entity to spawn a packet for - this is purely a local
	 * render swap, driven by {@link CloakClientState}).
	 * <p>
	 * Confirmed via the real {@code EntityRenderDispatcher#render} bytecode (not guessed) that by the time
	 * {@link RenderPlayerEvent.Pre} fires, the event's own {@code PoseStack} is already translated to the
	 * real player's render position - so the throwaway entity is drawn at a zero offset from it, no extra
	 * camera-relative math needed.
	 * <p>
	 * <b>Simplified, not the mechanic:</b> only position/rotation are copied onto the throwaway entity -
	 * walk-cycle animation state and worn equipment aren't mirrored, same category of gap as this project's
	 * already-accepted Retrocognition ghost fidelity (position/rotation/equipment only, no fire/invisible/
	 * glowing/pose).
	 * <p>
	 * Real now too: a local observer carrying {@link MSUMobEffects#MIND_FORTITUDE} sees straight through
	 * any disguise (the ghost substitution is skipped entirely) - a direct port of the original's own
	 * client-side {@code Minecraft.getMinecraft().player.isPotionActive(MIND_FORTITUDE)} check, which ran
	 * here (against the local/observing player) rather than in the server-side ability logic.
	 */
	@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
	public static final class ClientEvents
	{
		private static final Map<Integer, Entity> ghostCache = new HashMap<>();

		private ClientEvents()
		{
		}

		@SubscribeEvent
		private static void onRenderPlayer(RenderPlayerEvent.Pre event)
		{
			Player real = event.getEntity();
			EntityType<?> disguiseType = CloakClientState.getCloakType(real.getId());

			if(disguiseType == null)
			{
				ghostCache.remove(real.getId());
				return;
			}

			Minecraft mcInstance = Minecraft.getInstance();
			if(mcInstance.player != null && mcInstance.player.hasEffect(MSUMobEffects.MIND_FORTITUDE))
			{
				ghostCache.remove(real.getId());
				return;
			}

			Entity ghost = ghostCache.get(real.getId());
			if(ghost == null || ghost.getType() != disguiseType)
			{
				ghost = disguiseType.create(real.level());
				if(ghost == null)
					return;
				ghostCache.put(real.getId(), ghost);
			}

			ghost.setPos(real.getX(), real.getY(), real.getZ());
			ghost.setYRot(real.getYRot());
			ghost.setXRot(real.getXRot());
			ghost.setYHeadRot(real.getYHeadRot());

			if(ghost instanceof LivingEntity livingGhost)
				livingGhost.yBodyRot = real.getYRot();

			mcInstance.getEntityRenderDispatcher().render(ghost, 0, 0, 0, real.getYRot(), event.getPartialTick(),
					event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());

			event.setCanceled(true);
		}
	}
}
