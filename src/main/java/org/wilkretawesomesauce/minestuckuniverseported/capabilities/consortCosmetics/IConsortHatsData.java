package org.wilkretawesomesauce.minestuckuniverseported.capabilities.consortCosmetics;

import net.minecraft.world.item.ItemStack;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code capabilities.consortCosmetics.IConsortHatsData} - the
 * original also extended {@code capabilities.IMSUCapabilityBase<EntityLivingBase>} for
 * {@code writeToNBT}/{@code readFromNBT}/{@code setOwner}; those aren't repeated here since
 * {@link ConsortHatsData} already implements NeoForge's own {@code INBTSerializable} directly, the same
 * convention every other real capability class in this project's {@code capabilities} package uses
 * (see e.g. {@code capabilities.godTier.GodTierData}) now that Forge's capability-interface plumbing has
 * no NeoForge equivalent to mirror.
 */
public interface IConsortHatsData
{
	void setHeadStack(ItemStack stack);

	ItemStack getHeadStack();

	/**
	 * Real, user-requested extension - Consorts (only, not Frogs/Imps) can now also wear a chestplate, same
	 * general shape as the head slot above (spawn-time roll, dropped-item pickup, death drop, synced to
	 * observers) but tracked as its own separate field since a Consort can wear a hat and a chestplate at
	 * the same time. See {@link ConsortHatsData}'s own doc comment for why this stays plain attachment data
	 * rather than a real vanilla equipment slot, same reasoning as the hat.
	 */
	void setChestStack(ItemStack stack);

	ItemStack getChestStack();

	void setPickupDelay(int i);

	int getPickupDelay();

	int shrinkPickupDelay();

	/**
	 * Genuinely new, project-original quirk with no original 1.12.2 counterpart - rolled once per real
	 * equip (see {@link ConsortHatsData#equip}) at a real 0.1% chance, not a per-frame/per-render check,
	 * so a given wearer's hat orientation stays stable rather than flickering.
	 */
	boolean isHatUpsideDown();

	void setHatUpsideDown(boolean upsideDown);
}
