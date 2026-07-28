package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.mind;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

import java.util.HashMap;
import java.util.Map;

/**
 * Real client-side render substitution for {@code abilitech.heroAspect.mind.TechMindCloak} ("Illusory
 * Cloak") - cancels the real render of a cloaked player and draws a throwaway entity of the disguise
 * type in its place instead, the same "spawn a real {@code Entity} instance purely to feed it to a
 * renderer, never add it to the level" idiom {@code timeline.vision.GhostEntity} already validates for
 * past-vision ghosts, applied here to {@link net.minecraft.client.renderer.entity.EntityRenderDispatcher#render}
 * directly rather than a raw entity-add packet (there's no server-side entity to spawn a packet for -
 * this is purely a local render swap, driven by {@link CloakClientState}).
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
public final class CloakRenderEvents
{
	private static final Map<Integer, Entity> ghostCache = new HashMap<>();

	private CloakRenderEvents()
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
