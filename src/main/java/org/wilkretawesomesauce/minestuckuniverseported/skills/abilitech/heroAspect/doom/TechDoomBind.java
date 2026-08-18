package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.doom;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;

/**
 * Ported (simplified) from MinestuckUniverse (1.12.2)'s
 * {@code skills.abilitech.heroAspect.doom.TechDoomBind} ("Survivor's Bind") - passive: while enabled, a
 * hit that would otherwise kill you from above 5 HP instead leaves you at 1 HP, with a brief window of
 * extra invulnerability afterward.
 * <p>
 * The original only allowed this for damage sources found in a curated allowlist
 * ({@code GTEventHandler.BLOCKABLE_UNBLOCKABLES}) or tagged magic/non-unblockable - that allowlist class
 * isn't part of this port, so the source-type filtering is dropped entirely and this applies to any
 * incoming damage. Registers its own instance with {@link NeoForge#EVENT_BUS} directly (same reasoning as
 * {@code blood.TechBloodBleeding} - a static handler would need a not-yet-constructed
 * {@code MSUSkills} field to identify itself).
 */
public class TechDoomBind extends TechHeroAspect
{
	private static final float MIN_HEALTH_TO_TRIGGER = 5.0F;
	private static final int GRACE_INVULNERABILITY_TICKS = 40;

	public TechDoomBind()
	{
		super(Minestuckuniverseported.id("survivors_bind"), EnumAspect.DOOM, 888888, MSUTechType.PASSIVE);
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
	private void onIncomingDamage(LivingIncomingDamageEvent event)
	{
		LivingEntity entity = event.getEntity();
		if(entity.level().isClientSide())
			return;

		if(!(entity instanceof Player player))
			return;

		GodTierData godTier = player.getData(MSUAttachments.GOD_TIER);
		if(!godTier.isPassiveEnabledFor(this))
			return;

		if(entity.getHealth() > MIN_HEALTH_TO_TRIGGER && event.getAmount() >= entity.getHealth())
		{
			event.setAmount(entity.getHealth() - 1.0F);
			event.setInvulnerabilityTicks(GRACE_INVULNERABILITY_TICKS);
			MSUAbilitechParticles.oneshot(entity.level(), entity, EnumAspect.DOOM, 15);
		}
	}
}
