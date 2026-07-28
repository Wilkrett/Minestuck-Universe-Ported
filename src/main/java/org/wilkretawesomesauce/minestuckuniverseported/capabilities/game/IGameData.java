package org.wilkretawesomesauce.minestuckuniverseported.capabilities.game;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code capabilities.game.IGameData} - empty in the original
 * too (just {@code extends IMSUCapabilityBase<World>}, itself only {@code writeToNBT}/{@code readFromNBT}/
 * {@code setOwner}, none of which NeoForge's own {@code INBTSerializable} needs a marker interface for).
 * The original's real {@code GameData} API ({@code getItemVoid}/{@code addItemToVoid}/
 * {@code hasJujuSpawned}/{@code setJujuSpawned}) was always exposed as plain static methods on the class
 * itself, never through this interface - {@link GameData} matches that shape.
 */
public interface IGameData
{
}
