package org.wilkretawesomesauce.minestuckuniverseported.item;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Wraps a single {@link ItemStack} for use as {@code MSUItemComponents#STORED_DISC}'s value type.
 * {@code ItemStack} deliberately doesn't implement {@code equals()}/{@code hashCode()} in modern
 * Minecraft (confirmed for real, not guessed - a live NeoForge runtime error: "Data components must
 * implement equals and hashCode... Problematic class: ItemStack"), so it can't be a
 * {@code DataComponentType}'s {@code T} directly. Real vanilla precedent for this exact problem:
 * {@code BundleContents} wraps its own {@code List<ItemStack>} the same way, implementing equals/hashCode
 * via {@link ItemStack}'s own real static {@code listMatches}/{@code hashStackList} helpers rather than
 * hand-rolled comparison logic - this does the same, just for one stack instead of a list.
 */
public record JukinatorDisc(ItemStack disc)
{
	public static final JukinatorDisc EMPTY = new JukinatorDisc(ItemStack.EMPTY);

	public static final Codec<JukinatorDisc> CODEC = ItemStack.CODEC.xmap(JukinatorDisc::new, JukinatorDisc::disc);
	public static final StreamCodec<RegistryFriendlyByteBuf, JukinatorDisc> STREAM_CODEC =
			ItemStack.STREAM_CODEC.map(JukinatorDisc::new, JukinatorDisc::disc);

	@Override
	public boolean equals(Object other)
	{
		return this == other || (other instanceof JukinatorDisc that && ItemStack.matches(disc, that.disc));
	}

	@Override
	public int hashCode()
	{
		return ItemStack.hashStackList(List.of(disc));
	}
}
