package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.seer;

import com.mraof.minestuck.player.EnumClass;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.AbilitechLoadout;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.MSUSkills;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUClassColors;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroClass.TechSeerDodge} ("Foresight
 * Dodge") - passive: any blockable incoming hit is fully negated once per 1200 ticks (a real cooldown,
 * tracked via {@link AbilitechLoadout#getLastSeerDodgeTick}/{@code #setLastSeerDodgeTick}), with an
 * evasive hop.
 * <p>
 * <b>Gate</b>: the original's real condition was {@code allowlisted-unblockable OR magic-damage OR
 * blockable} - i.e. dodge nearly everything except a genuinely unblockable handful (out-of-world,
 * starving, etc.), explicitly including magic damage. The closest real modern equivalent boundary is
 * {@link DamageTypeTags#BYPASSES_INVULNERABILITY} (out-of-world, {@code /kill}, and similar
 * can't-be-prevented-at-all damage types) - this dodges anything <i>not</i> tagged with it, magic
 * included, matching the original's own permissive intent rather than guessing at a narrower modern tag
 * combination. The original's own separate {@code BLOCKABLE_UNBLOCKABLES} allowlist (real, but only ever
 * contained {@code FLY_INTO_WALL}) isn't reproduced as its own table for one entry.
 * <p>
 * <b>Hop direction/strength</b>: reconstructed directly from the original's real
 * {@code Entity#moveRelative(0, cos(pitch+90)/div, sin(pitch+90), speed)} call rather than substituted
 * with a different mechanic - {@code cos(pitch+90) = -sin(pitch)} becomes the vertical component (divided
 * by 2.5 grounded / 1.25 airborne) and {@code sin(pitch+90) = cos(pitch)} becomes the yaw-relative forward
 * component, both scaled by the same real 3.0 grounded / 1.5 airborne speed constant, translated directly
 * into an absolute {@link net.minecraft.world.entity.Entity#setDeltaMovement} delta since modern
 * {@code Entity} has no {@code moveRelative}-shaped convenience method to call directly.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public class TechSeerDodge extends TechHeroClass
{
	private static final int COOLDOWN_TICKS = 1200;
	private static final float GROUNDED_SPEED = 3.0F;
	private static final float AIRBORNE_SPEED = 1.5F;
	private static final float GROUNDED_UP_DIVISOR = 2.5F;
	private static final float AIRBORNE_UP_DIVISOR = 1.25F;

	public TechSeerDodge()
	{
		super(Minestuckuniverseported.id("foresight_dodge"), EnumClass.SEER, 3900, MSUTechType.DEFENSE);
	}

	@Override
	public boolean isUsableExternally(Level level, Player player)
	{
		return false;
	}

	@Override
	public void onPassiveToggle(Level level, Player player, boolean active)
	{
		super.onPassiveToggle(level, player, active);
		sendToggleMessage(player, active);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	private static void onIncomingDamage(LivingIncomingDamageEvent event)
	{
		if(event.getEntity().level().isClientSide() || !(event.getEntity() instanceof ServerPlayer player))
			return;

		if(event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))
			return;

		AbilitechLoadout badgeEffects = player.getData(MSUAttachments.ABILITECH_LOADOUT);
		GodTierData godTier = player.getData(MSUAttachments.GOD_TIER);
		if(!godTier.isPassiveEnabledFor(MSUSkills.FORESIGHT_DODGE))
			return;

		long now = player.level().getGameTime();
		if(now - badgeEffects.getLastSeerDodgeTick() < COOLDOWN_TICKS)
			return;

		badgeEffects.setLastSeerDodgeTick(now);
		MSUAbilitechParticles.oneshot(player.level(), player, 20, MSUClassColors.get(EnumClass.SEER));
		event.setCanceled(true);

		boolean grounded = player.onGround();
		float speed = grounded ? GROUNDED_SPEED : AIRBORNE_SPEED;
		float upDivisor = grounded ? GROUNDED_UP_DIVISOR : AIRBORNE_UP_DIVISOR;

		double pitchRad = Math.toRadians(player.getXRot());
		double upComponent = -Math.sin(pitchRad) / upDivisor;
		double forwardComponent = Math.cos(pitchRad);

		double yawRad = Math.toRadians(-player.getYRot());
		double motionX = Math.sin(yawRad) * forwardComponent * speed;
		double motionZ = Math.cos(yawRad) * forwardComponent * speed;
		double motionY = upComponent * speed;

		player.setDeltaMovement(player.getDeltaMovement().add(motionX, motionY, motionZ));
		player.hurtMarked = true;
	}
}
