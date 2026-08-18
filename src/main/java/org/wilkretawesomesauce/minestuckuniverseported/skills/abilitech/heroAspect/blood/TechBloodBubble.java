package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.blood;

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
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.blood.TechBloodBubble}
 * ("Haemodome"), now using the real {@link BubbleEntity} (see that class's own doc comment for the
 * shared bubble mechanics and what's adapted vs. the original Forge-1.12.2-only collision hooks).
 * <p>
 * Spawned {@code canEnter=false, canExit=true, ensnare=false}: a one-way ward that repels anyone/
 * anything trying to cross in, but never traps whoever's already inside. Placed at the caster's own
 * position at cast time and never moves again - matches the original exactly; this is a stationary
 * blood ward, not a bubble that follows the caster around.
 */
public class TechBloodBubble extends TechHeroAspect
{
	private static final float SIZE = 3.0F;
	private static final int COLOR = 0xB71015;
	private static final int LIFESPAN_TICKS = 25;

	public TechBloodBubble()
	{
		super(Minestuckuniverseported.id("haemodome"), EnumAspect.BLOOD, 9300, MSUTechType.DEFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 1)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(!(level instanceof ServerLevel serverLevel))
			return false;

		BadgeEffects badgeEffects = player.getData(MSUAttachments.BADGE_EFFECTS);
		Entity tether = badgeEffects.getTether(techSlot);
		BubbleEntity bubble = tether instanceof BubbleEntity existing && existing.isAlive() ? existing : null;

		if(state == AbilitechKeyState.PRESS)
		{
			if(bubble != null)
				bubble.remove(Entity.RemovalReason.DISCARDED);

			bubble = BubbleEntity.create(serverLevel, SIZE, COLOR, LIFESPAN_TICKS, false, true, false);
			bubble.setPos(player.getX(), player.getY() - 0.05, player.getZ());
			serverLevel.addFreshEntity(bubble);
			badgeEffects.setTether(techSlot, bubble);
		}

		if(bubble == null)
			return false;

		bubble.setLifespan(bubble.getLifespan() + 1);

		if(time % 20 == 0 && !player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);

		MSUAbilitechParticles.aura(level, player, EnumAspect.BLOOD, 2);

		return true;
	}

	@Override
	public void onUnequipped(Level level, Player player, int techSlot)
	{
		BadgeEffects badgeEffects = player.getData(MSUAttachments.BADGE_EFFECTS);
		if(badgeEffects.getTether(techSlot) instanceof BubbleEntity bubble && bubble.isAlive())
			bubble.remove(Entity.RemovalReason.DISCARDED);
		badgeEffects.setTether(techSlot, null);
	}
}
