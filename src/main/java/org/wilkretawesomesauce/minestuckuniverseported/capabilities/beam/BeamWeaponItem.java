package org.wilkretawesomesauce.minestuckuniverseported.capabilities.beam;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.MSUItemComponents;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

import java.util.UUID;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code items.weapons.ItemBeamWeapon}, this port's one concrete
 * beam weapon (the original's "Needlewand": {@code new ItemBeamWeapon(488, 6.5, -0.3, 0.05f, 10, 1, 60, ...)
 * .addProperties(new PropertyMagicBeam(), ...)}) - see {@code CLAUDE.md} for the full list of sibling
 * weapon items/{@code IPropertyBeam} flavor variants this pass deliberately doesn't duplicate; this one item
 * proves the real {@link Beam}/{@link BeamData} mechanic end to end, magic-damage-flavored directly rather
 * than through a separate {@code PropertyMagicBeam} class (see {@link IPropertyBeam}'s own doc comment for
 * why the full property-list indirection isn't ported).
 * <p>
 * Right-click starts charging (real vanilla bow-style {@link #use}/{@link #getUseAnimation}/
 * {@link #getUseDuration}), spawning a real anchored {@link Beam} that grows toward whatever the player is
 * aiming at every tick on both logical sides. Holding past {@link #beamChargeTicks} auto-releases it, same
 * as a fully-drawn bow; letting go early also releases it (handled by {@link Beam#onUpdate} itself detecting
 * the player is no longer actively using this exact stack, not by this item).
 */
public class BeamWeaponItem extends Item implements IBeamStats
{
	private final float beamRadius;
	private final float beamDamage;
	private final float beamSpeed;
	private final int beamChargeTicks;
	private final int beamHurtTicks;
	private final ResourceLocation beamTexture;

	public BeamWeaponItem(Properties properties, float beamRadius, float beamDamage, float beamSpeed, int beamChargeTicks, int beamHurtTicks, String beamTextureName)
	{
		super(properties);
		this.beamRadius = beamRadius;
		this.beamDamage = beamDamage;
		this.beamSpeed = beamSpeed;
		this.beamChargeTicks = beamChargeTicks;
		this.beamHurtTicks = beamHurtTicks;
		this.beamTexture = Minestuckuniverseported.id("textures/entity/projectiles/" + beamTextureName + ".png");
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity)
	{
		return 72000;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack)
	{
		return UseAnim.BOW;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack stack = player.getItemInHand(hand);

		if(!level.isClientSide())
		{
			Beam beam = new Beam(player, stack, beamSpeed);
			beam.damage = beamDamage;
			stack.set(MSUItemComponents.ACTIVE_BEAM_ID, beam.getUniqueID());

			Beam.fireBeam(beam);
		}

		player.startUsingItem(hand);
		return InteractionResultHolder.success(stack);
	}

	@Override
	public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration)
	{
		if(level.isClientSide() || !entity.getUseItem().equals(stack))
			return;

		UUID beamId = stack.get(MSUItemComponents.ACTIVE_BEAM_ID);
		if(beamId == null)
			return;

		int useTime = getUseDuration(stack, entity) - remainingUseDuration;
		if(useTime % 20 == 0)
			stack.hurtAndBreak(1, entity, entity.getUsedItemHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);

		if(useTime > beamChargeTicks)
		{
			BeamData data = level.getData(MSUAttachments.BEAM_DATA);
			Beam beam = data.getBeam(level, beamId);
			if(beam != null && !beam.isBeamReleased())
			{
				if(entity instanceof Player player)
					player.getCooldowns().addCooldown(stack.getItem(), beam.getDuration());
				beam.releaseBeam();
				BeamEvents.broadcast(level);
			}
		}
	}

	@Override
	public float getBeamRadius(ItemStack stack)
	{
		return beamRadius;
	}

	@Override
	public int getBeamHurtTime(ItemStack stack)
	{
		return beamHurtTicks;
	}

	@Override
	public ResourceLocation getBeamTexture()
	{
		return beamTexture;
	}
}
