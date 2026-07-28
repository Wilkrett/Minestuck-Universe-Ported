package org.wilkretawesomesauce.minestuckuniverseported.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.IronGolem;
import org.wilkretawesomesauce.minestuckuniverseported.Minestuckuniverseported;
import org.wilkretawesomesauce.minestuckuniverseported.entity.HopeGolemEntity;

/**
 * Ported from MinestuckUniverse (1.12.2)'s {@code client.render.RenderHopeGolem} - reuses vanilla's own
 * {@link IronGolemRenderer} (model, attack-animation rotation, everything) wholesale rather than
 * rebuilding it, swapping only the texture between the normal and "enraged" skins based on
 * {@link HopeGolemEntity#isAngry()}. Registering an {@code EntityRendererProvider<IronGolem>} for our
 * {@code EntityType<HopeGolemEntity>} works because {@code HopeGolemEntity extends IronGolem} - the
 * registration only requires {@code EntityType<? extends IronGolem>}, matched here directly.
 * <p>
 * Not ported: the original's two extra render layers (a held-flower layer and a decorative "skin"
 * overlay layer) - the texture swap alone already carries the golem's visual identity; the layers were
 * additional flourish on top of it, not core to recognizing the entity.
 */
public class HopeGolemRenderer extends IronGolemRenderer
{
	private static final ResourceLocation TEXTURE = Minestuckuniverseported.id("textures/entity/hope_golem.png");
	private static final ResourceLocation ANGRY_TEXTURE = Minestuckuniverseported.id("textures/entity/hope_golem_enraged.png");

	public HopeGolemRenderer(EntityRendererProvider.Context context)
	{
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(IronGolem entity)
	{
		return entity instanceof HopeGolemEntity golem && golem.isAngry() ? ANGRY_TEXTURE : TEXTURE;
	}
}
