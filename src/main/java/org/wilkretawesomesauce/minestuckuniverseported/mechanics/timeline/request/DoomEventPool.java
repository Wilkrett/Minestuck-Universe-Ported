package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.request;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

import java.util.List;
import java.util.Random;

/**
 * The pool of negative consequences {@link TimeRequestDoomEvents} spends a player's Doom Points on,
 * grouped by cost tier per the user/friend design doc's own 5/10/20 DP examples (low/medium/high
 * severity). This is a representative starter set (2-3 entries per tier from their own examples), not
 * the full breadth listed in the doc - extending it later is just adding more entries here, not a
 * structural change. See {@code CLAUDE.md}'s Time Request / Doom System section.
 */
public final class DoomEventPool
{
	private static final Random RANDOM = new Random();

	public static final List<DoomEvent> ALL = List.of(
			// --- 5 DP: low severity ---------------------------------------------------------------
			event("brief_weakness", 5, player ->
					player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0))),
			event("small_hostile_spawn", 5, player ->
					spawnNear(player, EntityType.ZOMBIE, 1)),

			// --- 10 DP: medium severity ------------------------------------------------------------
			event("stronger_debuff", 10, player ->
			{
				player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
				player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 200, 1));
			}),
			event("hostile_wave", 10, player ->
					spawnNear(player, EntityType.ZOMBIE, 3)),

			// --- 20 DP: high severity --------------------------------------------------------------
			event("paradox_explosion", 20, player ->
			{
				ServerLevel level = player.serverLevel();
				level.explode(null, player.getX(), player.getY(), player.getZ(), 2.0F, false, Level.ExplosionInteraction.NONE);
			}),
			event("powerful_enemy_spawn", 20, player ->
					spawnNear(player, EntityType.VINDICATOR, 1)),
			event("long_curse", 20, player ->
			{
				player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 2));
				player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 1200, 1));
			})
	);

	private DoomEventPool()
	{
	}

	private static DoomEvent event(String path, int cost, java.util.function.Consumer<ServerPlayer> apply)
	{
		return new DoomEvent(Minestuckuniverseported.id(path), cost, apply);
	}

	private static void spawnNear(ServerPlayer player, EntityType<? extends Mob> type, int count)
	{
		ServerLevel level = player.serverLevel();
		for(int i = 0; i < count; i++)
		{
			double angle = RANDOM.nextDouble() * Math.PI * 2;
			double distance = 3 + RANDOM.nextDouble() * 4;
			double x = player.getX() + Math.cos(angle) * distance;
			double z = player.getZ() + Math.sin(angle) * distance;

			Mob mob = type.create(level);
			if(mob == null)
				continue;
			mob.moveTo(x, player.getY(), z, RANDOM.nextFloat() * 360F, 0);
			mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), net.minecraft.world.entity.MobSpawnType.EVENT, null);
			level.addFreshEntity(mob);
		}
	}
}
