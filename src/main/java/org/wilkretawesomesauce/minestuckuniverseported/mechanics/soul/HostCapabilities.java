package org.wilkretawesomesauce.minestuckuniverseported.mechanics.soul;

/**
 * What a given {@link SoulHost} lets a controlling Soul actually do - design doc §14 ("different hosts
 * should expose different controls... a Soul should control the host through its available
 * capabilities"). {@link #FULL} is the only instance used this pass ({@link
 * org.wilkretawesomesauce.minestuckuniverseported.entity.UninhabitedBodyEntity} is a full player body,
 * it supports everything); this type exists now so a future mob/animal/item host (out of scope for this
 * pass - see {@link SoulHost}'s own doc comment) can report a narrower set (e.g. a Cow can't attack)
 * without any change to {@link SoulManager}/{@code SoulProjectionEvents}, matching the doc's own "avoid
 * giant entity-class switch statements" instruction.
 */
public record HostCapabilities(boolean movement, boolean jump, boolean attack, boolean useItem, boolean inventory)
{
	public static final HostCapabilities FULL = new HostCapabilities(true, true, true, true, true);
}
