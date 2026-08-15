package org.wilkretawesomesauce.minestuckuniverseported;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.wilkretawesomesauce.minestuckuniverseported.entity.BubbleEntity;
import org.wilkretawesomesauce.minestuckuniverseported.entity.GolemBoulderEntity;
import org.wilkretawesomesauce.minestuckuniverseported.entity.GolemEggEntity;
import org.wilkretawesomesauce.minestuckuniverseported.entity.GolemEntity;
import org.wilkretawesomesauce.minestuckuniverseported.entity.GolemFallingBlockEntity;
import org.wilkretawesomesauce.minestuckuniverseported.entity.HopeGolemEntity;
import org.wilkretawesomesauce.minestuckuniverseported.entity.MSUThrowableEntity;
import org.wilkretawesomesauce.minestuckuniverseported.entity.TornadoEntity;

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

	/** Ported from ModularBosses (1.8)'s {@code entity.EntityGolem} - see {@link GolemEntity}'s own doc comment. Hitbox matches the original's {@code setSize(2F, 6.5F)}. */
	public static final DeferredHolder<EntityType<?>, EntityType<GolemEntity>> GOLEM = REGISTER.register("golem",
			() -> EntityType.Builder.of(GolemEntity::new, MobCategory.MONSTER)
					.sized(2.0F, 6.5F)
					.clientTrackingRange(10)
					.build(Minestuckuniverseported.id("golem").toString()));

	/** Ported from ModularBosses (1.8)'s {@code entity.projectile.EntityBoulder} - see {@link GolemBoulderEntity}'s own doc comment. Sized to match the original's own {@code setSize(1, 1)} call in its target-throwing constructor. */
	public static final DeferredHolder<EntityType<?>, EntityType<GolemBoulderEntity>> GOLEM_BOULDER = REGISTER.register("golem_boulder",
			() -> EntityType.Builder.<GolemBoulderEntity>of(GolemBoulderEntity::new, MobCategory.MISC)
					.sized(1.0F, 1.0F)
					.clientTrackingRange(6)
					.updateInterval(10)
					.build(Minestuckuniverseported.id("golem_boulder").toString()));

	/** Ported from ModularBosses (1.8)'s {@code entity.projectile.EntityCustomEgg} - see {@link GolemEggEntity}'s own doc comment. Sized like vanilla's own thrown egg. */
	public static final DeferredHolder<EntityType<?>, EntityType<GolemEggEntity>> GOLEM_EGG = REGISTER.register("golem_egg",
			() -> EntityType.Builder.<GolemEggEntity>of(GolemEggEntity::new, MobCategory.MISC)
					.sized(0.25F, 0.25F)
					.clientTrackingRange(4)
					.updateInterval(10)
					.build(Minestuckuniverseported.id("golem_egg").toString()));

	/** Ported from ModularBosses (1.8)'s {@code entity.projectile.EntityCustomFallingBlock} - see {@link GolemFallingBlockEntity}'s own doc comment. Sized like a normal block. */
	public static final DeferredHolder<EntityType<?>, EntityType<GolemFallingBlockEntity>> GOLEM_FALLING_BLOCK = REGISTER.register("golem_falling_block",
			() -> EntityType.Builder.<GolemFallingBlockEntity>of(GolemFallingBlockEntity::new, MobCategory.MISC)
					.sized(1.0F, 1.0F)
					.clientTrackingRange(6)
					.updateInterval(10)
					.build(Minestuckuniverseported.id("golem_falling_block").toString()));

	/** Original design for this project, no 1.12.2 counterpart - see {@link TornadoEntity}'s own doc comment. Purely cosmetic, transient (never saved), sized to roughly cover the funnel's default footprint/height. */
	public static final DeferredHolder<EntityType<?>, EntityType<TornadoEntity>> TORNADO = REGISTER.register("tornado",
			() -> EntityType.Builder.of(TornadoEntity::new, MobCategory.MISC)
					.sized(2.2F, 3.5F)
					.noSave()
					.clientTrackingRange(10)
					.updateInterval(20)
					.build(Minestuckuniverseported.id("tornado").toString()));

	private MSUEntityTypes()
	{
	}
}
