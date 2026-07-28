package org.wilkretawesomesauce.minestuckuniverseported.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;

/**
 * Lazily-baked, cached {@link HumanoidModel} instances for {@code items.WizardHatItem}/
 * {@code ArchmageHatItem}'s {@code initializeClient} overrides.
 * <p>
 * <b>Deliberately kept entirely out of the common {@code items} package</b>: an earlier draft cached the
 * baked model in a field directly on the (common, both-sides-loaded) {@code ArmorItem} subclass - the same
 * class of dedicated-server classloading risk this project already hit once with a client-only reference
 * inside common code (see {@code AbilitechnosynthBlock}'s own history). Baking is genuinely not free
 * (walks the whole {@code LayerDefinition} tree), so it's still cached - just here, in a class that's only
 * ever touched from inside an {@code IClientItemExtensions} anonymous implementation, which - confirmed via
 * {@code javap} against {@code Item#initializeClient}'s real signature - is only ever invoked from
 * client-side registration, never on a dedicated server, so this class is never loaded there either.
 */
public final class MSUHatModels
{
	private static HumanoidModel<LivingEntity> wizardHat;
	private static HumanoidModel<LivingEntity> archmageHat;
	private static HumanoidModel<LivingEntity> frogHat;

	private MSUHatModels()
	{
	}

	public static HumanoidModel<LivingEntity> wizardHat()
	{
		if(wizardHat == null)
			wizardHat = new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(MSUModelLayers.WIZARD_HAT));
		return wizardHat;
	}

	public static HumanoidModel<LivingEntity> archmageHat()
	{
		if(archmageHat == null)
			archmageHat = new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(MSUModelLayers.ARCHMAGE_HAT));
		return archmageHat;
	}

	public static HumanoidModel<LivingEntity> frogHat()
	{
		if(frogHat == null)
			frogHat = new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(MSUModelLayers.FROG_HAT));
		return frogHat;
	}
}
