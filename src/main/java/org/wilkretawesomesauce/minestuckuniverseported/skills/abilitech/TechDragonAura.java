package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.wilkretawesomesauce.minestuckuniverseported.MSUItems;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.skills.TechBoondollarCost;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.TechDragonAura} ("Draconic Aura") - a
 * generic (non-aspect, non-class) tech, registered as {@code MSUSkills#DRACONIC_AURA} with 0 boondollar
 * cost but gated on holding a real {@link MSUItems#DRAGON_GEL} (see {@link #TechDragonAura()}, the same
 * {@code requiredStacks} mechanism {@link TechBoondollarCost} already provides, just never previously
 * exercised by any already-ported tech).
 * <p>
 * While held: gradually heals the caster (1 HP every 10 ticks) at the cost of hunger (1 point every 20
 * ticks), refusing to activate at all below 1 hunger point - matching the original's
 * {@code isUsableExternally}/in-tick exhaustion checks exactly. The real trade-off, ported faithfully: the
 * original's {@code hasDragonAura} capability field becomes {@code AbilitechLoadout#isDragonAuraActive()},
 * a plain per-player scratch flag set every held tick - while it's set, any damage landing on the caster
 * (real port of the original's own {@code LivingAttackEvent} hook, using this project's established
 * {@code LivingDamageEvent.Post} equivalent - see {@code heroClass.heir.TechHeir}'s own doc comment for why
 * that's this project's real substitute) detonates a real AoE retaliation nova (8x3x8 box, 10 explosion
 * damage to everyone else in range) and locks the caster out of every God Tier-gated tech for 30 real
 * seconds via {@code potions.GodTierLockEffect} at amplifier 3 - identical numbers to the original.
 * <p>
 * <b>Real simplification, not the original's own shape</b>: this flag used to be a registered
 * {@code MobEffect} ({@code DragonAuraEffect}, since deleted) purely so the static
 * {@link #onLivingDamage} handler below - which only ever gets handed an arbitrary {@code Player} from a
 * global damage event, with no direct reference to this class's own instance - could ask "is this player
 * currently holding Dragon Aura" from outside {@link #onUseTick}'s own scope. A registered {@code MobEffect} was
 * overkill for that: {@link #onLivingDamage} is server-only and never needed the automatic client sync a
 * real {@code MobEffect} provides, and (confirmed by grepping the whole codebase) nothing else anywhere ever
 * queried, applied, or removed {@code DRAGON_AURA} - unlike every one of this project's other
 * "marker effect" techs (Hoping/Wind Vessel/Soul Shock/etc.), each of which turned out to have a real
 * second consumer (a client HUD/render/input hook, a cross-tech check, or similar) that genuinely needs
 * the entity-queryable, auto-synced behavior a real {@code MobEffect} provides - those stay real effects.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public class TechDragonAura extends TechBoondollarCost
{
	private static final int HEAL_INTERVAL_TICKS = 10;
	private static final int FOOD_DRAIN_INTERVAL_TICKS = 20;
	private static final float NOVA_RADIUS_XZ = 8.0F;
	private static final float NOVA_RADIUS_Y = 3.0F;
	private static final float NOVA_DAMAGE = 10.0F;
	private static final int LOCK_DURATION_TICKS = 600;
	private static final int LOCK_AMPLIFIER = 3;

	public TechDragonAura()
	{
		super(Minestuckuniverseported.id("draconic_aura"), 18000, MSUTechType.OFFENSE); // new tech, no original cost to port - picked to fit this project's own cost spread, see class doc comment
		requiredStacks.add(() -> new ItemStack(MSUItems.DRAGON_GEL.get()));
	}

	@Override
	public void onUnequipped(Level level, Player player, int techSlot)
	{
		super.onUnequipped(level, player, techSlot);
		player.getData(MSUAttachments.ABILITECH_LOADOUT).setDragonAuraActive(false);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE)
		{
			player.getData(MSUAttachments.ABILITECH_LOADOUT).setDragonAuraActive(false);
			return false;
		}

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 1)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			player.getData(MSUAttachments.ABILITECH_LOADOUT).setDragonAuraActive(false);
			return false;
		}

		player.getData(MSUAttachments.ABILITECH_LOADOUT).setDragonAuraActive(true);

		if(time % HEAL_INTERVAL_TICKS == 0)
			player.heal(1.0F);
		if(time % FOOD_DRAIN_INTERVAL_TICKS == 0 && !player.isCreative())
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);

		RandomSource random = player.getRandom();
		int red = (int) ((0.7176471F + random.nextFloat() * (0.8745098F - 0.7176471F)) * 0xFF);
		int blue = (int) ((0.8235294F + random.nextFloat() * (0.9764706F - 0.8235294F)) * 0xFF);
		int color = (red << 16) | blue;
		MSUAbilitechParticles.aura(level, player, 10, color);

		return true;
	}

	@Override
	public boolean isUsableExternally(Level level, Player player)
	{
		return player.getFoodData().getFoodLevel() >= 1 && super.isUsableExternally(level, player);
	}

	@SubscribeEvent
	private static void onLivingDamage(LivingDamageEvent.Post event)
	{
		if(event.getEntity().level().isClientSide() || !(event.getEntity() instanceof Player player))
			return;

		if(!player.getData(MSUAttachments.ABILITECH_LOADOUT).isDragonAuraActive())
			return;

		player.getData(MSUAttachments.ABILITECH_LOADOUT).setDragonAuraActive(false);

		ServerLevel level = (ServerLevel) player.level();
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY() + 0.5, player.getZ(), 1, 0, 0, 0, 0);

		DamageSource explosion = level.damageSources().explosion(player, player);
		for(LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
				player.getBoundingBox().inflate(NOVA_RADIUS_XZ, NOVA_RADIUS_Y, NOVA_RADIUS_XZ), e -> e != player))
			target.hurt(explosion, NOVA_DAMAGE);

		player.addEffect(new MobEffectInstance(MSUMobEffects.GOD_TIER_LOCK, LOCK_DURATION_TICKS, LOCK_AMPLIFIER, false, false));
	}
}
