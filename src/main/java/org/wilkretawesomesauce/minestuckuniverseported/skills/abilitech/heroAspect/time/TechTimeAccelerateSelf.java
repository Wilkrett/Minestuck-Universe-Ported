package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;
import org.wilkretawesomesauce.minestuckuniverseported.network.StreakStateSyncPacket;
import org.wilkretawesomesauce.minestuckuniverseported.client.streak.StreakFlavours;
import org.wilkretawesomesauce.minestuckuniverseported.client.streak.StreakPreference;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * New "basic command" tech from the Time Aspect design discussion - "Accelerate": a charge-and-release
 * ability. Holding charges it (costing 1 food per 20 ticks, same as before this rework) for up to
 * {@link #MAX_CHARGE_TICKS} (10 seconds) - releasing unleashes a burst of vanilla Speed + Haste (and a
 * literal forward dash) scaled by how long it was charged, per the user's explicit request that this
 * become "a hold ability, the longer you hold = the faster burst of speed you get, capping out at 10
 * seconds." Holding past the cap doesn't make the eventual burst any stronger. Uses vanilla Speed + Haste
 * as the mechanical stand-in for the design doc's broader "speed up crafting/cooldowns/movement/healing" -
 * a genuine cooldown-reduction system isn't something this project has infrastructure for yet. All numbers
 * below (amplifier/duration/dash-strength curves, the 10-second cap's exact scaling shape) are this
 * rework's own judgment call, not derived from any design doc - this is an invented tech with no original
 * to match.
 * <p>
 * <b>Real gameplay reuse of the {@code streak} debug/demo system</b> (see that package's own doc
 * comments): while charging (held), the caster gets the ghost-afterimage half of the Streak effect on
 * themselves - no ribbon trail, red-tinted, and shown regardless of whether they're actually sprinting
 * (unlike the plain {@code /msu streak} debug toggle, which only shows ghosts while genuinely sprinting).
 * Driven directly through {@link StreakPreference} + {@link StreakStateSyncPacket} the same way the debug
 * command itself does, rather than adding a second, parallel "enable streak" mechanism. The afterimages
 * stay up continuously across both the charge <i>and</i> the resulting burst - they only cut off once the
 * released burst's own {@link #activeBurst} entry has actually run out ({@link BurstState#endTick}, a
 * per-player {@code WeakHashMap} tracking each active burst's expiry tick against {@link Level#getGameTime()},
 * the same lightweight self-contained-scratch-state idiom {@code doom.DecayEffect}'s own per-entity
 * hit-count map already uses - {@code onUseTick} is called every server tick for every equipped tech
 * regardless of key state, per {@code AbilitechEvents#onPlayerTick}, so a {@code state == NONE} tick is a
 * real, reliable place to check it).
 * <p>
 * <b>The burst is a one-time {@code setDeltaMovement} kick at release <i>plus</i> a fast-decaying
 * per-tick follow-through thrust</b>, not a flat constant push - the history here matters for anyone
 * tuning this again: a constant per-tick thrust was tried first (to fix "underwhelming compared to the
 * initial speed burst"), reverted for compounding against drag into a runaway terminal velocity ("WAYY
 * too fast"), then brought back with {@link #SUSTAIN_DECAY_FACTOR} multiplying the remaining thrust down
 * every tick instead of holding it constant - {@link BurstState#thrust} starts at
 * {@code MIN_SUSTAIN_STRENGTH}-{@code MAX_SUSTAIN_STRENGTH} (scaled by charge, same as everything else
 * here) and shrinks below {@link #SUSTAIN_MIN_THRESHOLD} within well under a second, so it reinforces the
 * initial kick for a brief window instead of decaying to nothing immediately via drag, without ever
 * reaching the old version's unbounded equilibrium speed.
 * <p>
 * <b>Known, accepted edge case</b>: {@code StreakPreference} is one shared per-player state, not
 * per-source - if a player also has {@code /msu streak} manually toggled on at the same time, charging
 * Accelerate will turn the whole effect off (clobbering the manual toggle) rather than restoring it.
 * Not worth a priority/stacking system for what's normally a creative-only debug command overlapping
 * with a real gameplay tech.
 * <p>
 * <b>Screen vignette also scales with hold duration</b>, per direct user request - a red-tinted
 * top/bottom band (the same visual shape {@code client.TimeDilationVignette} already established for
 * this project) that intensifies linearly from nothing at a bare tap up to full strength at the same
 * {@link #MAX_CHARGE_TICKS} cap the burst itself uses, via {@link AcceleratingEffect} +
 * {@code client.AcceleratingVignette} - see that effect's own doc comment for why the charge percentage
 * rides the amplifier slot instead of the duration one.
 */
public class TechTimeAccelerateSelf extends TechHeroAspect
{
	private static final int RED_TINT = 0xFF4040;
	private static final int MAX_CHARGE_TICKS = 200; // 10 seconds - charging past this doesn't strengthen the burst any further
	private static final int MIN_BURST_AMPLIFIER = 0; // Speed I / Haste I on a bare tap
	private static final int MAX_BURST_AMPLIFIER = 4; // Speed V / Haste V at a full 10-second charge
	private static final int MIN_BURST_DURATION_TICKS = 20;
	private static final int MAX_BURST_DURATION_TICKS = 180;
	private static final double MIN_DASH_STRENGTH = 0.4;
	private static final double MAX_DASH_STRENGTH = 4.2;
	private static final double MIN_SUSTAIN_STRENGTH = 0.15; // starting per-tick follow-through thrust on a bare tap, before decay
	private static final double MAX_SUSTAIN_STRENGTH = 0.9; // starting per-tick follow-through thrust at a full 10-second charge, before decay
	private static final double SUSTAIN_DECAY_FACTOR = 0.35; // remaining thrust is multiplied by this every tick - fast falloff, not a constant push
	private static final double SUSTAIN_MIN_THRESHOLD = 0.01; // below this the follow-through thrust is treated as spent

	private record BurstState(long endTick, double thrust)
	{
	}

	/** Per-player state of the currently-running burst - its expiry tick (against {@link Level#getGameTime()},
	 * for the afterimages) and its remaining, fast-decaying follow-through thrust. */
	private static final Map<Player, BurstState> activeBurst = new WeakHashMap<>();

	public TechTimeAccelerateSelf()
	{
		super(Minestuckuniverseported.id("accelerate"), EnumAspect.TIME, 1800, MSUTechType.UTILITY); // new tech, no original cost to port - picked to fit this project's own cost spread, see class doc comment
		setIcon("default");
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE)
		{
			BurstState burst = getActiveBurst(player, level);
			if(burst != null)
			{
				if(burst.thrust() >= SUSTAIN_MIN_THRESHOLD)
				{
					applySustainThrust(player, burst.thrust());
					activeBurst.put(player, new BurstState(burst.endTick(), burst.thrust() * SUSTAIN_DECAY_FACTOR));
				}
				enableAfterimages(player); // burst is still running its course - keep the visible feedback up
			}
			else
			{
				disableAfterimages(player);
			}
			return false;
		}

		if(state != AbilitechKeyState.RELEASED)
		{
			// still charging - hold-time feedback is the afterimages + the vignette, the actual payoff comes on release
			if(!player.isCreative() && player.getFoodData().getFoodLevel() < 1)
			{
				disableAfterimages(player);
				player.removeEffect(MSUMobEffects.ACCELERATING);
				player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
				return false;
			}

			if(time % 20 == 0 && !player.isCreative())
				player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);

			enableAfterimages(player);
			int chargePercent = (int) Math.round(chargeRatio(time) * 100);
			player.addEffect(new MobEffectInstance(MSUMobEffects.ACCELERATING, 5, chargePercent, true, false));
			return true;
		}

		// released - unleash the burst, scaled by how long it was charged (capped at MAX_CHARGE_TICKS)
		player.removeEffect(MSUMobEffects.ACCELERATING);
		double chargeRatio = chargeRatio(time);
		int amplifier = MIN_BURST_AMPLIFIER + (int) Math.round(chargeRatio * (MAX_BURST_AMPLIFIER - MIN_BURST_AMPLIFIER));
		int duration = MIN_BURST_DURATION_TICKS + (int) Math.round(chargeRatio * (MAX_BURST_DURATION_TICKS - MIN_BURST_DURATION_TICKS));
		double dashStrength = MIN_DASH_STRENGTH + chargeRatio * (MAX_DASH_STRENGTH - MIN_DASH_STRENGTH);

		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, amplifier, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, duration, amplifier, false, false));

		double yaw = Math.toRadians(-player.getYRot());
		player.setDeltaMovement(player.getDeltaMovement().add(Math.sin(yaw) * dashStrength, 0.0, Math.cos(yaw) * dashStrength));
		player.hurtMarked = true;

		MSUAbilitechParticles.burst(level, player, EnumAspect.TIME, 20 + (int) Math.round(chargeRatio * 30));

		double sustainStrength = MIN_SUSTAIN_STRENGTH + chargeRatio * (MAX_SUSTAIN_STRENGTH - MIN_SUSTAIN_STRENGTH);
		activeBurst.put(player, new BurstState(level.getGameTime() + duration, sustainStrength));
		enableAfterimages(player); // carry the afterimages straight through into the burst itself

		return true;
	}

	@Override
	public void onUnequipped(Level level, Player player, int techSlot)
	{
		super.onUnequipped(level, player, techSlot);
		activeBurst.remove(player);
		disableAfterimages(player);
		player.removeEffect(MSUMobEffects.ACCELERATING);
	}

	/** Ratio of how charged the current hold is, 0 (just pressed) to 1 (at or past {@link #MAX_CHARGE_TICKS}) -
	 * shared by the burst's own scaling and {@link AcceleratingEffect}'s synced charge percentage. */
	private static double chargeRatio(int time)
	{
		return Mth.clamp(time, 0, MAX_CHARGE_TICKS) / (double) MAX_CHARGE_TICKS;
	}

	private static BurstState getActiveBurst(Player player, Level level)
	{
		BurstState burst = activeBurst.get(player);
		if(burst == null)
			return null;
		if(level.getGameTime() >= burst.endTick())
		{
			activeBurst.remove(player);
			return null;
		}
		return burst;
	}

	/** Yaw-relative forward push, reapplied every tick the burst's follow-through thrust hasn't decayed
	 * below {@link #SUSTAIN_MIN_THRESHOLD} yet. */
	private static void applySustainThrust(Player player, double strength)
	{
		double yaw = Math.toRadians(-player.getYRot());
		player.setDeltaMovement(player.getDeltaMovement().add(Math.sin(yaw) * strength, 0.0, Math.cos(yaw) * strength));
		player.hurtMarked = true;
	}

	private static void enableAfterimages(Player player)
	{
		StreakPreference preference = player.getData(MSUAttachments.STREAK_PREFERENCE);
		if(preference.isEnabled() && preference.isHideTrail() && preference.isGhostsIgnoreSprint() && preference.getGhostTint() == RED_TINT)
			return; // already configured this tick's way - don't spam the sync packet every tick

		preference.setEnabled(true);
		preference.setHideTrail(true);
		preference.setGhostsIgnoreSprint(true);
		preference.setGhostTint(RED_TINT);
		if(preference.getFavouriteFlavour() == null)
			preference.setFavouriteFlavour(StreakFlavours.NAMES.get(0)); // irrelevant while hideTrail is true, just needs to be a valid name

		broadcast(player, preference);
	}

	private static void disableAfterimages(Player player)
	{
		StreakPreference preference = player.getData(MSUAttachments.STREAK_PREFERENCE);
		if(!preference.isEnabled())
			return;

		preference.setEnabled(false);
		broadcast(player, preference);
	}

	private static void broadcast(Player player, StreakPreference preference)
	{
		if(!(player instanceof ServerPlayer serverPlayer))
			return;

		PacketDistributor.sendToPlayersTrackingEntityAndSelf(serverPlayer, new StreakStateSyncPacket(serverPlayer.getId(),
				preference.isEnabled(), preference.resolveFlavour(), preference.isHideTrail(), preference.isGhostsIgnoreSprint(), preference.getGhostTint()));
	}
}
