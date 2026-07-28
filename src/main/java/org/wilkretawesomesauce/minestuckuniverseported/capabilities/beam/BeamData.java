package org.wilkretawesomesauce.minestuckuniverseported.capabilities.beam;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code capabilities.beam.BeamData}/{@code IBeamData} - a real
 * per-{@code Level} attachment (see
 * {@link org.wilkretawesomesauce.minestuckuniverseported.MSUAttachments#BEAM_DATA}), attached to every
 * dimension (unlike {@code GameData}/{@code MediumData}, which are deliberately Overworld/Land-only) -
 * beams can fire in any dimension, matching the original's own unconditional
 * {@code attachWorldCap(AttachCapabilitiesEvent<World>)} handling for this specific capability.
 * <p>
 * Driven by {@link BeamEvents} on both logical sides, same as the original's {@code onWorldTick}/
 * {@code onClientTick} pair. Every {@link Beam} needs a real {@code Level} reference it doesn't get handed
 * automatically by NeoForge's attachment (de)serialization hooks, unlike {@code TimelineData} (whose own
 * per-tick effects never needed to reconstruct a full object graph on load) - so unlike that class, this one
 * defers reconstructing its {@link Beam}s from NBT until a {@code Level} is actually available
 * ({@link #resolvePending}, called from every real entry point below), rather than storing a stale
 * {@code Level} reference on the attachment itself.
 */
public class BeamData implements IBeamData, INBTSerializable<CompoundTag>
{
	private final List<Beam> beams = new ArrayList<>();
	private ListTag pendingNbt;

	private void resolvePending(Level level)
	{
		if(pendingNbt == null)
			return;

		for(int i = 0; i < pendingNbt.size(); i++)
		{
			Beam beam = new Beam(level, UUID.randomUUID());
			beam.readFromNBT(pendingNbt.getCompound(i));
			beams.add(beam);
		}
		pendingNbt = null;
	}

	public void addBeam(Level level, Beam beam)
	{
		resolvePending(level);
		beams.add(beam);
	}

	public Beam getBeam(Level level, UUID id)
	{
		resolvePending(level);
		for(Beam beam : beams)
			if(beam.getUniqueID().equals(id))
				return beam;
		return null;
	}

	public List<Beam> getBeams(Level level)
	{
		resolvePending(level);
		return beams;
	}

	public void tickBeams(Level level)
	{
		resolvePending(level);

		Iterator<Beam> iterator = beams.iterator();
		while(iterator.hasNext())
		{
			Beam beam = iterator.next();
			beam.onUpdate();
			if(beam.isDead())
				iterator.remove();
		}
	}

	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider provider)
	{
		CompoundTag nbt = new CompoundTag();
		ListTag list = new ListTag();
		for(Beam beam : beams)
			list.add(beam.writeToNBT());
		nbt.put("Beams", list);
		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt)
	{
		beams.clear();
		pendingNbt = nbt.getList("Beams", Tag.TAG_COMPOUND);
	}
}
