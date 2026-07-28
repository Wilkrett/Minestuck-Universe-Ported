package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.blood;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroAspect.blood.TechBloodBleeding} -
 * a passive tech with no {@link #onUseTick} of its own (matching the original, which also always
 * returned {@code false} there): equip it and toggle its passive mode on, and any hit you land on a
 * living target has a chance to inflict {@link BleedingEffect}. The original also scaled the effect's
 * amplifier (0-5) by the attacker's "ATTACK skill level", from the skills/badges stat-leveling economy
 * this whole project already doesn't have (see {@code Abilitech}'s own scope note) - dropped, amplifier
 * is always 0. The proc-chance formula and the "bleed duration scales with the attacker's own current
 * health %" quirk are both kept faithfully, neither depends on anything unported.
 * <p>
 * Registers its own instance directly with {@link NeoForge#EVENT_BUS} in the constructor rather than
 * using a static {@code @SubscribeEvent} method the way {@code TimeStopEffect}/{@code StrifeRestrictionEvents}
 * do elsewhere in this project - a static handler here would need to reach back into
 * {@code MSUSkills.BLOOD_BLEEDING} to know "which tech instance is this", but that field doesn't
 * exist yet while {@code MSUSkills} is still in the middle of constructing it. Techs are already
 * effectively singletons (one instance per registered tech), so registering {@code this} sidesteps the
 * ordering problem entirely.
 */
public class TechBloodBleeding extends TechHeroAspect
{
	private static final float MAX_PROC_CHANCE = 0.8F;
	private static final float LUCK_DIVISOR = 25.0F;
	private static final int MAX_DURATION_TICKS = 600;

	public TechBloodBleeding()
	{
		super(Minestuckuniverseported.id("bleeding_edge"), EnumAspect.BLOOD, 4900, MSUTechType.PASSIVE);
		NeoForge.EVENT_BUS.register(this);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		return false;
	}

	@Override
	public void onPassiveToggle(Level level, Player player, boolean active)
	{
		super.onPassiveToggle(level, player, active);
		sendToggleMessage(player, active);
	}

	@SubscribeEvent
	private void onLivingDamage(LivingDamageEvent.Post event)
	{
		if(event.getEntity().level().isClientSide())
			return;

		if(!(event.getSource().getEntity() instanceof Player attacker))
			return;

		GodTierData godTier = attacker.getData(MSUAttachments.GOD_TIER);
		if(!godTier.isPassiveEnabledFor(this))
			return;

		LivingEntity target = event.getEntity();

		float procChance = Math.min(MAX_PROC_CHANCE, (float) (attacker.getAttributeValue(Attributes.LUCK) / LUCK_DIVISOR));
		if(attacker.level().getRandom().nextFloat() >= procChance)
			return;

		int duration = (int) ((attacker.getHealth() / attacker.getMaxHealth()) * MAX_DURATION_TICKS);
		if(duration <= 0)
			return;

		target.addEffect(new MobEffectInstance(MSUMobEffects.BLEEDING, duration, 0, false, false));
		MSUAbilitechParticles.oneshot(event.getEntity().level(), target, EnumAspect.BLOOD, 5);
	}
}
