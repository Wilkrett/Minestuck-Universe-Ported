package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.AbilitechLoadout;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported (simplified) from MinestuckUniverse (1.12.2)'s {@code TechTimeRecall}. Constantly records a
 * rolling 5-second history of the player's position/rotation/health; hold the key for exactly 20 ticks to
 * snap back to where you were at the start of that window. Costs 5 food.
 * <p>
 * Uses a small local snapshot record stored in {@link AbilitechLoadout}'s per-slot scratch history
 * instead of the original's {@code SoulData} class, which also captured motion, potion effects, held
 * hotbar slot, and a "decay time" value tied to {@code IBadgeEffects} - all dropped here as not central
 * to the "rewind to where you stood" effect. Potion effects specifically are worth revisiting later since
 * restoring them was arguably part of the point (undoing recent status effects along with position).
 */
public class TechTimeRecall extends TechHeroAspect
{
	private static final int ENERGY_USE = 5;
	private static final int RECALL_TICKS = 5 * 20;

	public TechTimeRecall()
	{
		super(Minestuckuniverseported.id("temporal_recall"), EnumAspect.TIME, 68460, MSUTechType.DEFENSE);
	}

	private record Snapshot(Vec3 pos, float yaw, float pitch, float health)
	{
		Snapshot(Player player)
		{
			this(player.position(), player.getYRot(), player.getXRot(), player.getHealth());
		}

		void apply(Player player)
		{
			player.setYRot(yaw);
			player.setXRot(pitch);
			player.setYHeadRot(yaw);
			player.teleportTo(pos.x, pos.y, pos.z);
			player.setDeltaMovement(Vec3.ZERO);
			player.setHealth(health);
		}
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		AbilitechLoadout loadout = player.getData(MSUAttachments.ABILITECH_LOADOUT);
		List<Object> history = new ArrayList<>(loadout.getSlotHistory(techSlot));
		history.add(new Snapshot(player));
		while(history.size() > RECALL_TICKS)
			history.remove(0);
		loadout.getSlotHistory(techSlot).clear();
		loadout.getSlotHistory(techSlot).addAll(history);

		if(state != AbilitechKeyState.HELD || time > 20)
			return false;

		if(time < 20)
		{
			MSUAbilitechParticles.aura(level, player, EnumAspect.TIME, 2);
			return true;
		}

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < ENERGY_USE)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		if(!history.isEmpty())
		{
			((Snapshot) history.get(0)).apply(player);
			loadout.getSlotHistory(techSlot).clear();
			MSUAbilitechParticles.oneshot(level, player, EnumAspect.TIME, 4);
		}

		if(!player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - ENERGY_USE);

		MSUAbilitechParticles.aura(level, player, EnumAspect.TIME, 4);

		return true;
	}
}
