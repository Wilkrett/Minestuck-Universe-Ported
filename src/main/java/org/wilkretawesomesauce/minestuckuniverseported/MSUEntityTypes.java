package org.wilkretawesomesauce.minestuckuniverseported;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.wilkretawesomesauce.minestuckuniverseported.entity.BubbleEntity;
import org.wilkretawesomesauce.minestuckuniverseported.entity.HopeGolemEntity;
import org.wilkretawesomesauce.minestuckuniverseported.entity.MSUThrowableEntity;

public final class MSUEntityTypes
{
	public static final DeferredRegister<EntityType<?>> REGISTER = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Minestuckuniverseported.MODID);

	/** Ported from MinestuckUniverse (1.12.2)'s {@code entity.EntityBubble} - see {@link BubbleEntity}'s own doc comment. */
	public static final DeferredHolder<EntityType<?>, EntityType<BubbleEntity>> BUBBLE = REGISTER.register("bubble",
			() -> EntityType.Builder.<BubbleEntity>of(BubbleEntity::new, MobCategory.MISC)
					.sized(3.0F, 3.0F)
					.noSave()
					.clientTrackingRange(10)
					.updateInterval(20)
					.build(Minestuckuniverseported.id("bubble").toString()));

	/** Ported from MinestuckUniverse (1.12.2)'s {@code entity.EntityHopeGolem} - see {@link HopeGolemEntity}'s own doc comment. Sized the same as vanilla's own Iron Golem. */
	public static final DeferredHolder<EntityType<?>, EntityType<HopeGolemEntity>> HOPE_GOLEM = REGISTER.register("hope_golem",
			() -> EntityType.Builder.of(HopeGolemEntity::new, MobCategory.MISC)
					.sized(1.4F, 2.7F)
					.clientTrackingRange(10)
					.build(Minestuckuniverseported.id("hope_golem").toString()));

	/** Ported (partially) from MinestuckUniverse (1.12.2)'s {@code entity.EntityMSUThrowable} - see {@link MSUThrowableEntity}'s own doc comment. */
	public static final DeferredHolder<EntityType<?>, EntityType<MSUThrowableEntity>> MSU_THROWABLE = REGISTER.register("msu_throwable",
			() -> EntityType.Builder.<MSUThrowableEntity>of(MSUThrowableEntity::new, MobCategory.MISC)
					.sized(0.25F, 0.25F)
					.clientTrackingRange(4)
					.updateInterval(10)
					.build(Minestuckuniverseported.id("msu_throwable").toString()));

	private MSUEntityTypes()
	{
	}
}
