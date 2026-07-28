package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.muse;

import com.mraof.minestuck.player.EnumClass;
import com.mraof.minestuck.player.Title;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.MSUSkills;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUClassColors;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.AbilitechTargetedEvent;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.MSUAspectAmbientEffects;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroClass.TechMuse} ("Muse's
 * Requiem") - a pure-passive death trigger: if the owner dies with this tech's passive toggled on, every
 * connected player with an opposite-signed (nonzero) Karma total gets blessed with the dying caster's own
 * {@link MSUAspectAmbientEffects} buffs (or Strength IV, matching {@code bard.TechBard}'s own no-Title
 * fallback exactly).
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public class TechMuse extends TechHeroClass
{
	public TechMuse()
	{
		super(Minestuckuniverseported.id("muse_requiem"), EnumClass.MUSE, 730000, MSUTechType.PASSIVE, MSUTechType.DEFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		return false;
	}

	@Override
	public boolean isUsableExternally(Level level, Player player)
	{
		return false;
	}

	@SubscribeEvent
	private static void onLivingDeath(LivingDeathEvent event)
	{
		if(event.getEntity().level().isClientSide() || !(event.getEntity() instanceof ServerPlayer player))
			return;

		GodTierData godTier = player.getData(MSUAttachments.GOD_TIER);
		if(!godTier.isPassiveEnabledFor(MSUSkills.MUSE_REQUIEM))
			return;

		var title = Title.getTitle(player);
		if(title.isEmpty())
			return;

		MSUAbilitechParticles.burst(player.level(), player, 30, MSUClassColors.get(EnumClass.MUSE));

		GodTierData casterGodTier = player.getData(MSUAttachments.GOD_TIER);
		int casterKarma = casterGodTier.getStaticKarma() + casterGodTier.getTempKarma();

		java.util.Set<Holder<MobEffect>> blessing = MSUAspectAmbientEffects.getAspectEffects(player).keySet();
		if(blessing.isEmpty())
			blessing = java.util.Set.of(MobEffects.DAMAGE_BOOST);

		for(ServerPlayer target : player.getServer().getPlayerList().getPlayers())
		{
			GodTierData targetGodTier = target.getData(MSUAttachments.GOD_TIER);
			int targetKarma = targetGodTier.getStaticKarma() + targetGodTier.getTempKarma();
			if(Math.signum(targetKarma) == Math.signum(casterKarma))
				continue;
			if(NeoForge.EVENT_BUS.post(new AbilitechTargetedEvent(player, target, MSUSkills.MUSE_REQUIEM, -1, false)).isCanceled())
				continue;

			for(Holder<MobEffect> effect : blessing)
				target.addEffect(new MobEffectInstance(effect, 1200, 9));

			MSUAbilitechParticles.oneshot(target.level(), target, 10, MSUClassColors.get(EnumClass.MUSE));
		}
	}
}
