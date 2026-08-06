package org.wilkretawesomesauce.minestuckuniverseported.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.MSUEntityTypes;
import org.wilkretawesomesauce.minestuckuniverseported.entity.GolemEggEntity;
import org.wilkretawesomesauce.minestuckuniverseported.entity.GolemEntity;

/**
 * Ported from ModularBosses (1.8)'s {@code items.ItemCustomEgg}/{@code items.dispenser.BehaviorDispenseCustomMobEgg} -
 * a real throwable spawn egg, not vanilla's own instant-place {@code SpawnEggItem}. Two distinct
 * behaviors, matching the original's own {@code onItemUse}/{@code onItemRightClick} split:
 * <ul>
 * <li>{@link #useOn} (right-clicking a block within reach) instantly spawns the golem on top of/against
 * that block - the original's own {@code onItemUse}.</li>
 * <li>{@link #use} (right-clicking with nothing in range, i.e. thrown) launches a real
 * {@link GolemEggEntity} projectile that spawns the golem wherever it lands - the original's own
 * {@code onItemRightClick}, modeled directly on vanilla's {@code EggItem}/{@code ThrownEgg} pair.</li>
 * </ul>
 * Implements {@link ProjectileItem} (again mirroring vanilla's own {@code EggItem}) so a dispenser can
 * throw one too, once registered - see this class's own registration call site for the real
 * {@code DispenserBlock.registerBehavior} equivalent of the original's dedicated dispenser behavior class.
 */
public class GolemSpawnEggItem extends Item implements ProjectileItem
{
	public GolemSpawnEggItem(Properties properties)
	{
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context)
	{
		Level level = context.getLevel();
		if(level.isClientSide())
			return InteractionResult.SUCCESS;

		BlockPos spawnPos = context.getClickedPos().relative(context.getClickedFace());

		GolemEntity golem = MSUEntityTypes.GOLEM.get().create(level);
		if(golem == null)
			return InteractionResult.FAIL;

		golem.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
				Mth.wrapDegrees(level.getRandom().nextFloat() * 360.0F), 0.0F);
		level.addFreshEntity(golem);

		ItemStack stack = context.getItemInHand();
		Player player = context.getPlayer();
		if(player != null && !player.hasInfiniteMaterials())
			stack.shrink(1);

		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack stack = player.getItemInHand(hand);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EGG_THROW, SoundSource.PLAYERS,
				0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

		if(!level.isClientSide())
		{
			GolemEggEntity egg = new GolemEggEntity(level, player);
			egg.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
			level.addFreshEntity(egg);
		}

		player.awardStat(Stats.ITEM_USED.get(this));
		stack.consume(1, player);
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	@Override
	public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction)
	{
		return new GolemEggEntity(level, pos.x(), pos.y(), pos.z());
	}
}
