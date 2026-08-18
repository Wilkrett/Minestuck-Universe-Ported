package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.light;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.badgeEffects.BadgeEffects;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;
import org.wilkretawesomesauce.minestuckuniverseported.entity.BubbleEntity;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.light.TechLightBubble}
 * ("Hardlight Bubble") - hold to raise a real shared {@link BubbleEntity} around yourself
 * (impassable both ways - {@code canEnter=false, canExit=false}, matching the original exactly),
 * growing its lifespan by 1 tick every tick held so it never actually times out while you hold the key,
 * and despawning it the instant you let go (the original re-created a fresh bubble on every press
 * rather than reusing one, so a stale one is always cleared out first). The original's "Cibernet" easter
 * -egg color branch for this tech's own particles is deliberately not reproduced, same judgment call as
 * {@code TechLightStriker}.
 */
public class TechLightBubble extends TechHeroAspect
{
	private static final float SIZE = 3.0F;
	private static final int COLOR = 0xF4ECB7;
	private static final int LIFESPAN_TICKS = 100;

	public TechLightBubble()
	{
		super(Minestuckuniverseported.id("hardlight_bubble"), EnumAspect.LIGHT, 9850, MSUTechType.DEFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE)
			return false;
		if(!(level instanceof ServerLevel serverLevel))
			return false;

		BadgeEffects badgeEffects = player.getData(MSUAttachments.BADGE_EFFECTS);

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 1)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			despawn(badgeEffects, techSlot);
			return false;
		}

		Entity tether = badgeEffects.getTether(techSlot);
		BubbleEntity bubble = tether instanceof BubbleEntity b && b.isAlive() ? b : null;

		if(state == AbilitechKeyState.PRESS)
		{
			despawn(badgeEffects, techSlot);
			bubble = BubbleEntity.create(serverLevel, SIZE, COLOR, LIFESPAN_TICKS, false, false, false);
			bubble.setPos(player.getX(), player.getY() - 0.05, player.getZ());
			serverLevel.addFreshEntity(bubble);
			badgeEffects.setTether(techSlot, bubble);
		}

		if(bubble == null)
			return false;

		bubble.setLifespan(bubble.getLifespan() + 1);

		if(!player.isCreative() && time % 20 == 0)
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);

		MSUAbilitechParticles.aura(level, player, EnumAspect.LIGHT, 2);

		return true;
	}

	@Override
	public void onUnequipped(Level level, Player player, int techSlot)
	{
		super.onUnequipped(level, player, techSlot);
		despawn(player.getData(MSUAttachments.BADGE_EFFECTS), techSlot);
	}

	private static void despawn(BadgeEffects badgeEffects, int techSlot)
	{
		if(badgeEffects.getTether(techSlot) instanceof BubbleEntity bubble)
			bubble.discard();
		badgeEffects.setTether(techSlot, null);
	}
}
