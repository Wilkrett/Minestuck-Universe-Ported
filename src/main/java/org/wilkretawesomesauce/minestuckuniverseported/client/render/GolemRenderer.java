package org.wilkretawesomesauce.minestuckuniverseported.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.wilkretawesomesauce.minestuckuniverseported.client.model.golem.GolemModel;
import org.wilkretawesomesauce.minestuckuniverseported.client.model.MSUModelLayers;
import org.wilkretawesomesauce.minestuckuniverseported.entity.GolemEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ported from ModularBosses (1.8)'s {@code client.render.entity.RenderGolem} - the original picked a
 * texture at runtime by baking the mimicked block's own {@code IBakedModel} and reading its sprite's
 * icon name back out as a file path; this is the same trick with the modern equivalent API
 * ({@link BakedModel#getParticleIcon()}'s {@link TextureAtlasSprite#contents()}{@code .name()}, which
 * names the real standalone PNG a block's particle sprite was stitched into the atlas from - not a
 * guessed "textures/block/&lt;registry name&gt;.png" convention). Results are cached per distinct
 * {@link BlockState} since re-resolving a block's model every frame for every visible golem would be
 * wasteful and this can only ever change a handful of times (once per golem, on mimicry resolution).
 */
public class GolemRenderer extends MobRenderer<GolemEntity, GolemModel>
{
	private static final Map<BlockState, ResourceLocation> TEXTURE_CACHE = new ConcurrentHashMap<>();
	private static final ResourceLocation FALLBACK = ResourceLocation.withDefaultNamespace("textures/block/stone.png");

	public GolemRenderer(EntityRendererProvider.Context context)
	{
		super(context, new GolemModel(context.bakeLayer(MSUModelLayers.GOLEM)), 0.9F);
	}

	@Override
	public ResourceLocation getTextureLocation(GolemEntity entity)
	{
		return textureFor(entity.getMimicBlock());
	}

	private static ResourceLocation textureFor(BlockState state)
	{
		return TEXTURE_CACHE.computeIfAbsent(state, GolemRenderer::resolveTexture);
	}

	private static ResourceLocation resolveTexture(BlockState state)
	{
		try
		{
			BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
			TextureAtlasSprite sprite = model.getParticleIcon();
			ResourceLocation name = sprite.contents().name();
			return ResourceLocation.fromNamespaceAndPath(name.getNamespace(), "textures/" + name.getPath() + ".png");
		}
		catch(Exception e)
		{
			return FALLBACK;
		}
	}
}
