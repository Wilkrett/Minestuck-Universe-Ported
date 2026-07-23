package org.wilkretawesomesauce.minestuckuniverseported;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.wilkretawesomesauce.minestuckuniverseported.block.ChloroballBlock;
import org.wilkretawesomesauce.minestuckuniverseported.block.TemporalSendificatorBlock;
import org.wilkretawesomesauce.minestuckuniverseported.blocks.AbilitechnosynthBlock;

public final class MSUBlocks
{
	public static final DeferredRegister.Blocks REGISTER = DeferredRegister.createBlocks(Minestuckuniverseported.MODID);

	public static final DeferredBlock<Block> ABILITECHNOSYNTH = REGISTER.register("abilitechnosynth",
			() -> new AbilitechnosynthBlock(BlockBehaviour.Properties.of()
					.mapColor(MapColor.METAL)
					.strength(3.5F)
					.sound(SoundType.METAL)
					.requiresCorrectToolForDrops()
					.noOcclusion()));

	public static final DeferredBlock<Block> TEMPORAL_SENDIFICATOR = REGISTER.register("temporal_sendificator",
			() -> new TemporalSendificatorBlock(BlockBehaviour.Properties.of()
					.mapColor(MapColor.METAL)
					.strength(3.5F)
					.sound(SoundType.METAL)
					.requiresCorrectToolForDrops()));

	public static final DeferredBlock<Block> CHLOROBALL = REGISTER.register("chloroball",
			() -> new ChloroballBlock(BlockBehaviour.Properties.of()
					.mapColor(MapColor.PLANT)
					.noCollission()
					.noOcclusion()
					.instabreak()
					.lightLevel(state -> 6)
					.sound(SoundType.GLASS)));

	private MSUBlocks()
	{
	}
}
