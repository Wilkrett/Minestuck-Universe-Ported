package org.wilkretawesomesauce.minestuckuniverseported.timeline;

import java.util.List;
import java.util.UUID;

/**
 * Ported from the request to actually play a rewind back over time instead of snapping the world to its
 * past state instantly. {@link TimelineManager#rewind} pops the relevant snapshots off {@link TimelineData}'s
 * history immediately (so the cost/debt is committed right away and the same ticks can't be rewound
 * twice), but doesn't apply them - it wraps them in one of these and hands it to {@link TimelineData}'s
 * active-rewind queue, and {@link TimelineRewindPlayback} applies a few steps per real tick until it's
 * done, spawning reverse-time particles as it goes.
 *
 * @param steps       the snapshots to apply, in application order (most recently recorded first, since
 *                    undoing has to happen in reverse-chronological order to be coherent)
 * @param initiatorId the player this rewind belongs to - used to exclude other players from entity
 *                    restoration, same as the instant-apply version did
 */
public final class ActiveRewind
{
	private final List<WorldTickSnapshot> steps;
	private final UUID initiatorId;
	private int index = 0;

	public ActiveRewind(List<WorldTickSnapshot> steps, UUID initiatorId)
	{
		this.steps = steps;
		this.initiatorId = initiatorId;
	}

	public boolean isDone()
	{
		return index >= steps.size();
	}

	public WorldTickSnapshot nextStep()
	{
		return steps.get(index++);
	}

	public int getRemainingSteps()
	{
		return steps.size() - index;
	}

	public UUID getInitiatorId()
	{
		return initiatorId;
	}
}
