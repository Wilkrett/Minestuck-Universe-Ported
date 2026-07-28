package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.mind;

/**
 * Client-only holder for whatever {@link org.wilkretawesomesauce.minestuckuniverseported.network.MindControlSyncPacket}
 * most recently told this client - i.e. whether (and how) the local player is currently being puppeted
 * by someone else's {@code TechMindControl} ("Mindflayer's Spell"). Read by
 * {@link MindControlClientEvents}'s target-side hook every {@code MovementInputUpdateEvent}.
 */
public final class MindControlClientState
{
	private static boolean active;
	private static float worldX;
	private static float worldZ;
	private static boolean jump;
	private static boolean sneak;

	private MindControlClientState()
	{
	}

	public static void update(boolean active, float worldX, float worldZ, boolean jump, boolean sneak)
	{
		MindControlClientState.active = active;
		MindControlClientState.worldX = worldX;
		MindControlClientState.worldZ = worldZ;
		MindControlClientState.jump = jump;
		MindControlClientState.sneak = sneak;
	}

	public static boolean isActive()
	{
		return active;
	}

	public static float getWorldX()
	{
		return worldX;
	}

	public static float getWorldZ()
	{
		return worldZ;
	}

	public static boolean isJump()
	{
		return jump;
	}

	public static boolean isSneak()
	{
		return sneak;
	}
}
