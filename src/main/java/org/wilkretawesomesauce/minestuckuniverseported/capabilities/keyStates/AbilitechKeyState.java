package org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates;

/**
 * Ported from MinestuckUniverse's {@code SkillKeyStates.KeyState}. Note this is the *derived* state
 * ({@link AbilitechLoadout} ticks it one step at a time toward the raw received press/release state,
 * exactly like the original), not the raw input - that's what makes {@code PRESS} and {@code RELEASED}
 * reliably last exactly one tick each regardless of how long the key is actually held/released for.
 */
public enum AbilitechKeyState
{
	PRESS,
	HELD,
	RELEASED,
	NONE
}
