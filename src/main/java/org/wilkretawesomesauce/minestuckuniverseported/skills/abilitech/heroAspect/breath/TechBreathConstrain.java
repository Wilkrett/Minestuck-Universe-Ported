package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.breath;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.freedom.FreedomData;
import org.wilkretawesomesauce.minestuckuniverseported.network.WindRibbonSyncPacket;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.AbilitechLoadout;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAspectColors;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;

/**
 * New tech for this project ("Stifling Calm") - no 1.12.2 counterpart, no original cost to port (see
 * this class's own cost comment below). The offensive mirror of {@link TechBreathLiberate}: hold and aim
 * at a target to gradually lower their hidden {@code mechanics.freedom.FreedomData} Freedom - the target
 * keeps wanting whatever it already wanted, it just loses the behavioral slack to act on anything else
 * (see {@code mechanics.freedom.FreedomEvents}' own doc comment for exactly what that suppresses at low
 * values) - explicitly not mind control, matching the source design doc's own "this is not mind control"
 * framing.
 * <p>
 * <b>Real visuals</b>, from the "Breath Wind Engine Visualizer Design" doc's own explicit visual
 * principle for this exact tech: <i>"Do not make this evil wind. Constrain represents the removal of
 * movement... the air itself is restricting movement."</i> {@link WindEngine#pressureInward} draws
 * particles converging on the target rather than flowing outward - the opposite motion from
 * {@link TechBreathLiberate}'s own {@link WindEngine#ribbon}/{@code spiralAroundTarget}, not a darker or
 * more sinister color (still Breath's own second real palette color, {@code 0x4379E6}, never anything
 * outside that aspect's established table). The compression visibly tightens as the target's Freedom
 * actually drops, mirroring Liberate's own progression in reverse.
 * <p>
 * <b>Real primary visual now</b>, per the later "Breath Visualizer Architecture Decision" doc's own
 * stricter rule (vanilla particles demoted to secondary/atmospheric only): {@code client.render.WindRibbonRenderer}'s
 * same ribbon+vortex mesh {@link TechBreathLiberate} uses, driven by the same {@link WindRibbonSyncPacket}
 * with {@code inward=true} - see that renderer's own doc comment for how the {@code inward} flag changes
 * both the vortex's motion (shrinking toward the target instead of holding constant radius) and its color.
 */
public class TechBreathConstrain extends TechHeroAspect
{
	private static final float FREEDOM_PER_TICK = 0.5F;

	private static final double PRESSURE_RADIUS_MAX = 1.4;
	private static final double PRESSURE_RADIUS_MIN = 0.5;
	private static final float PRESSURE_INTENSITY_MIN = 0.6F;
	private static final float PRESSURE_INTENSITY_MAX = 2.2F;

	private static final int RIBBON_RESYNC_INTERVAL_TICKS = 10;

	public TechBreathConstrain()
	{
		super(Minestuckuniverseported.id("stifling_calm"), EnumAspect.BREATH, 32000, MSUTechType.OFFENSE); // new tech, no original cost to port - same spread as TechBreathLiberate, its direct mirror
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		AbilitechLoadout badgeEffects = player.getData(MSUAttachments.ABILITECH_LOADOUT);

		if(state == AbilitechKeyState.NONE || state == AbilitechKeyState.RELEASED)
		{
			badgeEffects.setTether(techSlot, null);
			clearRibbon(player);
			return false;
		}

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 1)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			badgeEffects.setTether(techSlot, null);
			clearRibbon(player);
			return false;
		}

		Entity tether = badgeEffects.getTether(techSlot);
		LivingEntity target = tether instanceof LivingEntity livingTether && livingTether.isAlive() ? livingTether : null;

		if(target == null && state == AbilitechKeyState.PRESS)
		{
			LivingEntity raytraced = MSUAbilitechRayTrace.getTargetEntity(player);
			if(raytraced != null && raytraced != player)
			{
				badgeEffects.setTether(techSlot, raytraced);
				target = raytraced;
			}
		}

		if(target == null)
			return false;

		FreedomData data = target.getData(MSUAttachments.FREEDOM_DATA);
		data.addFreedom(-FREEDOM_PER_TICK);

		if(time % 20 == 0 && !player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);

		int color = MSUAspectColors.get(EnumAspect.BREATH)[1];
		float compressionFraction = 1.0F - data.getFreedom() / 100F;

		WindEngine.pressureInward(level, target.position().add(0, target.getBbHeight() * 0.5, 0),
				PRESSURE_RADIUS_MAX - compressionFraction * (PRESSURE_RADIUS_MAX - PRESSURE_RADIUS_MIN), color,
				PRESSURE_INTENSITY_MIN + compressionFraction * (PRESSURE_INTENSITY_MAX - PRESSURE_INTENSITY_MIN));

		if(time == 0 || time % RIBBON_RESYNC_INTERVAL_TICKS == 0)
			syncRibbon(player, target, compressionFraction);

		return true;
	}

	@Override
	public void onUnequipped(Level level, Player player, int techSlot)
	{
		player.getData(MSUAttachments.ABILITECH_LOADOUT).setTether(techSlot, null);
		clearRibbon(player);
	}

	private static void syncRibbon(Player player, LivingEntity target, float intensity)
	{
		if(player instanceof ServerPlayer serverPlayer)
			PacketDistributor.sendToPlayersTrackingEntityAndSelf(serverPlayer, new WindRibbonSyncPacket(serverPlayer.getId(), target.getId(), true, intensity));
	}

	private static void clearRibbon(Player player)
	{
		if(player instanceof ServerPlayer serverPlayer)
			PacketDistributor.sendToPlayersTrackingEntityAndSelf(serverPlayer, new WindRibbonSyncPacket(serverPlayer.getId(), -1, true, 0F));
	}
}
