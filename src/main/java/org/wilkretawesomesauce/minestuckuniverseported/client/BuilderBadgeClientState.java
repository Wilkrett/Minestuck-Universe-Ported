package org.wilkretawesomesauce.minestuckuniverseported.client;

/**
 * Client-side cache of whether the local player currently has {@code badges.BadgeBuilder} active -
 * populated entirely from {@code network.BuilderBadgeSyncPacket}. Sole consumer:
 * {@link BadgeBuilderClientEvents}.
 */
public final class BuilderBadgeClientState
{
	private static boolean active = false;

	private BuilderBadgeClientState()
	{
	}

	public static void set(boolean value)
	{
		active = value;
	}

	public static boolean isActive()
	{
		return active;
	}
}
