package org.wilkretawesomesauce.minestuckuniverseported.client.render;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.wilkretawesomesauce.minestuckuniverseported.entity.UninhabitedBodyEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders {@code entity.UninhabitedBodyEntity} wearing the owner's actual skin - design doc §7/§11
 * ("appearance/skin", "the physical representation should use Body B's skin"). Reuses vanilla's own
 * baked {@link ModelLayers#PLAYER} humanoid layer definition directly (same "reuse a vanilla baked layer
 * rather than build a new one" trick already proven by this project's
 * {@code client.render.ConsortHatGeoLayer}/{@code FrogHatLayer}) rather than building a dedicated model,
 * and resolves the real texture via vanilla's own {@link Minecraft#getSkinManager()} - the same mechanism
 * vanilla itself uses to resolve any arbitrary player's skin (player heads, NPCs), not a placeholder.
 * <p>
 * <b>Known simplification, stated plainly rather than faked</b>: no cape/elytra/slim-vs-wide model
 * variant handling, and this is the first time this project has resolved an <i>arbitrary offline
 * player's</i> skin this way - flag as unverified in a live client, same as every other render-heavy
 * feature in this project's CLAUDE.md.
 */
public class UninhabitedBodyRenderer extends MobRenderer<UninhabitedBodyEntity, HumanoidModel<UninhabitedBodyEntity>>
{
	private static final ResourceLocation DEFAULT_SKIN = ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");
	private static final Map<UUID, ResourceLocation> SKIN_CACHE = new ConcurrentHashMap<>();

	public UninhabitedBodyRenderer(EntityRendererProvider.Context context)
	{
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
	}

	@Override
	public ResourceLocation getTextureLocation(UninhabitedBodyEntity entity)
	{
		UUID owner = entity.getOwnerUUID();
		if(owner == null)
			return DEFAULT_SKIN;

		return SKIN_CACHE.computeIfAbsent(owner, id ->
		{
			GameProfile profile = new GameProfile(id, entity.getOwnerName());
			PlayerSkin skin = Minecraft.getInstance().getSkinManager().getInsecureSkin(profile);
			return skin.texture();
		});
	}
}
