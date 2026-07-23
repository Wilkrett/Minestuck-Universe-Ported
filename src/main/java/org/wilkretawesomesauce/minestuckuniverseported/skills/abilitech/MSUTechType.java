package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech;

/** Ported from MinestuckUniverse (1.12.2)'s {@code util.EnumTechType}. */
public enum MSUTechType
{
	OFFENSE("techType.offense", 0xFFAA66),
	DEFENSE("techType.defense", 0xC3D3D8),
	UTILITY("techType.utility", 0x66FF6D),
	PASSIVE("techType.passive", 0x66FFE8),
	HYBRID("techType.hybrid", 0xFFC300);

	public final String unloc;
	public final int color;

	MSUTechType(String unloc, int color)
	{
		this.unloc = unloc;
		this.color = color;
	}
}
