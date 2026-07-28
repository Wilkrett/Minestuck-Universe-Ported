package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.thief;

import com.mraof.minestuck.player.EnumClass;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUClassColors;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroClass.TechThiefDash} ("Hermes'
 * Dash") - a tap forward dash. The original drove this through a client-side
 * {@code MSUPacket.Type.PERFORM_DASH} packet (a pure client movement nudge); this instead applies a real
 * server-authoritative forward velocity impulse directly, the same yaw-relative dash idiom this project's
 * other charge/dash techs already use (e.g. {@code time.TechTimeAccelerateSelf}'s own burst).
 */
public class TechThiefDash extends TechHeroClass
{
	private static final double DASH_STRENGTH = 1.1;

	public TechThiefDash()
	{
		super(Minestuckuniverseported.id("hermes_dash"), EnumClass.THIEF, 350, MSUTechType.UTILITY);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.PRESS)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 4)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		double yaw = Math.toRadians(-player.getYRot());
		player.setDeltaMovement(player.getDeltaMovement().add(Math.sin(yaw) * DASH_STRENGTH, 0.0, Math.cos(yaw) * DASH_STRENGTH));
		player.hurtMarked = true;

		MSUAbilitechParticles.aura(level, player, 20, MSUClassColors.get(EnumClass.THIEF));

		if(!player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 4);

		return true;
	}

	@Override
	public boolean isUsableExternally(Level level, Player player)
	{
		return player.getFoodData().getFoodLevel() >= 4 && super.isUsableExternally(level, player);
	}
}
