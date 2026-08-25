package org.wilkretawesomesauce.minestuckuniverseported;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.wilkretawesomesauce.minestuckuniverseported.item.JukinatorDisc;
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

	// Stamps the randomly-rolled Abilitech id onto a Skaian Scroll stack at loot-roll time - see
	// item.SkaianScrollItem's own doc comment. Rolled once (loot modifier), read on every right-click and
	// in the tooltip; never rerolled after that, matching the original's own stack-NBT "Skill" string tag.
	public static final Supplier<DataComponentType<net.minecraft.resources.ResourceLocation>> SKAIAN_SCROLL_TECH = REGISTRY.registerComponentType(
			"skaian_scroll_tech", builder -> builder
					.persistent(net.minecraft.resources.ResourceLocation.CODEC)
					.networkSynchronized(net.minecraft.resources.ResourceLocation.STREAM_CODEC));

	// Real port of ItemSkaianScroll's own "Super" NBT flag - a scroll that bypasses Config#skaiaScrollLimit
	// entirely and renders with an enchant glint (see SkaianScrollItem#isFoil). Never set by the dungeon
	// loot modifier itself (matching the original, where SetRandomSkill never set it either) - reserved for
	// any future command/creative-only source, same as the original's own scope.
	public static final Supplier<DataComponentType<Boolean>> SKAIAN_SCROLL_SUPER = REGISTRY.registerComponentType(
			"skaian_scroll_super", builder -> builder
					.persistent(com.mojang.serialization.Codec.BOOL)
					.networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL));

	// The disc currently loaded into a Jukinator-3000 (item.JukinatorItem) - absence of this component
	// means no disc is loaded, no empty-stack sentinel needed. Wrapped in item.JukinatorDisc rather than a
	// bare ItemStack - see that class's own doc comment for the real runtime error (confirmed, not
	// guessed) that made the wrapper necessary: ItemStack itself doesn't implement equals()/hashCode().
	public static final Supplier<DataComponentType<JukinatorDisc>> STORED_DISC = REGISTRY.registerComponentType(
			"stored_disc", builder -> builder.persistent(JukinatorDisc.CODEC).networkSynchronized(JukinatorDisc.STREAM_CODEC));

	private MSUItemComponents()
	{
	}
}
