package org.wilkretawesomesauce.minestuckuniverseported;

import net.minecraft.world.level.GameRules;

/**
 * Real, original-design gamerule for {@code item.JukinatorItem}'s rhythm minigame - no 1.12.2
 * counterpart. {@link GameRules#register} is {@code private} in raw vanilla, but confirmed via
 * {@code javap} against this project's actual compiled-with-NeoForge classpath that NeoForge's own
 * access-widening patch makes it {@code public} there - the real, standard way NeoForge mods add
 * custom gamerules, no Mixin/access-transformer of this project's own needed.
 * <p>
 * Custom gamerules are <b>not</b> broadcast to clients (confirmed via {@code javap} against
 * {@code ClientboundLoginPacket} - only a hardcoded handful of individual vanilla booleans are ever
 * piped to the client, not a general {@code GameRules} sync), so this can only ever be read
 * server-side. {@code JukinatorItem}'s {@code use()} resolves it and hands the result down explicitly
 * via {@code network.OpenJukinatorPacket} - nothing client-side ever calls
 * {@code Level#getGameRules()} for this rule.
 */
public final class MSUGameRules
{
	// Default true: a fresh random chart every time the minigame opens. Set false (via
	// "/gamerule jukinatorRandomCharts false") for a chart seeded from the disc's own identity instead,
	// so the same disc always produces the same chart - see JukinatorScreen's own doc comment.
	public static final GameRules.Key<GameRules.BooleanValue> JUKINATOR_RANDOM_CHARTS =
			GameRules.register("jukinatorRandomCharts", GameRules.Category.MISC, GameRules.BooleanValue.create(true));

	/** Forces this class's static initializer (and so the registration above) to run early - same real
	 *  pattern as {@code strife.MSUKindAbstrata#init()}/{@code skills.MSUSkills#init()}. */
	public static void init()
	{
	}

	private MSUGameRules()
	{
	}
}
