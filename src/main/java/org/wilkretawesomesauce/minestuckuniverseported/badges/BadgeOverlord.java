package org.wilkretawesomesauce.minestuckuniverseported.badges;

import com.mraof.minestuck.player.EnumClass;
import com.mraof.minestuck.player.Title;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.skills.MSUSkills;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.ClasspectColorHandler;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.badges.BadgeOverlord} ("World Ender") - never
 * shop-unlockable ({@link #canUnlock} always {@code false}, hidden from lists), real unlock is a
 * {@code LivingDeathEvent} hook: a Lord-class player who dies to another real player with
 * {@code godtier.GodTierData#getSkillLevel()} &ge; 80 has their death cancelled, gets healed/boosted,
 * and permanently gains this badge. Consumed by {@code heroClass.lord.TechLord}'s own real
 * {@code isOverlord} branch.
 * <p>
 * <b>Real, stated simplification</b>: the original also required dying on a real
 * {@code IGodTierBlock} matching the player's own Title aspect - this project has no "God Tier block"
 * concept at all ({@code godtier.GodTierEvents} already dropped that same requirement for ascension
 * itself, for the same reason: it needs deeper Sburb/Land-dimension integration this project's God Tier
 * pass didn't build). Dropped here too, for consistency with that existing project-wide choice, not a new
 * one.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public class BadgeOverlord extends Badge
{
	public static final int REQUIRED_SKILL_LEVEL = 80;

	public BadgeOverlord()
	{
		super(Minestuckuniverseported.id("world_ender"));
	}

	@Override
	public boolean canUnlock(Level level, Player player)
	{
		return false;
	}

	@Override
	public boolean canAppearOnList(Level level, Player player)
	{
		return false;
	}

	@Override
	public boolean isReadable(Level level, Player player)
	{
		return player instanceof ServerPlayer serverPlayer && serverPlayer.getData(MSUAttachments.GOD_TIER).hasBadge(this);
	}

	@Override
	public boolean canUse(Level level, Player player)
	{
		return !(player.hasEffect(MSUMobEffects.GOD_TIER_LOCK) && player.getEffect(MSUMobEffects.GOD_TIER_LOCK).getAmplifier() >= 2);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	private static void onLivingDeath(LivingDeathEvent event)
	{
		if(event.getEntity().level().isClientSide() || !(event.getEntity() instanceof ServerPlayer player))
			return;

		var title = Title.getTitle(player);
		if(title.isEmpty() || title.get().heroClass() != EnumClass.LORD)
			return;

		GodTierData godTier = player.getData(MSUAttachments.GOD_TIER);

		if(godTier.getSkillLevel() < REQUIRED_SKILL_LEVEL)
		{
			player.displayClientMessage(Component.translatable("status.overlordSkillLevel", REQUIRED_SKILL_LEVEL).setStyle(Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE)), false);
			return;
		}

		if(!(event.getSource().getEntity() instanceof Player))
		{
			player.displayClientMessage(Component.translatable("status.overlordPvpDeath").setStyle(Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE)), false);
			return;
		}

		godTier.unlockBadge(MSUSkills.BADGE_OVERLORD);

		player.setDeltaMovement(player.getDeltaMovement().x, 0.8, player.getDeltaMovement().z);
		player.hurtMarked = true;

		player.getServer().getPlayerList().broadcastSystemMessage(
				Component.translatable("status.overlordAscend", player.getName()).setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE)), false);
		player.addEffect(new MobEffectInstance(MSUMobEffects.GOD_TIER_COMEBACK, 200, 3));

		MSUAbilitechParticles.oneshot(player.level(), player, 25, ClasspectColorHandler.get(EnumClass.LORD));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SPAWN, player.getSoundSource(), 1.0F, 1.0F);

		player.setHealth(10);
		event.setCanceled(true);
	}
}
