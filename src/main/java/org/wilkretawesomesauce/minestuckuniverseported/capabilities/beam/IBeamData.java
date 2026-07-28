package org.wilkretawesomesauce.minestuckuniverseported.capabilities.beam;

import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code capabilities.beam.IBeamData} - the original also
 * extended {@code capabilities.IMSUCapabilityBase<World>}; not repeated here for the same reason every
 * other real capability interface in this project's {@code capabilities} package skips it (NeoForge's own
 * {@code INBTSerializable}, which {@link BeamData} already implements directly, needs no marker
 * interface). Every method here gained an explicit {@link Level} parameter the original didn't need -
 * see {@link BeamData}'s own doc comment for why (reconstructing a {@link Beam} from NBT needs a real
 * {@code Level} reference this attachment isn't handed automatically on load).
 */
public interface IBeamData
{
	List<Beam> getBeams(Level level);

	Beam getBeam(Level level, UUID beamId);

	void addBeam(Level level, Beam beam);

	void tickBeams(Level level);
}
