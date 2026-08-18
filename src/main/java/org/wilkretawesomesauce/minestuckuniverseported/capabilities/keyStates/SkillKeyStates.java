package org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates;

import com.mraof.minestuck.computer.editmode.ServerEditHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.Abilitech;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code capabilities.keyStates.SkillKeyStates} - the real
 * activation input state machine, not just its two nested enums ({@link AbilitechKey}/
 * {@link AbilitechKeyState}, already real here - see that package's own doc comment). This class's own
 * logic briefly lived merged into {@code skills.abilitech.AbilitechLoadout} instead (alongside the
 * unrelated tech-equip-slot/badgeEffects-derived fields, both of which already moved back to their own
 * real homes - see {@code capabilities.godTier.GodTierData}/{@code capabilities.badgeEffects.BadgeEffects}) -
 * moved back here for real, closing out the last of the three original capabilities that got bundled into
 * one NeoForge attachment.
 * <p>
 * Real NBT persistence, matching the original's own {@code writeToNBT}/{@code readFromNBT} (this project's
 * earlier merged version never actually exercised this - {@code deserializeNBT} unconditionally called
 * {@link #resetKeyStates()} regardless of what NBT it was handed, silently discarding any prior state
 * every load). Restored for real here rather than kept vestigial, matching every other attachment in this
 * project's own {@code util.MSUAttachments} registry (all real {@code .serializable()} attachments, no
 * exceptions) - a relogged player's mid-press key state surviving a save/load is a harmless real behavior
 * to have back, not a risk.
 * <p>
 * {@link #onEntityJoinLevel}/{@link #onPlayerTick} are ported directly onto this class - the original's own
 * {@code onWorldJoin}/{@code onPlayerTick} live on {@code SkillKeyStates} itself, not a separate
 * {@code AbilitechEvents}-style file (that class never existed in the original; deleted). The
 * login/respawn client sync for {@link GodTierData} (which this key-state machine has nothing to do with)
 * is genuinely new NeoForge-only plumbing with no original counterpart at all - see that class's own
 * doc comment for why it lives there instead.
 */
@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME)
public class SkillKeyStates implements ISkillKeyStates, INBTSerializable<CompoundTag>
{
	public static final int SLOTS = 3;

	private final AbilitechKeyState[] receivedKeyStates = new AbilitechKeyState[SLOTS];
	private final AbilitechKeyState[] keyStates = new AbilitechKeyState[SLOTS];
	private final int[] keyTimes = new int[SLOTS];

	public SkillKeyStates()
	{
		resetKeyStates();
	}

	/** Call with the raw client-reported press/release state; the actual {@link AbilitechKeyState} only
	 * advances one step per {@link #tickKeyStates()} call, same as the original. */
	public void updateKeyState(AbilitechKey key, boolean pressed)
	{
		int i = key.ordinal();
		if(pressed && receivedKeyStates[i] != AbilitechKeyState.HELD)
			receivedKeyStates[i] = AbilitechKeyState.PRESS;
		else if(!pressed && receivedKeyStates[i] != AbilitechKeyState.NONE)
			receivedKeyStates[i] = AbilitechKeyState.RELEASED;
	}

	public AbilitechKeyState getKeyState(AbilitechKey key)
	{
		return keyStates[key.ordinal()];
	}

	public int getKeyTime(AbilitechKey key)
	{
		return keyTimes[key.ordinal()];
	}

	/** Advances the key state machine by one tick; call once per server player tick. */
	public void tickKeyStates()
	{
		for(int i = 0; i < SLOTS; i++)
		{
			if(receivedKeyStates[i] == AbilitechKeyState.PRESS)
				receivedKeyStates[i] = AbilitechKeyState.HELD;
			else if(receivedKeyStates[i] == AbilitechKeyState.RELEASED)
				receivedKeyStates[i] = AbilitechKeyState.NONE;

			if(keyStates[i] != receivedKeyStates[i])
				keyStates[i] = AbilitechKeyState.values()[(keyStates[i].ordinal() + 1) % AbilitechKeyState.values().length];

			if(keyStates[i] == AbilitechKeyState.PRESS)
				keyTimes[i] = 0;
			else
				keyTimes[i]++;
		}
	}

	public void resetKeyStates()
	{
		for(int i = 0; i < SLOTS; i++)
		{
			receivedKeyStates[i] = AbilitechKeyState.NONE;
			keyStates[i] = AbilitechKeyState.NONE;
			keyTimes[i] = 0;
		}
	}

	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider provider)
	{
		CompoundTag nbt = new CompoundTag();
		for(int i = 0; i < SLOTS; i++)
		{
			nbt.putInt(i + "Received", receivedKeyStates[i].ordinal());
			nbt.putInt(i + "State", keyStates[i].ordinal());
			nbt.putInt(i + "Time", keyTimes[i]);
		}
		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt)
	{
		for(int i = 0; i < SLOTS; i++)
		{
			receivedKeyStates[i] = nbt.contains(i + "Received") ? AbilitechKeyState.values()[nbt.getInt(i + "Received")] : AbilitechKeyState.NONE;
			keyStates[i] = nbt.contains(i + "State") ? AbilitechKeyState.values()[nbt.getInt(i + "State")] : AbilitechKeyState.NONE;
			keyTimes[i] = nbt.getInt(i + "Time");
		}
	}

	/** Ported from the original's own {@code SkillKeyStates.onWorldJoin}. */
	@SubscribeEvent
	private static void onEntityJoinLevel(EntityJoinLevelEvent event)
	{
		if(!event.getLevel().isClientSide() && event.getEntity() instanceof ServerPlayer player)
			player.getData(MSUAttachments.SKILL_KEY_STATES).resetKeyStates();
	}

	/**
	 * Ported from the original's own {@code SkillKeyStates.onPlayerTick} - drives every equipped tech's
	 * {@code onUseTick}/{@code onPassiveTick} off this player's own key-state machine, then advances that
	 * machine by one tick. Simplification from the original: the {@code IBadgeEffects}-based checks (skip
	 * ticking while time-stopped or soul-shocked) aren't ported - those specific fields were deliberately
	 * not among the handful pulled into this project's own {@code capabilities.badgeEffects.BadgeEffects}
	 * (see that class's own doc comment), since time-stop/soul-shock are already dedicated marker
	 * {@code MobEffect}s elsewhere and re-checking them here would be redundant with how those effects
	 * already gate behavior at their own call sites. The "not in edit mode" and "not spectator/dead" guards
	 * are kept - those don't depend on anything out of scope.
	 */
	@SubscribeEvent
	private static void onPlayerTick(PlayerTickEvent.Post event)
	{
		if(event.getEntity().level().isClientSide())
			return;
		if(!(event.getEntity() instanceof ServerPlayer player))
			return;

		SkillKeyStates keyStates = player.getData(MSUAttachments.SKILL_KEY_STATES);
		GodTierData godTier = player.getData(MSUAttachments.GOD_TIER);

		boolean canAct = !player.isSpectator() && player.isAlive() && ServerEditHandler.getData(player) == null;

		for(AbilitechKey key : AbilitechKey.values())
		{
			Abilitech tech = godTier.getTech(key.ordinal());
			if(tech == null)
				continue;

			if(canAct && tech.canUse(player.level(), player))
			{
				tech.onUseTick(player.level(), player, key.ordinal(), keyStates.getKeyState(key), keyStates.getKeyTime(key));

				if(godTier.isPassiveEnabled(key.ordinal()))
					tech.onPassiveTick(player.level(), player, key.ordinal());
			}
		}

		keyStates.tickKeyStates();
	}
}
