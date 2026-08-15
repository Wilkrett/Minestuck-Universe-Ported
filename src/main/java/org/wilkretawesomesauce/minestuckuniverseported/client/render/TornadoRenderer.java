package org.wilkretawesomesauce.minestuckuniverseported.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.wilkretawesomesauce.minestuckuniverseported.entity.TornadoEntity;

/**
 * Deliberate no-op - {@link TornadoEntity} draws nothing itself, every visual is the
 * server-broadcast {@code MSUParticles#spawnWindWisp} particles its own {@code tick()} spawns via
 * {@code WindEngine#tornado} (see that entity's own doc comment). This class exists only because
 * NeoForge requires a registered {@link EntityRenderer} for every entity type; the texture location
 * it returns is never actually used to draw anything.
 */
public class TornadoRenderer extends EntityRenderer<TornadoEntity>
{
	private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/particle/gust_0.png");

	public TornadoRenderer(EntityRendererProvider.Context context)
	{
		super(context);
	}

	@Override
	public void render(TornadoEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight)
	{
		// Intentionally empty - see class doc comment.
	}

	@Override
	public ResourceLocation getTextureLocation(TornadoEntity entity)
	{
		return TEXTURE;
	}
}
