package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.doom;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.badgeEffects.BadgeEffects;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;
import org.wilkretawesomesauce.minestuckuniverseported.entity.BubbleEntity;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.doom.TechDoomVoidBubble}
 * ("Abyss Cage"), now using the real {@link BubbleEntity} (see that class's own doc comment for the
 * shared bubble mechanics). Hold and aim at an entity or a block to cage it: {@code canEnter=true,
 * canExit=false, ensnare=true} - unlike Breath's Entombing Twister, anything can freely wander in, but
 * nothing that was ever inside can leave again.
 * <p>
 * Its particle effect uses two literal one-off colors (a Void blue plus a dark gray) rather than its
 * own Doom aspect's table entry - matches the original's own source exactly, kept as-is rather than
 * "corrected" to Doom's colors, same as {@code blood.TechBloodTransfusion}'s own Heart-tinted quirk.
 */
public class TechDoomVoidBubble extends TechHeroAspect
{
	private static final int COLOR = 0x181633;
	private static final int LIFESPAN_TICKS = 25;
	private static final float DEFAULT_SIZE = 4.0F;

	public TechDoomVoidBubble()
	{
		super(Minestuckuniverseported.id("abyss_cage"), EnumAspect.DOOM, 250000, MSUTechType.OFFENSE);
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

		HitResult hit = MSUAbilitechRayTrace.getMouseOver(player, player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE));
		if(hit.getType() == HitResult.Type.MISS)
			return false;

		BadgeEffects badgeEffects = player.getData(MSUAttachments.BADGE_EFFECTS);
		Entity tether = badgeEffects.getTether(techSlot);
		BubbleEntity bubble = tether instanceof BubbleEntity existing && existing.isAlive() ? existing : null;

		if(state == AbilitechKeyState.PRESS)
		{
			if(bubble != null)
				bubble.remove(Entity.RemovalReason.DISCARDED);

			float size = DEFAULT_SIZE;
			double x, y, z;
			if(hit instanceof EntityHitResult entityHit)
			{
				Entity target = entityHit.getEntity();
				size = Math.max(target.getBbWidth(), target.getBbHeight()) + 1.0F;
				x = target.getX();
				y = target.getY() - 0.05;
				z = target.getZ();
			}
			else
			{
				BlockPos pos = ((BlockHitResult) hit).getBlockPos();
				x = pos.getX() + 0.5;
				y = pos.getY() - 0.05;
				z = pos.getZ() + 0.5;
			}

			bubble = BubbleEntity.create(serverLevel, size, COLOR, LIFESPAN_TICKS, true, false, true);
			bubble.setPos(x, y, z);
			serverLevel.addFreshEntity(bubble);
			badgeEffects.setTether(techSlot, bubble);
		}

		if(bubble == null)
			return false;

		bubble.setLifespan(bubble.getLifespan() + 1);

		if(time % 20 == 0 && !player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);

		MSUAbilitechParticles.aura(level, player, 2, 0x001856, 0x1C1C1C);

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
