package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.witch;

import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import com.mraof.minestuck.player.Title;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.AbilitechLoadout;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUAbilitechRayTrace;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.MSUClassColors;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.MSUNegativeAspectEffects;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.abilitech.heroClass.TechWitchTrap} ("Wicked
 * Trap") - holding plants (or keeps extending, if already planted this slot's own) a real vanilla
 * {@link AreaEffectCloud} at the raytraced block/entity hit, effect-loaded from the caster's own Title
 * aspect's {@link MSUNegativeAspectEffects} debuff, tracked via the same real per-slot tether
 * {@code sylph.TechSylph} already uses. The original also force-tinted the cloud with the aspect's own
 * particle color; modern {@link AreaEffectCloud} has no matching {@code setFixedColor}-style override
 * left (confirmed via a real compile error, not guessed) - it already derives its render color from its
 * own potion contents automatically, which lines up with the applied effect anyway.
 */
public class TechWitchTrap extends TechHeroClass
{
	private static final float RADIUS = 3.5F;

	public TechWitchTrap()
	{
		super(Minestuckuniverseported.id("wicked_trap"), EnumClass.WITCH, 6500, MSUTechType.OFFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state == AbilitechKeyState.NONE)
			return false;

		if(!player.isCreative() && player.getFoodData().getFoodLevel() < 1)
		{
			player.displayClientMessage(Component.translatable("status.tooExhausted"), true);
			return false;
		}

		HitResult trace = MSUAbilitechRayTrace.getMouseOver(player, player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE));
		if(!(trace instanceof BlockHitResult) && !(trace instanceof EntityHitResult))
			return false;

		AbilitechLoadout badgeEffects = player.getData(MSUAttachments.ABILITECH_LOADOUT);
		AreaEffectCloud cloud = badgeEffects.getTether(techSlot) instanceof AreaEffectCloud existing ? existing : null;

		if(cloud != null && cloud.isRemoved())
			cloud = null;

		if(cloud == null)
		{
			if(!(player instanceof ServerPlayer serverPlayer))
				return false;
			var title = Title.getTitle(serverPlayer);
			if(title.isEmpty())
				return false;
			EnumAspect aspect = title.get().heroAspect();

			double x, y, z;
			if(trace instanceof EntityHitResult entityHit)
			{
				x = entityHit.getEntity().getX();
				y = entityHit.getEntity().getY() - 0.05;
				z = entityHit.getEntity().getZ();
			}
			else
			{
				BlockHitResult blockHit = (BlockHitResult) trace;
				BlockPos pos = blockHit.getBlockPos();
				x = pos.getX() + 0.5;
				y = pos.getY() + (blockHit.getDirection() == Direction.UP ? 1 : blockHit.getDirection() == Direction.DOWN ? -1 : 0);
				z = pos.getZ() + 0.5;
			}

			cloud = new AreaEffectCloud(level, x, y, z);
			MobEffectInstance negative = MSUNegativeAspectEffects.get(aspect);
			cloud.addEffect(new MobEffectInstance(negative.getEffect(), 30, negative.getAmplifier(), false, false));
			cloud.setRadius(RADIUS);
			cloud.setOwner(player);
			cloud.setDuration(30);
			cloud.setRadiusOnUse(0);
			level.addFreshEntity(cloud);
			badgeEffects.setTether(techSlot, cloud);
		}

		cloud.setDuration(cloud.getDuration() + 1);

		if(!player.isCreative() && time % 20 == 0)
			player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);
		else
			MSUAbilitechParticles.aura(level, player, 2, MSUClassColors.get(EnumClass.WITCH));

		return true;
	}

	@Override
	public boolean isUsableExternally(Level level, Player player)
	{
		return player.getFoodData().getFoodLevel() >= 1 && super.isUsableExternally(level, player);
	}
}
