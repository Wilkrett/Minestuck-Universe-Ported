package org.wilkretawesomesauce.minestuckuniverseported.entity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.MSUEntityTypes;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.soul.HostCapabilities;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.soul.RelayControlled;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.soul.SoulHost;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A physical player body whose Soul is no longer inhabiting it - design doc §7 ("UninhabitedBodyEntity").
 * Original design for this project, no 1.12.2 counterpart. Extends {@link Mob} (standard
 * {@code EntityType}-registered, automatic chunk/save persistence via {@link #addAdditionalSaveData}/
 * {@link #readAdditionalSaveData}) - deliberately <b>not</b> built on {@code util.MSUFakePlayer}, since a
 * {@code ServerPlayer} subclass has no {@code EntityType} and can't chunk-serialize the normal way
 * (confirmed: vanilla players are constructed directly by {@code PlayerList} on login, never through the
 * entity-type registry {@code MSUFakePlayer}'s own existing consumers never needed cross-restart
 * persistence for). Own fields below reproduce just the physical state a real {@code Player} gets for
 * free (inventory/XP/food) - see {@code mechanics.soul.SoulManager}'s own doc comment for how this
 * project's "swap, never duplicate" rule moves that state between this class and the real controlling
 * {@code ServerPlayer} on every enter/leave.
 * <p>
 * No AI goals - {@link #createNavigation} exists only because {@link Mob} requires it, not because this
 * class ever paths anywhere on its own; an abandoned body just stands there under normal gravity, and an
 * occupied one is driven entirely by {@link #setRelayInput} (see {@code mechanics.soul.SoulManager}'s doc
 * comment for the whole "hidden anchor" relay design), converted from the world-relative vector the
 * controller's client already sent into this body's own local strafe/forward fields
 * ({@link #xxa}/{@link #zza}, inherited from {@link net.minecraft.world.entity.LivingEntity}) using the
 * exact same {@code rotateYaw} math {@code TechMindControl.ClientEvents} already established for
 * "undoing" a world-relative vector back to a specific entity's own facing - vanilla's own
 * {@code LivingEntity#travel} then handles gravity/jumping/collision physics from those fields
 * unmodified, the same way an AI goal's movement input already does for any ordinary {@code Mob}.
 * {@link #removeWhenFarAway} is overridden to {@code false} - a real body must never vanish just because
 * no player is nearby, unlike an ordinary passive mob.
 * <p>
 * <b>Known simplification, stated plainly rather than faked</b>: skin/name rendering resolves the
 * owner's real {@code GameProfile} via vanilla's own {@code Minecraft#getSkinManager()} (see
 * {@code client.render.UninhabitedBodyRenderer}) - this is the first time this project has resolved an
 * <i>arbitrary offline player's</i> skin this way (previous skin-related work only ever touched the
 * locally-known player), so treat it as unverified in a live client, same category as every other
 * render-heavy feature in this project's CLAUDE.md.
 */
public class UninhabitedBodyEntity extends Mob implements SoulHost, RelayControlled
{
	private static final int INVENTORY_SIZE = 36;

	private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE);
	private int xpLevel;
	private float xpProgress;
	private int totalXp;
	private int foodLevel = 20;
	private float saturation = 5.0F;

	@Nullable
	private UUID ownerUUID;
	private String ownerName = "";
	@Nullable
	private UUID occupantSoulUUID;

	private float relayWorldX;
	private float relayWorldZ;
	private boolean relayJump;
	private boolean relaySneak;

	public UninhabitedBodyEntity(EntityType<? extends UninhabitedBodyEntity> type, Level level)
	{
		super(type, level);
	}

	/** Spawns and positions a fresh body for {@code owner} at their current position - {@code mechanics.soul.SoulManager#leaveBody} is the only real caller. */
	public static UninhabitedBodyEntity create(ServerLevel level, ServerPlayer owner)
	{
		UninhabitedBodyEntity body = new UninhabitedBodyEntity(MSUEntityTypes.UNINHABITED_BODY.get(), level);
		body.setOwner(owner.getUUID(), owner.getGameProfile().getName());
		body.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
		return body;
	}

	/** Real caller: {@code mechanics.soul.BodyManager}'s replacement-body spawn, where there's no live owner {@link ServerPlayer} instance handy (they may be offline). */
	public void setOwner(UUID ownerUUID, String ownerName)
	{
		this.ownerUUID = ownerUUID;
		this.ownerName = ownerName;
		setCustomName(Component.literal(ownerName));
		setCustomNameVisible(true);
	}

	/**
	 * Design doc §20 ("Uninhabited Body Death") - drop this body's own inventory/equipment/XP into the
	 * world exactly like an ordinary player death, then clear all of it. Real caller:
	 * {@code mechanics.soul.BodyManager#onBodyDeath}.
	 */
	public void dropContentsAndXp()
	{
		if(!(level() instanceof ServerLevel serverLevel))
			return;

		for(int i = 0; i < inventory.getContainerSize(); i++)
		{
			ItemStack stack = inventory.getItem(i);
			if(!stack.isEmpty())
				spawnAtLocation(stack);
		}
		inventory.clearContent();

		for(net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values())
		{
			ItemStack stack = getItemBySlot(slot);
			if(!stack.isEmpty())
			{
				spawnAtLocation(stack);
				setItemSlot(slot, ItemStack.EMPTY);
			}
		}

		if(totalXp > 0)
			net.minecraft.world.entity.ExperienceOrb.award(serverLevel, position(), totalXp);
		totalXp = 0;
		xpLevel = 0;
		xpProgress = 0.0F;
	}

	@Override
	protected PathNavigation createNavigation(Level level)
	{
		return new GroundPathNavigation(this, level);
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer)
	{
		return false;
	}

	// ================================================================================================
	// SoulHost
	// ================================================================================================

	@Override
	public boolean canBeInhabited()
	{
		return isAlive() && occupantSoulUUID == null;
	}

	@Override
	@Nullable
	public UUID getOccupyingSoul()
	{
		return occupantSoulUUID;
	}

	@Override
	public void setOccupyingSoul(UUID soulOwnerUUID)
	{
		occupantSoulUUID = soulOwnerUUID;
	}

	@Override
	public void clearOccupyingSoul()
	{
		occupantSoulUUID = null;
	}

	@Override
	public HostCapabilities getCapabilities()
	{
		return HostCapabilities.FULL;
	}

	@Override
	public void onSoulEnter(UUID soulOwnerUUID)
	{
		setOccupyingSoul(soulOwnerUUID);
	}

	@Override
	public void onSoulLeave()
	{
		clearOccupyingSoul();
		setRelayInput(0.0F, 0.0F, false, false);
	}

	@Override
	public void onSoulEject()
	{
		clearOccupyingSoul();
	}

	// ================================================================================================
	// RelayControlled - see class doc comment for the rotateYaw/xxa/zza reasoning
	// ================================================================================================

	@Override
	public void setRelayInput(float worldX, float worldZ, boolean jump, boolean sneak)
	{
		relayWorldX = worldX;
		relayWorldZ = worldZ;
		relayJump = jump;
		relaySneak = sneak;
	}

	@Override
	public void tick()
	{
		if(!level().isClientSide() && occupantSoulUUID != null)
		{
			float yaw = getYRot() * Mth.DEG_TO_RAD;
			float cos = Mth.cos(yaw);
			float sin = Mth.sin(yaw);
			this.xxa = relayWorldX * cos + relayWorldZ * sin;
			this.zza = relayWorldZ * cos - relayWorldX * sin;
			this.jumping = relayJump;
			setShiftKeyDown(relaySneak);
		}

		super.tick();
	}

	// ================================================================================================
	// Physical state - swapped with the controlling ServerPlayer by mechanics.soul.SoulManager, never
	// duplicated (design doc §9).
	// ================================================================================================

	public SimpleContainer getBodyInventory()
	{
		return inventory;
	}

	public int getXpLevel()
	{
		return xpLevel;
	}

	public void setXpLevel(int xpLevel)
	{
		this.xpLevel = xpLevel;
	}

	public float getXpProgress()
	{
		return xpProgress;
	}

	public void setXpProgress(float xpProgress)
	{
		this.xpProgress = xpProgress;
	}

	public int getTotalXp()
	{
		return totalXp;
	}

	public void setTotalXp(int totalXp)
	{
		this.totalXp = totalXp;
	}

	public int getBodyFoodLevel()
	{
		return foodLevel;
	}

	public void setBodyFoodLevel(int foodLevel)
	{
		this.foodLevel = foodLevel;
	}

	public float getBodySaturation()
	{
		return saturation;
	}

	public void setBodySaturation(float saturation)
	{
		this.saturation = saturation;
	}

	@Nullable
	public UUID getOwnerUUID()
	{
		return ownerUUID;
	}

	public String getOwnerName()
	{
		return ownerName;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag)
	{
		super.addAdditionalSaveData(tag);

		if(ownerUUID != null)
			tag.putUUID("Owner", ownerUUID);
		tag.putString("OwnerName", ownerName);
		if(occupantSoulUUID != null)
			tag.putUUID("Occupant", occupantSoulUUID);

		tag.putInt("XpLevel", xpLevel);
		tag.putFloat("XpProgress", xpProgress);
		tag.putInt("TotalXp", totalXp);
		tag.putInt("FoodLevel", foodLevel);
		tag.putFloat("Saturation", saturation);

		HolderLookup.Provider provider = level().registryAccess();
		ListTag items = new ListTag();
		for(int i = 0; i < inventory.getContainerSize(); i++)
		{
			ItemStack stack = inventory.getItem(i);
			if(stack.isEmpty())
				continue;
			CompoundTag slotTag = new CompoundTag();
			slotTag.putInt("Slot", i);
			slotTag.put("Item", stack.save(provider));
			items.add(slotTag);
		}
		tag.put("BodyInventory", items);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag)
	{
		super.readAdditionalSaveData(tag);

		ownerUUID = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
		ownerName = tag.getString("OwnerName");
		occupantSoulUUID = tag.hasUUID("Occupant") ? tag.getUUID("Occupant") : null;

		xpLevel = tag.getInt("XpLevel");
		xpProgress = tag.getFloat("XpProgress");
		totalXp = tag.getInt("TotalXp");
		foodLevel = tag.contains("FoodLevel") ? tag.getInt("FoodLevel") : 20;
		saturation = tag.contains("Saturation") ? tag.getFloat("Saturation") : 5.0F;

		HolderLookup.Provider provider = level().registryAccess();
		inventory.clearContent();
		ListTag items = tag.getList("BodyInventory", Tag.TAG_COMPOUND);
		for(int i = 0; i < items.size(); i++)
		{
			CompoundTag slotTag = items.getCompound(i);
			int slot = slotTag.getInt("Slot");
			if(slot >= 0 && slot < inventory.getContainerSize())
				inventory.setItem(slot, ItemStack.parseOptional(provider, slotTag.getCompound("Item")));
		}
	}
}
