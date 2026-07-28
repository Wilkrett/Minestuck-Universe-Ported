package org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code capabilities.keyStates.ISkillKeyStates} - the original
 * also extended {@code capabilities.IMSUCapabilityBase<EntityPlayer>}; not repeated here for the same
 * reason every other real capability interface in this project's {@code capabilities} package skips it
 * (NeoForge's own {@code INBTSerializable}, which {@link SkillKeyStates} already implements directly,
 * needs no marker interface).
 */
public interface ISkillKeyStates
{
	void updateKeyState(AbilitechKey key, boolean pressed);

	int getKeyTime(AbilitechKey key);

	AbilitechKeyState getKeyState(AbilitechKey key);

	void tickKeyStates();

	void resetKeyStates();
}
