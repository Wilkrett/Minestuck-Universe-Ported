package org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

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
 */
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
}
