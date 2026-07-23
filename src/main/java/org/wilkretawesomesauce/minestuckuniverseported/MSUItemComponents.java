package org.wilkretawesomesauce.minestuckuniverseported;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.wilkretawesomesauce.minestuckuniverseported.strife.StrifeSpecibusData;

import java.util.UUID;
import java.util.function.Supplier;

public final class MSUItemComponents
{
	public static final DeferredRegister.DataComponents REGISTRY =
			DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Minestuckuniverseported.MODID);

	public static final Supplier<DataComponentType<StrifeSpecibusData>> STRIFE_SPECIBUS = REGISTRY.registerComponentType(
			"strife_specibus", builder -> builder.persistent(StrifeSpecibusData.CODEC).networkSynchronized(StrifeSpecibusData.STREAM_CODEC));

	public static final Supplier<DataComponentType<org.wilkretawesomesauce.minestuckuniverseported.godtier.GodTierArmorData>> GOD_TIER_TITLE = REGISTRY.registerComponentType(
			"god_tier_title", builder -> builder
					.persistent(org.wilkretawesomesauce.minestuckuniverseported.godtier.GodTierArmorData.CODEC)
					.networkSynchronized(org.wilkretawesomesauce.minestuckuniverseported.godtier.GodTierArmorData.STREAM_CODEC));

	// Stamped onto an item borrowed via TechFutureRequest, carrying the originating TimeRequest's id.
	// The Temporal Sendificator rejects any stack carrying this component (regardless of which request
	// it names) as a repayment - only an untagged, freshly-obtained stack counts as "a new copy". See
	// timeline.request.TimeRequest's doc comment for why provenance-checking wasn't attempted instead.
	public static final Supplier<DataComponentType<UUID>> BORROWED_REQUEST_ID = REGISTRY.registerComponentType(
			"borrowed_request_id", builder -> builder.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));

	// Stamped onto a beam weapon stack while it's actively charging a shot, naming the beam.Beam instance
	// it's driving - modern equivalent of the original ItemBeamWeapon's raw stack.getTagCompound()
	// "Beam" UUID tag.
	public static final Supplier<DataComponentType<UUID>> ACTIVE_BEAM_ID = REGISTRY.registerComponentType(
			"active_beam_id", builder -> builder.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));

	// Carries a captured region's real StructureTemplate NBT (see StructureTemplate#save/#load) on a
	// Manipulated Matter stack - abilitech.heroAspect.space.TechSpaceManipulator's real port of
	// items.ItemManipulatedMatter, reusing vanilla's own structure-block capture/placement format
	// rather than hand-rolling block-by-block storage.
	public static final Supplier<DataComponentType<net.minecraft.nbt.CompoundTag>> MANIPULATED_MATTER = REGISTRY.registerComponentType(
			"manipulated_matter", builder -> builder
					.persistent(net.minecraft.nbt.CompoundTag.CODEC)
					.networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.TRUSTED_COMPOUND_TAG));

	private MSUItemComponents()
	{
	}
}
