package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.time;

import com.mraof.minestuck.player.EnumAspect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.wilkretawesomesauce.minestuckuniverseported.MSUMobEffects;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.client.gui.TimeLoopRewindScreen;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.EntitySnapshot;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.RewindVisuals;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.TimelineData;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.WorldTickSnapshot;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.loop.TimeLoopCaster;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.timeline.loop.TimeLoopZone;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "Timeloop &beta;" - real class name matches its in-game display name (a prior user-requested rename
 * from {@code TechTimeLoopNested}, sibling to {@link TechTimeLoopAlpha} and {@link TechTimeLoopOmega}).
 * Tagged both {@link MSUTechType#PASSIVE} and {@link MSUTechType#DEFENSE} - see {@link TechHeroAspect}'s
 * own doc comment for the array-of-{@link MSUTechType} support this tech is the reason for.
 * <p>
 * <b>Redesign, user-requested</b>: no longer an automatic "die, get healed to full, a Time Loop zone
 * appears" proc. The instant a lethal hit would land on this player (while equipped in any slot), it's
 * clamped down to leave them at 1 HP with a choice screen ({@link TimeLoopRewindScreen}) forced open
 * on their own client: accept, and {@link #resolvePrompt} casts a {@link TimeLoopZone.StackMode#NESTED}
 * zone over their own recorded window (position/HP/equipment - and every block/entity within
 * {@link TimeLoopZone#RADIUS} of them), then {@link #onPlayerReversing} walks them backward through
 * their own path over {@link TimeLoopZone#REVERSE_TICKS_EFFECTIVE} ticks - not an instant teleport to
 * {@link #REWIND_TICKS} (7 seconds by default) ago, a user-corrected fix (an instant snap read back as
 * "resetting", not "rewinding") - before the zone plays that same restored window forward once, "replaying
 * the past actions prior". Decline (or let the prompt time out), and they just keep their narrow 1 HP
 * survival with no rewind.
 * <p>
 * <b>No Mixin, despite the "you probably need a Mixin to revert the state of death" framing this redesign
 * was requested with</b> - a deliberate, considered call, not an oversight, and consistent with this
 * project's standing no-Mixin policy (see {@code TechBreathWindVessel}/{@code mechanics.freedom.FreedomEvents}
 * for the same policy documented elsewhere). Genuinely reversing a real vanilla death (after
 * {@code PlayerList#respawn} has already swapped in a brand-new entity instance and dropped whatever the
 * {@code keepInventory} gamerule didn't keep) would need one. This build sidesteps that problem entirely by
 * never letting a real death happen in the first place - see {@link #onIncomingDamage}'s own doc comment for
 * why that's built on {@code doom.TechDoomBind}'s already-proven "clamp the incoming hit" technique now,
 * not the {@code LivingDeathEvent}-cancellation approach this class's very first version used.
 * <p>
 * <b>Real, honestly-scoped limitation</b>: "sets... inventory... to that moment" is only as true as what
 * {@link EntitySnapshot} actually records, which is worn/held equipment only (see that record's own doc
 * comment, under "Not captured") - a full 36-slot inventory has never been part of this project's per-tick
 * timeline recording (recording a complete inventory copy for every tracked entity every tick would be far
 * more expensive than the rest of what {@code mechanics.timeline.TimelineRecorder} already tracks), so a
 * rewind restores what the player was wearing/wielding 7 seconds ago, not their full pack contents.
 */
public class TechTimeLoopBeta extends TechHeroAspect
{
	/** How many ticks back a death-save actually restores state to if accepted. 140 = 7 seconds. Also becomes the captured Time Loop zone's own window/duration - unlike TechTimeLoopAlpha/the plain Timeloop β trigger, this doesn't reuse {@link TimeLoopZone#DEFAULT_WINDOW_TICKS}, since the rewind depth here is the whole point of the ability rather than an incidental replay length. */
	private static final int REWIND_TICKS = 140;

	/** How long the "Rewind Time?" prompt stays open, and how long the dying player stays invulnerable while deciding, before it auto-expires as a decline. 100 = 5 seconds. */
	private static final int PROMPT_TICKS = 100;

	private static final float MIN_HEALTH_TO_TRIGGER = 5.0F;

	public TechTimeLoopBeta()
	{
		super(Minestuckuniverseported.id("time_loop_beta"), EnumAspect.TIME, 35000, new MSUTechType[]{MSUTechType.PASSIVE, MSUTechType.DEFENSE}); // new tech, no original cost to port - picked to fit this project's own cost spread, see class doc comment
		setIcon("time_loop_beta");
		NeoForge.EVENT_BUS.register(this);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		return false;
	}

	/**
	 * The dying player's own position, and the rewind window, both captured at the exact moment
	 * {@link #onIncomingDamage} decides to intervene - not re-derived later in {@link #resolvePrompt}. See
	 * that record's own use for the real bug this fixes ("should play back from the point of death").
	 */
	private record DeathMoment(Vec3 position, List<WorldTickSnapshot> window)
	{
	}

	/**
	 * Per-player, not persisted - a stale entry (the prompt expired on its own timeout with no click at all,
	 * so {@link #resolvePrompt} never ran to consume it) just gets overwritten the next time that same
	 * player triggers a new prompt, never actually unbounded, so this doesn't need its own cleanup hook the
	 * way an active-effect list would.
	 */
	private static final Map<UUID, DeathMoment> pendingDeaths = new HashMap<>();

	/**
	 * The dying player's own real recorded path across the rewound window, and the zone their reverse walk
	 * feeds into once it completes - see {@link #onPlayerReversing}'s own doc comment for why the real
	 * player needs a dedicated per-tick mover at all (they're never part of a {@code TimeLoopZone}'s own
	 * puppeted-entity set). Same "stale entry just gets overwritten, never truly unbounded" reasoning as
	 * {@link #pendingDeaths} - not that a stale entry is even reachable here, since {@link #onPlayerReversing}
	 * always removes its own entry the moment the walk finishes.
	 */
	private record ReversingPlayer(List<EntitySnapshot> path, long startTick, TimeLoopZone zone)
	{
	}

	private static final Map<UUID, ReversingPlayer> reversingPlayers = new HashMap<>();

	/**
	 * <b>Real bug fix, from a live report ("keeps you in a perma death state")</b>: this tech's very first
	 * version hooked {@code LivingDeathEvent} instead - cancel the death, {@code setHealth(1)}, and a manual
	 * {@code Entity#setInvulnerable(true)} that only a separate tick-watcher ever cleared back off. That's
	 * exactly the shape {@code life.TechLifeGrace}'s own {@code SavingGraceEvents} uses and it works fine
	 * there (an instant, one-shot resolution with nothing left pending) - but this tech defers the actual
	 * resolution behind a real player choice, and something about a real connected {@code ServerPlayer}'s
	 * own client-side death handling didn't fully undo on cancellation the way it does for
	 * {@code TechLifeGrace}'s immediate case, leaving players stuck. Rather than chase that down blind, this
	 * now uses the exact same real, already-proven technique {@code doom.TechDoomBind} ("Survivor's Bind")
	 * already has for the identical "don't let this hit actually kill you" problem: intercept the incoming
	 * damage itself, before health ever reaches 0 and {@code die()}/{@code LivingDeathEvent} ever fire at
	 * all - {@link LivingIncomingDamageEvent#setAmount} clamped to leave exactly 1 HP (matching
	 * {@code TechDoomBind}'s own math), and {@link LivingIncomingDamageEvent#setInvulnerabilityTicks} for the
	 * decision window - vanilla's own real, self-expiring hit-cooldown field ({@code LivingEntity#invulnerableTime},
	 * ticked down by the game itself every tick regardless of this mod), not a manual flag this class has to
	 * remember to clear.
	 * <p>
	 * <b>Second real bug fix, same live report ("same bug still persists")</b>: the first fix above was
	 * necessary but not sufficient. Once clamped to 1 HP, almost <i>any</i> further damage is "lethal"
	 * relative to that 1 HP too - and this used to have no guard against that, so a continuous
	 * damage-over-time source still active at the moment of death (fire, lava, drowning, poison, wither, a
	 * mob still swinging next tick) re-triggered this whole method every subsequent tick, resetting the
	 * prompt's own window before it ever got a chance to actually run out. A player already mid-decision
	 * (checked via {@link MSUMobEffects#TIME_LOOP_REWIND_PROMPT}) now just has all further damage zeroed
	 * outright instead.
	 * <p>
	 * <b>Third real bug fix, same live report ("I click 'Let It Be' and I'm just immortal now")</b>: the
	 * second fix only guarded the <i>active decision window</i> - it did nothing once the prompt actually
	 * resolved. Declining (or the no-recorded-history fallback in {@link #resolvePrompt}) leaves the player
	 * sitting at exactly 1 HP with no floor on when this method is willing to trigger again - so the very
	 * next hit of any size (even trivial chip damage) read as "lethal relative to 1 HP" and re-ran the whole
	 * save again, forever, with the player never actually able to drop below 1 HP again for the rest of the
	 * session. {@link #MIN_HEALTH_TO_TRIGGER} closes that gap the same real way {@code TechDoomBind} already
	 * does: only trigger from a meaningfully-alive health value, never from an already-clamped one - once a
	 * save resolves (either outcome), the player is genuinely mortal again the moment they take their next
	 * hit, exactly like before this tech ever existed, until they're actually healthy enough again to be
	 * worth saving from a second real lethal hit.
	 * <p>
	 * <b>Fourth fix, user-requested ("should play back from the point of death")</b>: the window/center
	 * used to be derived live, inside {@link #resolvePrompt}, at whatever moment the player actually
	 * answered the prompt - but that can be up to {@link #PROMPT_TICKS} (several seconds) after this method
	 * runs, and {@code mechanics.timeline.TimelineRecorder} keeps recording that whole time, so the "last
	 * {@link #REWIND_TICKS} ticks" at accept-time drift further from the actual death the longer the player
	 * takes to decide - up to and including eating into the decision window itself instead of actual
	 * pre-death action. Now captured right here, the instant the save actually triggers, and held in
	 * {@link #pendingDeaths} until {@link #resolvePrompt} is ready for it - the rewind is always anchored to
	 * this exact moment regardless of how long the choice takes.
	 */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	private void onIncomingDamage(LivingIncomingDamageEvent event)
	{
		if(!(event.getEntity() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator())
			return;

		if(player.hasEffect(MSUMobEffects.TIME_LOOP_REWIND_PROMPT))
		{
			event.setAmount(0F);
			return;
		}

		if(player.getHealth() <= MIN_HEALTH_TO_TRIGGER || event.getAmount() < player.getHealth())
			return; // too low to meaningfully "save", or not actually lethal - nothing to intervene on

		GodTierData godTier = player.getData(MSUAttachments.GOD_TIER);
		if(!godTier.isTechEquipped(this))
			return;

		if(player.level() instanceof ServerLevel serverLevel)
			pendingDeaths.put(player.getUUID(), captureDeathMoment(serverLevel, player));

		event.setAmount(Math.max(0F, player.getHealth() - 1.0F));
		event.setInvulnerabilityTicks(PROMPT_TICKS);
		player.addEffect(new MobEffectInstance(MSUMobEffects.TIME_LOOP_REWIND_PROMPT, PROMPT_TICKS, 0, false, false));

		MSUAbilitechParticles.burst(player.level(), player, EnumAspect.TIME, 30);
		player.displayClientMessage(Component.translatable("status.minestuckuniverseported.timeLoopNested.prompt"), true);
	}

	/** The exact same "last {@link #REWIND_TICKS} ticks" capture {@code TimeLoopCaster#cast} itself would do live - just done here, at death-time, and handed to {@code TimeLoopCaster#castWithCapturedWindow} later instead of re-derived. */
	private static DeathMoment captureDeathMoment(ServerLevel level, ServerPlayer player)
	{
		TimelineData data = level.getData(MSUAttachments.TIMELINE);
		List<WorldTickSnapshot> history = new ArrayList<>(data.getHistory());
		int windowTicks = Math.min(REWIND_TICKS, history.size());
		List<WorldTickSnapshot> window = windowTicks <= 0 ? List.of()
				: new ArrayList<>(history.subList(history.size() - windowTicks, history.size()));
		return new DeathMoment(player.position(), window);
	}

	/**
	 * Called by {@code network.TimeLoopRewindDecisionPacket} once the player answers
	 * {@link TimeLoopRewindScreen}. A stale/duplicate call (the prompt already resolved, or it expired
	 * server-side before the click arrived) is a safe no-op - checked via the same marker effect the prompt
	 * itself is built on, not a separate flag.
	 */
	public static void resolvePrompt(ServerPlayer player, boolean rewind)
	{
		if(!player.hasEffect(MSUMobEffects.TIME_LOOP_REWIND_PROMPT))
			return;

		player.removeEffect(MSUMobEffects.TIME_LOOP_REWIND_PROMPT);
		// The decision's been made - no reason to keep the rest of the original invulnerability window
		// (see onIncomingDamage's own doc comment for where it comes from) once there's nothing left to
		// decide. Plain vanilla field, not something this class has to poll/restore later.
		player.invulnerableTime = 0;

		// Always consumed, whether accepted or declined - a decline still needs to drop the stale entry
		// (see pendingDeaths' own doc comment for why leaving it isn't a real leak either way, but there's
		// no reason to hold onto it once this specific prompt is resolved).
		DeathMoment moment = pendingDeaths.remove(player.getUUID());

		if(!rewind)
		{
			player.displayClientMessage(Component.translatable("status.minestuckuniverseported.timeLoopNested.declined"), true);
			return;
		}

		if(!(player.level() instanceof ServerLevel serverLevel))
			return;

		TimeLoopZone zone;
		if(moment != null && !moment.window().isEmpty())
		{
			// Anchored to the moment onIncomingDamage actually triggered, not to right now.
			TimeLoopZone parent = TimeLoopCaster.findNestParent(serverLevel, moment.position());
			zone = TimeLoopCaster.castWithCapturedWindow(serverLevel, player, REWIND_TICKS,
					moment.position(), moment.window(), TimeLoopZone.StackMode.NESTED, parent != null ? parent.getId() : null);
		}
		else
		{
			// No captured death-moment to work from (e.g. the server restarted mid-decision) - fall back to
			// deriving live, same as this tech did before the point-of-death fix, rather than failing outright.
			TimeLoopZone parent = TimeLoopCaster.findNestParent(serverLevel, player.position());
			zone = TimeLoopCaster.cast(serverLevel, player, REWIND_TICKS, REWIND_TICKS,
					TimeLoopZone.StackMode.NESTED, parent != null ? parent.getId() : null);
		}

		if(zone == null)
		{
			// No recorded history to actually rewind into yet (e.g. this player only just joined) - fall
			// back to a plain full heal rather than leaving them stuck at a narrow 1 HP with nothing to
			// show for having accepted the prompt.
			player.setHealth(player.getMaxHealth());
			return;
		}

		List<EntitySnapshot> path = pathOf(zone.getWindow(), player.getUUID());
		if(path.isEmpty())
		{
			// No recorded snapshot of the player themselves anywhere in the window (e.g. they were only
			// just started being tracked) - nothing to walk them through; the zone still resets everything
			// else around them normally, and they're left at a plain full heal rather than untouched.
			player.setHealth(player.getMaxHealth());
			return;
		}

		// Keep them protected through the real walk below (a small buffer past the walk's own length, so
		// they don't land with a sliver of vulnerability the instant it finishes) - the same self-expiring
		// vanilla field the decision window itself already used, not a manual flag (see onIncomingDamage's
		// own doc comment for why that class of bug is worth avoiding here).
		player.invulnerableTime = TimeLoopZone.REVERSE_TICKS_EFFECTIVE + 5;
		reversingPlayers.put(player.getUUID(), new ReversingPlayer(path, serverLevel.getGameTime(), zone));
		RewindVisuals.showRewindGhost(player, path);
	}

	/** Every recorded snapshot of {@code entityId} across {@code window}, chronological - mirrors {@code timeline.loop.TimeLoopReplay}'s own private helper of the same shape (different package, can't be shared directly). */
	private static List<EntitySnapshot> pathOf(List<WorldTickSnapshot> window, UUID entityId)
	{
		List<EntitySnapshot> path = new ArrayList<>();
		for(WorldTickSnapshot step : window)
		{
			EntitySnapshot snapshot = step.entitySnapshots().get(entityId);
			if(snapshot != null)
				path.add(snapshot);
		}
		return path;
	}

	/**
	 * <b>User-requested movement, replacing an instant snap</b>: {@code TimeLoopZone}/
	 * {@code timeline.loop.TimeLoopReplay} walk every <i>puppeted</i> entity backward through its own
	 * path over {@link TimeLoopZone#REVERSE_TICKS_EFFECTIVE} ticks (see those classes' own doc comments) -
	 * but players are deliberately never part of that puppeted set (a connected {@code ServerPlayer} should
	 * never be moved the way a fake/mob entity is, same reasoning
	 * {@code mechanics.timeline.TimelineManager#applySnapshot} already documents), so the dying player needs
	 * this own dedicated per-tick mover, walking the identical shared formula
	 * ({@code mechanics.timeline.RewindVisuals#sampleReversePath}) against their own captured
	 * {@link ReversingPlayer#path}. Once the walk completes, floors health the same way the old one-shot
	 * restore always did, and sends the same confirmation message/particles.
	 */
	@SubscribeEvent
	private void onPlayerReversing(PlayerTickEvent.Post event)
	{
		if(!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide())
			return;

		ReversingPlayer reversing = reversingPlayers.get(player.getUUID());
		if(reversing == null)
			return;

		long elapsed = player.level().getGameTime() - reversing.startTick();
		int reverseTick = (int) Math.min(elapsed, TimeLoopZone.REVERSE_TICKS_EFFECTIVE - 1);

		RewindVisuals.sampleReversePath(reversing.path(), reverseTick, TimeLoopZone.REVERSE_TICKS_EFFECTIVE).applyTo(player);

		if(elapsed < TimeLoopZone.REVERSE_TICKS_EFFECTIVE - 1)
			return;

		reversingPlayers.remove(player.getUUID());
		// A snapshot recorded at a genuinely near-zero health (the player was already critically low at the
		// very start of the window) would otherwise leave them alive on a sliver rather than a meaningful
		// save - floor it, same safety margin the initial prompt already gives them.
		player.setHealth(Math.max(1.0F, player.getHealth()));

		if(player.level() instanceof ServerLevel serverLevel)
			MSUAbilitechParticles.burst(serverLevel, player, EnumAspect.TIME, 30);
		player.displayClientMessage(Component.translatable("status.minestuckuniverseported.timeLoopNested.deathRewind",
				reversing.zone().getWindowLength() / 20F), true);
	}

	/** Marker effect, no attribute modifiers or tick behavior of its own - exists purely so "is this player currently deciding" is network-synced to their own client for free, matching {@code heart.TechSoulStun.SoulShockedEffect}'s own established shape. */
	public static class RewindPromptEffect extends MobEffect
	{
		public RewindPromptEffect()
		{
			super(MobEffectCategory.NEUTRAL, 0x66FFE8);
		}
	}

	/** Forces {@link TimeLoopRewindScreen} open on the local player for as long as they hold {@link MSUMobEffects#TIME_LOOP_REWIND_PROMPT}, and closes it again the instant they don't - mirrors {@code heart.TechSoulStun.ClientEvents} exactly. */
	@EventBusSubscriber(modid = Minestuckuniverseported.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
	public static final class ClientEvents
	{
		private ClientEvents()
		{
		}

		@SubscribeEvent
		private static void onClientTick(ClientTickEvent.Post event)
		{
			Minecraft mc = Minecraft.getInstance();
			LocalPlayer player = mc.player;
			if(player == null)
				return;
//shit should not be an effect.
			boolean prompted = player.hasEffect(MSUMobEffects.TIME_LOOP_REWIND_PROMPT);

			if(prompted && !(mc.screen instanceof TimeLoopRewindScreen) && !(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen))
				mc.setScreen(new TimeLoopRewindScreen());
			else if(!prompted && mc.screen instanceof TimeLoopRewindScreen)
				mc.setScreen(null);
		}
	}
}
