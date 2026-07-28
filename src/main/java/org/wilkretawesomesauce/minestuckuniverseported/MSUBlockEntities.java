package org.wilkretawesomesauce.minestuckuniverseported;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.wilkretawesomesauce.minestuckuniverseported.blockentity.TemporalSendificatorBlockEntity;

/** Block entity type registry - starts with just what the Temporal Sendificator needs (see {@code CLAUDE.md}'s Time Request / Doom System section). */
public final class MSUBlockEntities
{
	public static final DeferredRegister<BlockEntityType<?>> REGISTER =
			DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Minestuckuniverseported.MODID);

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TemporalSendificatorBlockEntity>> TEMPORAL_SENDIFICATOR =
			REGISTER.register("temporal_sendificator", () -> BlockEntityType.Builder.of(
					TemporalSendificatorBlockEntity::new, MSUBlocks.TEMPORAL_SENDIFICATOR.get()).build(null));

	private MSUBlockEntities()
	{
	}
}
