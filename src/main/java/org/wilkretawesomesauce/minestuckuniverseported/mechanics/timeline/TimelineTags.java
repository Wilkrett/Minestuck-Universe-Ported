package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;

/**
 * The Braid-style "this doesn't rewind" idea (its own glowing-green puzzle pieces/characters that stay put
 * while everything else plays backward around them), reworked from an earlier per-instance-attachment/item
 * design into a real, data-driven, effectively-zero-runtime-cost tag system - same
 * {@code data/<namespace>/tags/<registry>/<path>.json} idiom {@link org.wilkretawesomesauce.minestuckuniverseported.strife.MSUKindAbstrata}
 * already established for kinds, applied here instead of a bespoke per-entity attachment.
 * <p>
 * <b>Why the rework, honestly stated:</b> the first version of this feature ({@code TimelineImmunityData},
 * a per-{@code LivingEntity} attachment toggled by a now-deleted {@code TemporalAnchorItem}) had a real,
 * confirmed memory problem: {@link TimelineManager#applySnapshot}/{@code loop.TimeLoopReplay} had to call
 * {@code entity.getData(...)} <i>unconditionally</i> on every entity a rewind ever touched to check
 * immunity, and NeoForge attachments lazily-but-permanently materialize (and then serialize, forever, into
 * that entity's own save data) their default value on first access - so every entity ever caught in a
 * rewind, whether anyone had ever actually marked it or not, would silently pick up a permanent extra
 * object and NBT tag for the rest of its life. A vanilla {@code TagKey} costs none of that: block/entity-
 * type/item membership is resolved once (into a cached {@code HolderSet}) when tags load, and a membership
 * check ({@link net.minecraft.world.level.block.state.BlockState#is(TagKey)}/
 * {@link net.minecraft.world.entity.EntityType#is(TagKey)}/{@link net.minecraft.world.item.ItemStack#is(TagKey)})
 * is a cheap lookup against that shared set - nothing per-instance is ever allocated.
 * <p>
 * <b>Instance-level marking</b> (a single specific mob/player/item, not a whole type) doesn't need new
 * infrastructure either, for the same reason: {@link net.minecraft.world.entity.Entity#getTags()} is a
 * plain {@code Set<String>} vanilla already carries on every entity (used by the vanilla {@code /tag}
 * command) and only ever gets serialized when non-empty - reusing it via {@link #IMMUNE_ENTITY_TAG} costs
 * literally nothing beyond vanilla's own baseline for entities nobody ever tags. The item-instance
 * equivalent is {@link org.wilkretawesomesauce.minestuckuniverseported.MSUItemComponents#TIMELINE_IMMUNE} -
 * a persistent data component, absent (and therefore free) on every stack that never opts in, settable
 * directly through vanilla's own item-component command/give syntax with no new command needed.
 * <p>
 * See {@link TimelineManager#applySnapshot}/{@code loop.TimeLoopReplay} for where blocks/entities are
 * excluded, and {@link EntitySnapshot#applyTo} for where an immune item keeps its equipment slot from being
 * overwritten.
 */
public final class TimelineTags
{
	private TimelineTags()
	{
	}

	/** {@code #minestuckuniverseported:timeline_immune} block tag - a whole block type that never rewinds. */
	public static final TagKey<Block> IMMUNE_BLOCKS = TagKey.create(Registries.BLOCK, id());

	/** {@code #minestuckuniverseported:timeline_immune} entity type tag - a whole entity type that never rewinds. */
	public static final TagKey<EntityType<?>> IMMUNE_ENTITY_TYPES = TagKey.create(Registries.ENTITY_TYPE, id());

	/** {@code #minestuckuniverseported:timeline_immune} item tag - holding/wearing one of these keeps that equipment slot from rewinding. */
	public static final TagKey<Item> IMMUNE_ITEMS = TagKey.create(Registries.ITEM, id());

	/**
	 * The vanilla scoreboard-style entity tag name for marking one specific entity/player immune, e.g.
	 * {@code /tag @s add minestuckuniverseported_timeline_immune} - no custom command needed, this is
	 * exactly what {@link net.minecraft.world.entity.Entity#addTag}/{@code removeTag}/{@code getTags} already are.
	 */
	public static final String IMMUNE_ENTITY_TAG = "minestuckuniverseported_timeline_immune";

	private static net.minecraft.resources.ResourceLocation id()
	{
		return Minestuckuniverseported.id("timeline_immune");
	}
}
