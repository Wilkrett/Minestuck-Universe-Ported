package org.wilkretawesomesauce.minestuckuniverseported.capabilities.consortCosmetics;

import com.mraof.minestuck.entity.FrogEntity;
import com.mraof.minestuck.entity.consort.ConsortEntity;
import com.mraof.minestuck.entity.underling.ImpEntity;
import com.mraof.minestuck.item.BugNetItem;
import com.mraof.minestuck.item.MSItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.MSUItems;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.network.ConsortChestSyncPacket;
import org.wilkretawesomesauce.minestuckuniverseported.network.ConsortHatSyncPacket;

import java.util.List;
import java.util.function.Supplier;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code capabilities.consortCosmetics.ConsortHatsData} -
 * Consorts spawn with a small chance of already wearing a hat, and Consorts/Frogs/Imps (see
 * {@link #isHatCapable}) will pick up any dropped headwear that wanders near them, wearing it until they
 * die or (for a Frog specifically) get interacted with using a bug net.
 * <p>
 * <b>Real capability-style data, not a vanilla equipment slot</b>: an earlier pass through this project
 * stored the worn hat in the real vanilla {@link EquipmentSlot#HEAD} slot instead of as data here, on the
 * reasoning that {@code ConsortEntity}/{@code FrogEntity} are ordinary {@link Mob} subclasses and vanilla
 * equipment sync/death-drop would come "for free". That reasoning didn't hold up: both entities render via
 * a custom GeckoLib model ({@code ConsortRenderer}), not vanilla's {@code HumanoidModel}/
 * {@code HumanoidArmorLayer}, so nothing ever actually consumed the vanilla equipment slot into a render -
 * the same real gap existed either way, just described differently. This port reverts to the original's
 * real approach instead: the hat is tracked as plain attachment data (see {@link #hat}), synced to
 * observers via the real {@code network.ConsortHatSyncPacket} (the modern equivalent of the original's own
 * bespoke {@code MSUPacket.Type.UPDATE_HATS}), and manually dropped as a real {@link ItemEntity} on death
 * or bug-net interaction rather than relying on vanilla's drop-chance mechanism.
 * <p>
 * <b>Same known gap as before, now correctly attributed</b>: wearing a hat here is mechanically real
 * (tracked, synced, drops on death/bug-net) but still won't visibly render on the Consort/Frog's body
 * without a dedicated GeckoLib render layer - real future work, not attempted this pass. Ready
 * infrastructure for it already exists client-side, see {@code client.ConsortHatClientState}.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ConsortHatsData implements IConsortHatsData, INBTSerializable<CompoundTag>
{
	private static final double PICKUP_RADIUS_XZ = 1.0;
	private static final double PICKUP_RADIUS_Y = 0.5;
	private static final int PICKUP_COOLDOWN_TICKS = 200;
	private static final float SPAWN_HAT_CHANCE = 0.05F;
	private static final float SPAWN_CHEST_CHANCE = 0.05F;
	private static final float UPSIDE_DOWN_CHANCE = 0.001F;

	/** Mirrors the original's static {@code HAT_SPAWN_POOL}. crumply_hat is Minestuck's own real item, reused directly rather than duplicated. */
	private static final List<Supplier<ItemStack>> HAT_SPAWN_POOL = List.of(
			() -> new ItemStack(MSItems.CRUMPLY_HAT.get()),
			() -> new ItemStack(MSUItems.WIZARD_HAT.get()),
			() -> new ItemStack(MSUItems.FROG_HAT.get()),
			() -> new ItemStack(Items.LEATHER_HELMET),
			() -> new ItemStack(Items.CHAINMAIL_HELMET)
	);

	/** Real, user-requested addition, no original counterpart - plain vanilla chestplates only, same low-key flavor as the plain leather/chainmail entries in {@link #HAT_SPAWN_POOL}. */
	private static final List<Supplier<ItemStack>> CHEST_SPAWN_POOL = List.of(
			() -> new ItemStack(Items.LEATHER_CHESTPLATE),
			() -> new ItemStack(Items.CHAINMAIL_CHESTPLATE)
	);

	private ItemStack hat = ItemStack.EMPTY;
	private ItemStack chest = ItemStack.EMPTY;
	private int pickupDelay = 0;
	private boolean hatUpsideDown = false;

	@Override
	public void setHeadStack(ItemStack stack)
	{
		hat = stack;
	}

	@Override
	public ItemStack getHeadStack()
	{
		return hat;
	}

	@Override
	public void setChestStack(ItemStack stack)
	{
		chest = stack;
	}

	@Override
	public ItemStack getChestStack()
	{
		return chest;
	}

	@Override
	public void setPickupDelay(int i)
	{
		pickupDelay = i;
	}

	@Override
	public int getPickupDelay()
	{
		return pickupDelay;
	}

	@Override
	public int shrinkPickupDelay()
	{
		pickupDelay = Math.max(0, pickupDelay - 1);
		return pickupDelay;
	}

	@Override
	public boolean isHatUpsideDown()
	{
		return hatUpsideDown;
	}

	@Override
	public void setHatUpsideDown(boolean upsideDown)
	{
		hatUpsideDown = upsideDown;
	}

	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider provider)
	{
		CompoundTag nbt = new CompoundTag();
		if(!hat.isEmpty())
		{
			nbt.put("Hat", hat.save(provider, new CompoundTag()));
			nbt.putBoolean("HatUpsideDown", hatUpsideDown);
		}
		if(!chest.isEmpty())
			nbt.put("Chest", chest.save(provider, new CompoundTag()));
		nbt.putInt("PickupDelay", pickupDelay);
		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt)
	{
		hat = nbt.contains("Hat") ? ItemStack.parse(provider, nbt.getCompound("Hat")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
		hatUpsideDown = nbt.getBoolean("HatUpsideDown");
		chest = nbt.contains("Chest") ? ItemStack.parse(provider, nbt.getCompound("Chest")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
		pickupDelay = nbt.getInt("PickupDelay");
	}

	@SubscribeEvent
	private static void onEntityJoinLevel(EntityJoinLevelEvent event)
	{
		// loadedFromDisk() excludes this from re-rolling every time an existing Consort's chunk reloads -
		// the original only ever rolled this once, in its capability's setOwner(), called exactly once
		// per entity ever (not per load).
		if(event.getLevel().isClientSide() || event.loadedFromDisk() || !(event.getEntity() instanceof ConsortEntity consort))
			return;

		// Skip if util.MSUConsorts' own higher-priority EntityJoinLevelEvent handler already force-equipped
		// the archmage hat this same join (a skill-shop-seller roll) - without this guard, this roll would
		// still fire unconditionally and could immediately overwrite that guaranteed hat with a random one.
		ConsortHatsData cap = consort.getData(MSUAttachments.CONSORT_HATS_DATA);
		if(cap.getHeadStack().isEmpty() && consort.getRandom().nextFloat() < SPAWN_HAT_CHANCE)
			equip(consort, HAT_SPAWN_POOL.get(consort.getRandom().nextInt(HAT_SPAWN_POOL.size())).get());

		if(cap.getChestStack().isEmpty() && consort.getRandom().nextFloat() < SPAWN_CHEST_CHANCE)
			equipChest(consort, CHEST_SPAWN_POOL.get(consort.getRandom().nextInt(CHEST_SPAWN_POOL.size())).get());
	}

	@SubscribeEvent
	private static void onEntityTick(EntityTickEvent.Post event)
	{
		if(!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide())
			return;

		boolean hatCapable = isHatCapable(mob);
		boolean chestCapable = isChestCapable(mob);
		if(!hatCapable && !chestCapable)
			return;

		ConsortHatsData cap = mob.getData(MSUAttachments.CONSORT_HATS_DATA);
		if(cap.shrinkPickupDelay() > 0)
			return;

		for(ItemEntity itemEntity : mob.level().getEntitiesOfClass(ItemEntity.class,
				mob.getBoundingBox().inflate(PICKUP_RADIUS_XZ, PICKUP_RADIUS_Y, PICKUP_RADIUS_XZ)))
		{
			if(!itemEntity.isAlive() || itemEntity.getItem().isEmpty() || itemEntity.hasPickUpDelay())
				continue;

			ItemStack stack = itemEntity.getItem();
			EquipmentSlot slot = mob.getEquipmentSlotForItem(stack);

			boolean pickUpAsHat = hatCapable && slot == EquipmentSlot.HEAD && !ItemStack.isSameItemSameComponents(stack, cap.getHeadStack());
			boolean pickUpAsChest = chestCapable && slot == EquipmentSlot.CHEST && !ItemStack.isSameItemSameComponents(stack, cap.getChestStack());
			if(!pickUpAsHat && !pickUpAsChest)
				continue;

			ItemStack pickedUp = stack.copyWithCount(1);
			stack.shrink(1);
			if(pickUpAsHat)
				equip(mob, pickedUp);
			else
				equipChest(mob, pickedUp);
			mob.take(itemEntity, 1);
			if(stack.isEmpty())
				itemEntity.discard();

			cap.setPickupDelay(PICKUP_COOLDOWN_TICKS);
			break;
		}
	}

	@SubscribeEvent
	private static void onEntityDeath(LivingDeathEvent event)
	{
		if(event.getEntity().level().isClientSide() || !(event.getEntity() instanceof Mob mob))
			return;

		if(isHatCapable(mob))
			dropHat(mob);
		if(isChestCapable(mob))
			dropChest(mob);
	}

	@SubscribeEvent
	private static void onEntityInteract(PlayerInteractEvent.EntityInteract event)
	{
		if(event.getLevel().isClientSide() || !(event.getTarget() instanceof FrogEntity frog))
			return;

		if(event.getEntity().getItemInHand(event.getHand()).getItem() instanceof BugNetItem)
			dropHat(frog);
	}

	@SubscribeEvent
	private static void onStartTracking(PlayerEvent.StartTracking event)
	{
		if(event.getEntity().level().isClientSide() || !(event.getTarget() instanceof Mob mob))
			return;
		if(!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer observer))
			return;

		ConsortHatsData cap = mob.getData(MSUAttachments.CONSORT_HATS_DATA);
		if(isHatCapable(mob) && !cap.getHeadStack().isEmpty())
			PacketDistributor.sendToPlayer(observer, new ConsortHatSyncPacket(mob.getId(), cap.getHeadStack(), cap.isHatUpsideDown()));
		if(isChestCapable(mob) && !cap.getChestStack().isEmpty())
			PacketDistributor.sendToPlayer(observer, new ConsortChestSyncPacket(mob.getId(), cap.getChestStack()));
	}

	/**
	 * Real, user-requested extension: Imps can now pick up/wear dropped hats too, the same way Frogs already
	 * could (no spawn-time random-hat roll for either - that stays a Consort-only real port of the original's
	 * own behavior). {@code client.render.ImpHatGeoLayer} is the real render-side consumer.
	 */
	private static boolean isHatCapable(Mob mob)
	{
		return mob instanceof ConsortEntity || mob instanceof FrogEntity || mob instanceof ImpEntity;
	}

	/** Real, user-requested addition, no original counterpart - chestplates are Consort-only, unlike hats which also cover Frogs/Imps. {@code client.render.ConsortChestGeoLayer} is the real render-side consumer. */
	private static boolean isChestCapable(Mob mob)
	{
		return mob instanceof ConsortEntity;
	}

	/** Public so {@code util.MSUConsorts} can force-equip the archmage hat on a rolled skill-shop-seller Consort. */
	public static void equip(Mob mob, ItemStack newHat)
	{
		ConsortHatsData cap = mob.getData(MSUAttachments.CONSORT_HATS_DATA);
		if(!cap.getHeadStack().isEmpty() && mob.level() instanceof net.minecraft.server.level.ServerLevel)
			mob.spawnAtLocation(cap.getHeadStack());

		cap.setHeadStack(newHat);
		cap.setHatUpsideDown(mob.getRandom().nextFloat() < UPSIDE_DOWN_CHANCE);
		PacketDistributor.sendToPlayersTrackingEntity(mob, new ConsortHatSyncPacket(mob.getId(), newHat, cap.isHatUpsideDown()));
	}

	private static void dropHat(Mob mob)
	{
		ConsortHatsData cap = mob.getData(MSUAttachments.CONSORT_HATS_DATA);
		ItemStack current = cap.getHeadStack();
		if(current.isEmpty())
			return;

		cap.setHeadStack(ItemStack.EMPTY);
		cap.setHatUpsideDown(false);
		mob.spawnAtLocation(current);
		PacketDistributor.sendToPlayersTrackingEntity(mob, new ConsortHatSyncPacket(mob.getId(), ItemStack.EMPTY, false));
	}

	/** Chestplate equivalent of {@link #equip} - no upside-down quirk, that's a hat-only Easter egg. */
	private static void equipChest(Mob mob, ItemStack newChest)
	{
		ConsortHatsData cap = mob.getData(MSUAttachments.CONSORT_HATS_DATA);
		if(!cap.getChestStack().isEmpty() && mob.level() instanceof net.minecraft.server.level.ServerLevel)
			mob.spawnAtLocation(cap.getChestStack());

		cap.setChestStack(newChest);
		PacketDistributor.sendToPlayersTrackingEntity(mob, new ConsortChestSyncPacket(mob.getId(), newChest));
	}

	private static void dropChest(Mob mob)
	{
		ConsortHatsData cap = mob.getData(MSUAttachments.CONSORT_HATS_DATA);
		ItemStack current = cap.getChestStack();
		if(current.isEmpty())
			return;

		cap.setChestStack(ItemStack.EMPTY);
		mob.spawnAtLocation(current);
		PacketDistributor.sendToPlayersTrackingEntity(mob, new ConsortChestSyncPacket(mob.getId(), ItemStack.EMPTY));
	}
}
