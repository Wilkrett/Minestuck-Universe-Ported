package org.wilkretawesomesauce.minestuckuniverseported.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAttachments;
import org.wilkretawesomesauce.minestuckuniverseported.capabilities.godTier.GodTierData;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.Abilitech;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroAspect.TechHeroAspect;
import org.wilkretawesomesauce.minestuckuniverseported.skills.abilitech.heroClass.TechHeroClass;
import org.wilkretawesomesauce.minestuckuniverseported.util.MSUAbilitechRegistry;
import org.wilkretawesomesauce.minestuckuniverseported.network.MSUAbilitechPackets;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * {@code /msu abilitech grant <player> <abilitech>} / {@code /msu abilitech grant <player> all} /
 * {@code /msu abilitech revoke <player> <abilitech>} / {@code /msu abilitech revoke <player> all} -
 * permission-level-2-gated ("cheats"/op, not tied to game mode) debug bridges for managing <i>another</i>
 * player's real unlock state, same shape/gating as {@code AbilitechUnlockCommand} (which only ever
 * targets the commanding player themselves). {@code <player>} comes before the tech/{@code all} choice
 * (not nested under a {@code user} sub-literal) - a direct user correction, this command's real shape.
 * <p>
 * A single-tech {@code grant} force-marks that one tech unlocked for the target, bypassing
 * {@code canUnlock}/{@code onUnlock} entirely (spends nothing) - same real debug-bypass semantics as
 * {@code /msu debug unlock all}'s self-targeted equivalent. {@code grant ... all} does the same for every
 * registered tech at once. {@code revoke} is the real inverse for both shapes: a single-tech revoke clears
 * just that one tech's unlocked flag, {@code revoke ... all} clears the whole set - either way, any slot
 * left equipped with a tech that's no longer unlocked is force-unequipped afterward (a bare unlock-flag
 * clear alone would leave an already-equipped-but-now-locked tech sitting in its slot, since this
 * project's own unlock gate only ever runs at equip time, not continuously).
 * <p>
 * {@code grant/revoke <player> aspect <aspect>} and {@code grant/revoke <player> classpect <class>} are a
 * third, category-scoped shape alongside the single-tech and {@code all} ones above, same force-bypass
 * semantics as {@code all}. {@code aspect} matches every {@code heroAspect} tech tagged with that
 * {@link EnumAspect} ({@link TechHeroAspect#getHeroAspect()}) plus every {@code heroClass} tech whose own
 * {@link TechHeroClass#getRequiredAspect()} happens to match it (e.g. {@code mage.blood.*}); {@code classpect}
 * matches every {@code heroClass} tech tagged with that {@link EnumClass} ({@link TechHeroClass
 * #getRealHeroClass()}) - {@code heroAspect} techs have no single owning class, so they're never included
 * here. Both resolve their name argument case-insensitively against the real enum constants, not a
 * separate name table.
 */
public final class AbilitechUserCommand
{
	private AbilitechUserCommand()
	{
	}

	public static LiteralArgumentBuilder<CommandSourceStack> getArgumentBuilder()
	{
		return Commands.literal("abilitech")
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal("grant")
						.then(Commands.argument("player", EntityArgument.player())
								.then(Commands.literal("all")
										.executes(context -> grantAll(context.getSource(), EntityArgument.getPlayer(context, "player"))))
								.then(Commands.literal("aspect")
										.then(Commands.argument("aspect", StringArgumentType.word())
												.suggests(ASPECT_SUGGESTIONS)
												.executes(context -> grantAspect(context.getSource(), EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "aspect")))))
								.then(Commands.literal("classpect")
										.then(Commands.argument("classpect", StringArgumentType.word())
												.suggests(CLASSPECT_SUGGESTIONS)
												.executes(context -> grantClasspect(context.getSource(), EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "classpect")))))
								.then(Commands.argument("tech", ResourceLocationArgument.id())
										.executes(context -> grant(context.getSource(), EntityArgument.getPlayer(context, "player"), ResourceLocationArgument.getId(context, "tech"))))))
				.then(Commands.literal("revoke")
						.then(Commands.argument("player", EntityArgument.player())
								.then(Commands.literal("all")
										.executes(context -> revokeAll(context.getSource(), EntityArgument.getPlayer(context, "player"))))
								.then(Commands.literal("aspect")
										.then(Commands.argument("aspect", StringArgumentType.word())
												.suggests(ASPECT_SUGGESTIONS)
												.executes(context -> revokeAspect(context.getSource(), EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "aspect")))))
								.then(Commands.literal("classpect")
										.then(Commands.argument("classpect", StringArgumentType.word())
												.suggests(CLASSPECT_SUGGESTIONS)
												.executes(context -> revokeClasspect(context.getSource(), EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "classpect")))))
								.then(Commands.argument("tech", ResourceLocationArgument.id())
										.executes(context -> revoke(context.getSource(), EntityArgument.getPlayer(context, "player"), ResourceLocationArgument.getId(context, "tech"))))));
	}

	private static final com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack> ASPECT_SUGGESTIONS =
			(context, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(EnumAspect.values()).map(a -> a.name().toLowerCase(Locale.ROOT)), builder);
	private static final com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack> CLASSPECT_SUGGESTIONS =
			(context, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(EnumClass.values()).map(c -> c.name().toLowerCase(Locale.ROOT)), builder);

	private static int grant(CommandSourceStack source, ServerPlayer target, ResourceLocation techId) throws CommandSyntaxException
	{
		Abilitech tech = MSUAbilitechRegistry.get(techId);
		if(tech == null)
		{
			source.sendFailure(Component.translatable("status.minestuckuniverseported.abilitech.unlock_not_found", techId.toString()));
			return 0;
		}

		GodTierData godTier = target.getData(MSUAttachments.GOD_TIER);
		if(godTier.isUnlocked(tech))
		{
			source.sendFailure(Component.translatable("status.minestuckuniverseported.abilitech.grant_already", target.getName(), tech.getDisplayName()));
			return 0;
		}

		godTier.markUnlocked(tech);
		MSUAbilitechPackets.sendLoadoutSync(target);

		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.abilitech.grant_success", tech.getDisplayName(), target.getName()), true);
		return 1;
	}

	private static int grantAll(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException
	{
		GodTierData godTier = target.getData(MSUAttachments.GOD_TIER);

		int count = 0;
		for(Abilitech tech : MSUAbilitechRegistry.getAll())
		{
			if(godTier.isUnlocked(tech))
				continue;
			godTier.markUnlocked(tech);
			count++;
		}

		MSUAbilitechPackets.sendLoadoutSync(target);

		int unlockedCount = count;
		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.abilitech.user_unlock_all_success", unlockedCount, target.getName()), true);
		return count;
	}

	private static int grantAspect(CommandSourceStack source, ServerPlayer target, String aspectName) throws CommandSyntaxException
	{
		EnumAspect aspect = parseAspect(source, aspectName);
		if(aspect == null)
			return 0;

		return grantCategory(source, target, tech -> getAspect(tech) == aspect, aspect.name());
	}

	private static int grantClasspect(CommandSourceStack source, ServerPlayer target, String classpectName) throws CommandSyntaxException
	{
		EnumClass classpect = parseClasspect(source, classpectName);
		if(classpect == null)
			return 0;

		return grantCategory(source, target, tech -> getHeroClass(tech) == classpect, classpect.name());
	}

	private static int grantCategory(CommandSourceStack source, ServerPlayer target, Predicate<Abilitech> matches, String label)
	{
		GodTierData godTier = target.getData(MSUAttachments.GOD_TIER);

		int count = 0;
		for(Abilitech tech : MSUAbilitechRegistry.getAll())
		{
			if(!matches.test(tech) || godTier.isUnlocked(tech))
				continue;
			godTier.markUnlocked(tech);
			count++;
		}

		MSUAbilitechPackets.sendLoadoutSync(target);

		int unlockedCount = count;
		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.abilitech.grant_category_success", unlockedCount, label, target.getName()), true);
		return count;
	}

	private static int revoke(CommandSourceStack source, ServerPlayer target, ResourceLocation techId) throws CommandSyntaxException
	{
		Abilitech tech = MSUAbilitechRegistry.get(techId);
		if(tech == null)
		{
			source.sendFailure(Component.translatable("status.minestuckuniverseported.abilitech.unlock_not_found", techId.toString()));
			return 0;
		}

		GodTierData godTier = target.getData(MSUAttachments.GOD_TIER);
		if(!godTier.isUnlocked(tech))
		{
			source.sendFailure(Component.translatable("status.minestuckuniverseported.abilitech.revoke_not_unlocked", target.getName(), tech.getDisplayName()));
			return 0;
		}

		godTier.revokeUnlocked(tech);
		unequipIfNowLocked(godTier, target);
		MSUAbilitechPackets.sendLoadoutSync(target);

		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.abilitech.revoke_success", tech.getDisplayName(), target.getName()), true);
		return 1;
	}

	private static int revokeAll(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException
	{
		GodTierData godTier = target.getData(MSUAttachments.GOD_TIER);
		godTier.clearUnlockedTechs();
		unequipIfNowLocked(godTier, target);
		MSUAbilitechPackets.sendLoadoutSync(target);

		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.abilitech.user_revoke_success", target.getName()), true);
		return 1;
	}

	private static int revokeAspect(CommandSourceStack source, ServerPlayer target, String aspectName) throws CommandSyntaxException
	{
		EnumAspect aspect = parseAspect(source, aspectName);
		if(aspect == null)
			return 0;

		return revokeCategory(source, target, tech -> getAspect(tech) == aspect, aspect.name());
	}

	private static int revokeClasspect(CommandSourceStack source, ServerPlayer target, String classpectName) throws CommandSyntaxException
	{
		EnumClass classpect = parseClasspect(source, classpectName);
		if(classpect == null)
			return 0;

		return revokeCategory(source, target, tech -> getHeroClass(tech) == classpect, classpect.name());
	}

	private static int revokeCategory(CommandSourceStack source, ServerPlayer target, Predicate<Abilitech> matches, String label)
	{
		GodTierData godTier = target.getData(MSUAttachments.GOD_TIER);

		int count = 0;
		for(Abilitech tech : MSUAbilitechRegistry.getAll())
		{
			if(!matches.test(tech) || !godTier.isUnlocked(tech))
				continue;
			godTier.revokeUnlocked(tech);
			count++;
		}

		unequipIfNowLocked(godTier, target);
		MSUAbilitechPackets.sendLoadoutSync(target);

		int revokedCount = count;
		source.sendSuccess(() -> Component.translatable("status.minestuckuniverseported.abilitech.revoke_category_success", revokedCount, label, target.getName()), true);
		return count;
	}

	@Nullable
	private static EnumAspect parseAspect(CommandSourceStack source, String name)
	{
		try
		{
			return EnumAspect.valueOf(name.toUpperCase(Locale.ROOT));
		}
		catch(IllegalArgumentException e)
		{
			source.sendFailure(Component.translatable("status.minestuckuniverseported.abilitech.category_not_found", "aspect", name));
			return null;
		}
	}

	@Nullable
	private static EnumClass parseClasspect(CommandSourceStack source, String name)
	{
		try
		{
			return EnumClass.valueOf(name.toUpperCase(Locale.ROOT));
		}
		catch(IllegalArgumentException e)
		{
			source.sendFailure(Component.translatable("status.minestuckuniverseported.abilitech.category_not_found", "classpect", name));
			return null;
		}
	}

	/** {@code null} for anything that isn't a {@code heroAspect} tech and isn't a {@code heroClass} tech with a {@link TechHeroClass#getRequiredAspect()} of its own. */
	@Nullable
	private static EnumAspect getAspect(Abilitech tech)
	{
		if(tech instanceof TechHeroAspect heroAspect)
			return heroAspect.getHeroAspect();
		if(tech instanceof TechHeroClass heroClass)
			return heroClass.getRequiredAspect();
		return null;
	}

	/** {@code null} for anything that isn't a {@code heroClass} tech - {@code heroAspect} techs have no single owning class. */
	@Nullable
	private static EnumClass getHeroClass(Abilitech tech)
	{
		return tech instanceof TechHeroClass heroClass ? heroClass.getRealHeroClass() : null;
	}

	private static void unequipIfNowLocked(GodTierData godTier, ServerPlayer target)
	{
		for(int slot = 0; slot < GodTierData.TECH_SLOTS; slot++)
		{
			Abilitech equipped = godTier.getTech(slot);
			if(equipped != null && !godTier.isUnlocked(equipped))
				godTier.unequipTech(target.level(), target, slot);
		}
	}
}
