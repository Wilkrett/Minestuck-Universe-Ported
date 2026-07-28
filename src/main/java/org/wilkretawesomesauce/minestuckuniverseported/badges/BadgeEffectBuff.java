package org.wilkretawesomesauce.minestuckuniverseported.badges;

import com.mraof.minestuck.alchemy.GristHelper;
import com.mraof.minestuck.api.alchemy.GristSet;
import com.mraof.minestuck.api.alchemy.GristTypes;
import com.mraof.minestuck.entity.FrogEntity;
import com.mraof.minestuck.player.GristCache;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

import java.util.List;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.MSUSkills#EFFECT_BUFF} - real cost: sacrifice 5
 * nearby real {@link FrogEntity}s + 5000 Quartz grist, using the real modern {@link GristCache}/
 * {@link GristSet} API (see {@link BadgeKarma}'s own doc comment for why that's a real API shape change,
 * not a guess). Consumed by {@code heroClass.heroClass.MSUAspectAmbientEffects}'s own real per-aspect
 * special-case doubling.
 * <p>
 * <b>Real, stated simplification</b>: the original filtered for a specific frog breed
 * ({@code EntityFrog#getType() == 6}, a raw 1.12.2-era numeric index). Modern {@link FrogEntity} replaced
 * that whole scheme with a named {@code FrogVariants} enum with no documented mapping back to the
 * original's numeric indices - rather than guess which named variant index 6 used to mean, this accepts
 * any 5 nearby frogs regardless of variant.
 */
public class BadgeEffectBuff extends BadgeLevel
{
	private static final double RADIUS = 10;
	private static final int FROG_COST = 5;
	private static final long QUARTZ_COST = 5000;

	public BadgeEffectBuff()
	{
		super(Minestuckuniverseported.id("effect_buff"), 4);
	}

	@Override
	public boolean canUnlock(Level level, Player player)
	{
		if(!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel))
			return false;

		GristSet gristCost = GristSet.of(GristTypes.QUARTZ.get().amount(QUARTZ_COST));
		GristCache gristCache = GristCache.get(serverPlayer);

		List<FrogEntity> frogs = level.getEntitiesOfClass(FrogEntity.class, player.getBoundingBox().inflate(RADIUS));
		if(frogs.size() < FROG_COST || !gristCache.canAfford(gristCost))
			return false;

		for(int i = 0; i < FROG_COST; i++)
		{
			FrogEntity frog = frogs.get(i);
			serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, frog.getX(), frog.getY() + 0.25, frog.getZ(), 30, 1, 0, 0, 0.2);
			frog.discard();
		}

		gristCache.tryTake(gristCost, GristHelper.EnumSource.SERVER);
		return true;
	}
}
