package org.wilkretawesomesauce.minestuckuniverseported.badges;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code skills.badges.BadgeLevel} - gates <i>readability</i>
 * (not unlock) behind {@code godtier.GodTierData}'s own real skill-level field. See that class's own doc
 * comment for why this project tracks a single flat level rather than the original's real per-{@code
 * StatType} map (only {@code StatType.GENERAL} is ever consumed anywhere reachable in this project).
 */
public class BadgeLevel extends Badge
{
	public final int requiredLevel;

	protected BadgeLevel(ResourceLocation id, int requiredLevel)
	{
		super(id);
		this.requiredLevel = requiredLevel;
	}

	@Override
	public Component getReadRequirements()
	{
		return Component.translatable("badge.level.read", requiredLevel);
	}

	@Override
	public boolean isReadable(Level level, Player player)
	{
		return !(player instanceof ServerPlayer serverPlayer) || serverPlayer.getData(MSUAttachments.GOD_TIER).getSkillLevel() >= requiredLevel;
	}
}
