package org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline;

import java.util.EnumSet;
import java.util.Set;

/**
 * What {@link TimelineRecorder} actually captures each tick - see
 * {@link org.wilkretawesomesauce.minestuckuniverseported.Config#timelineRecordedCategories} for the config
 * knob that gates these, and {@link TimelineRecorder}/{@link EntitySnapshot} for where each category is
 * actually checked.
 * <p>
 * <b>Scaffolding, not a used feature yet</b> - user-requested groundwork, deliberately not wired to
 * anything beyond the single global config toggle below. {@link #ALL} (every category) is the default and
 * currently the only thing any real consumer of the recorded history has ever asked for - nothing in this
 * project turns any of these off. What this actually enables for later: {@link TimelineRecorder} recording
 * a genuinely lighter tick (e.g. {@link #ENTITY_POSITION} only, skipping health/equipment/etc. entirely -
 * a real memory/perf saving, not just an unused field) if a future need for that ever materializes, without
 * having to design the category plumbing from scratch at that point.
 * <p>
 * <b>Still a single global recording, not a per-abilitech one</b> - every consumer
 * ({@code TimelineManager}, {@code timeline.loop.TimeLoopZone}, {@code timeline.vision.PastVisionSession},
 * {@code DoomedTimelineClone}, ...) reads the same shared {@code TimelineData#getHistory()}, so today this
 * is a single, server-wide "record less to save memory" knob, not "this specific abilitech only cares about
 * position." Making it vary per-consumer would mean either recording multiple parallel histories (real,
 * ongoing extra cost, worse than what's saved by trimming categories in the first place) or having
 * lighter-recording consumers accept that older history was captured under a different category set than
 * whatever's configured now - neither was asked for, so neither is built here.
 */
public enum TimelineRecordCategory
{
	/** Player break/place and fluid placement - see {@link TimelineRecorder}'s own doc comment for exactly what "blocks" covers (and doesn't). */
	BLOCKS,
	/** Position, rotation, ground state, and pose - the minimum needed to place an entity somewhere. */
	ENTITY_POSITION,
	/** Health. */
	ENTITY_HEALTH,
	/** On-fire duration, sprinting/swimming/sneaking/invisible/glowing/elytra-flying. */
	ENTITY_STATUS,
	/** Item-use and arm-swing state - what an entity's hands are doing. */
	ENTITY_ACTIONS,
	/** Worn/held items. */
	ENTITY_EQUIPMENT,
	/** What an entity is riding. */
	ENTITY_VEHICLE;

	public static final Set<TimelineRecordCategory> ALL = Set.copyOf(EnumSet.allOf(TimelineRecordCategory.class));
}
