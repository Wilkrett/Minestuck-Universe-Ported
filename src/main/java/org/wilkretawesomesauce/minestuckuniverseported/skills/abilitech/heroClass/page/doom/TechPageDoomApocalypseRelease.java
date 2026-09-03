package org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.page.doom;

import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.keyStates.AbilitechKeyState;
import org.wilkretawesomesauce.minestuckuniverseported.mechanics.doom.DoomData;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechParticles;
import org.wilkretawesomesauce.minestuckuniverseported.events.AbilitechTargetedEvent;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUTechType;

/**
 * "Apocalypse Release" - new tech, ported from the "Doom Class Abilities Framework" design document (no
 * 1.12.2 original), Page of Doom's Offensive ability: "the Page releases stored Doom as a destructive
 * event... converts personal accumulation into external destruction." Press to consume up to
 * {@link #MAX_CONSUME} of the Page's own currently-stored Doom (see
 * {@code TechPageDoomReservoir}, this class's own Core-tech sibling, for how it's actually accumulated)
 * and deal AoE damage scaled by however much was actually consumed - a Page who's stored nothing has
 * nothing to unleash.
 * <p>
 * Priced above Page's own existing ultimate ({@code TechPagePerseverantAwakening}, 1,000,000) - matching
 * the design doc's own framing of this as the payoff for everything Doom Reservoir spent time
 * accumulating, the single biggest one-shot in this pass.
 */
public class TechPageDoomApocalypseRelease extends TechHeroClass
{
	/** Radius (in blocks) of the AoE burst. */
	private static final double RADIUS = 8.0;
	/** Damage dealt per point of the Page's own Doom actually consumed. */
	private static final double DAMAGE_SCALE = 0.5;
	/** Max Doom this can consume from the Page's own stored Doom in one discharge - may consume less if they don't have this much stored. */
	private static final double MAX_CONSUME = 200.0;

	public TechPageDoomApocalypseRelease()
	{
		super(Minestuckuniverseported.id("apocalypse_release"), EnumClass.PAGE, EnumAspect.DOOM, 1200000, MSUTechType.OFFENSE);
	}

	@Override
	public boolean onUseTick(Level level, Player player, int techSlot, AbilitechKeyState state, int time)
	{
		if(state != AbilitechKeyState.PRESS)
			return false;

		DoomData data = player.getData(MSUAttachments.DOOM_DATA);
		double consumed = Math.min(MAX_CONSUME, data.getDoom());
		if(consumed <= 0)
		{
			player.displayClientMessage(Component.translatable("status.minestuckuniverseported.apocalypseReleaseEmpty"), true);
			return false;
		}

		data.removeDoom(consumed);
		double damage = consumed * DAMAGE_SCALE;

		for(LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RADIUS), e -> e != player))
		{
			if(NeoForge.EVENT_BUS.post(new AbilitechTargetedEvent(player, target, this, techSlot, false)).isCanceled())
				continue;
			target.hurt(level.damageSources().magic(), (float) damage);
		}

		MSUAbilitechParticles.burst(level, player, EnumAspect.DOOM, 30);
		return false;
	}
}
