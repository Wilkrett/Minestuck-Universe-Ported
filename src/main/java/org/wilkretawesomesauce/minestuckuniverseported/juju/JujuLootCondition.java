package org.wilkretawesomesauce.minestuckuniverseported.juju;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;
import org.wilkretawesomesauce.minestuckuniverseported.skills.MSUSkills;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.game.GameData;

/**
 * Ported 1:1 from MinestuckUniverse (1.12.2)'s
 * {@code world.storage.loot.conditions.JujuLootCondition} - a real, reusable, data-driven loot
 * condition (registered as {@code minestuckuniverseported:juju}, matching the original's own
 * {@code minestuckuniverse:juju}), not hardcoded to one item: {@code chance}/{@code juju} are read
 * straight from whatever loot JSON uses it. Gated by {@link GameData}'s spawn-tracking (the other
 * half of the original's {@code capabilities.game.GameData}, see that class's own doc comment) so a
 * given Juju item only ever drops once per world.
 * <p>
 * The killer's real chance multiplier for the Light aspect's "Skaian Insight" passive
 * ({@code abilitech.heroAspect.light.TechLightInsight}) is real here too - it only ever needed "is this
 * tech currently equipped", which this project already tracks via {@link GodTierData}, not the
 * skills/badges level-up economy that gated most of the rest of the original's skill tree. Reads the
 * real killer off {@link LootContextParams#LAST_DAMAGE_PLAYER}, matching the original's own killer-aware
 * loot injection.
 */
public record JujuLootCondition(float chance, Item juju) implements LootItemCondition
{
	private static final float SKAIAN_INSIGHT_MULTIPLIER = 5.0F;

	public static final MapCodec<JujuLootCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			com.mojang.serialization.Codec.FLOAT.fieldOf("chance").forGetter(JujuLootCondition::chance),
			BuiltInRegistries.ITEM.byNameCodec().fieldOf("juju").forGetter(JujuLootCondition::juju)
	).apply(instance, JujuLootCondition::new));

	@Override
	public LootItemConditionType getType()
	{
		return MSUJujuRegistry.JUJU_CONDITION.get();
	}

	@Override
	public boolean test(LootContext context)
	{
		GameData data = context.getLevel().getData(MSUAttachments.ITEM_VOID);
		if(data.hasJujuSpawned(juju))
			return false;

		float effectiveChance = chance;
		Player killer = context.getParamOrNull(LootContextParams.LAST_DAMAGE_PLAYER);
		if(killer != null)
		{
			GodTierData godTier = killer.getData(MSUAttachments.GOD_TIER);
			if(godTier.isTechEquipped(MSUSkills.LIGHT_INSIGHT))
				effectiveChance *= SKAIAN_INSIGHT_MULTIPLIER;
		}

		if(context.getRandom().nextFloat() <= effectiveChance)
		{
			data.setJujuSpawned(juju, true);
			return true;
		}
		return false;
	}
}
