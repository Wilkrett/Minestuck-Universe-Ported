package org.wilkretawesomesauce.minestuckuniverseported.network;

import com.mraof.minestuck.network.MSPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.badgeEffects.BadgeEffects;
import org.wilkretawesomesauce.minestuckuniverseported.entity.ai.EntityAIMindflayerTarget;

/**
 * Client -&gt; server half of {@code abilitech.heroAspect.mind.TechMindControl} ("Mindflayer's Spell")'s
 * movement puppeting - the modern equivalent of the original's own
 * {@code network.PacketMindflayerMovementInput}. Sent every client tick by
 * {@code TechMindControl.ClientEvents} while the controller carries
 * {@code abilitech.heroAspect.mind.TechMindControl.MindControllingEffect} (added for either a mob or a
 * player target - see that tech's own doc comment), carrying their own movement input already converted to
 * a world-relative vector (via their own head yaw) exactly like the original did before handing it to the
 * server.
 * <p>
 * <b>Real bug fix</b>: this used to look up the target by scanning the sender's own {@code GodTierData}
 * loadout slots for one currently holding {@code TechMindControl}, then reading that slot's entry out of
 * the generic per-slot {@link BadgeEffects#getTether}. The original does neither - it reads a single
 * dedicated {@code IBadgeEffects#getMindflayerEntity()} field directly, no slot/loadout involvement at all
 * (matching {@link BadgeEffects#getMindflayerEntity()}'s own doc comment). That mismatch was harmless for a
 * possessed player (the loop still found the right tether eventually), but for a possessed {@link Mob} it
 * was worse than just indirect: the mob-target code path never sent this packet in the first place (the
 * controller only ever got {@code MindControllingEffect} for a player target), so a mind-controlled mob
 * never received any movement input at all. Fixed by reading {@link BadgeEffects#getMindflayerEntity()}
 * directly (real single-field lookup, matching the original exactly) and, for a {@link Mob} target, relaying
 * straight into its own {@link EntityAIMindflayerTarget#setMove} - the original's real
 * {@code EntityAITasks.EntityAITaskEntry}/{@code setMove} relay, not a server-side raytrace.
 */
public record MindflayerMovementInputPacket(float worldX, float worldZ, boolean jump, boolean sneak) implements MSPacket.PlayToServer
{
	public static final Type<MindflayerMovementInputPacket> ID = new Type<>(Minestuckuniverseported.id("mind/control_input"));
	public static final StreamCodec<RegistryFriendlyByteBuf, MindflayerMovementInputPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, MindflayerMovementInputPacket::worldX,
			ByteBufCodecs.FLOAT, MindflayerMovementInputPacket::worldZ,
			ByteBufCodecs.BOOL, MindflayerMovementInputPacket::jump,
			ByteBufCodecs.BOOL, MindflayerMovementInputPacket::sneak,
			MindflayerMovementInputPacket::new
	);

	@Override
	public void execute(IPayloadContext context, ServerPlayer player)
	{
		Entity target = player.getData(MSUAttachments.BADGE_EFFECTS).getMindflayerEntity();

		if(target instanceof Mob mob)
		{
			for(WrappedGoal goal : mob.goalSelector.getAvailableGoals())
				if(goal.getGoal() instanceof EntityAIMindflayerTarget flayer)
					flayer.setMove(worldX, worldZ);
		}
		else if(target instanceof ServerPlayer possessed)
		{
			PacketDistributor.sendToPlayer(possessed, new MindflayerMovementSyncPacket(true, worldX, worldZ, jump, sneak));
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
